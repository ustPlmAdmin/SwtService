package com.skyway;

import com.dassault_systemes.enovia.changeaction.factory.ChangeActionFactory;
import com.dassault_systemes.enovia.changeaction.interfaces.IChangeActionServices;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;
import matrix.db.*;

import matrix.util.MatrixException;
import matrix.util.StringList;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.Set;

import static com.matrixone.apps.domain.DomainConstants.*;
import static org.apache.poi.ss.usermodel.CellStyle.*;
/**
 * Бекэнд виджета SWT Console
 * */
//@Path("")
public class Console extends SpecUtils {

    public Context authenticate(HttpServletRequest request) throws IOException {
        Context context = super.authenticate(request);
        String username = context.getSession().getUserName();
        if (username.equals("m.kim") || username.equals("s.beresnev") || username.equals("a.pagoda") || username.equals("a.pavlovich")  || username.equals("admin_platform"))
            return context;
        throw new AuthenticationException();
    }

    Map<String, Object> rec(Context ctx, String objectId) throws FrameworkException {
        List<Map<String, String>> children = select(ctx, objectId, "from[Subclass].to.id", "from[Subclass].to.name");
        if (children.size() > 0) {
            Map<String, Object> childMap = new LinkedHashMap<>();
            for (Map<String, String> child : children) {
                String childId = child.get("from[Subclass].to.id");
                String childName = child.get("from[Subclass].to.name");
                childMap.put(childName, rec(ctx, childId));
            }
            return childMap;
        } else {
            Map<String, Object> materials = new LinkedHashMap<>();
            List<String> items = list(ctx, objectId, "from[Classified Item].to.id");
            for (String id : items){
                materials.put(id, tree(ctx, id, "attribute[*]"));
            }
            return materials;
        }
    }

    @GET
    @Path("/catalog")
    public Response getCatalog(@javax.ws.rs.core.Context HttpServletRequest request,
                               @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            String objectId = findScalar(ctx, "General Library", name, "id");
            Map<String, Object> catalog = rec(ctx, objectId);
            return response(catalog);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/catalogs")
    public Response getCatalogList(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            List<String> names = new ArrayList<>();
            String[] attrs = {"name", "current"};
            List<Map<String, String>> mes = findObjects(ctx, "General Library", "*", attrs);
            for(Map<String, String> m: mes){
                if (m.get("current").equals("Active"))
                    names.add(m.get("name"));
            }
            return response(names);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    @GET
    @Path("/spec_schema")
    public Response getJson(@javax.ws.rs.core.Context HttpServletRequest request,
                            @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            Map<String, Object> items = recProducts(ctx, name, true, "attribute[*]", "current");
            if (items.size() == 0) {
                return page("Object name " + name + " not found", "");
            } else {
                return response(items);
            }
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    public Map<String, String> expand(Context ctx, String id) throws FrameworkException {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            String response = query(ctx, "expand bus " + id + " select bus id;");
            String[] lines = response.split("\n");
            for (int i = 0; i < lines.length / 2; i++) {
                String first = lines[i * 2];
                String tnr = first.substring(first.indexOf(" to ") + 5);
                String revision = tnr.substring(tnr.lastIndexOf(" ") + 1);
                tnr = tnr.substring(0, tnr.lastIndexOf(" ") - 1);
                String name = tnr.substring(tnr.lastIndexOf(" ") + 1);
                String type = tnr.substring(0, tnr.lastIndexOf(" ") - 1);
                tnr = "'" + type + "' " + name + " " + revision;
                String second = lines[i * 2 + 1];
                result.put(tnr, getValue(second));
            }
        } catch (Exception e) {
            error(e);
        }
        return result;
    }

    @GET
    @Path("/ca_delete_rels")
    public Response ca_delete_rels(@javax.ws.rs.core.Context HttpServletRequest request,
                                   @QueryParam("name") String ca_name) {
        try {
            Context ctx = authenticate(request);
            String id = findScalar(ctx, "Change Action", ca_name, "id");
            int deleted = 0;
            Map<String, String> map = expand(ctx, id);
            for (String tnr : map.keySet()) {
                List<String> rels = list(ctx, map.get(tnr), "paths.path.element[0].physicalid");
                for (String physicalId : rels) {
                    try {
                        scalar(ctx, physicalId, "owner");
                    } catch (FrameworkException e) { // phisicalid dosent exist
                        /*try {*/
                        query(ctx, "disconnect bus " + tnr + " from " + id);
                        deleted += 1;
                        /*} catch (Exception ss){
                            ss.printStackTrace();
                        }*/
                    }
                }
            }
            Map<String, Integer> response = new LinkedHashMap<>();
            response.put("deleted", deleted);
            return response(response);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/ca_asm_info")
    public Response ca_asm_info(@javax.ws.rs.core.Context HttpServletRequest request, @QueryParam("name") String root_ca_name) {
        try {

            Context ctx = authenticate(request);

            List<Map<String, String>> oList = findObjectsWhere(ctx, "*", root_ca_name, "attribute[IGAPartEngineering.IGASpecChapter]=='Assemblies'","id","type","name");

            if(! oList.isEmpty()) {
                List<Map<String, String>> objs = findRows(ctx, "*", root_ca_name,
                        "from[VPMInstance].to.id:partId",
                        "from[VPMInstance].to.type:partType",
                        "from[VPMInstance].to.attribute[IGAPartEngineering.IGASpecChapter]:partAsmType",
                        "from[VPMInstance].to.name:partName");

                Workbook book = new HSSFWorkbook();
                Sheet sheet = book.createSheet("Main");
                Map<String, CellStyle> styles = createStyles(book);

                addAsmInfoHeader(sheet,oList,styles.get("headerObjText"));
                addTitles(sheet,styles.get("header"));
                addRowData(sheet,objs,styles.get("data"));

                for (int i = 0; i <= 4; i++) {
                    sheet.autoSizeColumn(i);
                }
                return excel(book,root_ca_name);
            }

            return response("Object not exists or assembly");
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    void addRowData(Sheet sheet, List<Map<String, String>> objs, CellStyle style ){

        int columnNumber = 1;
        int rowNumber = 3;

        for( Map obj : objs){
            Row row = sheet.createRow(rowNumber);
            addCell(row, columnNumber++, obj.get("partId"), null, style);
            addCell(row, columnNumber++, obj.get("partType"), null, style);
            addCell(row, columnNumber++, obj.get("partAsmType"), null, style);
            addCell(row, columnNumber++, obj.get("partName"), null, style);
            columnNumber = 1 ;
            rowNumber = rowNumber + 1;

        }

    }

    void addAsmInfoHeader(Sheet sheet, List<Map<String, String>> data,  CellStyle style){

        Row row = sheet.createRow(0);

        addCell(row, 0, "Assembly", null, style);
        addCell(row, 1,"id : " + data.get(0).get("id") + '\n' +
                        "type : " + data.get(0).get("type") + '\n' +
                        "name : " + data.get(0).get("name"),
                null,  style);

    }

    void addTitles(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(2);
        row.setHeightInPoints(30);

        addCell(row, 1, "Id объекта", null, style);
        addCell(row, 2, "Тип", null, style);
        addCell(row, 3, "Имя", null, style);
        addCell(row, 4, "Сборка", null, style);

    }

    Map<String, CellStyle> createStyles(Workbook book) {
        Map<String, CellStyle> styles = new LinkedHashMap<>();

        CellStyle style = book.createCellStyle();
        Font font = book.createFont();
        font.setFontName("Arial");
        font.setColor(Font.COLOR_RED);
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(font);
        style.setAlignment(ALIGN_CENTER);
        style.setVerticalAlignment(VERTICAL_CENTER);
        style.setWrapText(true);
        styles.put("headerObjText", style);

        CellStyle style_h = book.createCellStyle();
        Font font_h = book.createFont();
        font_h.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style_h.setFont(font_h);
        style_h.setAlignment(ALIGN_CENTER);
        style_h.setVerticalAlignment(VERTICAL_CENTER);
        style_h.setWrapText(true);
        style_h.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style_h.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        style_h.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        style_h.setBorderTop(HSSFCellStyle.BORDER_THIN);
        style_h.setBorderRight(HSSFCellStyle.BORDER_THIN);
        style_h.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        styles.put("header", style_h);

        style = book.createCellStyle();
        font = book.createFont();
        style.setFont(font);
        style.setAlignment(ALIGN_CENTER);
        style.setVerticalAlignment(VERTICAL_CENTER);
        style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        style.setBorderTop(HSSFCellStyle.BORDER_THIN);
        style.setBorderRight(HSSFCellStyle.BORDER_THIN);
        style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        style.setWrapText(true);
        styles.put("data", style);

        return styles;
    }

    Cell addCell(Row row, Integer columnNumber, Object data, String link, CellStyle style) {
        Cell cell = row.createCell(columnNumber);
        if (link != null) {
            Hyperlink href = new HSSFHyperlink(HSSFHyperlink.LINK_URL);
            href.setAddress(link);
            cell.setHyperlink(href);
        }
        if (data instanceof String)
            cell.setCellValue((String) data);
        else if (data instanceof Integer)
            cell.setCellValue((Integer) data);
        else if (data instanceof Double)
            cell.setCellValue((Double) data);
        if (style != null)
            cell.setCellStyle(style);

        return cell;

    }


    @GET
    @Path("/ca_delete_docs")
    public Response ca_delete_docs(@javax.ws.rs.core.Context HttpServletRequest request,
                                   @QueryParam("name") String root_ca_name) {
        try {
            Context ctx = authenticate(request);

            int deleted = 0;

            List<Map<String, String>> archdocs = findRows(ctx, "*", root_ca_name,
                    "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].type:type",
                    "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].physicalid:physicalid");
            for (Map<String, String> archdoc : archdocs)
                if ("ARCHDocument".equals(archdoc.get("type"))) {

                    List<String> ca_names = findObjectsListWhere(ctx, "Change Action", "*",
                            "current == Cancelled && from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].physicalid == " + archdoc.get("physicalid"),
                            "name");


                    for (String ca_name : ca_names) {
                        List<Map<String, String>> paths = findRows(ctx, "*", ca_name,
                                "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].type:type",
                                "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].physicalid:physicalid",
                                "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].pathid:id");
                        for (Map<String, String> path : paths)
                            if ("ARCHDocument".equals(path.get("type")) && archdoc.get("physicalid").equals(path.get("physicalid"))) {
                                query(ctx, "del path " + path.get("id"));
                                deleted += 1;
                            }
                    }
                }

            Map<String, Integer> response = new LinkedHashMap<>();
            response.put("deleted", deleted);
            return response(response);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    /***
     * This script is used to migrate (specialize) assy structure (task #4861)
     */
    @GET
    @Path("/migrate_assy")
    public Response migrate_assy(@javax.ws.rs.core.Context HttpServletRequest request, @QueryParam("name") String name) throws Exception {

        Context context = authenticate(request);
        Map resp = new HashMap();
        // Context context = authWithSession("https://3dspace-m001.sw-tech.by:444/internal/", request.getCookies()[0].getValue(), "m.kim", "ctx::VPLMCreator.SkyWay.Common Space");
        //String[] args = new String [] { "36284.36534.21241.63054"};
        StringList currentSelects = new StringList() {{ add("id"); }};
        MapList mapList =  DomainObject.findObjects(context,
                "VPMReference",   //type
                name,        // name
                null,        // revision
                QUERY_WILDCARD,  // owner
                QUERY_WILDCARD,  // vault
                null,
                null,        // query name
                false,       // expand type
                currentSelects,     // selects
                (short) 0       // object limit
        );
        Map<String,String> map_ = (Map) mapList.get(0);
        String[] args = new String [] { map_.get("id")};

        if (args.length == 0) {
            throw new Exception("Provide root object id as first argument");
        }

        boolean debug = false;
        if (args.length == 2) {
            debug = "true".equalsIgnoreCase(args[1]);
            System.out.println("Debug mode is on. DB won't be impacted, only log will be created");
        }

        String sId = args[0];
        DomainObject assy = DomainObject.newInstance(context, sId);
        StringList objSelects = getAssySelectable();
        Map mAssy = assy.getInfo(context, objSelects);
        MapList mlStructure = new MapList();
        mlStructure.add(mAssy);
        mlStructure.addAll(getStructure(context, sId, objSelects));
        Set<String> sAllIDs = new HashSet<>();
        for (Object obj : mlStructure) {
            Map map = (Map) obj;
            String sPID = (String) map.get("physicalid");
            sAllIDs.add(sPID);
        }

        try {
            ContextUtil.startTransaction(context, true);

            long timeStart = System.currentTimeMillis();
            Set<String> sUniqueIDs = new HashSet<>();
            for (Object obj : mlStructure) {
                Map map = (Map) obj;
                MapList mlRevs = geAllRevs(context, map, objSelects);

                for (Object o : mlRevs) {
                    Map r_ = updateProduct(context, (Map) o, sUniqueIDs, sAllIDs, debug);
                    resp.putAll(r_);
                }
            }

            long time = System.currentTimeMillis() - timeStart;
            System.out.println("All products successfully processed and it takes: " + time + " ms");
            ContextUtil.commitTransaction(context);
            return response(resp);
        } catch (Exception e) {
            ContextUtil.abortTransaction(context);
            return error(e);
        } finally {
            finish(request);
        }
    }

    private Map updateProduct(Context context, Map m, Set<String> sUniqueIDs, Set<String> sAllIDs, boolean debug) {
        String sPID = (String) m.get("physicalid");
        String sType = (String) m.get(SELECT_TYPE);
        String sName = (String) m.get(SELECT_NAME);
        String sSpecChapter = (String) m.get("attribute[IGAPartEngineering.IGASpecChapter]");
        String sUsage = (String) m.get("attribute[PLMEntity.V_usage]");
        HashMap<String,String> res = new HashMap();
        boolean isChanged = false;
        boolean isFlexible = isFlexible(m);

        if (!sUniqueIDs.contains(sPID)) {
            StringList slActions = new StringList();
            String sNewType = "";
            boolean findSameVName = false;
            String[] extNames = new String[]{};

            if ("VPMReference".equals(sType)) {
                switch (sSpecChapter) {
                    case "Assemblies": {
                        if ("".equals(sUsage)) {
                            findSameVName = true;
                            if (!isFlexible)
                                sNewType = "Kit_Product";
                        } else
                            slActions.add(String.format("Usage is '%s'", sUsage));
                        break;
                    }
                    case "Parts": {
                        if ("3DPart".equals(sUsage)) {
                            findSameVName = true;
                            if (!isFlexible) {
                                sNewType = "Kit_Part";
                                extNames = new String[]{"Kit_MatExt", "Kit_MatNExt"};
                            }
                        } else
                            slActions.add(String.format("Usage is '%s'", sUsage));
                        break;
                    }
/*                    case RANGE_STANDARD_COMPONENTS: {
                        sNewType = TYPE_KIT_STANDARD_PRODUCT;
                        needExt = true;
                        break;
                    }
                    case RANGE_OTHER_COMPONENTS: {
                        sNewType = TYPE_KIT_OEM_PRODUCT;
                        needExt = true;
                        break;
                    }
                    case RANGE_MATERIALS: {
                        sNewType = TYPE_KIT_MATERIAL;
                        needExt = true;
                        break;
                    }*/
                }
            } else if ("Structure_Member".equals(sType) || "Structure_Plate".equals(sType)) {
                if ("Parts".equals(sSpecChapter))
                    extNames = new String[]{"Kit_MatExt", "Kit_MatNExt"};
                else
                    slActions.add(String.format("Chapter is '%s'", sSpecChapter));
            } else if ("Drawing".equals(sType)) {
                findSameVName = true;
                if (!isFlexible)
                    sNewType = "Kit_Drawing";
            } else
                slActions.add(String.format("Type is '%s'", sType));

            //change BO type if needed
            if (!"".equals(sNewType)) {
               try {
                   slActions.add(modType(context, sPID, sNewType, debug));
               } catch (Exception ex) {
                   res.put(sName, ex.getMessage());
               } finally {
                   isChanged = true;
               }
            }

            //change BO type if needed
            if (findSameVName)
            {
                try {
                    slActions.add(modDuplicates(context, m, sAllIDs, debug));
                } catch (Exception ex){
                    res.put(sName,ex.getMessage());
                } finally {
                    isChanged = true;
                }
            }

            //add interfaces if needed
            if (extNames.length > 0)
            {
                try {
                   slActions.add(addExtensions(context, sPID, extNames, debug));
                } catch (Exception ex){
                    res.put(sName,ex.getMessage());
                } finally {
                    isChanged = true;
                }
            }
            //log all found types in assy VPMReference, Drawing and others
            //logBO(m, String.join(ACTIONS_DELIMITER, slActions));

            //log mod bus at first then log mod SR
            if (!"".equals(sNewType))
            {
                try {
                    updateSemanticRelations(context, sPID, sType, sNewType, debug);
                } catch (Exception ex){
                    res.put(sName,ex.getMessage());
                } finally {
                    isChanged = true;
                }
            }

            if (!isChanged && !Arrays.asList("Kit_MatExt", "Kit_MatNExt", "Kit_Drawing","Kit_Product").contains(sType)) {
                res.put(sName," not migrated");
            }
            //no need to process one and the same object several times in any case
            sUniqueIDs.add(sPID);
        }

        return res;
    }


    private String addExtensions(Context context, String sPID, String[] extNames, boolean debug) throws MatrixException {
        for (String extName : extNames)
            execWithDebug(context, String.format("mod bus %s add interface '%s'", sPID, extName), debug);

        return String.format("Interfaces '%s' were added", String.join(";", extNames));
    }

    private String modDuplicates(Context context, Map m, Set<String> sAllIDs, boolean debug) throws MatrixException {
        String logicalID = (String) m.get("logicalid");
        String type = (String) m.get(SELECT_TYPE);
        String title = (String) m.get("attribute[PLMEntity.V_Name]");
        StringList slActions = new StringList();
        if (UIUtil.isNotNullAndNotEmpty(title)) {
            String where = String.format("%s=='%s'&&%s!=%s", "attribute[PLMEntity.V_Name]", title, "logicalid", logicalID);
            MapList ml = DomainObject.findObjects(context, type, "vplm", where, getAssySelectable());

            String sNewTitle = String.format("Dup_%s_%s", System.currentTimeMillis(), title);
            for (Object o : ml) {
                Map mp = (Map) o;
                modDuplicate(context, mp, sAllIDs, sNewTitle, debug);
            }
        } else
            slActions.add("Title is empty");

        return String.join(";", slActions);
    }

    private String modDuplicate(Context context, Map m, Set<String> sAllIDs, String sNewTitle, boolean debug) throws MatrixException {

        final List<String> typesToExclude = new ArrayList<>(Arrays.asList(
                "PPRContext",
                "Electrical3DSystem",
                "ElectricalGeometry"
        ));

        String sAction = "";
        String sPID = (String) m.get("physicalid");
        String sName = (String) m.get(SELECT_NAME);
        String sType = (String) m.get(SELECT_TYPE);
        boolean isFlex = isFlexible(m);
        boolean isChild = sAllIDs.contains(sPID);
        boolean isExcluded = typesToExclude.contains(sType);

        if (!isExcluded) {
            if (isChild)
                sAction = String.format("Duplicated Title (name=%s, child) hasn't been changed", sName);

            if (isChild && isFlex)
                sAction = String.format("Duplicated Title (name=%s, flex and child) hasn't been changed", sName);

            if(!isChild) {
                execWithDebug(context, String.format( "mod bus %s '%s' '%s'", sPID, "PLMEntity.V_Name", sNewTitle), debug);
                if(isFlex)
                    sAction = String.format("Duplicated Title (name=%s, flex) was replaced by '%s'", sName, sNewTitle);
                else
                    sAction = String.format("Duplicated Title (name=%s) was replaced by '%s'", sName, sNewTitle);
            }
        } else
            sAction = String.format("Objects of '%s' type are excluded from duplicating algorithm", sType);

        return sAction;
    }

    //return boolean and put string val into the map for log
    private boolean isFlexible(Map m) {
        String sType = (String) m.get(SELECT_TYPE);
        String sFlexIGA = (String) m.get("attribute[IGAPartEngineering.IGATFlexible]");
        String sFlexSW = (String) m.get("attribute[IGAPartEngineering.IGAIGAFlexible]");
        boolean isFlexible = "True".equalsIgnoreCase(sFlexIGA) || "True".equalsIgnoreCase(sFlexSW);

        if ("Drawing".equals(sType)) {
            List<String> lDrwFlexIGA = normalizeToList(m.get(String.format("to[%s].from.%s", "VPMRepInstance", "attribute[IGAPartEngineering.IGATFlexible]")));
            List<String> lDrwFlexSW = normalizeToList(m.get(String.format("to[%s].from.%s", "VPMRepInstance", "attribute[IGAPartEngineering.IGAIGAFlexible]")));
            isFlexible = lDrwFlexIGA.contains("TRUE") || lDrwFlexSW.contains("TRUE");
        }

        //for log
        m.put("isFlexible", String.valueOf(isFlexible));

        return isFlexible;
    }

    public static List<String> normalizeToList(Object obj) {

        final String DELIMITER_BELL = "\u0007";

        List<String> list = new ArrayList<>();

        // 1) DomainObject.findObjects() return String with "BELL" char as delimiter: "id1id2..."

        if (obj instanceof String) {
            String str = (String) obj;
            if (str.contains(DELIMITER_BELL))
                list = Arrays.asList(str.split(DELIMITER_BELL));
            else
                list.add(str);
        } else if (obj instanceof StringList) {
            StringList sl = (StringList) obj;
            list = sl.toList();
        }

        return list;
    }


    private String modType(Context context, String sPID, String sNewType, boolean debug) throws MatrixException {
        execWithDebug(context, String.format("mod bus %s type '%s'", sPID, sNewType), debug);
        return String.format("Type was replaced by %s", sNewType);
    }

    private void execWithDebug(Context context, String cmd, boolean debug) throws MatrixException {
        if (!debug)
            MQLCommand.exec(context, cmd);
    }

    private MapList geAllRevs(Context context, Map map, StringList objSelects) throws Exception {
        //found object's map itself
        MapList mlRevs = new MapList();
        mlRevs.add(map);
        //all its revs if exist
        String sRefPID = (String) map.get("physicalid");
        DomainObject domObj = DomainObject.newInstance(context, sRefPID);
        mlRevs.addAll(domObj.getRevisionsInfo(context, objSelects, new StringList()));

        return mlRevs;
    }

    private MapList getStructure(Context context, String sId, StringList objSelects) throws Exception {
        DomainObject assy = DomainObject.newInstance(context, sId);

        // getRelatedObjects parameters
        String relPattern = String.format("%s,%s", "VPMInstance", "VPMRepInstance");
        String typePattern = String.format("%s,%s", "VPMReference", "Drawing");
        StringList relSelects = new StringList(SELECT_RELATIONSHIP_ID);
        boolean getTo = false;
        boolean getFrom = true;
        short recurseToLevel = 0;
        String objectWhere = "";
        String relWhere = "";
        int limit = 0;

        return assy.getRelatedObjects(context, relPattern, typePattern, objSelects, relSelects, getTo, getFrom, recurseToLevel, objectWhere, relWhere, limit);
    }

    private StringList getAssySelectable() {
        StringList objSelects = new StringList();
        objSelects.add("physicalid");
        objSelects.add("logicalid");
        objSelects.add(SELECT_TYPE);
        objSelects.add(SELECT_NAME);
        objSelects.add(SELECT_REVISION);
        objSelects.add(SELECT_CURRENT);
        objSelects.add(SELECT_OWNER);
        objSelects.add(SELECT_PROJECT);
        objSelects.add("attribute[IGAPartEngineering.IGASpecChapter]");
        objSelects.add("attribute[PLMEntity.V_usage]");
        objSelects.add("attribute[PLMEntity.V_Name]");
        objSelects.add("attribute[IGAPartEngineering.IGATFlexible]");
        objSelects.add("attribute[IGAPartEngineering.IGAIGAFlexible]");
        objSelects.add(String.format("to[%s].from.%s", "VPMRepInstance", "attribute[IGAPartEngineering.IGATFlexible]"));
        objSelects.add(String.format("to[%s].from.%s", "VPMRepInstance", "attribute[IGAPartEngineering.IGAIGAFlexible]"));
        return objSelects;
    }


    /***Перевод VPMReference в Kit_Product или Kit_Part ***/
    @GET
    @Path("/migrate_vpref")
    public Response migrate_vref(@javax.ws.rs.core.Context HttpServletRequest request,
                                  @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
           // Context ctx = authWithSession("https://3dspace-m001.sw-tech.by:444/3dspace/", request.getCookies()[0].getValue(), "m.kim", "ctx::VPLMCreator.SkyWay.Common Space");
            StringList currentSelects = new StringList() {{
                add("physicalid");
                add(SELECT_TYPE);
                add(SELECT_NAME);
                add(SELECT_REVISION);
                add("attribute[IGAPartEngineering.IGASpecChapter]");
                add("attribute[PLMEntity.V_usage]");
                add("attribute[PLMEntity.V_Name]");
            }};

            MapList mapList =  DomainObject.findObjects(ctx,
                    "VPMReference",   //type
                    name,        // name
                    null,        // revision
                    QUERY_WILDCARD,  // owner
                    QUERY_WILDCARD,  // vault
                    null,
                    null,        // query name
                    false,       // expand type
                    currentSelects,     // selects
                    (short) 0       // object limit
            );

            Map<String,String> map = (Map) mapList.get(0);

          //  Map<String,String> map = findObject(ctx,"VPMReference", name,"physicalid","attribute[PLMEntity.V_Name]","attribute[IGAPartEngineering.IGASpecChapter]","attribute[PLMEntity.V_usage]");

            String sRefPID = (String) map.get("physicalid");
            String specChapter = (String) map.get("attribute[IGAPartEngineering.IGASpecChapter]");
            String v_usage = (String) map.get("attribute[PLMEntity.V_usage]");

            String sNewType = "";
            if ("3DPart".equals(v_usage) && "Parts".equals(specChapter))
                sNewType = "Kit_Part";
            if (UIUtil.isNullOrEmpty(v_usage) && "Assemblies".equals(specChapter))
                sNewType = "Kit_Product";
            if ("".equals(sNewType)) { throw new Exception("Object is not VPMReference type or attributes IGASpecChapter and V_usage are empty");}

            MapList mlRevs = new MapList();
            mlRevs.add(map);
            DomainObject domObj = DomainObject.newInstance(ctx, sRefPID);
            mlRevs.addAll(domObj.getRevisionsInfo(ctx, currentSelects, new StringList()));
            for (Object o : mlRevs) {
                Map m = (Map) o;
                String sPID = (String) m.get("physicalid");
                String sType = (String) m.get(SELECT_TYPE);
                MQLCommand.exec(ctx, String.format("mod bus %s type '%s'", sPID, sNewType));
                updateSemanticRelations(ctx, sPID, sType, sNewType, false);
            }
           return response(sRefPID + " migarte successfully");
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    private void updateSemanticRelations(Context context, String pid, String oldType, String newType, boolean debug) throws Exception {
        StringList pathSelects = new StringList();
        pathSelects.add("id");
        pathSelects.add("element.type");
        pathSelects.add("element.physicalid");
        PathQuery pQuery = new PathQuery();
        pQuery.setPathType("SemanticRelation");
        pQuery.setVaultPattern("vplm");
        PathQuery.QueryKind qKind = PathQuery.CONTAINS;
        ArrayList alCriteria = new ArrayList();
        alCriteria.add(pid);
        pQuery.setCriterion(qKind, alCriteria);
        short sPageSize = 10;
        PathQueryIterator pqItr = pQuery.getIterator(context, pathSelects, sPageSize, new StringList());

        ArrayList<String> alCmds = new ArrayList<>();
        while (pqItr.hasNext()) {
            PathWithSelect pathInfo = pqItr.next();
            String sPathId = pathInfo.getSelectData("id");
            StringList slTypes = pathInfo.getSelectDataList("element.type");
            StringList slPIDs = pathInfo.getSelectDataList("element.physicalid");

            for (int i = 0; i < slTypes.size(); i++) {
                String sType = slTypes.get(i);
                String sPid = slPIDs.get(i);
                if (sType.equals(oldType) && pid.equals(sPid)) {
                    alCmds.add(String.format("modify path %s element %s type '%s'", sPathId, Integer.toString(i), newType));
                }
            }
        }
        pqItr.close();
        //because pqItr must be closed at first then you can mod objects otherwise #1400004: Object is not open for update
        if (!debug)
            for (String cmd : alCmds)
                MQLCommand.exec(context, cmd);
    }

    /***Удаление проекта по его id. Обратить внимание на права***/
    @GET
    @Path("/delete_obj")
    public Response rq_delete_obj(@javax.ws.rs.core.Context HttpServletRequest request,
                               @QueryParam("objId") String objectId) {
        try {
            Context ctx = authenticate(request);
            delItr(ctx,objectId);
            return response(objectId + " deleted successfully");
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    private void delItr(Context ctx, String objectId) {

        try {
            List<Map<String,String>> objs = select(ctx, objectId,"from[Subtask].to.id:id");
              for( Map obj : objs){
                  delItr(ctx, obj.get("id").toString());
              }
            ContextUtil.startTransaction(ctx, true);
            delObj(ctx,objectId);
            if(ContextUtil.isTransactionActive(ctx)) {
                ContextUtil.commitTransaction(ctx);
            }
        } catch (Exception e) {
             ContextUtil.abortTransaction(ctx);
            throw new RuntimeException(e);
        }
    }

    private void delObj(Context ctx, String objectId) throws Exception {

        String notify = "";
        try {
            List<String> curList = list(ctx,objectId,"current");
            if (!curList.isEmpty() && curList.get(0).equals("Complete")) {
              notify = String.format("mod bus %s current %s ", objectId, "Create") ;
              query(ctx, String.format("mod bus %s current %s ", objectId, "Create"));
            }
            notify = String.format("mod bus %s owner %s ", objectId, ctx.getSession().getUserName());
            query(ctx, String.format("mod bus %s owner %s ", objectId, ctx.getSession().getUserName()));
            notify = String.format("del bus %s ", objectId);
            query(ctx, "del bus " + objectId);
            System.out.println("Object deleted " + objectId );
        } catch (FrameworkException e) {

            throw new Exception(notify);
        }

    }


    void step(Context ctx, String base, Map<String, Context> ownersContexts, String objectId, String mode) throws Exception {

        Map<String, String> object = row(ctx, objectId,
                "physicalid",
                "type",
                "current",
                "name",
                "owner",
                "project",
                "reserved",
                "reservedby",
                "attribute[PLMEntity.V_description]:desc");

        if (mode.equals("prepare") && !object.get("desc").contains("@")
                || mode.equals("restore") && object.get("desc").contains("@")) {

            boolean isBlockedByChangeControl = list(ctx, objectId, "interface").contains("Change Control");

            DomainObject subObject = DomainObject.newInstance(ctx, objectId);

            try {

                if (object.get("reserved").equals("TRUE") && !object.get("reservedby").equals("m.kim")) {
                    /*Context reservedContext = ownersContexts.get(object.get("reservedby"));
                    if (reservedContext == null) {
                        List<String> accesses = findList(ctx, "Person", object.get("reservedby"), "from[Assigned Security Context].to");
                        List<String> accessList = new ArrayList<>();
                        accessList.add("VPLMViewer");
                        accessList.add("VPLMCreator");
                        accessList.add("VPLMProjectAdministrator");
                        accessList.add("VPLMProjectLeader");
                        accessList.add("VPLMAdmin");
                        String bigAccess = null;
                        for (String access : accessList) {
                            String accessName = access + ".SkyWay." + object.get("project");
                            if (accesses.contains(accessName))
                                bigAccess = accessName;
                        }

                        if (bigAccess == null)
                            throw new NullPointerException();

                        reservedContext = internalAuth(base, object.get("reservedby"), "ctx::" + bigAccess);
                        ownersContexts.put(object.get("reservedby"), reservedContext);
                    }*/
                    subObject.unreserve(ctx);
                    subObject.reserve(ctx, "123");
                }

                if (isBlockedByChangeControl) {
                    List<String> pids = new ArrayList<>();
                    pids.add(object.get("physicalid"));
                    IChangeActionServices iChangeActionServices = ChangeActionFactory.CreateChangeActionFactory();
                    ContextUtil.startTransaction(ctx, true);
                    iChangeActionServices.unsetChangeControlFromPidList(ctx, pids);
                    ContextUtil.commitTransaction(ctx);
                }

                if (!object.get("current").equals("IN_WORK"))
                    query(ctx, "mod bus " + objectId + " current IN_WORK");

                query(ctx, "mod bus " + objectId + " owner m.kim");

                try {
                    if (mode.equals("prepare"))
                        query(ctx, "mod bus " + objectId + " PLMEntity.V_description \"" + object.get("desc") + "@" + object.get("name") + "\"");
                    if (mode.equals("restore")){
                        String resDesc = object.get("desc").substring(0, object.get("desc").indexOf("@"));
                        query(ctx, "mod bus " + objectId + " PLMEntity.V_description \"" + resDesc + "\"");
                    }
                } catch (Exception e) {
                    throw new NullPointerException();
                }


            } finally {

                if (!object.get("current").equals("IN_WORK"))
                    query(ctx, "mod bus " + objectId + " current " + object.get("current"));

                query(ctx, "mod bus " + objectId + " owner " + object.get("owner"));

                if (isBlockedByChangeControl) {
                    List<String> pids = new ArrayList<>();
                    pids.add(object.get("physicalid"));
                    IChangeActionServices iChangeActionServices = ChangeActionFactory.CreateChangeActionFactory();
                    ContextUtil.startTransaction(ctx, true);
                    iChangeActionServices.setChangeControlFromPidList(ctx, pids);
                    ContextUtil.commitTransaction(ctx);
                }

                if (object.get("reserved").equals("TRUE") && !object.get("reservedby").equals("m.kim")) {
                    subObject.unreserve(ctx);
                    /*Context reservedContext = ownersContexts.get(object.get("reservedby"));
                    subObject.reserve(reservedContext, "123");*/
                }
            }
        }

        List<String> childrenIds = list(ctx, objectId, "from[VPMInstance].to.id");
        for (String childId : childrenIds)
            step(ctx, base, ownersContexts, childId, mode);
    }


    @GET
    @Path("/step_prepare")
    public Response step_prepare_request(@javax.ws.rs.core.Context HttpServletRequest request,
                                         @QueryParam("name") String prd_name) {
        try {
            Context ctx = internalAuth(request);
            String base = getBaseUrl(request);
            String prdId = findScalar(ctx, "*", prd_name, "id");
            Map<String, Context> ownersContexts = new HashMap<>();
            step(ctx, base, ownersContexts, prdId, "prepare");
            Object tree = recProducts(ctx, prd_name, false, "id", "name", "attribute[PLMEntity.V_description]", "owner");
            return response(tree);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    @GET
    @Path("/")
    public Response step_restore_request(@javax.ws.rs.core.Context HttpServletRequest request,
                                         @QueryParam("name") String prd_name) {
        try {
            Context ctx = internalAuth(request);
            String base = getBaseUrl(request);
            String prdId = findScalar(ctx, "*", prd_name, "id");
            Map<String, Context> ownersContexts = new HashMap<>();
            step(ctx, base, ownersContexts, prdId, "restore");
            Object tree = recProducts(ctx, prd_name, false, "id", "name", "attribute[PLMEntity.V_description]", "owner");
            return response(tree);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/sign_pdf")
    public Response test(@javax.ws.rs.core.Context HttpServletRequest request,
                         @QueryParam("name") String ca) {
        try {
            Context ctx = internalAuth(request);
            String temp = findScalar(ctx, "*", "ERP Connect Order", "id");
            System.out.println(temp);
            new CAEChangeActionApprove_mxJPO().signAllDocumentsInCA(ctx, findScalar(ctx, "*", ca, "physicalid"));
            return response("OK");
        } catch (Exception e) {
            e.printStackTrace();
            return error(e);
        } finally {
            finish(request);
        }

    }

    @GET
    @Path("/fill_unit")
    public Response fillUnit(@javax.ws.rs.core.Context HttpServletRequest request,
                                @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            String objectId = findScalar(ctx, "General Library", name, "id");
            Map<String, Object> catalog = fillUnit(ctx, objectId);
            return response(catalog);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/library")
    public Response fillCatalog(@javax.ws.rs.core.Context HttpServletRequest request,
                                @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            String objectId = findScalar(ctx, "General Library", name, "id");
            Map<String, Object> catalog = fillUnitCode(ctx, objectId);
            return response(catalog);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    Map<String, Object> fillUnit(Context ctx, String objectId) throws FrameworkException {
        List<Map<String, String>> children = select(ctx, objectId, "from[Subclass].to.id", "from[Subclass].to.name");
        if (children.size() > 0) {
            Map<String, Object> childMap = new LinkedHashMap<>();
            for (Map<String, String> child : children) {
                String childId = child.get("from[Subclass].to.id");
                String childName = child.get("from[Subclass].to.name");
                childMap.put(childName, fillUnit(ctx, childId));
            }
            return childMap;
        } else {
            Map<String, Object> materials = new LinkedHashMap<>();
            List<String> items = list(ctx, objectId, "from[Classified Item].to.id");
            for (String id : items) {
                query(ctx, "mod bus \"" + id + "\" \"Kit_UnitExt.Kit_Unit\" \"piece\"");
            }
            return materials;
        }
    }

    Map<String, Object> fillUnitCode(Context ctx, String objectId) throws FrameworkException {
        List<Map<String, String>> children = select(ctx, objectId, "from[Subclass].to.id", "from[Subclass].to.name");
        if (children.size() > 0) {
            Map<String, Object> childMap = new LinkedHashMap<>();
            for (Map<String, String> child : children) {
                String childId = child.get("from[Subclass].to.id");
                String childName = child.get("from[Subclass].to.name");
                childMap.put(childName, fillUnitCode(ctx, childId));
            }
            return childMap;
        } else {
            Map<String, Object> materials = new LinkedHashMap<>();
            List<String> items = list(ctx, objectId, "from[Classified Item].to.id");
            for (String id : items){
                String unit = row(ctx, id, "attribute[Kit_UnitExt.Kit_Unit]").get("attribute[Kit_UnitExt.Kit_Unit]");
                String code = "";
                switch (unit) {
                    case "piece":
                        code = "796";
                        break;
                    case "mm":
                        code = "003";
                        break;
                    case "m2":
                        code = "055";
                        break;
                    case "m3":
                        code = "113";
                        break;
                    case "g":
                        code = "163";
                        break;
                    case "mg":
                        code = "161";
                        break;
                    case "l":
                        code = "112";
                        break;
                    case "kg":
                        code = "166";
                        break;
                    case "m":
                        code = "006";
                        break;
                    case "ml":
                        code = "111";
                        break;
                    default:
                        break;
                }
                if (!code.equals(""))
                query(ctx, "mod bus \"" + id + "\" \"Kit_UnitExt.Kit_UnitCode\" " + code);
            }
            return materials;
        }
    }
}
