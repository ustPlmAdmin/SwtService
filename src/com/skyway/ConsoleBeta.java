package com.skyway;

import com.matrixone.apps.domain.util.ContextUtil;
import matrix.db.Context;
import matrix.util.MatrixException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

public class ConsoleBeta extends SkyService {

    @GET
    @Path("/console_find")
    public Response console_find(@javax.ws.rs.core.Context HttpServletRequest request,
                                     @QueryParam("name") String name) {
        try {
            Context ctx = authenticate(request);
            List<Map<String, String>> res = new LinkedList<>();

            List<Map<String, String>> find_name = findRows(ctx, "*", name, "current", "revision", "id", "physicalid", "name", "type");
            try {
                List<Map<String, String>> find_id = select(ctx, name, "current", "revision", "id", "physicalid", "name", "type");
                if (find_id != null && find_id.size() > 0) {
                    res.addAll(find_id);
                }
            } catch (Exception ignored) {

            }
            if (find_name != null && find_name.size() > 0) {
                res.addAll(find_name);
            }

            return response(res);
        } catch (Exception e) {
            return errorWithText(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/console_select")
    public Response console_select(@javax.ws.rs.core.Context HttpServletRequest request,
                                    @QueryParam("id") String id) throws MatrixException, IOException {

        Context ctx = internalAuth(request);
        try {

            List<Map<String, String>> info = select(ctx, id, "attribute[*], *");
            List<Map<String, Object>> newList = new LinkedList<>();
            List<String> states = new LinkedList<>();
            StringBuilder history = new StringBuilder();
            for (Map<String, String> item: info) {
                for (String key: item.keySet()) {
                    if (key.equals("state")) {
                        states.add(item.get(key));
                    } else if (key.equals("history")) {
                        history.append(item.get(key)).append(" ").append(System.lineSeparator()).append(" ");
                    }
                }
            }
            Map<String, Object> hist = new LinkedHashMap<>();
            hist.put("key", "history");
            hist.put("value", history);

            for (Map<String, String> item: info) {
                Map<String, Object> temp = new LinkedHashMap<>();
                for (String key: item.keySet()) {
                    if (!key.equals("attribute") && !key.equals("to") && !key.equals("from") &&
                            !key.equals("history") && !key.equals("state") && !key.equals("interface") && !key.equals("relationship")) {
                        temp.put("key", key);
                        temp.put("value", item.get(key));
                        if (key.equals("current")) {
                            temp.put("values", states);
                        }
                        newList.add(temp);
                    }
                }
            }
            newList.add(hist);

            return response(newList);
        } catch (Exception e) {
            return errorWithText(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/console_modify")
    public Response console_modify(@javax.ws.rs.core.Context HttpServletRequest request,
                                   @QueryParam("id") String id,
                                   @QueryParam("attr") String attr,
                                   @QueryParam("value") String value) throws MatrixException, IOException {

        Context ctx = internalAuth(request);
        try {
            if (attr.contains("[")) {
                attr = attr.substring(attr.indexOf("[") + 1, attr.indexOf("]"));
            }

            ContextUtil.startTransaction(ctx, true);
            query(ctx, "mod bus \"" + id + "\"  \"" + attr + "\" \"" + value + "\"");
            ContextUtil.commitTransaction(ctx);


            return response("OK");
        } catch (Exception e) {
            ContextUtil.abortTransaction(ctx);
            return errorWithText(e);
        } finally {
            finish(request);
        }
    }

}
