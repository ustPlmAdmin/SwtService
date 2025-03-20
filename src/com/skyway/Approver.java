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

/**
 * Бекэнд виджета SWT Approver
 * */
public class Approver extends SkyService {

    @GET
    @Path("/approve_list")
    public Response getDrawing(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            String username = ctx.getSession().getUserName();
            List<Map<String, String>> my_tasks = new ArrayList<>();
            List<Map<String, String>> group_tasks = new ArrayList<>();
            List<Map<String, String>> tasks = findObjectsWhere(ctx, "Inbox Task", "*",
                    "attribute[Approval Status] == \"None\"" +
                            " && state[Complete] == \"FALSE\"" +
                            " && (from[Project Task].to.name == \"" + username + "\" " +
                            " || from[Candidate Assignee].to.from[Group Member].to.name == \"" + username + "\"" +
                            " || from[Project Task].to.type == \"Route Task User\")",
                    "id",
                    "name",
                    "attribute[Title]:description",
                    "originated:start",
                    "attribute[Scheduled Completion Date]:finish",
                    "from[Route Task].to.name:route",
                    "from[Project Task].to.type:assigner_type",
                    "owner:assigner_name");

            List<Map<String, String>> groupNames = findObjectsWhere(ctx, "Group", "*", "from[Group Member].to.name == \"" + username + "\"", "attribute[Title]:name");
            List<String> groups = new ArrayList<>();
            for (Map<String, String> groupName : groupNames)
                groups.add(groupName.get("name").toLowerCase());

            for (Map<String, String> task : tasks) {
                if (task.get("assigner_type") != null && task.get("assigner_name") != null && task.get("assigner_type").equals("Route Task User") && !groups.contains(task.get("assigner_name").toLowerCase())) {
                    continue;
                }
                task.put("start", printDateFormat.format(dateFormat.parse(task.get("start"))));
                if (task.get("finish") != null && !task.get("finish").equals("") ) {
                    task.put("finish", printDateFormat.format(dateFormat.parse(task.get("finish"))));
                } else {
                    task.put("finish", String.valueOf(Calendar.getInstance().getTime()));
                }
                Map<String, String> route_owner = findObject(ctx, "Inbox Task", task.get("name"),//владелец маршрута
                        "from[Route Task].to.owner:caowner");
                Map<String, String> caowner = findObject(ctx, "Person", route_owner.get("caowner"),
                        "attribute[First Name]:firstname",
                        "attribute[Last Name]:lastname");
                task.put("caowner", caowner.get("firstname").substring(0, 1) + ". " + caowner.get("lastname") + " (" + route_owner.get("caowner") + ")");
                Map<String, String> assigner = new HashMap<>();//                                    //исполнитель задачи
                String caId = scalar(ctx, task.get("id"), "from[Route Task].to.to[Object Route].to.to[Object Route].from.id");
                if (caId != null) {
                    Map<String, String> ca = row(ctx, caId,
                            "name:caname",
//                            "owner:caowner",
                            "current"/*,
                            "description:cadescription"*/);

                    if (ca.get("current").equals("In Approval"))
                        if (task.get("assigner_type").equals("Person")) {
                            if (task.get("assigner_name").equals(username)) {
                                assigner = findObject(ctx, "Person", task.get("assigner_name"),
                                        "attribute[First Name]:firstname",
                                        "attribute[Last Name]:lastname");
                                task.put("assigner_name", assigner.get("firstname").substring(0, 1) + ". " + assigner.get("lastname") + " (" + task.get("assigner_name") + ")");
                                my_tasks.add(task);
                            }
                        } else {
                            group_tasks.add(task);
                        }

                    task.putAll(ca);
                } else {
                    if (task.get("assigner_type") != null && task.get("assigner_name") != null && task.get("assigner_type").equals("Person") && task.get("assigner_name").equals(username)) {
                        assigner = findObject(ctx, "Person", task.get("assigner_name"),
                                "attribute[First Name]:firstname",
                                "attribute[Last Name]:lastname");
                        task.put("assigner_name", assigner.get("firstname").substring(0, 1) + ". " + assigner.get("lastname") + " (" + task.get("assigner_name") + ")");
                        my_tasks.add(task);
                    }
                }
            }

            //  for debug
            username = "al.kuznetsov";
            String koz = "a.kozlovskiy";
            String kir = "s.kirichenko";
            List<Map<String, String>> ERProutes = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && attribute[Route Status] == \"Started\" " +
                            " && owner == \"" + username + "\" || owner == \"" + koz + "\" || owner == \"" + kir + "\"",
                    "id",
                    "name",
                    "owner",
                    "attribute[Route Status]:status",
                    "to[Object Route]:ca_exist");
            List<Map<String, String>> routes = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && attribute[Route Status] == \"Started\" " +
                            " && owner == \"" + username + "\" || owner == \"" + koz + "\" || owner == \"" + kir + "\"",
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
                                " && from[Object Route].to.owner == \"" + username + "\" || from[Object Route].to.owner == \"" + koz + "\" || from[Object Route].to.owner == \"" + kir + "\"",
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
                        "to[Route Task].from.attribute[Title]:description",
                        "to[Route Task].modified:assigned_time",
                        "to[Route Task].from.attribute[Scheduled Completion Date]:finish",
                        "to[Route Task].from.from[Project Task].to:assigner",
                        "to[Route Task].from.from[Project Task].to.attribute[First Name]:assigner_first_name",
                        "to[Route Task].from.from[Project Task].to.attribute[Last Name]:assigner_last_name");
                Map<String, String> first_route_task = route_tasks.get(0);
                route.put("task", first_route_task.get("task"));
                route.put("description", first_route_task.get("description"));
                if (first_route_task.get("assigner_first_name") != null && first_route_task.get("assigner_last_name") != null && !first_route_task.get("assigner_first_name").equals("") && !first_route_task.get("assigner_last_name").equals("")) {
                    route.put("assigner", first_route_task.get("assigner_first_name").substring(0, 1) + ". " + first_route_task.get("assigner_last_name") + " (" + first_route_task.get("assigner") + ")");
                }
                Map<String, String> route_owner = findObject(ctx, "Person", route.get("owner"),
                        "attribute[First Name]:firstname",
                        "attribute[Last Name]:lastname");
                route.put("owner", route_owner.get("firstname").substring(0, 1) + ". " + route_owner.get("lastname") + " (" + route.get("owner") + ")");
                route.put("assigned_time", printDateFormat.format(dateFormat.parse(first_route_task.get("assigned_time"))));
                route.put("finish", printDateFormat.format(dateFormat.parse(first_route_task.get("finish"))));

                if (route.get("status") != null && route.get("status").equals("Stopped")) {
                    route.put("color", "#ffd9d9");
                }
                String r = findScalar(ctx, "*", route.get("name"), "to[Object Route].from.name");//PARENT
                if (r != null && r.length() > 0 && r.startsWith("ERP")) {
                    route.put("parent", r);
                } else {
                    r = findScalar(ctx, "*", route.get("name"), "to[Task Sub Route].from.from[Route Task].to.name");
                    if (r != null && r.length() > 0 && r.startsWith("ERP")) {
                        route.put("parent", r);
                    } else {
                        r = findScalar(ctx, "*", route.get("name"), "to[Task Sub Route].from.from[Route Task].to.to[Object Route].from.name");
                        if (r != null && r.length() > 0 && r.startsWith("ERP")) {
                            route.put("parent", r);
                        } else {
                            route.put("parent", " ");
                        }
                    }
                }
                if (!route.get("parent").equals(" ")) {//ROOT
                    String root = findScalar(ctx, "Task", r, "to[Subtask].from.name");
                    route.put("root", root);
                } else {
                    route.put("root", " ");
                }
            }

            //SORTING
            List<Map<String, String>> ERP = new LinkedList<>();
            Set<String> roots = new TreeSet<>();
            for (Map<String, String> temp : ERProutes) {
                roots.add(temp.get("root"));
            }
            for (String str : roots) {
                for (Map<String, String> temp : ERProutes) {
                    if (str.equals(temp.get("root"))) {
                        ERP.add(temp);
                    }
                }
            }

            for (Map<String, String> route : routes) {
                if ((route.get("ca_exist")).equals("TRUE"))
                    route.put("caname", findScalar(ctx, "Route", route.get("name"), "to[Object Route].from.name"));
                List<Map<String, String>> route_tasks = findRows(ctx, "Route", route.get("name"),
                        "to[Route Task].from.name:task", "to[Route Task].modified:assigned_time",
                        "to[Route Task].from.attribute[Scheduled Completion Date]:finish",
                        "to[Route Task].from.attribute[Title]:description",
                        "to[Route Task].from.from[Project Task].to:assigner",
                        "to[Route Task].from.from[Project Task].to.attribute[First Name]:assigner_first_name",
                        "to[Route Task].from.from[Project Task].to.attribute[Last Name]:assigner_last_name");
                Map<String, String> first_route_task = route_tasks.get(0);
                route.put("task", first_route_task.get("task"));
                route.put("description", first_route_task.get("description"));
                if (first_route_task.get("assigner_first_name") != null && first_route_task.get("assigner_last_name") != null && !first_route_task.get("assigner_first_name").equals("") && !first_route_task.get("assigner_last_name").equals("")) {
                    route.put("assigner", first_route_task.get("assigner_first_name").substring(0, 1) + ". " + first_route_task.get("assigner_last_name") + " (" + first_route_task.get("assigner") + ")");
                }
                Map<String, String> caowner = findObject(ctx, "Person", route.get("owner"),
                        "attribute[First Name]:firstname",
                        "attribute[Last Name]:lastname");
                route.put("owner", caowner.get("firstname").substring(0, 1) + ". " + caowner.get("lastname") + " (" + route.get("owner") + ")");

                route.put("assigned_time", printDateFormat.format(dateFormat.parse(first_route_task.get("assigned_time"))));
                if (first_route_task.get("finish") != null && !first_route_task.get("finish").equals("") ) {
                    route.put("finish", printDateFormat.format(dateFormat.parse(first_route_task.get("finish"))));
                } else {
                    route.put("finish", String.valueOf(Calendar.getInstance().getTime()));
                }

            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("my_tasks", my_tasks);
            result.put("group_tasks", group_tasks);
            result.put("ERP", ERP);
            result.put("routes", routes);
            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

}
