package com.skyway;

import com.dassault_systemes.system_cockpit.shared.util.MimeType;
import com.itextpdf.xmp.impl.Base64;
import com.matrixone.apps.domain.util.FrameworkException;
import com.mql.Log;
import com.mql.MqlService;
import matrix.db.Context;
import org.apache.poi.ss.usermodel.Workbook;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
/**
 * Общий сервис по расширению сервиса MQL
 */
public class SkyService extends MqlService {

    public Response response(Object object) {
        return Response.ok(json.toJson(object), MimeType.APPLICATION_JSON).build();
    }

    public void finish(HttpServletRequest request) {
        Log.requestFinish(request);
    }

    public Response error(Exception e) {
        return Log.e(e);
    }

    public Response ok() {
        return Response.ok().build();
    }

    public Response file(ByteArrayOutputStream outputStream, String mimetype, String filename) {
        Response.ResponseBuilder responseBuilder = Response.ok(outputStream.toByteArray());
        responseBuilder.type(mimetype);
        responseBuilder.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        return responseBuilder.build();
    }

    public Response excel(Workbook workbook, String filename) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return file(outputStream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filename + ".xls");
    }

    public Response word(WordprocessingMLPackage wordMLPackage, String filename) throws IOException, Docx4JException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        wordMLPackage.save(outputStream);
        return file(outputStream, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", filename + ".docx");
    }

    public static Response page(String title, String message) {
        String template = Base64.decode("PGh0bWw+Cjxib2R5Pgo8ZGl2IHN0eWxlPSJ0ZXh0LWFsaWduOiBjZW50ZXI7IHBvc2l0aW9uOiBhYnNvbHV0ZTsgdG9wOiA1MCU7IGxlZnQ6IDUwJTsgbWFyZ2luLXJpZ2h0OiAtNTAlOyB0cmFuc2Zvcm06IHRyYW5zbGF0ZSgtNTAlLCAtNTAlKSI+CiAgICA8aDE+VElUTEU8L2gxPgogICAgPGgyPk1FU1NBR0U8L2gyPjwvZGl2Pgo8L2JvZHk+CjwvaHRtbD4=");
        template = template.replace("TITLE", title == null ? "" : title);
        template = template.replace("MESSAGE", message == null ? "" : message);
        Response.ResponseBuilder responseBuilder = Response.status(BAD_REQUEST).entity(template);
        responseBuilder.type("text/html");
        return responseBuilder.build();
    }
}
