package com.skyway;

import com.dassault_systemes.platform.ven.apache.commons.net.util.Base64;
import com.matrixone.apps.common.InboxTask;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

import javax.servlet.http.*;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.matrixone.apps.program.Task;
import org.apache.xmlbeans.impl.piccolo.util.DuplicateKeyException;

/**
 * Класс по созданию заказа с 1С
 * */
public class Orders extends SkyService {

    public Context auth(HttpServletRequest request, String username) throws Exception {
        String BASIC = "Basic ";
        String auth = request.getHeader("Authorization");

        if (auth == null || !auth.startsWith(BASIC))
            return null;

        String credentials = new String(Base64.decodeBase64(auth.substring(BASIC.length())), StandardCharsets.UTF_8);
        int p = credentials.indexOf(":");
        if (p == -1)
            return null;

        String login = credentials.substring(0, p);
        String password = credentials.substring(p + 1);

        if (!login.equals("Test") || !password.equals("123"))
            return null;

        return internalAuth(getBaseUrl(request), username, null);
    }

    class TaskCreateRequest {
        String task_name;
        String task_title;
        String task_assignee;
        String task_description;
        String parent_task_name;
        String erp_product_name;
        String product_type;
    }

    @POST
    @Path("/task/create")
    public Response createTask(@javax.ws.rs.core.Context HttpServletRequest request) throws Exception {
        Context ctx = auth(request, "m.kim");

        try {

            TaskCreateRequest taskProperties = (TaskCreateRequest) getRequestObject(request, TaskCreateRequest.class);

            if (ctx == null)
                return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();

            if (taskProperties.task_name == null || taskProperties.task_name.isEmpty() || taskProperties.task_name.contains("\""))
                throw new NullPointerException();

            if (taskProperties.task_title == null || taskProperties.task_title.isEmpty())
                throw new NullPointerException();

            if (taskProperties.task_assignee == null || taskProperties.task_assignee.isEmpty())
                throw new NullPointerException();

            if (taskProperties.task_description == null || taskProperties.task_description.isEmpty())
                throw new NullPointerException();

            if (taskProperties.parent_task_name != null) {
                if (taskProperties.erp_product_name == null || taskProperties.erp_product_name.isEmpty())
                    throw new NumberFormatException();
            }

            Map<String, String> basicTaskInfoMap = new HashMap();
            Map<String, String> taskAttributeMap = new HashMap();
            Map<String, String> relatedInfoMap = new HashMap();

            if (taskProperties.parent_task_name == null) {
                basicTaskInfoMap.put("type", "Phase");
                taskProperties.parent_task_name = "ERP Connect Order";
            } else {
                basicTaskInfoMap.put("type", "Task");
                if (taskProperties.product_type == null || taskProperties.product_type.isEmpty())
                    throw new NullPointerException();
            }
            if (taskProperties.task_name == null || taskProperties.task_name.isEmpty()) {
                basicTaskInfoMap.put("AutoName", "true");
            } else {
                String owner = findScalar(ctx, "*", taskProperties.task_name, "owner");
                if (owner != null)
                    throw new DuplicateKeyException(taskProperties.task_name + " is not unique");
                basicTaskInfoMap.put("name", taskProperties.task_name);
            }

            if (taskProperties.parent_task_name != null){
                String parentId = findScalar(ctx, "*", taskProperties.parent_task_name, "id");
                if (parentId == null)
                    throw new NullPointerException();
                basicTaskInfoMap.put("ParentId", parentId);
                basicTaskInfoMap.put("selectedObjectId", parentId);
            }
            basicTaskInfoMap.put("policy", "Project Task");
            basicTaskInfoMap.put("description", taskProperties.task_description);
            basicTaskInfoMap.put("HowMany", "1");

            basicTaskInfoMap.put("AddTask", "addTaskBelow");
            taskAttributeMap.put("Duration", "1");
            taskAttributeMap.put("DurationUnit", "d");
            taskAttributeMap.put("TaskConstraintType", "As Soon As Possible");
            taskAttributeMap.put("NeedsReview", "No");
            taskAttributeMap.put("TaskRequirement", "Optional");
            taskAttributeMap.put("Project Role", "");
            taskAttributeMap.put("TaskConstraintDate", "");
            taskAttributeMap.put("DurationKeywords", null);

            String ownerId = "";
            String assigneeId = "";
            if ("assembly".equals(taskProperties.product_type)) {
                ownerId = findScalar(ctx, "Person", "al.kuznetsov", "id");
                assigneeId = findScalar(ctx, "Person", taskProperties.task_assignee, "id");
            } else {
                ownerId = findScalar(ctx, "Person", taskProperties.task_assignee, "id");
                assigneeId = ownerId;
            }
            relatedInfoMap.put("Owner", ownerId);
            relatedInfoMap.put("Assignee", assigneeId);
            relatedInfoMap.put("Calendar", "");
            relatedInfoMap.put("deliverableId", null);

            Task task = new Task();
            task = task.createTask(ctx,
                    basicTaskInfoMap,
                    taskAttributeMap,
                    relatedInfoMap);

            task.promote(ctx);

            if (taskProperties.product_type == null || taskProperties.product_type.isEmpty()) {
                query(ctx, "mod bus " + task.getObjectId() +
                        " Title \"" + taskProperties.task_title + "\"" +
                        (taskProperties.erp_product_name != null ? " Notes \"" + taskProperties.erp_product_name + "\"" : "")
                );
            } else if (taskProperties.product_type.equals("assembly")) {
                query(ctx, "mod bus " + task.getObjectId() +
                        " Title \"" + taskProperties.task_title + "\"" +
                        (taskProperties.erp_product_name != null ? " Notes \"" + taskProperties.erp_product_name + "\"" : "") +
                        " \"Product Type\" \"" + taskProperties.product_type + "\"" +
                        " \"Route Owner\" \"" + taskProperties.task_assignee + "\"" +
                        " \"IT assignee\" \"" + taskProperties.task_assignee + "\""
                );
            } else if (taskProperties.product_type.equals("detail")) {
                query(ctx, "mod bus " + task.getObjectId() +
                        " Title \"" + taskProperties.task_title + "\"" +
                        (taskProperties.erp_product_name != null ? " Notes \"" + taskProperties.erp_product_name + "\"" : "") +
                        " \"Product Type\" \"" + taskProperties.product_type + "\"" +
                        " \"Route Owner\" \"" + taskProperties.task_assignee + "\"" +
                        " \"IT assignee\" \"" + taskProperties.task_assignee + "\""
                );
            }

            query(ctx, "mod bus \"" + task.getObjectId() + "\" current \"Review\"");

            Map<String, String> result = new LinkedHashMap<>();
            result.put("task_name", task.getName());

            routeCreate(ctx);

            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    @GET
    @Path("/order_tasks")
    public Response orderTasks(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);

            List<Map<String, String>> tasks = findObjectsWhere(ctx, "Task", "*", "attribute[Notes] != ''",
                    "id:task_id",
                    "name",
                    "attribute[Title]:title",
                    "attribute[Notes]:order",
                    "from[Object Route].to:route",
                    "from[Object Route].to.id:route_id",
                    "attribute[Task Estimated Start Date]:start",
                    "attribute[Task Estimated Finish Date]:finish",
                    "from[Task Deliverable].to:result",
                    "from[Task Deliverable].to.id:result_id");
            for (Map<String, String> task : tasks) {
                task.put("assigners", String.join(",", findRowsList(ctx, "*", task.get("name"), "to[Assigned Tasks].from")));
                task.put("start", printDateFormat.format(dateFormat.parse(task.get("start"))));
                task.put("finish", printDateFormat.format(dateFormat.parse(task.get("finish"))));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tasks", tasks);

            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/create_routes")
    public Response routeRq(@javax.ws.rs.core.Context HttpServletRequest request/*, Context ctx*/) {
        try {
            Context ctx = authenticate(request);
            return routeCreate(ctx);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    public Response routeCreate(Context ctx) {
       try {
           List<String> task_ids = findObjectsListWhere(ctx, "Task", "*", "attribute[Notes] != ''", "physicalid");
           for (String task_id : task_ids) {
               Map<String, String> task = row(ctx, task_id, "from[Object Route].to");
               String r = row(ctx, task_id, "from[Object Route].to").get("from[Object Route].to");
               String product_type = row(ctx, task_id, "attribute[Product Type]").get("attribute[Product Type]");
               String owner = row(ctx, task_id, "attribute[Route Owner]").get("attribute[Route Owner]");
               String person = row(ctx, task_id, "attribute[IT assignee]").get("attribute[IT assignee]");
               if (task.get("from[Object Route].to") == null && !product_type.equals("") && product_type != null && !product_type.equals("null") && !owner.equals("") && !person.equals("")) {

                   String[] args;
                   switch (product_type) {
                       case "assembly":
                           args = new String[]{task_id, null, "56A6EB96000086B06081698B0000005E"};
                           break;
                       case "detail":
                           args = new String[]{task_id, null, "56A6EB9600003FA46022334700000334"};
                           break;
                       default:
                           throw new NullPointerException();
                   }

                   String routeId = JPO.invoke(ctx, "emxLifecycle", new String[0], "createRouteFromTemplate", args, String.class);

                   Route route = new Route(routeId);
                   route.getRouteTasks(ctx, new StringList(), new StringList(), null, false);
                   route.promote(ctx);
                   query(ctx, "mod bus \"" + routeId + "\" \"Route Completion Action\" \"Promote Connected Object\"");

                   String itId = row(ctx, route.getObjectId(), "to[Route Task].from.physicalid").get("to[Route Task].from.physicalid");
                   InboxTask it = (InboxTask) DomainObject.newInstance(ctx, itId);
                   it.setOwner(ctx, person);
                   route.setOwner(ctx, owner);
                   List<String> connectionId = findObjectsListWhere(ctx, "*", it.getName(), "", "relationship[Project Task].physicalid");
                   query(ctx, "mod connection \"" + connectionId.get(0) + "\" to Person " + person + " - from " + it.getId(ctx));

               }
           }
       } catch (Exception e) {
           return error(e);
       }

       return Response.ok().build();
    }

}
