package com.skyway;

import com.matrixone.apps.domain.util.FrameworkException;
import matrix.db.Context;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

public class Monitor extends SkyService {
    @GET
    @Path("/monitor_export")
    public Response getTaskHistory(@javax.ws.rs.core.Context HttpServletRequest request, @QueryParam("erp_name") String erpName) {

        Map<String, Object> result = new LinkedHashMap<>();
        String type;

        try {
            Context ctx = authenticate(request);
            String taskId = findScalar(ctx, "*", erpName, "id");

            if (erpName.startsWith("ERP"))  {
                type = "Task";
            } else if (erpName.startsWith("R")) {
                type = "Route";
            } else {
                type = "Inbox Task";
            }
            result = deti(ctx, type, taskId, erpName);

        } catch (IOException | FrameworkException e) {
            e.printStackTrace();
        }

        return response(result);
    }

    private Map<String, Object> deti(Context ctx, String type, String id, String name) throws FrameworkException {
        List<Map<String, Object>> children = new LinkedList<>();
        Map<String, Object> realChildren = new LinkedHashMap<>();
        Map<String, Object> child = new LinkedHashMap<>();
        child.put("name", name);
        child.put("title", findScalar(ctx, type, name, "attribute[Title]"));
        child.put("status", findScalar(ctx, type, name, "current"));
        String conn = "";
        switch (type) {
            case "Task":
                conn = "from[Object Route].to";
                realChildren = tree(ctx, id, conn);
                type = "Route";
                break;
            case "Route":
                conn = "to[Route Task].from";
                realChildren = tree(ctx, id, conn);
                type = "Inbox Task";
                break;
            case "Inbox Task":
                conn = "from[Task Sub Route].to";
                realChildren = tree(ctx, id, conn);
                type = "Route";
                break;
        }
        for (Object c : realChildren.values()) {
            ArrayList<String> list = new ArrayList<>();
            if (c.getClass().getName().equals("java.lang.String")) {
                if (isValid(String.valueOf(c), type)) {
                    Map<String, Object> deti = deti(ctx, type, findScalar(ctx, type, String.valueOf(c), "id"), String.valueOf(c));
                    if (!deti.isEmpty()) {
                        child.put("children", deti);
                    } else {
                        child.put("children", realChildren.get(conn));
                    }
                }
            } else {
                list = (ArrayList<String>) c;
                for (String str : list) {
                    if (isValid(str, type)) {
                        children.add(deti(ctx, type, findScalar(ctx, type, str, "id"), str));
                    }
                }
                child.put("children", children);
            }
        }

        return child;
    }

    boolean isValid (String name, String type) {
        if (type.equals("Task") && name.startsWith("ERP")) {
            return true;
        } else if (type.equals("Route") && name.startsWith("R-")) {
            return true;
        } else if (type.equals("Inbox Task") && name.startsWith("IT-")) {
            return true;
        } else {
            return false;
        }

    }

}
