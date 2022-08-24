package com.skyway;

import com.mql.Log;
import matrix.db.Context;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Общие утилиты
 * */
public class Utils extends SkyService {
    public static final long startTime = new Date().getTime();

    @GET
    @Path("/ping")
    public Response ping(@javax.ws.rs.core.Context HttpServletRequest request) {
        return Response.ok().build();
    }

    @GET
    @Path("/queries")
    public Response getQueries(@javax.ws.rs.core.Context HttpServletRequest request) {
        return Response.ok(collect(Log.queries)).build();
    }

    @GET
    @Path("/errors")
    public Response getLogs(@javax.ws.rs.core.Context HttpServletRequest request) {
        return Response.ok(collect(Log.errors)).build();
    }

    @GET
    @Path("/active")
    public Response getActives(@javax.ws.rs.core.Context HttpServletRequest request) {
        return response(Log.request_active);
    }

    @GET
    @Path("/usage")
    public Response getUsage(@javax.ws.rs.core.Context HttpServletRequest request) {
        return response(Log.request_usage);
    }

    /**
     * Функция нужна для определения когда был последний раз обновлен прод сервер
     * */
    @GET
    @Path("/version")
    public Response getVersion(@javax.ws.rs.core.Context HttpServletRequest request) {
        try {
            List<String> out = new ArrayList<>();
            Context ctx = internalAuth(request);

            String dbversion = scalar(ctx, "print expression SWTVersion select value;");
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            String version = dateFormat.format(new Date(new File(jarPath).lastModified()));
            //query(ctx, "mod program eServiceSystemInformation.tcl add property appVersionSWT value \"" + version + "\";");
            out.add("db version: " + dbversion);
            out.add("jar modification time: " + version);
            out.add("start instance time: " + dateFormat.format(new Date(startTime)));
            return Response.ok(collect(out)).build();
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    /**
     * Получение ссылки на скачивание документа
     * */
    @GET
    @Path("/document")
    public Response download(@javax.ws.rs.core.Context HttpServletRequest request,
                             @QueryParam("name") String name) {
        try {
            Context ctx = internalAuth(request);
            Map<String, String> document = findObject(ctx, "Document", name, "attribute[Title]:title", "id");
            return Response.status(Response.Status.MOVED_PERMANENTLY)
                    .header("Location", getUrl(request, ctx, document.get("id"), document.get("title"), "generic"))
                    .build();
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    /**
     * Функция останавивает выполнение вечных циклов
     * */
    @GET
    @Path("/stop")
    public Response stop(@javax.ws.rs.core.Context HttpServletRequest request) {
        stopTime = new Date().getTime() + 5 * 1000;
        return Response.ok("all requests stopped during next 15 sec").build();
    }

    /**
     * Скачивание документа
     */
    @GET
    @Path("/download")
    public Response download(@javax.ws.rs.core.Context HttpServletRequest request,
                             @QueryParam("objectId") String objectId,
                             @QueryParam("filename") String filename) {
        try {
            Context ctx = internalAuth(request);

            Map<String, String> docs = getDocs(request, ctx, objectId);
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(docs.get(filename));
            HttpResponse response1 = client.execute(get);

            return Response.ok(response1.getEntity().getContent(), response1.getEntity().getContentType().getValue()).build();
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }

    @GET
    @Path("/test")
    public Response test(@javax.ws.rs.core.Context HttpServletRequest request,
                         @QueryParam("first") String first,
                         @QueryParam("second") String second,
                         @QueryParam("third") String third) {
        try {
            Context ctx = internalAuth(request);
            new CAEChangeActionApprove_mxJPO().signAllDocumentsInCA(ctx, findScalar(ctx, "*", first, "physicalid"));
            return response("Success");
        } catch (Throwable e) {
            e.printStackTrace();
            return response("Error");
        } finally {
            finish(request);
        }

    }

    /**
     * Редирект с имени на ссылку по идентификатору
     */
    @GET
    @Path("/open")
    public Response open(@javax.ws.rs.core.Context HttpServletRequest request,
                         @QueryParam("type") String type,
                         @QueryParam("name") String name) {
        try {
            Context ctx = internalAuth(request);
            Map<String, String> obj = findObject(ctx, type, name, "id");
            if (obj.get("id") == null)
                return Log.e(new FileNotFoundException());
            return Response.status(Response.Status.MOVED_PERMANENTLY)
                    .header("Location", getBaseUrl(request) + "/3dspace/common/emxNavigator.jsp?objectId=" + obj.get("id"))
                    .build();
        } catch (Exception ignored) {
            return Response.ok().build();
        } finally {
            finish(request);
        }
    }

    String collect(List<String> out) {
        StringBuilder sb = new StringBuilder();
        if (out != null) {
            for (String line : out)
                sb.insert(0, line.replaceAll("\n", "<br>")).insert(0, "<br><br>");
        }
        return sb.toString();
    }
}
