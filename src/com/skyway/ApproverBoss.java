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
            String[] groups = {
                    "Carbody Group",
                    "Chassis Group",
                    "Electric Group",
                    "other"
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
                            " && to[Object Route].from.type == \"Change Action\" ",
                    attrs);

            filterRoutesWithUserGroupTasks(ctx, username, routesWithWaitingTasks);

            Map<String, String[]> members = new HashMap<>();
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

            members.put("m.yudenok", new String[]{ //сотрудники УПС
                    "a.vodopetov",
                    "a.gurinovich",
                    "n.lipskiy",
                    "s.sosinovich",
                    "a.lutsko",
                    "k.tsupikova",
                    "y.nemarovskiy",
                    "o.derbenev",
                    "a.krupets",
                    "a.kostushko",
                    "v.vlasovets",
                    "s.rudko",
                    "d.evsyukov",
                    "m.yudenok",
                    "t.andreev",
                    "p.kakura",
                    "d.kazak",
                    "s.kulapin",
                    "e.kulapina",
                    "s.belonovskiy",
                    "p.poplavskiy",
                    "d.vikhrenko",
                    "s.klyaus",
                    "d.baryshev",
                    "v.valuk",
                    "t.kamyshan",
                    "n.botov",
                    "a.savritskiy",
                    "a.kakhanovich",
                    "yu.tishuk",
                    "al.belov",
                    "e.bikovskiy",
                    "s.zakharova",
                    "y.rakhlei",
                    "o.navitskaya"
            });
            members.put("e.zhichko", new String[]{ //подчинённые Жичко
                    "n.rutkevich",
                    "m.lomako",
                    "o.sakovets",
                    "a.kostomarov",
                    "a.matveychuk",
                    "s.kernoga",
                    "a.voroshkevich",
                    "a.kornetenko",
                    "a.savko",
                    "g.valko"
            });
            members.put("other", new String[]{
                    "i.zhagalskiy",
                    "v.chukhley",
                    "v.kudris",
                    "a.khvalev",
                    "v.yatsenko",
                    "y.shrubok",

                    "y.kashchuk",
                    "i.murog",
                    "a.morozov",
                    "m.kuntsevich",
                    "s.polkhovskiy",
                    "m.kharlamova",
                    "d.fedorenko",
                    "s.dubkov",
                    "a.rutkevich",
                    "a.kurakin",
                    "y.ulezlo",
                    "m.tatarov",
                    "a.kotashevich",
                    "m.mishuk",
                    "a.strizhenkov",
                    "a.kovshirko",
                    "d.tishuk",
                    "s.dyatko",
                    "v.shchurko",
                    "a.symanovich",
                    "v.voronovich",
                    "a.stepanov",
                    "a.savitskiy",
                    "n.lahun",
                    "m.fruzin",
                    "y.badanov",
                    "e.klimovich",
                    "o.kochetov",
                    "d.kuryan",
                    "a.shafalovich",
                    "d.fedorashko",
                    "y.andrianov",
                    "a.poleshchuk"
            });
            members.put("a.pagoda", new String[]{
                    "a.pagoda"
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
            }
            result.put("routesMembers", memberRotes);

            List<Map<String, String>> routesFromRSD = new LinkedList<>();
            for (String group : groups) {
                StringBuilder owners = new StringBuilder();
                if (group.equals("other")) {
                    for (String member: members.get("other"))
                        owners.append(" || owner == \"").append(member).append("\" ");
                } else {
                    List<Map<String, String>> groupMembers = findRows(ctx, "Group", group, "from[Group Member].to:name");
                    for (Map<String, String> member : groupMembers)
                        owners.append(" || owner == \"").append(member.get("name")).append("\" ");
                }
                String ownersStr = owners.substring(3);
                List<Map<String, String>> groupRoutes = findObjectsWhere(ctx, "Route", "*",
                        "current == \"In Process\"" +
                                " && attribute[Route Status] == \"Started\" " +
                                " && (" + ownersStr + ") ",
                        attrs);
                for (Map<String, String> route: groupRoutes) {
                    if (route.containsKey("ca_name") && route.get("ca_name").startsWith("ca")) {
                        if (group.equals("other")) {
                            route.put("group", "other");
                        } else {
                            route.put("group", group);
                        }
                        routesFromRSD.add(route);
                    }
                }
            }

            result.put("routesFromRSD", routesFromRSD);

            Object[] arr = {routesWithUserTasks, memberRotes, routesFromRSD, routesWithWaitingTasks};
            approverList(ctx, arr, username);

            return response(result);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }
}