package com.skyway;

import com.skyway.res.Resources;
import matrix.db.Context;
import matrix.util.MatrixException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * Бекенд для виджета SWT Spec
 */
public class SpecDocs extends SpecUtils {

    void recDocs(HttpServletRequest request, Context ctx, String objectId, String dirName, ZipOutputStream out) throws MatrixException, IOException, ParseException {

        Map<String, String> object = row(ctx, objectId, "current", "attribute[PLMEntity.V_Name]:title");
        dirName = dirName + object.get("title") + "/";
        if (object.get("current").equals("RELEASED"))
        {
            Map<String, String> docs = getDocs(request, ctx, objectId);
            for (String fileName : docs.keySet()) {
                CloseableHttpClient client = HttpClients.createDefault();
                HttpGet get = new HttpGet(docs.get(fileName));
                HttpResponse response1 = client.execute(get);
                try {
                    ZipEntry zipEntry = new ZipEntry(dirName + fileName);
                    out.putNextEntry(zipEntry);
                    out.write(IOUtils.toByteArray(response1.getEntity().getContent()));
                    out.closeEntry();
                } catch (ZipException ignored) {
                } finally {
                    client.close();
                }
            }
        }

        List<String> childrenNames = list(ctx, objectId, "from[VPMInstance].to.name");
        List<String> childrenNamesWithoutDuplicates = new ArrayList<>(new HashSet<>(childrenNames));
        for (String childName : childrenNamesWithoutDuplicates) {
            String childId = findScalar(ctx, "*", childName, "id");
            recDocs(request, ctx, childId, dirName, out);
        }
    }

    @GET
    @Path("/spec_docs")
    @Produces("application/zip")
    public Response getVPI(@javax.ws.rs.core.Context HttpServletRequest request,
                           @QueryParam("objectId") String objectId) {
        try {
            Context ctx = authenticate(request);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            recDocs(request, ctx, objectId, "", zos);
            zos.close();
            if (baos.size() == 22) {
                return page("Documents not found", "Please check docs in \"Reference Documents\"");
            } else {
                return file(baos, "application/zip", "spec_docs.zip");
            }
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }
}

