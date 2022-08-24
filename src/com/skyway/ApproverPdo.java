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
 * Бекэнд виджета SWT ApproverPdo
 * */
public class ApproverPdo extends SkyService {

    @GET
    @Path("/approve_pdo_list")
    public Response getDrawing(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            String username = ctx.getSession().getUserName();


            username = "a.fomenkov";

            List<Map<String, String>> routes = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && owner == \"" + username + "\" " +
                            " && attribute[Route Status] == \"Started\" ",
                    "id",
                    "name",
                    "owner:route_owner",
                    "to[Object Route]:ca_exist");


            for (Map<String, String> route : routes) {
                if (route.get("ca_exist").equals("TRUE"))
                    route.put("ca_name", findScalar(ctx, "Route", route.get("name"), "to[Object Route].from.name"));

                List<Map<String, String>> route_tasks = findRows(ctx, "Route", route.get("name"),
                        "to[Route Task].from.name:task_name",
                        "to[Route Task].from.from[Project Task].to.attribute[First Name]:assigner_first_name",
                        "to[Route Task].from.from[Project Task].to.attribute[Last Name]:assigner_last_name",
                        "to[Route Task].from.from[Project Task].to.originated:task_originated",
                        "to[Route Task].from.attribute[Route Instructions]:task_comment");
                Map<String, String> first_route_task = route_tasks.get(0);
                first_route_task.put("task_originated", printDateFormat.format(dateFormat.parse(first_route_task.get("task_originated"))));
                first_route_task.put("task_assigner", first_route_task.get("assigner_first_name") + " " + first_route_task.get("assigner_last_name"));
                first_route_task.remove("assigner_first_name");
                first_route_task.remove("assigner_last_name");
                route.putAll(first_route_task);
            }






            Map<String, String> ca_names_filtered = new LinkedHashMap<>();
            List<String> ca_names = findObjectsListWhere(ctx, "Change Action", "*", "current == Complete", "name");
            ca_names.add("ca00001957");

            for (String ca_name : ca_names) {
                List<Map<String, String>> proposedActivities = findRows(ctx, "Change Action", ca_name,
                        "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].type:type"
                        , "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].physicalid:id");
                for (Map<String, String> pa : proposedActivities) {
                    if (pa.get("type").equals("VPMReference")) {
                        try {
                            Map<String, String>  prd = row(ctx, pa.get("id"), "name",
                                    "attribute[IGAPartEngineering.IGASpecChapter]:type", "to[VPMInstance]:is_root");
                            if ("Assemblies".equals(prd.get("type")) && prd.get("is_root").equals("FALSE")) {

                                List<Map<String, String>> route_list = findRows(ctx, "*", ca_name,
                                        "from[Object Route].to.type:type",
                                        "from[Object Route].to.owner:owner",
                                        "from[Object Route].to.id:id");

                                for (Map<String, String> route : route_list)
                                    if (route.get("type").equals("Route") && route.get("owner").equals(username)) {
                                        ca_names_filtered.put(ca_name, pa.get("id") + ":" + route.get("id"));
                                        break;
                                    }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            List<Map<String, String>> ca_result = new ArrayList<>();
            for (String ca_name : ca_names_filtered.keySet()) {
                try {
                    String prd_and_route = ca_names_filtered.get(ca_name);
                    String[] prd_and_route_list = prd_and_route.split(":");
                    String prd_id = prd_and_route_list[0];
                    String route_id = prd_and_route_list[1];

                    String ca_owner = findScalar(ctx, "*", ca_name, "owner");
                    Map<String, String> ca = findObject(ctx, "Person", ca_owner,
                            "id",
                            "current:ca_current",
                            "attribute[First Name]:ca_owner_first_name",
                            "attribute[Last Name]:ca_owner_last_name");
                    ca.put("ca_owner", ca.get("ca_owner_first_name") + " " + ca.get("ca_owner_last_name"));
                    ca.put("ca_name", ca_name);
                    ca.put("prd_title", scalar(ctx, prd_id, "attribute[PLMEntity.V_description]"));
                    ca.putAll(row(ctx, route_id,
                            "name:route_name",
                            "current:route_current",
                            "from[Route Node].from.to[Route Task].from.from[Project Task].to.attribute[First Name]:responsible_first_name",
                            "from[Route Node].from.to[Route Task].from.from[Project Task].to.attribute[Last Name]:responsible_last_name",
                            "to[Route Task].from.attribute[Route Instructions]:route_task_comment"));
                    ca.put("route_responsible", ca.get("responsible_first_name") + " " + ca.get("responsible_last_name"));
                    ca.remove("responsible_first_name");
                    ca.remove("responsible_last_name");
                    ca.remove("ca_owner_first_name");
                    ca.remove("ca_owner_last_name");
                    ca_result.add(ca);
                } catch (Exception ignored) {
                }
            }


            Map<String, Object> result = new LinkedHashMap<>();
            result.put("routes", routes);
            result.put("ca", ca_result);

            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }
}
