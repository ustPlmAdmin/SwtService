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
    List<Map<String, String>> history = new ArrayList<>();
    @GET
    @Path("/monitor_export")
    public Response getTaskHistory(@javax.ws.rs.core.Context HttpServletRequest request, @QueryParam("erp_name") String erpName) {

        history.clear();
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Context ctx = authenticate(request);
            List<Map<String, String>> children = getChild(ctx, erpName);
            history = getInner(ctx, children, erpName);
            result.put("history", history);
        } catch (IOException | FrameworkException e) {
            e.printStackTrace();
        }

        return response(result);
    }

    private List<Map<String, String>> getChild(Context ctx, String parent) throws FrameworkException {
        List<Map<String, String>> child = new LinkedList<>();
        if (parent.startsWith("E")) {
            child = findRows(ctx, "Task", parent, "from[Object Route].to:name", "from[Object Route].to.attribute[Title]:title", "from[Object Route].to.current:status", "from[Object Route].to.id:id");
        } else if (parent.startsWith("R")) {
            child = findRows(ctx, "Route", parent, "to[Route Task].from:name","to[Route Task].from.attribute[Title]:title", "to[Route Task].from.current:status", "to[Route Task].from.id:id");
        } else {
            child = findRows(ctx, "Inbox Task", parent, "from[Task Sub Route].to:name", "from[Task Sub Route].to.attribute[Title]:title", "from[Task Sub Route].to.current:status", "from[Task Sub Route].to.id:id");
        }
        return child;
    }

    private List<Map<String, String>> getInner(Context ctx, List<Map<String, String>> children,  String p) throws FrameworkException {
//        List<Map<String, String>> history = new ArrayList<>();

        if (children.size() > 0) {
            for (Map<String, String> c : children) {
                history.add(c);
                children = getChild(ctx, c.get("name"));
                getInner(ctx, children, c.get("name"));
            }


        }
            return history;
    }

}
