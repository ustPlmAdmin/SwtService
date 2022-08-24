package com.skyway;

import com.matrixone.apps.domain.util.FrameworkUtil;
import matrix.db.*;
import matrix.util.StringList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Класс для получения трудового и материального нормирования и техпроцессов
 * */
public class Norm extends SkyService {

    public static Map<String, StringList> findSRs(Context context, String pid, String ownerType) throws Exception {
        Map<String, StringList> result = new HashMap<>();
        StringList pathSelects = new StringList();
        pathSelects.add("id");
        pathSelects.add("owner.name");
        pathSelects.add("owner.type");
        PathQuery pQuery = new PathQuery();
        pQuery.setPathType("SemanticRelation");
        pQuery.setVaultPattern("vplm");
        PathQuery.QueryKind qKind = PathQuery.CONTAINS;
        ArrayList<String> alCriteria = new ArrayList();
        alCriteria.add(pid);
        pQuery.setCriterion(qKind, alCriteria);
        short sPageSize = 10;
        PathQueryIterator pqItr = pQuery.getIterator(context, pathSelects, sPageSize, new StringList());
        while (pqItr.hasNext()) {
            PathWithSelect pathInfo = pqItr.next();
            String sPathId = pathInfo.getSelectData("id");
            StringList slOwnerNames = pathInfo.getSelectDataList("owner.name");
            StringList slOwnerTypes = pathInfo.getSelectDataList("owner.type");
            if (slOwnerTypes.contains(ownerType))
                result.put(sPathId, slOwnerNames);
        }
        pqItr.close();
        return result;
    }

    public List<String> getImplementedLinks(Context ctx, String sInstPID, String composeeType) throws Exception {
        Map<String, StringList> pathMap = findSRs(ctx, sInstPID, composeeType);
        List<String> owners = new ArrayList<>();
        List<String> excludePathIds = new ArrayList<>();
        pathMap.forEach((k, v) -> {
            excludePathIds.add(k);
            owners.addAll((Collection) v);
        });
        StringList objSelects = new StringList();
        objSelects.add("paths[SemanticRelation].path.id");
        BusinessObjectWithSelectList bowsl = BusinessObject.getSelectBusinessObjectData(ctx, owners.<String>toArray(new String[0]), objSelects, false);
        List<String> pathPIDs = new ArrayList<>();
        bowsl.forEach(bows -> {
            String[] sIDs = bows.getSelectData("paths[SemanticRelation].path.id").split("\007");
            pathPIDs.addAll(Arrays.asList(sIDs));
        });
        pathPIDs.removeIf(excludePathIds::contains);
        return pathPIDs;
    }

    List<Map<String, Object>> gsys(HttpServletRequest request, Context ctx, String name, String barName) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();

        List<String> childrenNames = findList(ctx, "*", name, "from[DELLmiGeneralSystemInstance].to.name");
        for (String childName : childrenNames) {
            Map<String, Object> child = new LinkedHashMap<>(findAttributes(ctx, childName));
            List<Map<String, Object>> children = gsys(request, ctx, childName, barName);
            children.sort((o1, o2) -> {
                String key = "attribute[Kit_MainOp.Kit_NumOperation]";
                if (o1.get(key) == null && o2.get(key) == null) return 0;
                if (o1.get(key) == null && o2.get(key) != null) return 1;
                if (o1.get(key) != null && o2.get(key) == null) return -1;
                return Integer.compare(Integer.valueOf((String) o1.get(key)), Integer.valueOf((String) o1.get(key)));
            });
            child.put("children", children);
            result.add(child);
        }
        List<Map<String, String>> headers = findRows(ctx, "*", name,
                "from[DELLmiHeaderOperationInstance].to.name:name",
                "from[DELLmiHeaderOperationInstance].physicalid:id");
        if (headers.size() > 0) {
            for (Map<String, String> header : headers) {
                String childName = header.get("name");
                String childId = header.get("id");
                Map<String, Object> child = new LinkedHashMap<>(findAttributes(ctx, childName));


                List<Object> mbom = (List<Object>) child.computeIfAbsent("mbom", s -> new ArrayList<>());

                Map<String, Map<String, Object>> objectsWithoutDuplicates = new LinkedHashMap<>();
                for (String pathId : getImplementedLinks(ctx, childId, "MfgProductionPlanning")) {
                    String path = query(ctx, "print path " + pathId + " select element.physicalid element.kind owner.name dump |");
                    List<String> pathdata = FrameworkUtil.split(path, "|");
                    String pathid = pathdata.get(0);
                    String kind = pathdata.get(1);
                    String owner = pathdata.get(2);

                    Map<String, Object> mbomObj = new HashMap<>();

                    if (kind.equals("connection")) {
                        mbomObj.putAll(conn(ctx, pathid));
                    }

                    if (kind.equals("businessobject")) {
                        String mbomName = scalar(ctx, "print bus " + pathid + " select name");
                        mbomObj.putAll(findAttributes(ctx, mbomName));
                    }

                    if (mbomObj.size() != 0) {
                        mbomObj.put("quantity", 1);
                        String objName = (String) mbomObj.get("name");

                        if (objName.startsWith("prodp") || objName.startsWith("kmassy")) {
                            for (String prd : paths(ctx, objName, "Kit_Part", "Kit_Product", "VPMReference")) {
                                for (Map<String, String> obj : select(ctx, prd, "to[VPMInstance].from.name:name", "to[VPMInstance].from.current:current"))
                                    if (obj.get("name").startsWith("ppr") && obj.get("current").equals("RELEASED")) {
                                        mbomObj.put("ppr", obj.get("name"));
                                        break;
                                    }
                                if (mbomObj.get("ppr") != null)
                                    break;
                            }
                        }

                        Map<String, Object> objectInMbom = objectsWithoutDuplicates.get(objName);
                        if (objName.startsWith("scontmat")) {
                            mbom.add(mbomObj);
                        } else if (objectInMbom == null) {
                            objectsWithoutDuplicates.put(objName, mbomObj);
                        } else {
                            objectInMbom.put("quantity", (Integer) objectInMbom.get("quantity") + 1);
                        }


                        if (mbomObj.get("type").equals("Kit_MfgRawMaterial")) {
                            mbomObj.put("roommate", findAttributes(ctx, barName));
                        }
                    }
                }

                mbom.addAll(objectsWithoutDuplicates.values());
                if (child.get("type").equals("Kit_MainOp")) {
                    List<Map<String, String>> resources = new ArrayList<>();
                    for (String phisicalId : paths(ctx, childName)) {
                        String resName = scalar(ctx, phisicalId, "name");
                        if (resName.startsWith("rim") || resName.startsWith("rwk") || resName.startsWith("ncm") || resName.startsWith("assy") || resName.startsWith("prd"))
                            resources.add(findAttributes(ctx, resName));
                    }
                    if (resources.size() > 0)
                        child.put("resources", resources);
                }
                child.put("actions", gsys(request, ctx, childName, barName));
                result.add(child);
            }
        }

        childrenNames = findList(ctx, "*", name, "from[DELWkiInstructionInstance].to.name");
        for (String childName : childrenNames) {
            Map<String, Object> child = new LinkedHashMap<>(findAttributes(ctx, childName));
            List<Map<String, String>> docs = new ArrayList<>();
            for (String phisicalId : paths(ctx, childName)) {
                try {
                    Map<String, String> res = row(ctx, phisicalId, "name", "type");
                    if (res.get("type").equals("Document")) {
                        Map<String, String> docAttrs = findAttributes(ctx, res.get("name"));
                        docAttrs.put("url", getBaseUrl(request) + "/internal/sw/document?name=" + res.get("name"));
                        docs.add(docAttrs);
                    } else {
                        docs.add(findAttributes(ctx, res.get("name")));
                    }
                } catch (Exception ignored) {
                }
            }
            if (docs.size() > 0)
                child.put("docs", docs);
            result.add(child);

        }
        return result;
    }

    @GET
    @Path("/normirovanie")
    public Response getNormirovanieNew(@javax.ws.rs.core.Context HttpServletRequest request,
                                       @QueryParam("task_name") String task_name,
                                       @QueryParam("route_name") String route_name,
                                       @QueryParam("ppr_name") String ppr_name) {
        try {
            Context ctx = internalAuth(request);

            if (route_name != null) {
                List<String> prrs = findRowsList(ctx, "Route", route_name, "to[Object Route].from")
                        .stream().filter(name -> name.startsWith("ppr")).collect(Collectors.toList());
                if (prrs.size() > 0)
                    ppr_name = prrs.get(0);
            }

            if (task_name != null) {
                String taskNotes = findScalar(ctx, "*", task_name, "attribute[Notes]");
                List<Map<String, String>> contents = findRows(ctx, "*", task_name,
                        "from[Object Route].to.to[Route Task].from.from[Task Sub Route].to.to[Object Route].from.attribute[PLMEntity.V_Name]:title",
                        "from[Object Route].to.to[Route Task].from.from[Task Sub Route].to.to[Object Route].from.type:type",
                        "from[Object Route].to.to[Route Task].from.from[Task Sub Route].to.to[Object Route].from.name:name");
                List<Map<String, String>> add = findRows(ctx, "*", task_name,
                        "from[Object Route].to.to[Object Route].from.attribute[PLMEntity.V_Name]:title",
                        "from[Object Route].to.to[Object Route].from.type:type",
                        "from[Object Route].to.to[Object Route].from.name:name");
                contents.addAll(add);
                for (Map<String, String> content : contents) {
                    if (content.get("type").equals("PPRContext") && content.get("title").trim().contains(taskNotes.trim())) {
                        ppr_name = content.get("name");
                        break;
                    }
                }
            }

            Map<String, Object> resultTree = new LinkedHashMap<>();

            List<Map<String, Object>> processes = new ArrayList<>();
            resultTree.put("processes", processes);



            for (String phisycalId : paths(ctx, ppr_name, "Kit_Factory")) {
                String name = scalar(ctx, phisycalId, "name");
                Map<String, Object> child = new LinkedHashMap<>(findAttributes(ctx, name));
                List<String> partIds = paths(ctx, name, "Kit_MfgProducedPart");
                String barName = null;
                if (partIds.size() > 0) {
                    List<String> prodpChildrenNames = list(ctx, partIds.get(0), "from[DELFmiFunctionIdentifiedInstance].to.name");
                    for (String prodpChildrenName : prodpChildrenNames)
                        if (prodpChildrenName.startsWith("bar"))
                            barName = prodpChildrenName;
                }

                child.put("children", gsys(request, ctx, name, barName));
                processes.add(child);
            }

            return response(resultTree);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

}
