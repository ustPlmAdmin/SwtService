package com.skyway;

import com.iga.cae.CAUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.mql.MqlService;
import matrix.db.Context;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import static org.apache.poi.ss.usermodel.CellStyle.ALIGN_CENTER;
import static org.apache.poi.ss.usermodel.CellStyle.VERTICAL_CENTER;


/**
 * Бекэнд виджета SWT Activities
 * */
public class Activities extends SkyService {

    @GET
    @Path("/users")
    public Response getUsers(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            Map<String, Object> result = new HashMap<>();
            result.put("users", findObjectsList(ctx, "Person", "*", "name"));
            result.put("groups", findObjectsList(ctx, "Group", "*", "name"));
            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    class Dataset {
        String label;
        String backgroundColor;
        int[] data;
    }

    class Chart {
        List<String> labels = new ArrayList<>();
        List<Dataset> datasets = new ArrayList<>();
        Map<Integer, List<Map<String, String>>> events = new HashMap<>();
    }

/*    @GET
    @Path("/user/login")
    public Response getLogin(@javax.ws.rs.core.Context HttpServletRequest request,
                             @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            List<String> history = findList(ctx, "Person", name, "*", "history");
            Map<Long, Map<String, String>> events = new HashMap<>();
            Map<String, String> user = new HashMap<>();
            user.put("name", name);
            user.put("current", "Active");
            if (history != null)
                for (String action : history) {
                    if (action.contains("Last Login Date")) {
                        String time = StringUtils.substringBetween(action, "Last Login Date: ", "was: ");
                        if (time != null && !time.isEmpty())
                            events.put(dateFormat.parse(time).getTime(), user);
                    }
                }
            return response(events);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }*/

    enum COLS {
        DATE,
        OWNER,
        TITLE,
        ID,
        TYPE,
        CURRENT,
        CA,
        CA_CURRENT,
    }

    Chart events(Context ctx, String type, String ownerName, Long from, Long to) throws FrameworkException, ParseException {
        if (from >= to) return null;

        Chart chart = new Chart();

        SimpleDateFormat sdf = new SimpleDateFormat("dd EEE");
        long msInDay = 1000 * 60 * 60 * 24;
        long dayIterator = from;
        while (dayIterator <= to) {
            chart.labels.add(sdf.format(new Date(dayIterator)));
            dayIterator += msInDay;
        }
        String where = "owner == \"" + ownerName + "\""
                + " && originated >= \"" + dateFormat.format(new Date(from)) + "\""
                + " && originated <= \"" + dateFormat.format(new Date(to)) + "\"";
        List<Map<String, String>> products = findObjectsWhere(ctx, type, "*", where,
                "originated", "name", "current",
                "attribute[PLMEntity.V_Name]");
        Map<String, Dataset> datasets = new HashMap<>();
        for (Map<String, String> product : products) {
            long eventTime = 0;
            try {
                eventTime = dateFormat.parse(product.get("originated")).getTime();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (from <= eventTime && eventTime <= to) {
                Dataset dataset = datasets.get(product.get("current"));
                if (dataset == null) {
                    datasets.put(product.get("current"), dataset = new Dataset());
                    dataset.label = product.get("current");
                    dataset.data = new int[chart.labels.size()];
                }
                int dayIndex = (int) ((eventTime - from) / msInDay);
                dataset.data[dayIndex] += 1;
                List<Map<String, String>> day = chart.events.computeIfAbsent(dayIndex, k -> new ArrayList<>());
                day.add(product);
            }
        }
        chart.datasets = new ArrayList<>(datasets.values());
        return chart;
    }

    @GET
    @Path("/user/drawings")
    public Response getDrawing(@javax.ws.rs.core.Context HttpServletRequest request,
                               @QueryParam("name") String name,
                               @QueryParam("from") Long from,
                               @QueryParam("to") Long to) {
        try {
            Context ctx = authenticate(request);
            Chart chart = events(ctx, "Drawing", name, from, to);
            return response(chart);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/user/products")
    public Response getProducts(@javax.ws.rs.core.Context HttpServletRequest request,
                                @QueryParam("name") String name,
                                @QueryParam("from") Long from,
                                @QueryParam("to") Long to) {
        try {
            Context ctx = authenticate(request);
            Chart chart = events(ctx, "VPMReference", name, from, to);
            return response(chart);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    Workbook getUserActivity(Context ctx, Long from, Long to, String name) throws Exception {

        long distance = 1 * 30 * 24 * 60 * 60 * 1000;

        String dateWhere = "owner == \"" + name + "\""
                + " && (originated >= \"" + dateFormat.format(new Date(from - distance)) + "\""
                + " && originated <= \"" + dateFormat.format(new Date(to + distance)) + "\")"
                + " || (modified >= \"" + dateFormat.format(new Date(from - distance)) + "\""
                + " && modified <= \"" + dateFormat.format(new Date(to + distance)) + "\")";

        String[] types = new String[]{"VPMReference", "3DShape", "Kit_Product", "Kit_Part", "Kit_Material", "Kit_StandardProduct",
                "Kit_OEMProduct", "Kit_CoreMaterial", "Kit_Drawing", "Kit_MfgBar", "Kit_MfgAssembly", "Kit_MfgStandardComponent",
                "Kit_MfgOEMComponent", "MfgRawMaterial", "Kit_MfgContinuousPrvd"};
        List<String> ids = new ArrayList<>();
        for (String type : types)
            ids.addAll(findObjectsListWhere(ctx, type, "*", dateWhere, "id"));

        if (ids.size() == 0) {
            return null;
        } else {

            Workbook book = new HSSFWorkbook();
            Sheet sheet = book.createSheet("Main");

            Calendar fromDate = Calendar.getInstance();
            fromDate.setTimeInMillis(from);
            int fromDayNumber = fromDate.get(Calendar.YEAR) * 365 + fromDate.get(Calendar.DAY_OF_YEAR);
            Calendar toDate = Calendar.getInstance();
            toDate.setTimeInMillis(to);
            int toDayNumber = toDate.get(Calendar.YEAR) * 365 + toDate.get(Calendar.DAY_OF_YEAR);

            Map<Integer, Map<String, Map<String, String>>> groupByDay = new LinkedHashMap<>();


            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                Map<String, String> product = row(ctx, id, "name", "current", "type",
                        "attribute[PLMEntity.V_Name]",
                        "attribute[PLMEntity.V_description]",
                        "attribute[IGAPartEngineering.IGASpecChapter]");

                MapList ca = CAUtil.getRelatedCA(ctx, id);
                if (ca.size() > 0) {
                    Map<String, String> caMap = (Map<String, String>) ca.get(0);
                    try {
                        product.put("ca_name", scalar(ctx, caMap.get("id"), "name"));
                        product.put("ca_current", caMap.get("current"));
                    } catch (Exception e) {
                        product.put("ca_name", "No Access");
                    }
                }

                List<String> history = list(ctx, id, "history");
                for (String event : history) {

                    try {
                        Calendar eventDate = Calendar.getInstance();
                        String time = StringUtils.substringBetween(event, "time:", " state:");
                        if (time == null || time.isEmpty())
                            continue;
                        eventDate.setTime(dateFormat.parse(time));

                        int eventDayNumber = eventDate.get(Calendar.YEAR) * 365 + eventDate.get(Calendar.DAY_OF_YEAR);
                        if (fromDayNumber <= eventDayNumber && eventDayNumber <= toDayNumber) {
                            Integer dayInInterval = eventDayNumber - fromDayNumber;
                            Map<String, Map<String, String>> day = groupByDay.computeIfAbsent(dayInInterval, k -> new LinkedHashMap<>());
                            String groupId = product.get("type").equals("3DShape") ? product.get("attribute[PLMEntity.V_Name]") : product.get("name");
                            if (day.get(groupId) == null) {
                                Map<String, String> productCopy = new LinkedHashMap<>();
                                productCopy.putAll(product);
                                productCopy.put("modified", printDateFormat.format(eventDate.getTime()));
                                day.putIfAbsent(groupId, productCopy);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }


            int lineNumber = 1;
            for (int i = 0; i <= toDayNumber - fromDayNumber; i++) {
                Map<String, Map<String, String>> day = groupByDay.get(i);
                if (day != null)
                    for (Map<String, String> event : day.values()) {
                        String title = event.get("attribute[PLMEntity.V_Name]").isEmpty() ? event.get("attribute[PLMEntity.V_description]") : event.get("attribute[PLMEntity.V_Name]");
                        Row row = sheet.createRow(lineNumber);
                        addCell(row, COLS.DATE.ordinal(), event.get("modified"));
                        addCell(row, COLS.OWNER.ordinal(), name);
                        addCell(row, COLS.TITLE.ordinal(), title);
                        addCell(row, COLS.ID.ordinal(), event.get("name"));
                        addCell(row, COLS.TYPE.ordinal(), event.get("attribute[IGAPartEngineering.IGASpecChapter]"));
                        addCell(row, COLS.CURRENT.ordinal(), event.get("current"));
                        addCell(row, COLS.CA.ordinal(), event.get("ca_name"));
                        addCell(row, COLS.CA_CURRENT.ordinal(), event.get("ca_current"));
                        lineNumber += 1;
                    }
            }
            addTitles(book, sheet);
            return book;
        }
    }


    @GET
    @Path("/activity_export")
    public Response activityExport(@javax.ws.rs.core.Context HttpServletRequest request,
                                   @QueryParam("from") Long from,
                                   @QueryParam("to") Long to,
                                   @QueryParam("name") String name) {

        try {
            Context ctx = authenticate(request);
            Workbook book = getUserActivity(ctx, from, to, name);
            if (book == null)
                return page("Objects name " + name + " not found", "");
            return excel(book, name);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    @GET
    @Path("/group_activity_export")
    public Response groupActivityExport(@javax.ws.rs.core.Context HttpServletRequest request,
                                        @QueryParam("from") Long from,
                                        @QueryParam("to") Long to,
                                        @QueryParam("name") String group_name) {

        try {
            Context ctx = authenticate(request);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream out = new ZipOutputStream(baos);

            List<String> group_members = findList(ctx, "Group", group_name, "from[Group Member].to.name");
            if (true)
                return null;
            for (String member : group_members) {
                Workbook book = getUserActivity(ctx, from, to, member);
                if (book != null) {
                    try {
                        ZipEntry zipEntry = new ZipEntry(member + ".xls");
                        out.putNextEntry(zipEntry);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        book.write(outputStream);
                        out.write(outputStream.toByteArray());
                        out.closeEntry();
                    } catch (ZipException ignored) {
                    }
                }
            }

            out.close();
            if (baos.size() == 22) {
                return page("Documents not found", "Please check docs in \"Reference Documents\"");
            } else {
                return file(baos, "application/zip", group_name + ".zip");
            }

        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    void addTitles(Workbook book, Sheet sheet) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(30);
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

        addCell(row, COLS.DATE.ordinal(), "Дата", style);
        addCell(row, COLS.OWNER.ordinal(), "Пользователь", style);
        addCell(row, COLS.TITLE.ordinal(), "Наименование", style);
        addCell(row, COLS.ID.ordinal(), "Идентификатор", style);
        addCell(row, COLS.TYPE.ordinal(), "Тип", style);
        addCell(row, COLS.CURRENT.ordinal(), "Состояние", style);
        addCell(row, COLS.CA.ordinal(), "Change Action", style);
        addCell(row, COLS.CA_CURRENT.ordinal(), "CA Состояние", style);


        for (int i = 0; i < SpecComp.COLS.values().length; i++)
            sheet.autoSizeColumn(i);

    }

    void addCell(Row row, Integer columnNumber, Object data) {
        addCell(row, columnNumber, data, null);
    }

    void addCell(Row row, Integer columnNumber, Object data, CellStyle style) {
        Cell cell = row.createCell(columnNumber);
        if (data instanceof String)
            cell.setCellValue((String) data);
        else if (data instanceof Integer)
            cell.setCellValue((Integer) data);
        if (style != null)
            cell.setCellStyle(style);
    }

}
