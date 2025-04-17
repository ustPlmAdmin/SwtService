package com.skyway;


import com.dassault_systemes.enovia.changeaction.impl.ProposedActivity;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;
import com.mql.MqlService;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import matrix.util.StringList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static com.matrixone.apps.domain.DomainConstants.*;

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


    @GET
    @Path("/approve_cat_find")
    public Response approve_cat_find(@javax.ws.rs.core.Context HttpServletRequest request,
                                 @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            StringList currentSelects = new StringList() {{
                add("id");
                add("physicalid");
                add(SELECT_TYPE);
                add(SELECT_NAME);
                add(SELECT_REVISION);
                add("current");
                add("attribute[IGAPartEngineering.IGASpecChapter]");
                add("attribute[PLMEntity.V_usage]");
                add("attribute[PLMEntity.V_Name]");
                add("attribute[Change Id]");
            }};

            MapList mapList =  DomainObject.findObjects(ctx,
                    null,   //type
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

            for( Object item : mapList){

                HashMap<String,String> map = (HashMap<String,String>)item;
                map.put("physicalId", map.remove("physicalid"));
                map.put("changeId", map.remove("attribute[Change Id]"));
                map.put("vName", map.remove("attribute[PLMEntity.V_Name]"));
                map.put("vUsage", map.remove("attribute[PLMEntity.V_usage]"));
                map.put("igaSpecChapter", map.remove("attribute[IGAPartEngineering.IGASpecChapter]"));

            }
            return response(mapList);
        } catch (Exception e) {
            return errorWithText(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/approve_cat_clear_interface")
    public Response clear_interface(@javax.ws.rs.core.Context HttpServletRequest request,
                              @QueryParam("id") String id) throws  MatrixException {
        Context ctx = internalAuth(request);
        try {

            DomainObject domainObject = new DomainObject(id);

            ContextUtil.startTransaction(ctx, true);
            domainObject.setAttributeValue(ctx,"Change Id","");
            ContextUtil.commitTransaction(ctx);

            return response("Object deleted ");
        } catch (Exception e) {
            ContextUtil.abortTransaction(ctx);
            return errorWithText(e);
        } finally {
            finish(request);
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
    @Path("/approve_cat_list")
    public Response getRoutes(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            String username = ctx.getSession().getUserName();
            Calendar prevMonth = Calendar.getInstance();
//            prevMonth.add(Calendar.MONTH, -1);
            prevMonth.add(Calendar.DAY_OF_YEAR, 1 -Calendar.getInstance().getTime().getDate());
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
//                    "current == \"Complete\"" +
//                            " && attribute[Route Status] == \"Finished\" " +
//                            " && from[Route Node].to.name == \"" + username + "\" " +
//                            " && modified >= \"" + dateFormat.format(prevMonth.getTime()) + "\" ",
                    "from[Route Node].to.name == \"" + username + "\" " +
                            " && modified >= \"" + dateFormat.format(prevMonth.getTime()) + "\" " +
                            " && to[Route Task].from.current == \"Complete\" " +
                            " || to[Route Task].from.current == \"Assigned\" ",
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
