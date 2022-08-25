package com.skyway;

import matrix.db.Context;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.*;
/**
 * Бекэнд виджета SWT ApproverBoss
 * */
public class ApproverBoss extends ApproverCat {

    @GET
    @Path("/approve_boss_list")
    public Response getRoutes(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            String username =  ctx.getSession().getUserName();

            Calendar prevMonth = Calendar.getInstance();
            prevMonth.add(Calendar.MONTH, -1);
            String[] attrs = {
                    "name:route_name",
                    "id",
                    "to[Object Route].from:ca_name",
                    "to[Object Route].from.owner:ca_owner",
            };
            // due date
            List<Map<String, String>> routesWithUserTasks = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && attribute[Route Status] == \"Started\" " +
                            " && to[Route Task].from.owner == \"" + username + "\" " +
                            " && to[Object Route].from.type == \"Change Action\" ", // && here
                    attrs);

            filterRoutesWithUserTasks(ctx, username, routesWithUserTasks);

            List<Map<String, String>> routesWithWaitingTasks = findObjectsWhere(ctx, "Route", "*",
                    "current == \"In Process\"" +
                            " && attribute[Route Status] == \"Started\" " +
                            " && from[Route Node].to.from[Group Member].to.name == \"" + username + "\" " +
                            " && to[Object Route].from.type == \"Change Action\" ", // && here, // && here
                    attrs);

            filterRoutesWithUserGroupTasks(ctx, username, routesWithWaitingTasks);

            Map<String, String[]> members = new HashMap<>();
// delete below
            members.put("v.ilich", new String[]{
                    "s.sosinovich",
                    "v.valuk",
                    "v.vyrvich",
                    "o.derbenev",
                    "d.evsyukov",
                    "a.kostushko",
                    "a.krupets" ,
                    "a.lutsko",
                    "a.mikulich",
                    "u.nemarovskiy",
                    "s.rudko",
                    "k.tsupikova",
                    "e.chernikov"
            });
//delete above
            members.put("s.sosinovich", new String[]{
                    "v.valuk",
                    "v.vyrvich",
                    "o.derbenev",
                    "d.evsyukov",
                    "a.kostushko",
                    "a.krupets",
                    "a.lutsko",
                    "a.mikulich",
                    "u.nemarovskiy",
                    "s.rudko",
                    "k.tsupikova",
                    "e.chernikov"
            });

            members.put("m.kim", new String[]{
                    "m.gaiduk",
            });
            Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
            result.put("routesWithMyActiveTasks", routesWithUserTasks);
            result.put("routesWithWaitingTasks", routesWithWaitingTasks);
            List<Map<String, String>> memberRotes = new ArrayList<>();

            if (members.get(username) != null){
                StringBuilder whereOwners = new StringBuilder();
                for (String member: members.get(username))
                    whereOwners.append(" || owner == \"").append(member).append("\" ");
                String whereOwnersStr = whereOwners.substring(3);

                memberRotes = findObjectsWhere(ctx, "Route", "*",
                        "current == \"In Process\"" +
                                " && attribute[Route Status] == \"Started\" " +
                                " && (" + whereOwnersStr + ") ",
                        attrs);
                result.put("routesMembers", memberRotes);
            }
            Object[] arr = {routesWithUserTasks, memberRotes, routesWithWaitingTasks};
            approverList(ctx, arr, username);

            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }
}