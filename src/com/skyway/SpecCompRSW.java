package com.skyway;

import com.iga.cae.CAUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import matrix.db.Context;
import matrix.util.MatrixException;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;

import static org.apache.poi.ss.usermodel.CellStyle.ALIGN_CENTER;
import static org.apache.poi.ss.usermodel.CellStyle.VERTICAL_CENTER;

/**
 * Бекенд для виджета SWT SpecComp
 */
public class SpecCompRSW extends SpecUtils {

    void removeNotPKI(Map<String, Object> root, Map<String, Map<String, Object>> result) {
        if (root.get("spec_type").equals("Buy") || root.get("spec_type").equals("OtherComponents") || root.get("spec_type").equals("StandardComponents")) {
            String name = (String) root.get("name");
            Map<String, Object> kpi = result.get(name);
            System.out.println("OK");
            if (kpi == null) {
                result.put(name, root);
            } else {
                kpi.put("quantity", (Integer) kpi.get("quantity") + (Integer) root.get("quantity"));
                kpi.put("parent", kpi.get("parent") + "," + root.get("name"));
                result.put(name, kpi);
            }
        }

        if (root.get("children") instanceof List)
            for (Map<String, Object> child : (List<Map<String, Object>>) root.get("children"))
                removeNotPKI(child, result);
    }

    void removeNotStandard(Map<String, Object> root, Map<String, Map<String, Object>> result){
        if (root.get("spec_type").equals("StandardComponents")) {
            String name = (String) root.get("name");
            Map<String, Object> kpi = result.get(name);
            System.out.println("OK");
            if (kpi == null) {
                result.put(name, root);
            } else {
                kpi.put("quantity", (Integer) kpi.get("quantity") + (Integer) root.get("quantity"));
                kpi.put("parent", kpi.get("parent") + "," + root.get("name"));
                result.put(name, kpi);
            }
        }

        if (root.get("children") instanceof List)
            for (Map<String, Object> child : (List<Map<String, Object>>) root.get("children"))
                removeNotStandard(child, result);
    }

    enum COLS {
        LEVEL,
        CODE,
//        NAME,
//        REVISION,
//        TITLE,
//        STATUS,
        QUANTITY,
        MEASURE
//        OWNER,
//        PARENT,
//        MASS,
//        MASS_SUM,
//        MATERIAL,
//        PDF,
//        CA,
    }


    @GET
    @Path("/spec_comp_prepare")
    public Response getXlsPrepare(@javax.ws.rs.core.Context HttpServletRequest request,
                                  @QueryParam("name") String name,
                                  @QueryParam("mode") String mode) {
        try {
            Context ctx = internalAuth(request);
            Map<String, Object> tree = recProductNameTree(ctx, name);
            FileUtils.writeByteArrayToFile(new File("spec_comp.json"), json.toJson(tree).getBytes());
            return Response.ok("success").build();
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/spec_comp_finish")
    public Response getXlsFinish(@javax.ws.rs.core.Context HttpServletRequest request,
                                 @QueryParam("name") String name,
                                 @QueryParam("mode") String mode) {
        try {
            Context ctx = internalAuth(request);

            Map<String, Object> tree = json.fromJson(FileUtils.readFileToString(new File("spec_comp.json"),  Charset.defaultCharset()), Map.class);

            return getXls(request, ctx, name, mode, tree);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/spec_schema_xls_comp_rsw")
    public Response getXls(@javax.ws.rs.core.Context HttpServletRequest request,
                           @QueryParam("name") String name,
                           @QueryParam("mode") String mode) {

        try {
            Context ctx = internalAuth(request);
            Map<String, Object> tree = recProductNameTree(ctx, name);
            return getXls(request, ctx, name, mode, tree);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    public Response getXls(HttpServletRequest request, Context ctx, String name, String mode, Map<String, Object> tree) throws MatrixException, IOException {

        if (mode == null)
            mode = "all";
        if (tree == null) {
            return page("Object name " + name + " not found", "");
        } else {

//            if (mode.equals("pki")) {
//                Map<String, Map<String, Object>> result = new HashMap<>();
//                mulChildrenQuantity(tree, 1);
//                removeNotPKI(tree, result);
//                List<Map<String, Object>> children = new ArrayList<>();
//                tree.put("children", children);
//                for (Map<String, Object> item : result.values())
//                    item.remove("children");
//                List<String> keys = new ArrayList<>(result.keySet());
//                List<String> sortSeq = new ArrayList<>();
//                sortSeq.add("Buy");
//                sortSeq.add("OtherComponents");
//                sortSeq.add("StandardComponents");
//                keys.sort((o1, o2) -> {
//                    Integer index1 = sortSeq.indexOf(result.get(o1).get("spec_type"));
//                    Integer index2 = sortSeq.indexOf(result.get(o2).get("spec_type"));
//                    return index1.compareTo(index2);
//                });
//                for (String key: keys)
//                    children.add(result.get(key));
//            }
//
//            if (mode.equals("sprd")){
//                Map<String, Map<String, Object>> result = new HashMap<>();
//                mulChildrenQuantity(tree, 1);
//                removeNotStandard(tree, result);
//                List<Map<String, Object>> children = new ArrayList<>();
//                tree.put("children", children);
//                for (Map<String, Object> item : result.values())
//                    item.remove("children");
//                List<String> keys = new ArrayList<>(result.keySet());
//                List<String> sortSeq = new ArrayList<>();
////                sortSeq.add("Buy");
////                sortSeq.add("OtherComponents");
//                sortSeq.add("StandardComponents");
//                keys.sort((o1, o2) -> {
//                    Integer index1 = sortSeq.indexOf(result.get(o1).get("spec_type"));
//                    Integer index2 = sortSeq.indexOf(result.get(o2).get("spec_type"));
//                    return index1.compareTo(index2);
//                });
//                for (String key: keys)
//                    children.add(result.get(key));
//            }

            Workbook book = new HSSFWorkbook();
            Sheet sheet = book.createSheet("Main");
            Map<String, CellStyle> styles = createStyles(book);
            addRow(request, ctx, sheet, tree, 0, mode, 1, styles);
            addTitles(sheet, styles);
            return excel(book, name);
        }

    }

    // sheet styles

    Map<String, CellStyle> createStyles(Workbook book) {
        Map<String, CellStyle> styles = new LinkedHashMap<>();

        CellStyle style = book.createCellStyle();
        Font font = book.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(font);
        style.setAlignment(ALIGN_CENTER);
        style.setVerticalAlignment(VERTICAL_CENTER);
        style.setWrapText(true);
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        style.setBorderTop(HSSFCellStyle.BORDER_THIN);
        style.setBorderRight(HSSFCellStyle.BORDER_THIN);
        style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        styles.put("header", style);

        style = book.createCellStyle();
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.ORANGE.index);
        styles.put("level0", style);

        style = book.createCellStyle();
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.index);
        styles.put("level1", style);

        style = book.createCellStyle();
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GOLD.index);
        styles.put("level2", style);

        style = book.createCellStyle();
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.YELLOW.index);
        styles.put("level3", style);

        style = book.createCellStyle();
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.WHITE.index);
        styles.put("white", style);

        style = book.createCellStyle();
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.index);
        styles.put("parts", style);

        style = book.createCellStyle();
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.index);
        styles.put("standards", style);

        style = book.createCellStyle();
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        styles.put("drw", style);

        return styles;
    }

    // columns titles

    void addTitles(Sheet sheet, Map<String, CellStyle> styles) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(30);

        addCell(row, COLS.LEVEL.ordinal(), "Уровень", styles.get("header"));
        addCell(row, COLS.CODE.ordinal(), "Номенклатура, ТМЦ (по КД)", styles.get("header"));
//        addCell(row, COLS.NAME.ordinal(), "Name", styles.get("header"));
//        addCell(row, COLS.REVISION.ordinal(), "Ревизия", styles.get("header"));
//        addCell(row, COLS.TITLE.ordinal(), "Наименование", styles.get("header"));
//        addCell(row, COLS.STATUS.ordinal(), "Состояние", styles.get("header"));
        addCell(row, COLS.QUANTITY.ordinal(), "Кол-во", styles.get("header"));
        addCell(row, COLS.MEASURE.ordinal(), "Ед. изм.", styles.get("header"));
//        addCell(row, COLS.OWNER.ordinal(), "Владелец", styles.get("header"));
//        addCell(row, COLS.PARENT.ordinal(), "Родитель", styles.get("header"));
//        addCell(row, COLS.MASS.ordinal(), "Масса", styles.get("header"));
//        addCell(row, COLS.MASS_SUM.ordinal(), "Общая масса", styles.get("header"));
//        addCell(row, COLS.MATERIAL.ordinal(), "Материал", styles.get("header"));
//        addCell(row, COLS.PDF.ordinal(), "PDF", styles.get("header"));
//        addCell(row, COLS.CA.ordinal(), "Change Action", styles.get("header"));

        for (int i = 0; i < COLS.values().length; i++)
            sheet.autoSizeColumn(i);
    }

    class AddRowResult {
        Integer lineNumber = 0;

        public AddRowResult() {
        }

        public AddRowResult(Integer lineNumber, Double mass_sum) {
            this.lineNumber = lineNumber;
        }
    }

    AddRowResult addRow(HttpServletRequest request, Context ctx, Sheet sheet, Map<String, Object> item, Integer level,
                        String mode, Integer lineNumber, Map<String, CellStyle> styles) throws MatrixException {
        Double mass_sum = 0d;
        if(level < 2) {
            if (item == null)
                return null;
            Map<String, String> obj = findObject(ctx, "*", (String) item.get("name"), "attribute[PLMEntity.V_Name]:title",
                    "attribute[PLMEntity.V_description]:description",
                    "attribute[IGAPartEngineering.IGASpecChapter]:spec_type",
                    "name"
                    );
            obj.putIfAbsent("spec_type", "");

            boolean visible = true;

//            Double mass;
//            if (Double.parseDouble("0" + obj.get("mass_declared")) != 0d)
//                mass = Double.parseDouble("0" + obj.get("mass_declared"));
//            else
//                mass = Double.parseDouble("0" + obj.get("mass_computed"));


//            try {
//                mass_sum = mass * (int) item.get("quantity");
//            } catch (Exception e) {
//                mass_sum = mass * (double) item.get("quantity");
//            }

//        if (mode.equals("details")) {
//            visible = false;
//            if (obj.get("spec_type").equals("Buy") || obj.get("spec_type").equals("OtherComponents")) // or comm this?
//                return null;
//            if (obj.get("spec_type").equals("Parts"))
//                visible = true;
//            if (item.get("children") instanceof List) {
//                List<Map<String, Object>> children = (List<Map<String, Object>>) item.get("children");
//                for (Map<String, Object> child : children)
//                    if (child.get("spec_type").equals("Parts"))
//                        visible = true;
//            }
//        }

            CellStyle style = styles.get("white");
            if (level <= 3)
                style = styles.get("level" + level);
            if (obj.get("spec_type").equals("Parts"))
                style = styles.get("parts");
            if (obj.get("spec_type").equals("StandardComponents"))
                style = styles.get("standards");

//            if (/*visible && mode.equals("only_1level") && */!obj.get("spec_type").equals("Assemblies")) // or 2nd half of this?
//                visible = false;

            Row row = sheet.createRow(lineNumber);
            Integer rowNumber = lineNumber;

            if (visible) {
                lineNumber += 1;

                addCell(row, COLS.LEVEL.ordinal(), level, style);
                addCell(row, SpecComp.COLS.CODE.ordinal(), !(obj.get("spec_type").equals("StandardComponents") || obj.get("title").equals("")) ? obj.get("title") + " " + obj.get("description") : obj.get("description"), style); // стандарт/покупные
//                addCell(row, COLS.TITLE.ordinal(), obj.get("description"), style);
//                addCell(row, COLS.NAME.ordinal(), obj.get("name"), style);
//                addCell(row, COLS.REVISION.ordinal(), obj.get("revision"), style);
//                addCell(row, COLS.STATUS.ordinal(), obj.get("current"), style);
                addCell(row, COLS.QUANTITY.ordinal(), item.get("quantity"), style);
                addCell(row, COLS.MEASURE.ordinal(), "шт", style);
//                addCell(row, COLS.OWNER.ordinal(), obj.get("owner"), style);
//                addCell(row, COLS.PARENT.ordinal(), item.get("parent"), style);
//                addCell(row, COLS.MASS.ordinal(), mass, style);
//                addCell(row, COLS.MATERIAL.ordinal(), item.get("material"), style);
//                addCell(row, COLS.PDF.ordinal(), "", style);

//            String ca_name = "";
//            try {
//                MapList ca = CAUtil.getRelatedCA(ctx, (String) obj.get("id"));
//                if (ca.size() > 0)
//                    ca_name = scalar(ctx, ((Map<String, String>) ca.get(0)).get("id"), "name");
//            } catch (Throwable ignored) {
//            }
//            addCell(row, COLS.CA.ordinal(), ca_name, style);

//                if (!mode.equals("ute")) {
//
//                    for (String drwId : findRowsList(ctx, "*", (String) item.get("name"), "from[VPMRepInstance].to.id")) {
//                        Row drwRow = sheet.createRow(lineNumber);
//
//                        Map<String, String> drw = row(ctx, drwId,
//                                "attribute[PLMEntity.V_Name]:title",
//                                "attribute[PLMEntity.V_description]:description",
//                                "owner",
//                                "revision",
//                                "type",
//                                "name",
//                                "current");
//
//                        if ((!mode.equals("all") && drw.get("type").equals("3DShape")) || mode.equals("only_1level")) // if (!mode.equals("all") && drw.get("type").equals("3DShape"))
//                            continue;
//
//                        lineNumber += 1;
//
//
//                        addCell(drwRow, COLS.LEVEL.ordinal(), "", styles.get("drw"));
//                        addCell(drwRow, COLS.CODE.ordinal(), drw.get("title"), styles.get("drw"));
////                        addCell(drwRow, COLS.NAME.ordinal(), drw.get("name"), styles.get("drw"));
////                        addCell(drwRow, COLS.REVISION.ordinal(), drw.get("revision"), styles.get("drw"));
////                        addCell(drwRow, COLS.TITLE.ordinal(), drw.get("description"), styles.get("drw"));
////                        addCell(drwRow, COLS.STATUS.ordinal(), drw.get("current"), styles.get("drw"));
//                        addCell(drwRow, COLS.QUANTITY.ordinal(), 1, styles.get("drw"));
////                        addCell(drwRow, COLS.OWNER.ordinal(), drw.get("owner"), styles.get("drw"));
////                        addCell(drwRow, COLS.PARENT.ordinal(), "", styles.get("drw"));
////                        addCell(drwRow, COLS.MASS.ordinal(), "", styles.get("drw"));
////                        addCell(drwRow, COLS.MATERIAL.ordinal(), "", styles.get("drw"));
////                        addCell(drwRow, COLS.MASS_SUM.ordinal(), "", styles.get("drw"));
//
////                        String filename = scalar(ctx, drwId, "from[ARCHReferenceObject].to.from[Active Version].to.attribute[Title]:filename");
////                        if (filename != null)
////                            addCell(drwRow, COLS.PDF.ordinal(), filename,
////                                    getBaseUrl(request) + "/internal/sw/download?objectId=" + item.get("id") + "&filename=" + URLEncoder.encode(filename), styles.get("drw"));
////                        else
////                            addCell(drwRow, COLS.PDF.ordinal(), "", styles.get("drw"));
//
//
////                        addCell(drwRow, COLS.CA.ordinal(), "", styles.get("drw"));
//                    }
//                }
            }

            if (item.get("children") instanceof List)
                for (Map<String, Object> child : (List<Map<String, Object>>) item.get("children")) {
                    AddRowResult result = addRow(request, ctx, sheet, child, level + 1, mode, lineNumber, styles);
                    lineNumber = result.lineNumber;
                }

//        if (visible)
//            addCell(row, COLS.MASS_SUM.ordinal(), mass_sum, style);

            if (lineNumber - rowNumber > 1)
                sheet.groupRow(rowNumber + 1, lineNumber - 1);
        }
        return new AddRowResult(lineNumber, mass_sum);
    }

    AddRowResult addRow(HttpServletRequest request, Context ctx, Sheet sheet, Map<String, Object> item,
                        String mode, Map<String, CellStyle> styles) {


        return new AddRowResult(1, 1d);
    }

    void addCell(Row row, Integer columnNumber, Object data) {
        addCell(row, columnNumber, data, null, null);
    }

    void addCell(Row row, Integer columnNumber, Object data, CellStyle style) {
        addCell(row, columnNumber, data, null, style);
    }

    void addCell(Row row, Integer columnNumber, Object data, int level) {

    }

    void addCell(Row row, Integer columnNumber, Object data, String link, CellStyle style) {
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
    }

    @GET
    @Path("/detail_search")
    public Response getList(@javax.ws.rs.core.Context HttpServletRequest request,
                           @QueryParam("name") String name) {

        try {
            Context ctx = internalAuth(request);
            List<String> names = findRowsList(ctx, "*", name, "relationship[VPMInstance].from.name");
            List<String> ids = findRowsList(ctx, "*", name, "relationship[VPMInstance].from.id");
            List<String> uniqueNames = new ArrayList<>(new HashSet<>(names));
            List<String> uniqueIDs = new ArrayList<>(new HashSet<>(names));
            Map<String, String> result = new HashMap<>();
//            Map<String, Object> all = recProductNameTree(ctx,names.get(0));
            Map<String, Object> all = new LinkedHashMap<>();
//            mulChildrenQuantity(all, 1);
            for (String str : names) {
                Map<String, Object> unit = getQuantityMap(ctx, str, name);
//                Set<String> keys = unit.keySet();
//                for (Map.Entry<String, Object> item: unit.entrySet()) {
//                    all.put(str, (Map<String, Object>) new LinkedHashMap<String, Object>().put(item.getKey(), item.getValue()));
//                }
                all.put(str, unit.get(name));
            }

//            getXls(request, ctx, name, "details", all);



            return getXls(request, ctx, name, "details", all);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


}
