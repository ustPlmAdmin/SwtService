package com.skyway;

import com.mql.MqlService;
import matrix.db.Context;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.ss.usermodel.Hyperlink;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Бекэнд виджета SWT Approver Norm
 * */
public class ApproverNorm extends SkyService {

    @GET
    @Path("/approve_norm_list")
    public Response getDrawing(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            String username = ctx.getSession().getUserName();
            List<Map<String, String>> my_tasks = new ArrayList<>();
            List<Map<String, String>> ERPtasks = new ArrayList<>();
            List<Map<String, String>> sorted_tasks = new LinkedList<>();
//            List<Map<String, String>> tasks = findObjectsWhere(ctx, "Inbox Task", "*",
//                    "attribute[Approval Status] == \"None\"" +
//                            " && state[Complete] == \"FALSE\"" +
//                            " && (from[Project Task].to.name == \"" + username + "\" " +
//                            " || from[Candidate Assignee].to.from[Group Member].to.name == \"" + username + "\"" +
//                            " || from[Project Task].to.type == \"Route Task User\")",
//                    "id",
//                    "name",
//                    "description",
//                    "from[Project Task].to.type:assigner_type",
//                    "from[Project Task].to:assigner_name");
            List<Map<String, String>> tasks = findObjectsWhere(ctx, "Inbox Task", "*",
                    "attribute[Approval Status] == \"None\"" +
                            " && state[Complete] == \"FALSE\"" +
                            " && from[Project Task].to == \"" + username + "\" ",
                    "id",
                    "name",
                    "owner",
                    "modified",
                    "from[Project Task].to.type:assigner_type",
                    "from[Project Task].to:assigner_name");

            for (Map<String, String> task: tasks) {
                List<Map<String, String>> attr = findRows(ctx, "Inbox Task", task.get("name"),
                        "from[Route Task].to.name:route",
                        "from[Route Task].to.to[Object Route].from.name:erp",
                        "from[Route Task].to.to[Object Route].from.to[Subtask].from.name:phs");
                if (attr.size() > 0 && task.get("assigner_name").equals(username)) {
                    if (attr.get(0).get("erp").startsWith("ERP")) {
                        task.put("route", attr.get(0).get("route"));
                        task.put("erp", attr.get(0).get("erp"));
                        task.put("phs", attr.get(0).get("phs"));
                        task.put("assigner", task.get("assigner_name"));
                        task.put("assigned_time", printDateFormat.format(dateFormat.parse(task.get("modified"))));
                        ERPtasks.add(task);
                    }
                }
                task.put("color", "#e2efda");
            }
            //SORTING
            Set<String> erps = new TreeSet<>();
            for (Map<String, String> temp : ERPtasks) {
                erps.add(temp.get("erp"));
            }
            for (String str : erps) {
                for (Map<String, String> temp : ERPtasks) {
                    if (str.equals(temp.get("erp"))) {
                        sorted_tasks.add(temp);
                    }
                }
            }

            //  for debug
            List<Map<String, String>> ERProutes = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && attribute[Route Status] == \"Started\" " +
                            " && from[Route Node].to == \"Test Group Tech\" " +//TEST
//                            " && from[Route Node].to == \"Time norm group\" " +//PROD
                            " && owner == \"al.kuznetsov\" || owner == \"s.kirichenko\" || owner == \"m.kim\"",
                    "id",
                    "name",
                    "owner",
                    "attribute[Route Status]:status",
                    "to[Object Route]:ca_exist");

            //НЕЗАВЕРШЕННЫЕ ЗАДАЧИ
            try {
                List<Map<String, String>> parents = findObjectsWhere(ctx, "Task", "*",
                        "current == \"Review\" " +
                                " && from[Object Route].to.attribute[Route Status] == \"Finished\" " +
                                " && from[Object Route].to.from[Route Node].to == \"Test Group Tech\" " +//TEST
//                                " && from[Object Route].to.from[Route Node].to == \"Time norm group\" " +//PROD
                                " && from[Object Route].to.owner == \"al.kuznetsov\" || from[Object Route].to.owner == \"s.kirichenko\" || from[Object Route].to.owner == \"m.kim\"",
                        "from[Object Route].to.id:id",
                        "from[Object Route].to.name:name",
                        "from[Object Route].to.owner:owner",
                        "from[Object Route].to.attribute[Route Status]:status",
                        "from[Object Route].to.to[Object Route]:ca_exist",
                        "current");
                ERProutes.addAll(parents);
            } catch (Exception e) {
                error(e);
            }

            //ВЛОЖЕННЫЕ МАРШРУТЫ
            try {
                List<Map<String, String>> temp = new ArrayList<>();
                Iterator<Map<String, String>> iter = ERProutes.iterator();
                while (iter.hasNext()) {
                    List<Map<String, String>> r = findRows(ctx, "Route", iter.next().get("name"),
                            "to[Route Task].from.from[Task Sub Route].to.id:id",
                            "to[Route Task].from.from[Task Sub Route].to.name:name",
                            "to[Route Task].from.from[Task Sub Route].to.owner:owner",
                            "to[Route Task].from.from[Task Sub Route].to.attribute[Route Status]:status");
                    if (r.size() > 0) {
                        temp.addAll(r);
                    }
                }
                ERProutes.addAll(temp);
                temp.clear();
            } catch (Exception e) {
                error(e);
            }


            for (Map<String, String> route : ERProutes) {
                List<Map<String, String>> route_tasks = findRows(ctx, "Route", route.get("name"),
                        "to[Route Task].from.name:task",
                        "to[Route Task].modified:assigned_time",
                        "to[Route Task].from.from[Project Task].to:assigner");
                Map<String, String> first_route_task = route_tasks.get(0);
                route.put("task", first_route_task.get("task"));
                route.put("assigner", first_route_task.get("assigner"));
                route.put("assigned_time", printDateFormat.format(dateFormat.parse(first_route_task.get("assigned_time"))));
                if (route.get("status") != null && route.get("status").equals("Finished")) {
                    route.put("color", "#e2efda");
                } else if (route.get("status") != null && route.get("status").equals("Started")) {
                    route.put("color", "#ffc000");
                } else if (route.get("status") != null && route.get("status").equals("Stopped")) {
                    route.put("color", "#e34f4f");
                }
                String r = findScalar(ctx, "*", route.get("name"), "to[Object Route].from.name");//PARENT
                String t = findScalar(ctx, "*", r, "attribute[Title]");
                if (r != null && r.length() > 0 && r.startsWith("ERP")) {
                    route.put("parent", r.trim() + " " + t.trim());
                } else {
                    r = findScalar(ctx, "*", route.get("name"), "to[Task Sub Route].from.from[Route Task].to.name");
                    if (r != null && r.length() > 0 && r.startsWith("ERP")) {
                        route.put("parent", r.trim() + " " + t.trim());
                    } else {
                        r = findScalar(ctx, "*", route.get("name"), "to[Task Sub Route].from.from[Route Task].to.to[Object Route].from.name");
                        if (r != null && r.length() > 0 && r.startsWith("ERP")) {
                            route.put("parent", r.trim() + " " + t.trim());
                        } else {
                            route.put("parent", " ");
                        }
                    }
                }
                if (route.get("status") != null && route.get("status").equals("Review")) {
                    route.put("color", "#ffffff");
                }
                if (!route.get("parent").equals(" ")) {//ROOT
                    String root = findScalar(ctx, "Task", r, "to[Subtask].from.name");
                    String title;
                    if (root != null) {
                        title = findScalar(ctx, "Phase", root, "attribute[Title]");
                        route.put("root", root.trim() + " " + title.trim());
                    }
                } else {
                    route.put("root", " ");
                }
            }

            //SORTING
            List<Map<String, String>> ERP = new LinkedList<>();
            Set<String> roots = new TreeSet<>();
            for (Map<String, String> temp : ERProutes) {
                if (temp.containsKey("root"))
                    roots.add(temp.get("root"));
            }
            for (String str : roots) {
                for (Map<String, String> temp : ERProutes) {
                    if (str.equals(temp.get("root"))) {
                        if (!temp.get("root").equals(" "))
                            ERP.add(temp);
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("my_tasks", sorted_tasks);
            result.put("ERP", ERP);
            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

}
