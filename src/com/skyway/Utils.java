package com.skyway;

import com.dassault_systemes.enovia.e6wv2.foundation.FoundationException;
import com.dassault_systemes.enovia.e6wv2.foundation.ServiceJson;
import com.dassault_systemes.enovia.e6wv2.foundation.db.MqlUtil;
import com.dassault_systemes.enovia.e6wv2.foundation.jaxb.Dataobject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.mql.Log;
import matrix.db.Context;
import matrix.db.MQLCommand;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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


    /*
     * Исправляет кодировку в emxFrameworkStringResource_en_Custom.properties
     */
    @GET
    @Path("/fix_encoding")
    public Response chFrontEncoding (@javax.ws.rs.core.Context HttpServletRequest request, @QueryParam("prj_id")  String prj_id) {

        String caData = new String();
        try {
            //  Context ctx = authWithSession("https://3dspace-m001.sw-tech.by:444/3dspace/", request.getCookies()[0].getValue(), "m.kim", "ctx::VPLMCreator.SkyWay.Common Space");
            Context ctx = authenticate(request);
            StringBuffer finalData = new StringBuffer();
            caData = MqlUtil.mqlCommand(ctx, "list page $1 select content","emxFrameworkStringResource_en_Custom.properties");
            caData = caData.substring(caData.indexOf("emxFramework."));
            for(String line : caData.split("((?=emxFramework\\.))")){
                for (String hex : line.split("\\\\u")) {
                    if (hex.substring(0, 4).matches("^[0-9a-fA-F]{4}$")) {
                        char out_ = (char) Integer.parseInt(hex.substring(0, 4), 16);
                        StringBuffer sb = new StringBuffer();
                        sb.append(out_);
                        line = line.replace("\\u" + hex.substring(0, 4), new String() out_);
                    }
                }
                line = line.replace("\n","");
                line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                finalData.append(line + "\n");
            }
            caData = "#Parameterization NLS information \n" + finalData.toString();
            MqlUtil.mqlCommand(ctx, "mod page $1 content $2","emxFrameworkStringResource_en_Custom.properties",caData);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return Response.ok(caData).build();

    }


    /**
     * Блок для пересчета (удаление) несуществующих тасков в списке атрибута ProjectSequence
     */
    @GET
    @Path("/recal_projectsequence")
    public Response reseqPrj(@javax.ws.rs.core.Context HttpServletRequest request, @QueryParam("prj_id")  String prj_id){

        try {
            Context ctx = authenticate(request);
            String prj_pal_id = scalar(ctx,prj_id,"to[Project Access List].from.id");
            if( prj_pal_id != null) {
                String var5 = MqlUtil.mqlCommand(ctx, true, "print bus $1 select $2 dump", true, new String[]{prj_pal_id, "attribute[ProjectSequence]"});
                if (!var5.isEmpty()) {
                    String var7 = null;
                    try {
                        var7 = unzip(var5);
                    } catch (IOException var9) {
                        var9.printStackTrace();
                    }
                    if (!var7.isEmpty()) {
                        List<String> physIds = list(ctx, prj_pal_id, "from[].to.physicalid");
                        Dataobject objs = ServiceJson.readDataobjectfromJson(var7);
                        delBusbyId(ctx, objs.getChildren(), physIds);
                        String var8 = ServiceJson.generateJsonStringfromJAXB(objs);

                        if (var8 != null && !var8.isEmpty()) {
                            try {
                                var8 = zip(var8);
                            } catch (IOException var14) {
                                var14.printStackTrace();
                            }
                        }
                        String var6 = "modify bus $1 $2 $3";
                        try {
                            ContextUtil.startTransaction(ctx, true);
                            (new MQLCommand()).executeCommand(ctx, false, false, var6, new String[]{prj_pal_id, "ProjectSequence", var8});
                        } catch (Exception var12) {
                            var12.printStackTrace();
                            ContextUtil.abortTransaction(ctx);
                        } finally {
                            ContextUtil.commitTransaction(ctx);
                        }
                    }
                }
            }
            return Response.ok().build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /* в ручном режиме */
    @GET
    @Path("/unzip")
    public Response unzip(@javax.ws.rs.core.Context HttpServletRequest request, String zip) {

        String var7 = "";
        try {
            var7 = unzip(zip);
        } catch (IOException var9) {
            var9.printStackTrace();
        }
        return Response.ok(var7).build();
    }


    @GET
    @Path("/zip")
    public Response zip(@javax.ws.rs.core.Context HttpServletRequest request, String json) {

        Dataobject var6 = ServiceJson.readDataobjectfromJson(json);
        String var8 = ServiceJson.generateJsonStringfromJAXB(var6);

        if (var8 != null && !var8.isEmpty()) {
            try {
                var8 = zip(var8);
            } catch (IOException var14) {
                var14.printStackTrace();
            }
        }
        return Response.ok(var8).build();
    }



    private void delBusbyId(Context ctx , List<Dataobject> dObjs,  List<String> phyIds){

        for (Iterator<Dataobject> iter = dObjs.iterator(); iter.hasNext();){
            Dataobject dObj = iter.next();
            try {
                MqlUtil.mqlCommand(ctx, true, "print bus $1 select $2 ", true, new String[]{dObj.getId(), "id"});
                phyIds.stream().filter(id -> id.equals(dObj.getId())).findAny().orElseThrow(() -> { return new FoundationException(" not found");});
            } catch (FoundationException e) {
                iter.remove();
            } finally {
                delBusbyId(ctx,  dObj.getChildren(), phyIds);
            }

        }
    }

    private static String zip(String var0) throws IOException {
        ByteArrayOutputStream var1 = new ByteArrayOutputStream();
        Throwable var2 = null;

        String var32;
        try {
            GZIPOutputStream var3 = new GZIPOutputStream(var1);
            Throwable var4 = null;

            try {
                var3.write(var0.getBytes(StandardCharsets.UTF_8));
            } catch (Throwable var27) {
                var4 = var27;
                throw var27;
            } finally {
                if (var3 != null) {
                    if (var4 != null) {
                        try {
                            var3.close();
                        } catch (Throwable var26) {
                            var4.addSuppressed(var26);
                        }
                    } else {
                        var3.close();
                    }
                }

            }

            Base64.Encoder var31 = Base64.getEncoder();
            var32 = var31.encodeToString(var1.toByteArray());
        } catch (Throwable var29) {
            var2 = var29;
            throw var29;
        } finally {
            if (var1 != null) {
                if (var2 != null) {
                    try {
                        var1.close();
                    } catch (Throwable var25) {
                        var2.addSuppressed(var25);
                    }
                } else {
                    var1.close();
                }
            }

        }

        return var32;
    }

    private static String unzip(String var0) throws IOException {
        Base64.Decoder var1 = Base64.getDecoder();
        byte[] var2 = var1.decode(var0);
        if (!isCompressed(var2)) {
            return var0;
        } else {
            ByteArrayInputStream var3 = new ByteArrayInputStream(var2);
            Throwable var4 = null;

            try {
                GZIPInputStream var5 = new GZIPInputStream(var3);
                Throwable var6 = null;

                try {
                    InputStreamReader var7 = new InputStreamReader(var5, StandardCharsets.UTF_8);
                    Throwable var8 = null;

                    try {
                        BufferedReader var9 = new BufferedReader(var7);
                        Throwable var10 = null;

                        try {
                            StringBuilder var11 = new StringBuilder();

                            String var12;
                            while((var12 = var9.readLine()) != null) {
                                var11.append(var12);
                            }

                            String var13 = var11.toString();
                            return var13;
                        } catch (Throwable var85) {
                            var10 = var85;
                            throw var85;
                        } finally {
                            if (var9 != null) {
                                if (var10 != null) {
                                    try {
                                        var9.close();
                                    } catch (Throwable var84) {
                                        var10.addSuppressed(var84);
                                    }
                                } else {
                                    var9.close();
                                }
                            }

                        }
                    } catch (Throwable var87) {
                        var8 = var87;
                        throw var87;
                    } finally {
                        if (var7 != null) {
                            if (var8 != null) {
                                try {
                                    var7.close();
                                } catch (Throwable var83) {
                                    var8.addSuppressed(var83);
                                }
                            } else {
                                var7.close();
                            }
                        }

                    }
                } catch (Throwable var89) {
                    var6 = var89;
                    throw var89;
                } finally {
                    if (var5 != null) {
                        if (var6 != null) {
                            try {
                                var5.close();
                            } catch (Throwable var82) {
                                var6.addSuppressed(var82);
                            }
                        } else {
                            var5.close();
                        }
                    }

                }
            } catch (Throwable var91) {
                var4 = var91;
                throw var91;
            } finally {
                if (var3 != null) {
                    if (var4 != null) {
                        try {
                            var3.close();
                        } catch (Throwable var81) {
                            var4.addSuppressed(var81);
                        }
                    } else {
                        var3.close();
                    }
                }

            }
        }
    }

    private static boolean isCompressed(byte[] var0) {
        return var0[0] == 31 && var0[1] == -117;
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
