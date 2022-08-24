package com.skyway;

import com.itextpdf.text.*;
import com.skyway.res.Resources;
import com.itextpdf.text.pdf.*;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Выгрузка спецификации в PDF
 * */
public class SpecPdf extends SpecUtils {
    Font font;
    public static final String ORGANIZATION_NAME = "ЗАО \"Струнные технологии\"";

    public SpecPdf() throws IOException, DocumentException {
        font = new Font(BaseFont.createFont("GOSTA.TTF", "cp1251", BaseFont.EMBEDDED, false, Resources.getBytes("spec/GOSTA.TTF"), null));
    }

    StringList lines(String text, int size, int lineWidth) {
        StringList lines = new StringList();
        if (lineWidth == 0) {
            lines.add(text);
        } else {
            StringList words = FrameworkUtil.split(text, " ");
            String line = "";
            for (String word : words) {
                String nextLine = line + word;
                if (font.getBaseFont().getWidthPoint(nextLine, size) <= lineWidth) {
                    line = nextLine + " ";
                } else {
                    if (line.isEmpty()) {
                        lines.add(word);
                    } else {
                        lines.add(line);
                        line = word + " ";
                    }
                }
            }
            lines.add(line);
        }
        return lines;
    }

    float textX(PdfContentByte stream, String text, float x, float y, int size, boolean center, Font.FontStyle style, int lineWidth, int lineHeight, int maxY) {
        StringList lines = lines(text, size, lineWidth);

        for (int i = 0; i < lines.size(); i++) {
            text = lines.get(i);
            font.setStyle(-1);
            font.setSize(size);
            float yWithOffset = maxY - y - i * lineHeight;
            float textWidth = font.getBaseFont().getWidthPoint(text, size);
            if (lineWidth != 0 && textWidth > lineWidth) {
                for (int j = 1; j < size * 2; j++) {
                    float newSize = size - (j / 2f);
                    if (font.getBaseFont().getWidthPoint(text, newSize) <= lineWidth) {
                        font.setSize(newSize);
                        textWidth = font.getBaseFont().getWidthPoint(text, newSize);
                        break;
                    }
                }
            }
            if (style != null) {
                if (style == Font.FontStyle.UNDERLINE) {
                    stream.setLineWidth(0.8);
                    stream.moveTo(x - textWidth / 2, yWithOffset - 1);
                    stream.lineTo(x + textWidth / 2, yWithOffset - 1);
                    stream.closePathStroke();
                } else
                    font.setStyle(style.getValue());
            }
            Phrase categoryTitle = new Phrase(text, font);
            ColumnText.showTextAligned(stream, center ? Element.ALIGN_CENTER : Element.ALIGN_LEFT, categoryTitle, x, yWithOffset, 0);
        }
        return lines.size() * lineHeight;
    }


    float text2(PdfContentByte stream, String text, float x, float y, int size, boolean center, Font.FontStyle style, int lineWidth, int lineHeight) {
        return textX(stream, text, x, y, size, center, style, lineWidth, lineHeight, 842);
    }

    float text2(PdfContentByte stream, String text, float x, float y, int size) {
        return text2(stream, text, x, y, size, false, null, 0, 0);
    }

    float text2(PdfContentByte stream, String text, float x, float y, int size, boolean center) {
        return text2(stream, text, x, y, size, center, null, 0, 0);
    }

    float text2(PdfContentByte stream, String text, float x, float y, int size, boolean center, Font.FontStyle style) {
        return text2(stream, text, x, y, size, center, style, 0, 0);
    }


    float text4(PdfContentByte stream, String text, float x, float y, int size, boolean center, Font.FontStyle style, int lineWidth, int lineHeight) {
        return textX(stream, text, x, y, size, center, style, lineWidth, lineHeight, 595);
    }

    float text4(PdfContentByte stream, String text, float x, float y, int size) {
        return text4(stream, text, x, y, size, false, null, 0, 0);
    }

    float text4(PdfContentByte stream, String text, float x, float y, int size, boolean center) {
        return text4(stream, text, x, y, size, center, null, 0, 0);
    }

    float text4(PdfContentByte stream, String text, float x, float y, int size, boolean center, Font.FontStyle style) {
        return text4(stream, text, x, y, size, center, style, 0, 0);
    }

    public static String getTranslation(String type) {
        Map<String, String> groupTranslations = new HashMap<>();
        groupTranslations.put("StandardComponents", "Стандартные изделия");
        groupTranslations.put("OtherComponents", "Прочие изделия");
        groupTranslations.put("Parts", "Детали");
        groupTranslations.put("Materials", "Материалы");
        groupTranslations.put("Documents", "Документация");
        groupTranslations.put("Kits", "Коплекты");
        groupTranslations.put("Complex", "Комплексы");
        groupTranslations.put("Assemblies", "Сборочные единицы");
        groupTranslations.put("DocumentsCommon", "Документация общая");
        groupTranslations.put("DocumentsParts", "Документация на составные части изделия");
        return groupTranslations.get(type);
    }

    private List<String> documentsIds(Context ctx, String objectId) throws MatrixException {
        List<String> ids = new ArrayList<>();
        Map<String, String> programMap = new HashMap<>();
        programMap.put("objectId", objectId);
        String[] args = JPO.packArgs(programMap);
        MapList response = JPO.invoke(ctx, "VPLMDocument", null, "getDocuments", args, MapList.class);
        if (response != null && response.size() > 0) {
            for (Object responseObj : response) {
                Map<String, String> responseMap = (Map<String, String>) responseObj;
                String docId = responseMap.get("id");
                ids.add(docId);
            }
        }
        return ids;
    }

    private void documents(Context ctx, String objectId, Map<String, Map<String, String>> children) throws MatrixException {
        for (String docId : documentsIds(ctx, objectId)) {
            Map<String, String> doc = row(ctx, docId, "attribute[Title]");
            doc.put("type", "Documents");
            doc.put("title", doc.get("attribute[Title]"));
            children.put(docId, doc);
        }
    }

    Map<String, Map<String, String>> children(Context ctx, Map<String, List<String>> tree, boolean onlyBuy) throws FrameworkException {
        Map<String, Map<String, String>> attrs = new LinkedHashMap<>();
        for (String parentName : tree.keySet()) {
            List<String> level = tree.get(parentName);
            for (String childName : level) {
                Map<String, String> child = findObject(ctx, "*", childName, "name",
                        "attribute[PLMEntity.V_Name]:title",
                        "attribute[PLMEntity.V_description]:description",
                        "attribute[IGAPartEngineering.IGASpecChapter]:type",
                        "attribute[IGAPartEngineering.IGAComment]:comment");

                if (onlyBuy && (!"Buy".equals(child.get("type")) && !"OtherComponents".equals(child.get("type"))))
                    continue;

                child.put("parent", parentName);

                if (child.get("type").equals("ExcludeFromSpec") || child.get("type").equals("Unassigned"))
                    continue;

                Map<String, String> childData = attrs.get(childName);
                if (childData == null) {
                    child.put("count", "1");
                    attrs.put(child.get("name"), child);
                } else {
                    childData.put("count", "" + (Integer.valueOf(childData.get("count")) + 1));
                }
            }
        }
        return attrs;
    }


    void groupByType(Map<String, Map<String, String>> children, Map<String, List<String>> groups) {
        List<String> names = new ArrayList<>(children.keySet());
        for (String childId : names) {
            String childType = children.get(childId).get("type");
            List<String> group = groups.get(childType);
            if (group == null)
                groups.put(childType, group = new StringList());
            group.add(childId);
        }
        Map<String, List<String>> groupsWithSort = new LinkedHashMap<>();
        String[] sortSequence = new String[]{"Documents", "Complex", "Assemblies",
                "Parts", "StandardComponents", "OtherComponents", "Materials", "Kits"};

        for (String groupType : sortSequence) {
            List<String> groupIds = groups.get(groupType);
            if (groupIds != null) {
                sortByTitle(children, groupIds);
                groupsWithSort.put(groupType, groupIds);
            }
        }

        groups.clear();
        groups.putAll(groupsWithSort);
    }

    public Integer toNum(String strNum) {
        if (strNum == null)
            return null;
        try {
            return Integer.valueOf(strNum);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    void sortByTitle(Map<String, Map<String, String>> children, List<String> groupIds) {
        if (groupIds != null)
            groupIds.sort((childName1, childName2) -> {
                String[] mainDivideRevision1 = children.get(childName1).get("title").split("-");
                String[] mainDivideRevision2 = children.get(childName2).get("title").split("-");
                String title1 = mainDivideRevision1[0];
                String title2 = mainDivideRevision2[0];
                if (title1.equals(title2)) {
                    title1 = mainDivideRevision1.length > 1 ? mainDivideRevision1[1] : "00";
                    title2 = mainDivideRevision2.length > 1 ? mainDivideRevision2[1] : "00";
                    return title1.compareTo(title2);
                }
                List<String> arr1 = Arrays.asList(title1.split("\\."));
                List<String> arr2 = Arrays.asList(title2.split("\\."));
                for (int i = 0; i < Math.min(arr1.size(), arr2.size()); i++) {
                    Integer id1 = toNum(arr1.get(i));
                    Integer id2 = toNum(arr2.get(i));
                    if (id1 != null && id2 != null) {
                        int compareResult = Integer.compare(id1, id2);
                        if (compareResult != 0)
                            return compareResult;
                    }
                    if (id1 != null && id2 == null)
                        return 1;
                    if (id1 == null && id2 != null)
                        return -1;
                    if (id1 == null && id2 == null) {
                        int compareResult = arr1.get(i).compareTo(arr2.get(i));
                        if (compareResult != 0)
                            return compareResult;
                    }
                }
                return 0;
            });
    }

    @GET
    @Path("/spec")
    @Produces("application/pdf")
    public Response getSpecification(@javax.ws.rs.core.Context HttpServletRequest request,
                                     @QueryParam("objectId") String objectId) {
        try {
            Context ctx = authenticate(request);

            PdfReader spec_item = new PdfReader(Resources.getStream("spec/item.pdf"));
            PdfReader spec_main = new PdfReader(Resources.getStream("spec/main.pdf"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            spec_main.selectPages("1");
            PdfStamper stamper = new PdfStamper(spec_main, outputStream);

            PdfContentByte stream = stamper.getOverContent(1);

            Map<String, String> root = row(ctx, objectId,
                    "owner",
                    "organization",
                    "attribute[PLMEntity.V_Name]:title",
                    "attribute[PLMEntity.V_description]:description");

            String lastName = findScalar(ctx, "Person", root.get("owner"), "attribute[Last Name]");
            text2(stream, lastName, 107, 769, 11);

            text2(stream, ORGANIZATION_NAME, 512, 813, 14, true);
            text2(stream, root.get("title"), 410, 746, 26, true);
            int descLineWidth = 435 - 250;
            int descLineHeight = 24;
            int descLines = lines(root.get("description"), 20, descLineWidth).size();
            text2(stream, root.get("description"),
                    343, 800 - (descLines == 1 ? 0 : descLineHeight / 2), 20, true, Font.FontStyle.NORMAL, descLineWidth, descLineHeight);

            boolean createNextPage = false;
            int pageCount = 1;
            int startTop = 100;
            int top = startTop;
            int lineHeight = 25;
            int maxLines = 24;

            List<String> names = list(ctx, objectId, "from[VPMInstance].to.name");
            Map<String, List<String>> tree = new HashMap<>();
            tree.put(root.get("name"), names);

            Map<String, Map<String, String>> attrs = children(ctx, tree, false);
            Map<String, List<String>> groups = new LinkedHashMap<>();
            documents(ctx, objectId, attrs);
            groupByType(attrs, groups);

            boolean printGroupTitle;
            int childIndex = 0;
            top -= lineHeight * 2;
            for (String group : groups.keySet()) {
                top += lineHeight * 2;
                printGroupTitle = true;
                List<String> groupChildren = groups.get(group);
                for (int i = 0; i < groupChildren.size(); i++) {
                    String childName = groupChildren.get(i);
                    if (createNextPage) {
                        stamper.insertPage(++pageCount, spec_item.getPageSizeWithRotation(1));
                        stream = stamper.getOverContent(pageCount);
                        stream.addTemplate(stamper.getImportedPage(spec_item, 1), 0, 0);
                        top = startTop;
                        maxLines = 28;
                        createNextPage = false;
                        text2(stream, root.get("title"), 405, 816, 26, true);
                        text2(stream, "" + pageCount, 566, 820, 14, true);
                    }

                    if (printGroupTitle) {
                        if (top + lineHeight > startTop + lineHeight * (maxLines - 1)) {
                            i--;
                            createNextPage = true;
                            continue;
                        }
                        text2(stream, getTranslation(group), 396, top, 16, true, Font.FontStyle.UNDERLINE);
                        top += lineHeight;
                        printGroupTitle = false;
                    }

                    Map<String, String> child = attrs.get(childName);

                    int nameWidth = 477 - 313;
                    int descriptionWidth = 477 - 313;
                    int commentWidth = 577 - 517;
                    int maxRowLines = lines(child.get("title"), 16, nameWidth).size();
                    maxRowLines = Math.max(maxRowLines, lines(child.get("description"), 16, descriptionWidth).size());
                    maxRowLines = Math.max(maxRowLines, lines(child.get("comment"), 16, commentWidth).size());
                    if (top + maxRowLines * lineHeight <= startTop + lineHeight * maxLines) {
                        text2(stream, "" + ++childIndex, 100, top, 16, true);
                        text2(stream, child.get("count"), 499, top, 16, true);
                        text2(stream, child.get("title"), 117, top, 16);
                        text2(stream, child.get("description"), 313, top, 16, false, Font.FontStyle.NORMAL, descriptionWidth, lineHeight);
                        text2(stream, child.get("comment"), 517, top, 16, false, Font.FontStyle.NORMAL, commentWidth, lineHeight);
                        top += maxRowLines * lineHeight;
                    } else {
                        i--;
                        createNextPage = true;
                    }
                }
            }

            stream = stamper.getOverContent(1);
            text2(stream, "" + pageCount, 550, 783, 12, true);
            stamper.setFullCompression();
            stamper.close();
            return file(outputStream, "application/pdf", root.get("title") + ".spec.pdf");
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    public void recSubItems(Context ctx, String name, Map<String, List<String>> tree) throws FrameworkException {
        List<Map<String, String>> subItems = findRows(ctx, "*", name,
                "from[VPMInstance].to.name:name",
                "from[VPMInstance].to.attribute[IGAPartEngineering.IGASpecChapter]:type");

        StringList children = new StringList();
        for (Map<String, String> child : subItems) {
            String childName = child.get("name");
            String childType = child.get("type");
            children.add(childName);
            if ("Assemblies".equals(childType) || "Kits".equals(childType))
                recSubItems(ctx, childName, tree);
        }
        if (children.size() > 0)
            tree.put(name, children);
    }

    @GET
    @Path("/vpi")
    @Produces("application/pdf")
    public Response getVPI(@javax.ws.rs.core.Context HttpServletRequest request,
                           @QueryParam("objectId") String objectId) {
        try {
            Context ctx = authenticate(request);
            PdfReader vpi_item = new PdfReader(Resources.getStream("vpi/item.pdf"));
            PdfReader vpi_main = new PdfReader(Resources.getStream("vpi/main.pdf"));
            PdfReader vpi_footer = new PdfReader(Resources.getStream("vpi/footer.pdf"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            vpi_main.selectPages("1");
            PdfStamper stamper = new PdfStamper(vpi_main, outputStream);


            Map<String, String> root = row(ctx, objectId,
                    "name",
                    "owner",
                    "organization",
                    "attribute[PLMEntity.V_Name]:title",
                    "attribute[PLMEntity.V_description]:description");


            PdfContentByte stream = stamper.getOverContent(1);

            text2(stream, ORGANIZATION_NAME, 1106, 813, 14, true);
            text2(stream, root.get("title"), 1010, 746, 26, true);

            int descLineWidth = 435 - 250;
            int descLineHeight = 24;
            int descLines = lines(root.get("description"), 20, descLineWidth).size();
            text2(stream, root.get("description"),
                    935, 800 - (descLines == 1 ? 0 : descLineHeight / 2), 20, true, Font.FontStyle.NORMAL, descLineWidth, descLineHeight);


            String lastName = findScalar(ctx, "Person", root.get("owner"), "attribute[Last Name]");
            text2(stream, lastName, 703, 769, 11);

            boolean createNextPage = false;
            int pageCount = 1;
            int startTop = 131;
            int top = startTop;
            int lineHeight = 23;
            int maxLines = 23;

            List<String> names = list(ctx, objectId, "from[VPMInstance].to.name");
            Map<String, List<String>> tree = new HashMap<>();
            tree.put(root.get("name"), names);

            Map<String, Map<String, String>> attrs = children(ctx, tree, true);
            Map<String, List<String>> groups = new LinkedHashMap<>();
            groupByType(attrs, groups);

            boolean printGroupTitle;
            int childIndex = 0;
            top -= lineHeight * 2;
            for (String group : groups.keySet()) {
                printGroupTitle = true;
                top += lineHeight * 2;
                List<String> groupChildren = groups.get(group);
                for (int i = 0; i < groupChildren.size(); i++) {
                    String childName = groupChildren.get(i);
                    if (createNextPage) {
                        stamper.insertPage(++pageCount, vpi_item.getPageSizeWithRotation(1));
                        stream = stamper.getOverContent(pageCount);
                        PdfImportedPage page = stamper.getImportedPage(vpi_item, 1);
                        stream.addTemplate(page, 0, -1f, 1f, 0, 0, 842);
                        top = startTop;
                        maxLines = 28;
                        createNextPage = false;
                        text2(stream, root.get("title"), 993, 816, 26, true);
                        text2(stream, "" + pageCount, 1162, 820, 14, true);
                    }

                    if (printGroupTitle) {
                        if (top + lineHeight > startTop + lineHeight * (maxLines - 1)) {
                            i--;
                            createNextPage = true;
                            continue;
                        }
                        text2(stream, getTranslation(group), 173, top, 16, true, Font.FontStyle.UNDERLINE);
                        top += lineHeight;
                        printGroupTitle = false;
                    }

                    Map<String, String> child = attrs.get(childName);
                    int nameWidth = 254 - 88;
                    int descriptionWidth = 574 - 386;
                    int rootNameWidth = 923 - 736;
                    int commentWidth = 1174 - 1109;
                    int maxRowLines = lines(child.get("title"), 16, nameWidth).size();
                    maxRowLines = Math.max(maxRowLines, lines(root.get("title"), 16, rootNameWidth).size());
                    maxRowLines = Math.max(maxRowLines, lines(child.get("description"), 16, descriptionWidth).size());
                    maxRowLines = Math.max(maxRowLines, lines(child.get("comment"), 16, commentWidth).size());
                    if (top + maxRowLines * lineHeight <= startTop + lineHeight * maxLines) {
                        text2(stream, "" + ++childIndex, 70, top, 16, true);
                        text2(stream, child.get("title"), 89, top, 16, false, Font.FontStyle.NORMAL, nameWidth, lineHeight);
                        String parentName = findScalar(ctx, "*", child.get("parent"), "attribute[PLMEntity.V_Name]");
                        text2(stream, parentName, 736, top, 16, false, Font.FontStyle.NORMAL, rootNameWidth, lineHeight);
                        text2(stream, child.get("count"), 950, top, 16, true);
                        text2(stream, child.get("count"), 1085, top, 16, true);
                        text2(stream, child.get("description"), 386, top, 16, false, Font.FontStyle.NORMAL, descriptionWidth, lineHeight);
                        text2(stream, child.get("comment"), 1109, top, 16, false, Font.FontStyle.NORMAL, commentWidth, lineHeight);
                        top += maxRowLines * lineHeight;
                    } else {
                        i--;
                        createNextPage = true;
                    }
                }
            }


            pageCount += 1;
            stamper.insertPage(pageCount, vpi_footer.getPageSizeWithRotation(1));
            stream = stamper.getOverContent(pageCount);
            stream.addTemplate(stamper.getImportedPage(vpi_footer, 1), 0, 0);

            stream = stamper.getOverContent(pageCount);
            text2(stream, root.get("title"), 395, 816, 26, true);
            text2(stream, "" + pageCount, 566, 820, 12, true);

            stream = stamper.getOverContent(1);
            text2(stream, "" + pageCount, 1148, 783, 12, true);
            stamper.setFullCompression();
            stamper.close();

            return file(outputStream, "application/pdf", root.get("title") + ".vpi.pdf");
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    private void operationalDocuments(/*int level, */Context ctx, String objectId, Map<String, Map<String, String>> attrs, Map<String, List<String>> groups) throws MatrixException {
        //if (level >= 5) return;
        List<String> group = new ArrayList<>();
        for (String docId : documentsIds(ctx, objectId)) {
            Map<String, String> doc = row(ctx, docId,
                    "attribute[Title]:title", "description", "attribute[ARCHSheetsNumber]:count", "attribute[ARCHNotes]:comment");
            String title = doc.get("title");
            if (title != null && title.length() > 2
                    && (title.charAt(title.length() - 1) >= 'А' && title.charAt(title.length() - 1) <= 'Я')
                    && (title.charAt(title.length() - 2) >= 'А' && title.charAt(title.length() - 2) <= 'Я')) {
                if (doc.get("count").equals("0"))
                    doc.put("count", "1");
                group.add(docId);
                attrs.put(docId, doc);
            }
        }
        if (groups.size() == 0)
            groups.put("DocumentsCommon", group);
        else {
            List<String> existGroup = groups.get("DocumentsParts");
            if (existGroup == null)
                existGroup = group;
            else
                existGroup.addAll(group);
            groups.put("DocumentsParts", existGroup);
        }
        List<String> childrenNames = list(ctx, objectId, "from[VPMInstance].to.name");
        List<String> childrenNamesWithoutDuplicates = new ArrayList<>(new HashSet<>(childrenNames));
        for (String childName : childrenNamesWithoutDuplicates) {
            String childId = findScalar(ctx, "*", childName, "id");
            operationalDocuments(/*level + 1,*/ ctx, childId, attrs, groups);
        }
    }

    @GET
    @Path("/operational")
    @Produces("application/pdf")
    public Response getOperationalDocs(@javax.ws.rs.core.Context HttpServletRequest request,
                                       @QueryParam("objectId") String objectId) {
        try {
            Context ctx = authenticate(request);

            PdfReader spec_item = new PdfReader(Resources.getStream("spec/item.pdf"));
            PdfReader spec_main = new PdfReader(Resources.getStream("spec/main.pdf"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            spec_main.selectPages("1");
            PdfStamper stamper = new PdfStamper(spec_main, outputStream);

            PdfContentByte stream = stamper.getOverContent(1);

            Map<String, String> root = row(ctx, objectId,
                    "name",
                    "owner",
                    "organization",
                    "attribute[PLMEntity.V_Name]:title");

            root.put("title", root.get("title") + "ВЭ");
            root.put("count", "1");
            root.put("description", "Ведомость эксплутационных документов");

            String lastName = findScalar(ctx, "Person", root.get("owner"), "attribute[Last Name]");
            text2(stream, lastName, 107, 769, 11);

            text2(stream, ORGANIZATION_NAME, 512, 813, 14, true);
            text2(stream, root.get("title"), 410, 746, 26, true);
            int descLineWidth = 435 - 250;
            int descLineHeight = 24;
            int descLines = lines(root.get("description"), 18, descLineWidth).size();
            text2(stream, root.get("description"),
                    343, 800 - (descLines == 1 ? 0 : descLineHeight / 2), 18, true, Font.FontStyle.NORMAL, descLineWidth, descLineHeight);

            boolean createNextPage = false;
            int pageCount = 1;
            int startTop = 100;
            int top = startTop;
            int lineHeight = 25;
            int maxLines = 24;

            Map<String, Map<String, String>> attrs = new LinkedHashMap<>();
            Map<String, List<String>> groups = new LinkedHashMap<>();
            operationalDocuments(ctx, objectId, attrs, groups);

            sortByTitle(attrs, groups.get("DocumentsCommon"));
            sortByTitle(attrs, groups.get("DocumentsParts"));

            attrs.put("root", root);
            groups.get("DocumentsCommon").add(0, "root");

            boolean printGroupTitle;
            int childIndex = 0;
            top -= lineHeight * 2;
            for (String group : groups.keySet()) {
                top += lineHeight * 2;
                printGroupTitle = true;
                List<String> groupChildren = groups.get(group);
                for (int i = 0; i < groupChildren.size(); i++) {
                    String childName = groupChildren.get(i);
                    if (createNextPage) {
                        stamper.insertPage(++pageCount, spec_item.getPageSizeWithRotation(1));
                        stream = stamper.getOverContent(pageCount);
                        stream.addTemplate(stamper.getImportedPage(spec_item, 1), 0, 0);
                        top = startTop;
                        maxLines = 28;
                        createNextPage = false;
                        text2(stream, root.get("title"), 405, 816, 26, true);
                        text2(stream, "" + pageCount, 566, 820, 14, true);
                    }

                    if (printGroupTitle) {
                        if (top + lineHeight > startTop + lineHeight * (maxLines - 1)) {
                            i--;
                            createNextPage = true;
                            continue;
                        }
                        int descriptionWidth = 477 - 313;
                        int titleLines = lines(getTranslation(group), 16, descriptionWidth).size();
                        text2(stream, getTranslation(group), 396, top, 16, true, Font.FontStyle.UNDERLINE, descriptionWidth, lineHeight);
                        top += titleLines * lineHeight;
                        printGroupTitle = false;
                    }

                    Map<String, String> child = attrs.get(childName);

                    int nameWidth = 477 - 313;
                    int descriptionWidth = 477 - 313;
                    int commentWidth = 577 - 517;
                    int maxRowLines = lines(child.get("title"), 16, nameWidth).size();
                    maxRowLines = Math.max(maxRowLines, lines(child.get("description"), 16, descriptionWidth).size());
                    maxRowLines = Math.max(maxRowLines, lines(child.get("comment"), 16, commentWidth).size());
                    if (top + maxRowLines * lineHeight <= startTop + lineHeight * maxLines) {
                        text2(stream, "" + ++childIndex, 100, top, 16, true);
                        text2(stream, child.get("count"), 499, top, 16, true);
                        text2(stream, child.get("title"), 117, top, 16);
                        text2(stream, child.get("description"), 313, top, 16, false, Font.FontStyle.NORMAL, descriptionWidth, lineHeight);
                        text2(stream, child.get("comment"), 517, top, 16, false, Font.FontStyle.NORMAL, commentWidth, lineHeight);
                        top += maxRowLines * lineHeight;
                    } else {
                        i--;
                        createNextPage = true;
                    }
                }
            }

            stream = stamper.getOverContent(1);
            text2(stream, "" + pageCount, 550, 783, 12, true);
            stamper.setFullCompression();
            stamper.close();

            return file(outputStream, "application/pdf", root.get("title") + ".operational.pdf");
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    int childrenCount(Map<String, List<String>> tree, Map<String, Map<String, String>> attrs, String current, String searchName, int count) {
        List<String> children = tree.get(current);
        if (children != null)
            for (String childId : children) {
                if (attrs.get(childId).get("name").equals(searchName))
                    count += 1;
                count = childrenCount(tree, attrs, childId, searchName, count);
            }
        return count;
    }

    @GET
    @Path("/group_spec")
    @Produces("application/pdf")
    public Response getGroupSpec(@javax.ws.rs.core.Context HttpServletRequest request,
                                 @QueryParam("objectId") String objectId) {
        try {
            Context ctx = authenticate(request);
            PdfReader item = new PdfReader(Resources.getStream("group/item.pdf"));
            PdfReader main = new PdfReader(Resources.getStream("group/main.pdf"));
            PdfReader footer = new PdfReader(Resources.getStream("vpi/footer.pdf"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            main.selectPages("1");
            PdfStamper stamper = new PdfStamper(main, outputStream);

            Map<String, String> root = row(ctx, objectId,
                    "name",
                    "owner",
                    "organization",
                    "attribute[PLMEntity.V_Name]:title",
                    "attribute[PLMEntity.V_description]:description");

            PdfContentByte stream = stamper.getOverContent(1);


            text4(stream, "Кол. на исполн. " + root.get("title"), 582, 71, 16, true);

            text4(stream, root.get("title"), 656, 500, 26, true);
            text4(stream, ORGANIZATION_NAME, 759, 565, 14, true);

            int descLineWidth = 676 - 500;
            int descLineHeight = 24;
            int descLines = lines(root.get("description"), 20, descLineWidth).size();
            text4(stream, root.get("description"),
                    590, 550 - (descLines == 1 ? 0 : descLineHeight / 2), 20, true, Font.FontStyle.NORMAL, descLineWidth, descLineHeight);

            String lastName = findScalar(ctx, "Person", root.get("owner"), "attribute[Last Name]");
            text4(stream, lastName, 358, 522, 11);

            boolean createNextPage = false;
            int pageCount = 1;
            int startTop = 118;
            int top = startTop;
            int lineHeight = 25;
            int maxLines = 13;

            Map<String, List<String>> tree = new HashMap<>();
            recSubItems(ctx, root.get("name"), tree);


            Map<String, Map<String, String>> attrs = children(ctx, tree, false);
            Map<String, List<String>> groups = new LinkedHashMap<>();
            groupByType(attrs, groups);

            for (String groupName : tree.get(root.get("name"))) {
                String[] items = attrs.get(groupName).get("title").split("-");
                int groupIndex = items.length == 1 ? 0 : Integer.parseInt(items[1]);
                text4(stream, items.length == 1 ? "-" : items[1], 455 + groupIndex * 29, 94, 16, true);
            }

            boolean printGroupTitle;
            int childIndex = 0;
            top -= lineHeight * 2;
            for (String group : groups.keySet()) {
                printGroupTitle = true;
                top += lineHeight * 2;
                List<String> groupChildren = groups.get(group);
                for (int i = 0; i < groupChildren.size(); i++) {
                    String childName = groupChildren.get(i);

                    if (attrs.get(childName).get("parent").equals(root.get("name")))
                        continue;

                    if (createNextPage) {
                        stamper.insertPage(++pageCount, item.getPageSizeWithRotation(1));
                        stream = stamper.getOverContent(pageCount);
                        PdfImportedPage page = stamper.getImportedPage(item, 1);
                        stream.addTemplate(page, 0, -1f, 1f, 0, 0, 595);
                        top = startTop;
                        maxLines = 17;
                        createNextPage = false;
                        text4(stream, root.get("title"), 650, 570, 26, true);
                        text4(stream, "" + pageCount, 814, 574, 14, true);
                    }

                    if (printGroupTitle) {
                        if (top + lineHeight > startTop + lineHeight * (maxLines - 1)) {
                            i--;
                            createNextPage = true;
                            continue;
                        }
                        text4(stream, getTranslation(group), 356, top, 16, true, Font.FontStyle.UNDERLINE);
                        top += lineHeight;
                        printGroupTitle = false;
                    }

                    Map<String, String> childAttrs = attrs.get(childName);
                    int nameWidth = 260 - 74;
                    int descriptionWidth = 435 - 270;
                    int commentWidth = 825 - 725;
                    int maxRowLines = lines(childAttrs.get("title"), 16, nameWidth).size();
                    maxRowLines = Math.max(maxRowLines, lines(childAttrs.get("description"), 16, descriptionWidth).size());
                    maxRowLines = Math.max(maxRowLines, lines(childAttrs.get("comment"), 16, commentWidth).size());
                    if (top + maxRowLines * lineHeight <= startTop + lineHeight * maxLines) {
                        text4(stream, "" + ++childIndex, 58, top, 16, true);

                        String[] mainDivideRevision = childAttrs.get("title").split("-");

                        boolean firstTime = false;
                        for (Map<String, String> map : attrs.values()) {
                            if (map.get("title").split("-")[0].equals(mainDivideRevision[0])) {
                                if (map.get("main_record") == null) {
                                    firstTime = true;
                                    map.put("main_record", "1");
                                }
                                break;
                            }
                        }
                        if (mainDivideRevision[0].isEmpty()) firstTime = true;

                        text4(stream, firstTime ? childAttrs.get("title") : "-" + mainDivideRevision[1],
                                (firstTime ? 0 : font.getBaseFont().getWidthPoint(mainDivideRevision[0], 16)) + 74,
                                top, 16, false, Font.FontStyle.NORMAL, nameWidth, lineHeight);

                        for (String groupName : tree.get(root.get("name"))) {
                            String[] items = attrs.get(groupName).get("title").split("-");
                            int groupIndex = items.length == 1 ? 0 : Integer.parseInt(items[1]);
                            int groupCount = childrenCount(tree, attrs, groupName, childName, 0);
                            if (groupCount != 0)
                                text4(stream, "" + groupCount, 455 + groupIndex * 29, top, 16, true);
                        }

                        text4(stream, childAttrs.get("description"), 270, top, 16, false, Font.FontStyle.NORMAL, descriptionWidth, lineHeight);
                        text4(stream, childAttrs.get("comment"), 725, top, 16, false, Font.FontStyle.NORMAL, commentWidth, lineHeight);
                        top += maxRowLines * lineHeight;
                    } else {
                        i--;
                        createNextPage = true;
                    }
                }
            }


            pageCount += 1;
            stamper.insertPage(pageCount, footer.getPageSizeWithRotation(1));
            stream = stamper.getOverContent(pageCount);
            stream.addTemplate(stamper.getImportedPage(footer, 1), 0, 0);

            stream = stamper.getOverContent(pageCount);
            text2(stream, root.get("title"), 395, 816, 26, true);
            text2(stream, "" + pageCount, 566, 820, 12, true);

            stream = stamper.getOverContent(1);
            text4(stream, "" + pageCount, 800, 536, 12, true);
            stamper.setFullCompression();
            stamper.close();

            return file(outputStream, "application/pdf", root.get("title") + ".vpi.pdf");
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

}
