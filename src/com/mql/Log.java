package com.mql;

import com.matrixone.jsystem.util.ExceptionUtils;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import matrix.db.Context;

public class Log {
    public static LinkedList<String> queries = new LinkedList<>();

    public static LinkedList<String> errors = new LinkedList<>();

    public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    public static Map<String, Integer> request_active = new LinkedHashMap<>();

    public static Map<String, Map<String, Integer>> request_usage = new LinkedHashMap<>();

    public static String currentTime() {
        return formatter.format(new Date(System.currentTimeMillis()));
    }

    public static void requestStart(HttpServletRequest request, Context context) {
        try {
            request_usage.putIfAbsent(request.getRequestURI(), new LinkedHashMap<>());
            Map<String, Integer> usage = request_usage.get(request.getRequestURI());
            Integer usage_by_user = usage.get(context.getSession().getUserName());
            usage.put(context.getSession().getUserName(), Integer.valueOf((usage_by_user == null) ? 1 : (usage_by_user.intValue() + 1)));
            Integer active = request_active.get(request.getRequestURI());
            request_active.put(request.getRequestURI(), Integer.valueOf((active == null) ? 1 : (active.intValue() + 1)));
        } catch (Exception e) {
            request_active = new LinkedHashMap<>();
            request_usage = new LinkedHashMap<>();
        }
    }

    public static void requestFinish(HttpServletRequest request) {
        try {
            Integer active = request_active.get(request.getRequestURI());
            if (active != null)
                if (active.intValue() == 1) {
                    request_active.remove(request.getRequestURI());
                } else {
                    request_active.put(request.getRequestURI(), Integer.valueOf(active.intValue() - 1));
                }
        } catch (Exception e) {
            request_active = new LinkedHashMap<>();
        }
    }

    public static void q(String query, String response) {
        try {
            queries.add(currentTime() + " I: " + query + "\n" + response);
            while (!queries.isEmpty() && queries.size() > 100)
                queries.removeFirst();
        } catch (Exception e) {
            queries = new LinkedList<>();
        }
    }

    public static Response e(Exception exception) {
        try {
            errors.add(currentTime() + " E: " + exception.getClass().getName() + "\n" + exception
                    .getMessage() + "\n" + (
                    (exception.getCause() != null) ? (exception.getCause().getMessage() + "\n") : "") +
                    ExceptionUtils.getStackTrace(exception));
            while (errors.size() > 100)
                errors.removeFirst();
        } catch (Exception e) {
            errors = new LinkedList<>();
        }
        exception.printStackTrace();
        byte[] message = Base64.getDecoder().decode("PGh0bWw+CjxoZWFkPgogICAgPG1ldGEgaHR0cC1lcXVpdj0iQ29udGVudC1UeXBlIiBjb250ZW50PSJ0ZXh0L2h0bWw7IGNoYXJzZXQ9dXRmLTgiPgo8L2hlYWQ+Cjxib2R5Pgo8dGFibGUgd2lkdGg9IjMwMCIgYm9yZGVyPSIwIiBzdHlsZT0icG9zaXRpb246IGFic29sdXRlOyB0b3A6IDUwJTsgbGVmdDogNTAlOyBtYXJnaW4tcmlnaHQ6IC01MCU7IHRyYW5zZm9ybTogdHJhbnNsYXRlKC01MCUsIC01MCUpIj4KICAgIDx0cj4KICAgICAgICA8dGQ+PGgxPk9vb3BzITwvaDE+PC90ZD4KICAgIDwvdHI+CiAgICA8dHI+CiAgICAgICAgPHRkPkludGVybmFsIFNlcnZlciBFcnJvci4gQ29udGFjdCB5b3VyIGFkbWluaXN0cmF0b3IgdG8gcmVzb2x2ZSB0aGlzIGlzc3VlLjwvdGQ+CiAgICA8L3RyPgo8L3RhYmxlPgo8L2JvZHk+CjwvaHRtbD4=");
        Response.ResponseBuilder responseBuilder = Response.ok(message);
        responseBuilder.status(Response.Status.INTERNAL_SERVER_ERROR);
        responseBuilder.header("Content-Type", "text/html");
        return responseBuilder.build();
    }
}
