package com.skyway;

import com.dassault_systemes.enovia.changeaction.factory.ChangeActionFactory;
import com.dassault_systemes.enovia.changeaction.interfaces.IChangeActionServices;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import matrix.db.Context;

import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * Бекэнд виджета SWT Console
 * */
public class Console extends SpecUtils {

    public Context authenticate(HttpServletRequest request) throws IOException {
        Context context = super.authenticate(request);
        String username = context.getSession().getUserName();
        if (username.equals("m.kim") || username.equals("m.gaiduk") || username.equals("a.pagoda") || username.equals("a.pavlovich"))
            return context;
        throw new AuthenticationException();
    }

    Map<String, Object> rec(Context ctx, String objectId) throws FrameworkException {
        List<Map<String, String>> children = select(ctx, objectId, "from[Subclass].to.id", "from[Subclass].to.name");
        if (children.size() > 0) {
            Map<String, Object> childMap = new LinkedHashMap<>();
            for (Map<String, String> child : children) {
                String childId = child.get("from[Subclass].to.id");
                String childName = child.get("from[Subclass].to.name");
                childMap.put(childName, rec(ctx, childId));
            }
            return childMap;
        } else {
            Map<String, Object> materials = new LinkedHashMap<>();
            List<String> items = list(ctx, objectId, "from[Classified Item].to.id");
            for (String id : items)
                materials.put(id, tree(ctx, id, "attribute[*]"));
            return materials;
        }
    }

    @GET
    @Path("/catalog")
    public Response getCatalog(@javax.ws.rs.core.Context HttpServletRequest request,
                               @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            String objectId = findScalar(ctx, "General Library", name, "id");
            Map<String, Object> catalog = rec(ctx, objectId);
            return response(catalog);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/catalogs")
    public Response getCatalogList(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            Context ctx = authenticate(request);
            List<String> names = new ArrayList<>();
            String[] attrs = {"name", "current"};
            List<Map<String, String>> mes = findObjects(ctx, "General Library", "*", attrs);
            for(Map<String, String> m: mes){
                if (m.get("current").equals("Active"))
                names.add(m.get("name"));
            }
            return response(names);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    @GET
    @Path("/spec_schema")
    public Response getJson(@javax.ws.rs.core.Context HttpServletRequest request,
                            @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            Map<String, Object> items = recProducts(ctx, name, true, "attribute[*]", "current");
            if (items.size() == 0) {
                return page("Object name " + name + " not found", "");
            } else {
                return response(items);
            }
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    public Map<String, String> expand(Context ctx, String id) throws FrameworkException {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            String response = query(ctx, "expand bus " + id + " select bus id;");
            String[] lines = response.split("\n");
            for (int i = 0; i < lines.length / 2; i++) {
                String first = lines[i * 2];
                String tnr = first.substring(first.indexOf(" to ") + 5);
                String revision = tnr.substring(tnr.lastIndexOf(" ") + 1);
                tnr = tnr.substring(0, tnr.lastIndexOf(" ") - 1);
                String name = tnr.substring(tnr.lastIndexOf(" ") + 1);
                String type = tnr.substring(0, tnr.lastIndexOf(" ") - 1);
                tnr = "'" + type + "' " + name + " " + revision;
                String second = lines[i * 2 + 1];
                result.put(tnr, getValue(second));
            }
        } catch (Exception e) {
           error(e);
        }
        return result;
    }

    @GET
    @Path("/ca_delete_rels")
    public Response ca_delete_rels(@javax.ws.rs.core.Context HttpServletRequest request,
                                   @QueryParam("name") String ca_name) {
        try {
            Context ctx = authenticate(request);
            String id = findScalar(ctx, "Change Action", ca_name, "id");
            int deleted = 0;
            Map<String, String> map = expand(ctx, id);
            for (String tnr : map.keySet()) {
                List<String> rels = list(ctx, map.get(tnr), "paths.path.element[0].physicalid");
                for (String physicalId : rels) {
                    try {
                        scalar(ctx, physicalId, "owner");
                    } catch (FrameworkException e) { // phisicalid dosent exist
                        /*try {*/
                            query(ctx, "disconnect bus " + tnr + " from " + id);
                            deleted += 1;
                        /*} catch (Exception ss){
                            ss.printStackTrace();
                        }*/
                    }
                }
            }
            Map<String, Integer> response = new LinkedHashMap<>();
            response.put("deleted", deleted);
            return response(response);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/ca_delete_docs")
    public Response ca_delete_docs(@javax.ws.rs.core.Context HttpServletRequest request,
                                   @QueryParam("name") String root_ca_name) {
        try {
            Context ctx = authenticate(request);

            int deleted = 0;

            List<Map<String, String>> archdocs = findRows(ctx, "*", root_ca_name,
                    "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].type:type",
                    "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].physicalid:physicalid");
            for (Map<String, String> archdoc : archdocs)
                if ("ARCHDocument".equals(archdoc.get("type"))) {

                    List<String> ca_names = findObjectsListWhere(ctx, "Change Action", "*",
                            "current == Cancelled && from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].physicalid == " + archdoc.get("physicalid"),
                            "name");


                    for (String ca_name : ca_names) {
                        List<Map<String, String>> paths = findRows(ctx, "*", ca_name,
                                "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].type:type",
                                "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].physicalid:physicalid",
                                "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].pathid:id");
                        for (Map<String, String> path : paths)
                            if ("ARCHDocument".equals(path.get("type")) && archdoc.get("physicalid").equals(path.get("physicalid"))) {
                                query(ctx, "del path " + path.get("id"));
                                deleted += 1;
                            }
                    }
                }

            Map<String, Integer> response = new LinkedHashMap<>();
            response.put("deleted", deleted);
            return response(response);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    void step(Context ctx, String base, Map<String, Context> ownersContexts, String objectId, String mode) throws Exception {

        Map<String, String> object = row(ctx, objectId,
                "physicalid",
                "type",
                "current",
                "name",
                "owner",
                "project",
                "reserved",
                "reservedby",
                "attribute[PLMEntity.V_description]:desc");

        if (mode.equals("prepare") && !object.get("desc").contains("@")
            || mode.equals("restore") && object.get("desc").contains("@")) {

            boolean isBlockedByChangeControl = list(ctx, objectId, "interface").contains("Change Control");

            DomainObject subObject = DomainObject.newInstance(ctx, objectId);

            try {

                if (object.get("reserved").equals("TRUE") && !object.get("reservedby").equals("m.kim")) {
                    /*Context reservedContext = ownersContexts.get(object.get("reservedby"));
                    if (reservedContext == null) {
                        List<String> accesses = findList(ctx, "Person", object.get("reservedby"), "from[Assigned Security Context].to");
                        List<String> accessList = new ArrayList<>();
                        accessList.add("VPLMViewer");
                        accessList.add("VPLMCreator");
                        accessList.add("VPLMProjectAdministrator");
                        accessList.add("VPLMProjectLeader");
                        accessList.add("VPLMAdmin");
                        String bigAccess = null;
                        for (String access : accessList) {
                            String accessName = access + ".SkyWay." + object.get("project");
                            if (accesses.contains(accessName))
                                bigAccess = accessName;
                        }

                        if (bigAccess == null)
                            throw new NullPointerException();

                        reservedContext = internalAuth(base, object.get("reservedby"), "ctx::" + bigAccess);
                        ownersContexts.put(object.get("reservedby"), reservedContext);
                    }*/
                    subObject.unreserve(ctx);
                    subObject.reserve(ctx, "123");
                }

                if (isBlockedByChangeControl) {
                    List<String> pids = new ArrayList<>();
                    pids.add(object.get("physicalid"));
                    IChangeActionServices iChangeActionServices = ChangeActionFactory.CreateChangeActionFactory();
                    ContextUtil.startTransaction(ctx, true);
                    iChangeActionServices.unsetChangeControlFromPidList(ctx, pids);
                    ContextUtil.commitTransaction(ctx);
                }

                if (!object.get("current").equals("IN_WORK"))
                    query(ctx, "mod bus " + objectId + " current IN_WORK");

                query(ctx, "mod bus " + objectId + " owner m.kim");

                try {
                    if (mode.equals("prepare"))
                        query(ctx, "mod bus " + objectId + " PLMEntity.V_description \"" + object.get("desc") + "@" + object.get("name") + "\"");
                    if (mode.equals("restore")){
                        String resDesc = object.get("desc").substring(0, object.get("desc").indexOf("@"));
                        query(ctx, "mod bus " + objectId + " PLMEntity.V_description \"" + resDesc + "\"");
                    }
                } catch (Exception e) {
                    throw new NullPointerException();
                }


            } finally {

                if (!object.get("current").equals("IN_WORK"))
                    query(ctx, "mod bus " + objectId + " current " + object.get("current"));

                query(ctx, "mod bus " + objectId + " owner " + object.get("owner"));

                if (isBlockedByChangeControl) {
                    List<String> pids = new ArrayList<>();
                    pids.add(object.get("physicalid"));
                    IChangeActionServices iChangeActionServices = ChangeActionFactory.CreateChangeActionFactory();
                    ContextUtil.startTransaction(ctx, true);
                    iChangeActionServices.setChangeControlFromPidList(ctx, pids);
                    ContextUtil.commitTransaction(ctx);
                }

                if (object.get("reserved").equals("TRUE") && !object.get("reservedby").equals("m.kim")) {
                    subObject.unreserve(ctx);
                    /*Context reservedContext = ownersContexts.get(object.get("reservedby"));
                    subObject.reserve(reservedContext, "123");*/
                }
            }
        }

        List<String> childrenIds = list(ctx, objectId, "from[VPMInstance].to.id");
        for (String childId : childrenIds)
            step(ctx, base, ownersContexts, childId, mode);
    }


    @GET
    @Path("/step_prepare")
    public Response step_prepare_request(@javax.ws.rs.core.Context HttpServletRequest request,
                                         @QueryParam("name") String prd_name) {
        try {
            Context ctx = internalAuth(request);
            String base = getBaseUrl(request);
            String prdId = findScalar(ctx, "*", prd_name, "id");
            Map<String, Context> ownersContexts = new HashMap<>();
            step(ctx, base, ownersContexts, prdId, "prepare");
            Object tree = recProducts(ctx, prd_name, false, "id", "name", "attribute[PLMEntity.V_description]", "owner");
            return response(tree);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    @GET
    @Path("/")
    public Response step_restore_request(@javax.ws.rs.core.Context HttpServletRequest request,
                                         @QueryParam("name") String prd_name) {
        try {
            Context ctx = internalAuth(request);
            String base = getBaseUrl(request);
            String prdId = findScalar(ctx, "*", prd_name, "id");
            Map<String, Context> ownersContexts = new HashMap<>();
            step(ctx, base, ownersContexts, prdId, "restore");
            Object tree = recProducts(ctx, prd_name, false, "id", "name", "attribute[PLMEntity.V_description]", "owner");
            return response(tree);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/sign_pdf")
    public Response test(@javax.ws.rs.core.Context HttpServletRequest request,
                         @QueryParam("name") String ca) {
        try {
            Context ctx = internalAuth(request);
            String temp = findScalar(ctx, "*", "ERP Connect Order", "id");
            System.out.println(temp);
            new CAEChangeActionApprove_mxJPO().signAllDocumentsInCA(ctx, findScalar(ctx, "*", ca, "physicalid"));
            return response("OK");
        } catch (Exception e) {
            e.printStackTrace();
            return error(e);
        } finally {
            finish(request);
        }

    }

}
