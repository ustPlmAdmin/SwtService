package com.skyway;

import com.matrixone.apps.domain.util.FrameworkException;
import com.mql.MqlService;
import matrix.db.Context;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.*;
/**
 * Бекэнд виджета SWT ApproverCat
 * */
public class ApproverCat extends SkyService {

    void filterRoutesWithUserTasks(Context ctx, String username, List<Map<String, String>> routes) throws FrameworkException {
        getCa(ctx, routes);
        Iterator<Map<String, String>> iter = routes.iterator();
        while (iter.hasNext()) {
            List<Map<String, String>> tasks = select(ctx, iter.next().get("id"),
                    "to[Route Task].from.current:current",
                    "to[Route Task].from.owner:owner");

            boolean exist = false;
            for (Map<String, String> task : tasks)
                if (task.get("owner").equals(username) && task.get("current").equals("Assigned"))
                    exist = true;
            if (!exist)
                iter.remove();
        }
    }

    void filterRoutesWithUserGroupTasks(Context ctx, String username, List<Map<String, String>> routes) throws FrameworkException {
        getCa(ctx, routes);
        Iterator<Map<String, String>> iter = routes.iterator();
        while (iter.hasNext()) {
            List<Map<String, String>> tasks = select(ctx, iter.next().get("id"),
                    "to[Route Task].from.name:name",
                    "to[Route Task].from.current:current");

            boolean exist = false;
            for (Map<String, String> task : tasks)
                if (task.get("current").equals("Assigned"))
                    if (findList(ctx, "*", task.get("name"), "from[Project Task].to.from[Group Member].to.name").contains(username))
                        exist = true;
            if (!exist)
                iter.remove();
        }
    }

    void filterCompletedRoutes(Context ctx, List<Map<String, String>> routes) throws FrameworkException {
        getCa(ctx, routes);
    }

    private void getCa(Context ctx, List<Map<String, String>> routes) throws FrameworkException {
        for (Map<String, String> route: routes) {
            List<String> ca_name = findList(ctx, "Route", route.get("route_name"), "to[Object Route].from.name:ca_name");
            if (ca_name.size() > 0) {
                for (String ca : ca_name) {
                    if (ca != null && ca.startsWith("ca")) {
                        route.put("ca_name", ca);
                        route.put("ca_owner", findScalar(ctx, "*", ca, "owner"));
                        break;
                    } else {
                        route.put("ca_name", "");
                        route.put("ca_owner", findScalar(ctx, "*", route.get("route_name"), "owner"));
                    }
                }
            } else {
                route.put("ca_name", "");
                route.put("ca_owner", findScalar(ctx, "*", route.get("route_name"), "owner"));
            }
        }
    }

    public void approverList(Context ctx, Object[] arr, String username) throws ParseException, FrameworkException {
        Calendar now = Calendar.getInstance();
        for (Object routesGroup: arr) {
            List<Map<String, String>> routes = (List<Map<String, String>>) routesGroup;
            for (Map<String, String> route : routes) {
                for (Map<String, String> task : findRows(ctx, "*", route.get("route_name"),
                        "to[Route Task].from.name:task_name",
                        "to[Route Task].from.current:task_current",
                        "to[Route Task].from.originated:task_originated",
                        "to[Route Task].from.attribute[Title]:task_comment",
                        "to[Route Task].from.attribute[Scheduled Completion Date]:task_finish",
                        "to[Route Task].from.from[Project Task].to:assigner_id",
                        "to[Route Task].from.from[Project Task].to.attribute[First Name]:assigner_first_name",
                        "to[Route Task].from.from[Project Task].to.attribute[Last Name]:assigner_last_name")) {

                    task.put("task_originated", printDateFormat.format(dateFormat.parse(task.get("task_originated"))));
                    if (task.get("task_finish") != null && !task.get("task_finish").isEmpty()){
                        Calendar finish = Calendar.getInstance();
                        finish.setTime(dateFormat.parse(task.get("task_finish")));
                        if (finish.before(now))
                            task.put("color", "#ffd9d9");
                        else{
                            finish.add(Calendar.DAY_OF_YEAR, -1);
                            if (finish.before(now)) {
                                task.put("color", "#ffd9d9");
                            } else {
                                task.put("color", "#e7ffc3");
                            }
                        }
                        task.put("task_finish", printDateFormat.format(finish.getTime()));
                    }

                    task.putIfAbsent("assigner_id", username);

                    task.put("task_assigner", (task.getOrDefault("assigner_first_name", "") + " "
                            + task.getOrDefault("assigner_last_name", "") + " "
                            + task.get("assigner_id")).trim());

                    if (task.get("assigner_id").equals(username))
                        route.putAll(task);

                    if (task.get("task_current").equals("Assigned")) {
                        route.putAll(task);
                        if (task.get("assigner_id").equals(username))
                            break;
                    }
                }
            }
        }
    }

    @GET
    @Path("/approve_cat_list")
    public Response getRoutes(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            String username = ctx.getSession().getUserName();
            Calendar prevMonth = Calendar.getInstance();
            prevMonth.add(Calendar.MONTH, -1);
            String[] attrs = {
                    "name:route_name",
                    "id"
            };
            // due date
            List<Map<String, String>> routesWithMyActiveTasks = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && attribute[Route Status] == \"Started\" " +
                            " && to[Route Task].from.owner == \"" + username + "\" ",
                    attrs);

            filterRoutesWithUserTasks(ctx, username, routesWithMyActiveTasks);

            List<Map<String, String>> routesWithWaitingTasks = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && attribute[Route Status] == \"Started\" " +
                            " && from[Route Node].to.from[Group Member].to.name == \"" + username + "\" ",
                    attrs);
            filterRoutesWithUserGroupTasks(ctx, username, routesWithWaitingTasks);

            List<Map<String, String>> routesFinishedWithMyTasks = findObjectsWhere(ctx, "Route", "*",
                    "current == \"Complete\"" +
                            " && attribute[Route Status] == \"Finished\" " +
                            " && from[Route Node].to.name == \"" + username + "\" " +
                            " && modified >= \"" + dateFormat.format(prevMonth.getTime()) + "\" ",
                    attrs);
            filterCompletedRoutes(ctx, routesFinishedWithMyTasks);

            Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
            result.put("routesWithMyActiveTasks", routesWithMyActiveTasks);
            result.put("routesWithWaitingTasks", routesWithWaitingTasks);
            result.put("routesFinishedWithMyTasks", routesFinishedWithMyTasks);


            Object[] arr = {routesWithMyActiveTasks, routesWithWaitingTasks, routesFinishedWithMyTasks};

            approverList(ctx, arr, username);

            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }
}
