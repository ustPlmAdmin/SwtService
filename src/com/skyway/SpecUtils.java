package com.skyway;

import com.matrixone.apps.domain.util.FrameworkException;
import com.mql.MqlService;
import matrix.db.Context;

import java.util.*;
/**
 * Утилиты для созадния дерева сборок
 * */
public class SpecUtils extends SkyService {

    void mulChildrenQuantity(Map<String, Object> root, Integer factor) {
        Integer rootQuantity = (Integer) root.get("quantity");
        root.put("quantity", rootQuantity * factor);
        if (root.get("children") != null) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) root.get("children");
            for (Map<String, Object> child : children)
                mulChildrenQuantity(child, rootQuantity * factor);
        }
    }


    public Map<String, Object> recProductNameTree(Context ctx, String name) throws FrameworkException {
        Map<String, Object> all = new LinkedHashMap<>();

        List<String> childrenNames = new ArrayList<>();
        String responseStr = query(ctx, "temp query bus * " + name + " * orderby -revision select id attribute[IGAPartEngineering.IGASpecChapter] from[VPMInstance].to.name;");

        String[] lines = responseStr.split("\n");
        String id = getValue(lines[1]);
        String spec_type = getValue(lines[2]);
        for (int i = 3; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("business"))
                break;
            childrenNames.add(getValue(line));
        }

        List<String> childrenNamesWithoutDuplicates = new ArrayList<>(new HashSet<>(childrenNames));
        List<Map<String, Object>> children = new ArrayList<>();
        for (String childName : childrenNamesWithoutDuplicates) {
            Map<String, Object> child = recProductNameTree(ctx, childName);
            if (child != null) {
                child.put("quantity", Collections.frequency(childrenNames, childName));
                child.put("parent", name);
                children.add(child);
            }
        }
        List<String> sortSeq = new ArrayList<>();
        sortSeq.add("Materials");
        sortSeq.add("OtherComponents");
        sortSeq.add("Buy");
        sortSeq.add("StandardComponents");
        sortSeq.add("Parts");

        children.sort((o1, o2) -> {
            Integer index1 = sortSeq.indexOf((String) o1.get("spec_type"));
            Integer index2 = sortSeq.indexOf((String) o2.get("spec_type"));
            return index1.compareTo(index2);
        });


        all.put("material", getMaterial(ctx, id));
        all.put("id", id);
        all.put("name", name);
        all.put("spec_type", spec_type);
        all.put("quantity", 1);
        all.put("children", children);
        return all;
    }

    public Map<String, Object> getQuantityMap(Context ctx, String assembly, String detail) throws FrameworkException {
        Map<String, Object> result = new LinkedHashMap<>();
//        result.put("quantity", 1);
        List<String> childrenNames = new ArrayList<>();
        String responseStr = query(ctx, "temp query bus * " + assembly + " * orderby -revision select from[VPMInstance].to.name;");
        String[] lines = responseStr.split("\n");
//        String id = getValue(lines[1]);
//        String spec_type = getValue(lines[2]);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
//            if (line.startsWith("business"))
//                break;
            if (line.contains(detail)){
                line = line.substring(32);
                childrenNames.add(line);
            }
        }
        for (String child : childrenNames) {
            if (result.get(child) != null) {
                result.put(child, (Integer) result.get(child) + 1);
            } else {
                result.put(child, 1);
            }
        }

//        result.put("children", childrenNames);

        return result;
    }

    public Map<String, Object> recProducts(Context ctx, String name, boolean addMaterials, String... fields) throws FrameworkException {
        String objectId = findScalar(ctx, "*", name, "id");
        if (objectId == null) return null;
        Map<String, Object> all = tree(ctx, objectId, fields);
        List<String> childrenNames = list(ctx, objectId, "from[VPMInstance].to.name");
        List<String> childrenNamesWithoutDuplicates = new ArrayList<>(new HashSet<>(childrenNames));
        List<Object> children = new ArrayList<>();
        for (String childName : childrenNamesWithoutDuplicates) {
            Map<String, Object> child = recProducts(ctx, childName, addMaterials, fields);
            if (child != null) {
                child.put("quantity", Collections.frequency(childrenNames, childName));
                child.put("parent", name);
                children.add(child);
            }
        }
        all.put("quantity", 1);
        if (addMaterials)
            all.put("material", getMaterial(ctx, objectId));
        all.put("children", children);

        return all;
    }

    private Object getMaterial(Context ctx, String objectId) throws FrameworkException {
        try {
            String name = scalar(ctx, objectId, "name");
            List<String> coreMaterial = paths(ctx, name, "Kit_CoreMaterial");
            if (coreMaterial.size() > 0)
                return scalar(ctx, coreMaterial.get(0), "attribute[PLMEntity.V_Name]");
        } catch (Exception e) {
            return "None";
        }
        /*List<String> types = list(ctx, objectId, "from[VPLMrel/PLMConnection/V_Owner].to.paths[SemanticRelation].path.element[0].type");
        if (types.indexOf("dsc_matref_ref_Core") != -1) {
            List<String> ids = list(ctx, objectId, "from[VPLMrel/PLMConnection/V_Owner].to.paths[SemanticRelation].path.element[0].physicalid");
            String physicalId = ids.get(types.indexOf("dsc_matref_ref_Core"));
            MapList mapList = DomainObject.getInfo(ctx, new String[]{physicalId}, new StringList(new String[]{"id"}));
            Map<String, Object> map = (Map<String, Object>) mapList.get(0);
            return tree(ctx, "" + map.get("id"), "attribute[*]");
        }*/
        return null;
    }
}
