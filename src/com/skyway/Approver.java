package com.skyway;

import com.mql.MqlService;
import matrix.db.Context;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                    "description",
                    "from[Project Task].to.type:assigner_type",
                    "owner:assigner_name");

            List<Map<String, String>> groupNames = findObjectsWhere(ctx, "Group", "*", "from[Group Member].to.name == \"" + username + "\"", "attribute[Title]:name");
            List<String> groups = new ArrayList<>();
            for (Map<String, String> groupName : groupNames)
                groups.add(groupName.get("name").toLowerCase());

            for (Map<String, String> task : tasks) {
                if (task.get("assigner_type").equals("Route Task User") && !groups.contains(task.get("assigner_name").toLowerCase())) {
                    continue;
                }
                String caId = scalar(ctx, task.get("id"), "from[Route Task].to.to[Object Route].to.to[Object Route].from.id");
                if (caId != null) {
                    Map<String, String> ca = row(ctx, caId,
                            "name:caname",
                            "owner:caowner",
                            "current",
                            "description:cadescription");

                    if (ca.get("current").equals("In Approval"))
                        if (task.get("assigner_type").equals("Person")) {
                            if (task.get("assigner_name").equals(username))
                                my_tasks.add(task);
                        } else {
                            group_tasks.add(task);
                        }


                    Map<String, String> caowner = findObject(ctx, "Person", ca.get("caowner"),
                            "attribute[First Name]:firstname",
                            "attribute[Last Name]:lastname");

                    ca.put("caowner", caowner.get("firstname").substring(0, 1) + ". " + caowner.get("lastname") + " (" + ca.get("caowner") + ")");

                    task.putAll(ca);
                }
            }

            //  for debug
            username = "al.kuznetsov";

            List<Map<String, String>> routes = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && owner == \"" + username + "\" " +
                            " && attribute[Route Status] == \"Started\" ",
                    "id",
                    "name",
                    "owner",
                    "to[Object Route]:ca_exist");


            for (Map<String, String> route : routes) {
                if (route.get("ca_exist").equals("TRUE"))
                    route.put("caname", findScalar(ctx, "Route", route.get("name"), "to[Object Route].from.name"));

                List<Map<String, String>> route_tasks = findRows(ctx, "Route", route.get("name"),
                        "to[Route Task].from.name:task",
                        "to[Route Task].modified:assigned_time",
                        "to[Route Task].from.from[Project Task].to.attribute[First Name]:assigner_first_name",
                        "to[Route Task].from.from[Project Task].to.attribute[Last Name]:assigner_last_name");
                Map<String, String> first_route_task = route_tasks.get(0);
                route.put("task", first_route_task.get("task"));
                route.put("assigner", first_route_task.get("assigner_first_name") + " " + first_route_task.get("assigner_last_name"));
                route.put("assigned_time", printDateFormat.format(dateFormat.parse(first_route_task.get("assigned_time"))));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("my_tasks", my_tasks);
            result.put("group_tasks", group_tasks);
            result.put("routes", routes);
            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

}
