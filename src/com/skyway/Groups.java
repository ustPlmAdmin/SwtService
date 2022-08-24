package com.skyway;

import com.mql.MqlService;
import matrix.db.Context;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.*;
/**
 * Бекэнд виджета SWT Groups
 * */
public class Groups extends SkyService {

    @GET
    @Path("/groups")
    public Response getGroups(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);

            List<Object> groups = new ArrayList<>();
            List<Map<String, String>> groupIds = findObjects(ctx, "Group", "*", "id");
            for (Map<String, String> groupMap : groupIds) {
                String groupId = groupMap.get("id");
                Map<String, Object> group = tree(ctx, groupId,
                        "name",
                        "description",
                        "attribute[Title]:title",
                        "from[Group Member].to.name:members",
                        "to[Route Node].to.name:routes",
                        "from[Group Assigned Security Context].to.name:context");
                if (group.get("members") instanceof String)
                    group.put("members", new String[]{(String) group.get("members")});
                groups.add(group);
            }
            List<String> userNames = findObjectsList(ctx, "Person", "*", "name");
            List<String> contexts = findObjectsList(ctx, "Security Context", "*", "name");
            Collections.sort(contexts);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groups);
            response.put("user_names", userNames);
            response.put("contexts", contexts);

            return response(response);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/group_select")
    public Response getGroupsSelect(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = internalAuth(request);
            List<Map<String, Object>> result = new ArrayList<>();
            List<Map<String, String>> groups = findObjects(ctx, "Group", "*",
                    "id",
                    "name",
                    "description",
                    "attribute[Title]:title");
            for (Map<String, String> group : groups) {
                String groupId = group.get("id");
                List<Map<String, String>> members = rows(ctx, groupId,
                        "from[Group Member].to.name:name",
                        "from[Group Member].to.id:id");
                Map<String, Object> res_object = new HashMap<>();
                res_object.putAll(group);
                res_object.put("members", members);
                result.add(res_object);
            }
            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    @GET
    @Path("/add_group")
    public Response addGroup(@javax.ws.rs.core.Context HttpServletRequest request,
                             @QueryParam("group_name") @NotNull String group_name,
                             @QueryParam("group_description") @NotNull String group_description,
                             @QueryParam("group_title") @NotNull String group_title,
                             @QueryParam("context") @NotNull String context) {
        try {
            Context ctx = authenticate(request);

            String groupId = findScalar(ctx, "Group", group_name, "id");
            if (groupId != null) {
                query(ctx, "modify bus Group \"" + group_name + "\" - " +
                        " description \"" + group_description + "\"" +
                        " \"Title\" \"" + group_title + "\"");

                String oldSecurityContext = scalar(ctx, groupId, "from[Group Assigned Security Context].to.name");
                query(ctx, "disconnect bus Group \"" + group_name + "\" - " +
                        "relationship \"Group Assigned Security Context\" to " +
                        "\"Security Context\" \"" + oldSecurityContext + "\" -");

                query(ctx, "connect bus Group \"" + group_name + "\" - " +
                        "relationship \"Group Assigned Security Context\" to " +
                        "\"Security Context\" \"" + context + "\" -");

            } else {

                query(ctx, "add bus Group \"" + group_name + "\" - " +
                        " vault \"eService Production\"" +
                        " policy \"Group Proxy\"" +
                        " description \"" + group_description + "\"" +
                        " organization \"SkyWay\"" +
                        " \"Title\" \"" + group_title + "\"");

                query(ctx, "connect bus Group \"" + group_name + "\" - " +
                        "relationship \"Group Assigned Security Context\" to " +
                        "\"Security Context\" \"" + context + "\" -");

            }

            return ok();
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/del_group")
    public Response delGroup(@javax.ws.rs.core.Context HttpServletRequest request,
                             @QueryParam("group_name") String group_name) {
        try {
            Context ctx = authenticate(request);

            query(ctx, "delete bus Group \"" + group_name + "\" - ");

            return ok();
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/add_group_member")
    public Response addGeoupMember(@javax.ws.rs.core.Context HttpServletRequest request,
                                   @QueryParam("user_name") String user_name,
                                   @QueryParam("group_name") String group_name) {
        try {
            Context ctx = authenticate(request);

            query(ctx, "connect bus Group \"" + group_name + "\" - " +
                    " relationship \"Group Member\" to " +
                    " Person \"" + user_name + "\" -");

            return ok();
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/del_group_member")
    public Response delGroupMember(@javax.ws.rs.core.Context HttpServletRequest request,
                                   @QueryParam("user_name") String user_name,
                                   @QueryParam("group_name") String group_name) {
        try {
            Context ctx = authenticate(request);

            query(ctx, "disconnect bus Group \"" + group_name + "\" - " +
                    " relationship \"Group Member\" to " +
                    " Person \"" + user_name + "\" -");

            return ok();
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }
}
