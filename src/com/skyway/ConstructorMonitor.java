package com.skyway;

import com.matrixone.apps.domain.util.FrameworkException;
import matrix.db.Context;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.*;

public class ConstructorMonitor extends SkyService {

    void filterGroupTasks(Context ctx, String username, List<Map<String, String>> routes) throws FrameworkException {
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

    void filterTasks(Context ctx, String username, List<Map<String, String>> routes) throws FrameworkException, ParseException {
        getCa(ctx, routes);
        Calendar now = Calendar.getInstance();
        for (Map<String, String> route : routes) {
            for (Map<String, String> task : findRows(ctx, "*", route.get("task_name"),
                    "current:task_current",
                    "originated:task_originated",
                    "attribute[Title]:task_comment",
                    "attribute[Scheduled Completion Date]:task_finish",
                    "from[Project Task].to:assigner_id",
                    "from[Project Task].to.attribute[First Name]:assigner_first_name",
                    "from[Project Task].to.attribute[Last Name]:assigner_last_name")) {

                task.put("task_originated", printDateFormat.format(dateFormat.parse(task.get("task_originated"))));
                if (task.get("task_finish") != null && !task.get("task_finish").isEmpty()){
                    Calendar finish = Calendar.getInstance();
                    finish.setTime(dateFormat.parse(task.get("task_finish")));
                    if (finish.before(now)) {
                        task.put("color", "#ffd9d9");
                    }
                    task.put("task_finish", printDateFormat.format(finish.getTime()));
                }

                task.putIfAbsent("assigner_id", username);

                task.put("task_assigner", (task.getOrDefault("assigner_first_name", "") + " "
                        + task.getOrDefault("assigner_last_name", "") + " "
                        + task.get("assigner_id")).trim());

                if (task.get("assigner_id").equals(username))
                    route.putAll(task);
            }
        }
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

    List<Map<String, String>> filterMyCA(Context ctx, List<Map<String, String>> cas, String username) throws FrameworkException, ParseException {
        List<Map<String, String>> myCA = new ArrayList<>();
        for (Map<String, String> ca : cas) {
            List<String> routes = findList(ctx, "Change Action", ca.get("ca_name"), "from[Object Route].to.name:route_name");
            if (routes.size() > 0) {
                for (String route : routes) {
                    List<Map<String, String>> route_attrs = findRows(ctx, "*", route, "attribute[Route Status]:route_state", "type", "owner");
                    if (route_attrs.size() > 0) {
                        if (route_attrs.get(0).get("type").equals("Route")
                                && !route_attrs.get(0).get("route_state").equals("Complete")
                                && route_attrs.get(0).get("owner").equals(username)) {
                            List<Map<String, String>> reslist = new ArrayList<>();
                            Map<String, String> temp = new LinkedHashMap<>();
                            temp.put("ca_name", ca.get("ca_name"));
                            temp.put("id", ca.get("id"));
                            temp.put("ca_owner", ca.get("ca_owner"));
                            temp.put("ca_state", ca.get("ca_state"));
                            temp.put("ca_title", findScalar(ctx, "*", ca.get("ca_name"), "attribute[Synopsis]"));
                            temp.put("route_name", route);
                            temp.put("route_state", route_attrs.get(0).get("route_state"));
                            List<String> tasks = findList(ctx, "Route", route, "to[Route Task].from.name:task_name");
                            if (tasks.size() > 0) {
                                for (String task : tasks) {
                                    List<Map<String, String>> task_attrs = findRows(ctx, "Inbox Task", task, "current:task_state",
                                            "originated:task_originated",
                                            "attribute[Title]:task_title",
                                            "from[Route Task].to.attribute[Route Status]:route_state",
                                            "attribute[Title]:task_comment",
                                            "attribute[Scheduled Completion Date]:task_finish",
                                            "from[Project Task].to:assigner_id",
                                            "from[Project Task].to.attribute[First Name]:assigner_first_name",
                                            "from[Project Task].to.attribute[Last Name]:assigner_last_name");
                                    if (!task_attrs.get(0).get("task_state").equals("Complete")) {
                                        temp.put("task_originated", printDateFormat.format(dateFormat.parse(task_attrs.get(0).get("task_originated"))));
                                        if (task_attrs.get(0).get("task_finish") != null && !task_attrs.get(0).get("task_finish").isEmpty()) {
                                            Calendar finish = Calendar.getInstance();
                                            finish.setTime(dateFormat.parse(task_attrs.get(0).get("task_finish")));
                                            temp.put("task_finish", printDateFormat.format(finish.getTime()));
                                        }
                                        temp.put("task_assigner", (task_attrs.get(0).getOrDefault("assigner_first_name", "") + " "
                                                + task_attrs.get(0).getOrDefault("assigner_last_name", "") + " "
                                                + task_attrs.get(0).get("assigner_id")).trim());

                                        temp.put("task_name", task);
                                        temp.put("task_comment", task_attrs.get(0).get("task_comment"));
                                        reslist.add(new LinkedHashMap<>(temp));
                                        myCA.add(reslist.get(reslist.size() - 1));
                                    }
                                }
                            }
                            if (route_attrs.get(0).get("route_state").equals("Stopped") && reslist.size() < 1) {
                                ca.put("ca_name", ca.get("ca_name"));
                                ca.put("ca_state", ca.get("ca_state"));
                                ca.put("ca_title", findScalar(ctx, "*", ca.get("ca_name"), "attribute[Synopsis]"));
                                ca.put("route_name", route);
                                ca.put("route_state", route_attrs.get(0).get("route_state"));
                                ca.put("task_originated", "");
                                ca.put("task_finish", "");
                                ca.put("task_assigner", "");
                                ca.put("task_name", "");
                                ca.put("task_comment", "");
                                myCA.add(ca);
                            }
                        } else if (routes.size() == 1 && !route_attrs.get(0).get("type").equals("Route")) {
                            ca.put("ca_name", ca.get("ca_name"));
                            ca.put("ca_state", ca.get("ca_state"));
                            ca.put("ca_title", findScalar(ctx, "*", ca.get("ca_name"), "attribute[Synopsis]"));
                            ca.put("route_name", "");
                            ca.put("route_state", "");
                            ca.put("task_originated", "");
                            ca.put("task_finish", "");
                            ca.put("task_assigner", "");
                            ca.put("task_name", "");
                            ca.put("task_comment", "");
                            myCA.add(ca);
                        }
                    }
                }
            } else {
                ca.put("ca_name", ca.get("ca_name"));
                ca.put("ca_state", ca.get("ca_state"));
                ca.put("ca_title", findScalar(ctx, "*", ca.get("ca_name"), "attribute[Synopsis]"));
                ca.put("route_name", "");
                ca.put("route_state", "");
                ca.put("task_originated", "");
                ca.put("task_finish", "");
                ca.put("task_assigner", "");
                ca.put("task_name", "");
                ca.put("task_comment", "");
                myCA.add(ca);
            }
        }

        return myCA;
    }

    List<Map<String, String>> filterStandard(Context ctx, List<Map<String, String>> routes) throws FrameworkException, ParseException {
        List<Map<String, String>> standardRoutes = new ArrayList<>();
        if (routes.size() > 0) {
            for (Map<String, String> route : routes) {
                List<Map<String, String>> route_attrs = findRows(ctx, "Route", route.get("route_name"), "attribute[Route Status]:route_state", "from[Initiating Route Template].to:template", "owner");
                if (route_attrs.size() > 0) {
                    if (route_attrs.get(0).get("template") != null && route_attrs.get(0).get("template").length() > 1 &&
                            (route_attrs.get(0).get("template").toLowerCase(Locale.ROOT).contains("стандартн") ||
                            route_attrs.get(0).get("template").toLowerCase(Locale.ROOT).contains("материал") ||
                            route_attrs.get(0).get("template").toLowerCase(Locale.ROOT).contains("покупн"))) {
                        List<Map<String, String>> tasks = findRows(ctx, "Route", route.get("route_name"),
                                "to[Route Task].from.name:task_name",
                                "to[Route Task].from.current:task_state",
                                "to[Route Task].from.originated:task_originated",
                                "to[Route Task].from.attribute[Title]:task_comment",
                                "to[Route Task].from.attribute[Scheduled Completion Date]:task_finish",
                                "to[Route Task].from.from[Project Task].to:assigner_id",
                                "to[Route Task].from.from[Project Task].to.attribute[First Name]:assigner_first_name",
                                "to[Route Task].from.from[Project Task].to.attribute[Last Name]:assigner_last_name");
                        Map<String, String> temp = new LinkedHashMap<>();
                        temp.put("route_name", route.get("route_name"));
                        temp.put("route_owner", route_attrs.get(0).get("owner"));
                        temp.put("id", route.get("id"));
                        Map<String, String> active_task = new LinkedHashMap<>();
                        if (tasks.size() > 0) {
                            for (Map<String, String> task : tasks) {
                                if (!task.get("task_state").equals("Complete")) {
                                    active_task.clear();
                                    active_task.put("task_name", task.get("task_name"));
                                    active_task.put("task_originated", printDateFormat.format(dateFormat.parse(task.get("task_originated"))));
                                    if (task.get("task_finish") != null && !task.get("task_finish").isEmpty()) {
                                        Calendar finish = Calendar.getInstance();
                                        finish.setTime(dateFormat.parse(task.get("task_finish")));
                                        active_task.put("task_finish", printDateFormat.format(finish.getTime()));
                                    }
                                    active_task.put("task_assigner", (task.getOrDefault("assigner_first_name", "") + " "
                                            + task.getOrDefault("assigner_last_name", "") + " "
                                            + task.get("assigner_id")).trim());
                                    active_task.put("task_comment", task.get("task_comment"));
                                    temp.putAll(active_task);
                                    standardRoutes.add(temp);
                                }
                            }
                        }
                        if (active_task.size() < 1) {
                            standardRoutes.add(temp);

                        }
                    }
                }
            }
        }

        return standardRoutes;
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
                    if (task.get("task_finish") != null && !task.get("task_finish").isEmpty()) {
                        Calendar finish = Calendar.getInstance();
                        finish.setTime(dateFormat.parse(task.get("task_finish")));
                        if (finish.before(now)) {
                            task.put("color", "#ffd9d9");
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
    @Path("/constructor_list")
    public Response getRoutes(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            String username = ctx.getSession().getUserName();

            String[] attrs = {
                    "name:route_name",
                    "id"
            };

//            if (username.equals("a.pagoda")) {
//                username = "a.lutsko";
//            }
            List<Map<String, String>> myTasks = findObjectsWhere(ctx, "Inbox Task", "*",
                    "current == \"Assigned\"" +
                            " && from[Route Task].to.current == \"In Process\"" +
                            " && from[Route Task].to.attribute[Route Status] == \"Started\" " +
                            " && from[Project Task].to == \"" + username + "\" ",
                    "from[Route Task].to:route_name", "name:task_name", "from[Route Task].to.id:id");

            filterTasks(ctx, username, myTasks);

            List<Map<String, String>> groupTasks = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && attribute[Route Status] == \"Started\" " +
                            " && from[Route Node].to.from[Group Member].to.name == \"" + username + "\" ",
                    attrs);
            filterGroupTasks(ctx, username, groupTasks);

            List<Map<String, String>> myCA = findObjectsWhere(ctx, "Change Action", "*",
                    "current != \"Complete\"" +
                    " && current != \"Cancelled\"" +
                    " && owner == \"" + username + "\" ",
                    "name:ca_name", "id", "owner:ca_owner", "current:ca_state");
            myCA = filterMyCA(ctx, myCA, username);

            List<Map<String, String>> standard = findObjectsWhere(ctx, "Route", "*",
                    "current != \"Complete\"" +
                            " && owner == \"" + username + "\" ",
                    attrs);
            standard = filterStandard(ctx, standard);


            Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
            result.put("myTasks", myTasks);
            result.put("groupTasks", groupTasks);
            result.put("myCA", myCA);
            result.put("standard", standard);

            Object[] arr = {groupTasks};

            approverList(ctx, arr, username);

            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }
}
