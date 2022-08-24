package com.skyway;

import com.matrixone.apps.domain.util.MqlUtil;
import com.mql.MqlService;
import org.apache.commons.text.StringEscapeUtils;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.model.structure.PageDimensions;
import org.docx4j.model.structure.SectionWrapper;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;
/**
 * Выгрузка тербований в Word
 * */
public class Requirements extends SkyService {

    public void rec(matrix.db.Context ctx, WordprocessingMLPackage wordMLPackage, MainDocumentPart mainDocumentPart, String objectId, String parentId, String mode) throws Exception {

        if (parentId.length() > 20) return;

        List<String> children = list(ctx, objectId, "from[Specification Structure].to.id");
        children.addAll(list(ctx, objectId, "from[Sub Requirement].to.id"));

        List<String> childrenOrder = list(ctx, objectId, "from[Specification Structure].attribute[TreeOrder]");
        childrenOrder.addAll(list(ctx, objectId, "from[Sub Requirement].attribute[TreeOrder]"));

        Map<Double, String> order = new HashMap<>();
        for (int i = 0; i < Math.min(children.size(), childrenOrder.size()); i++)
            order.put(Double.valueOf(childrenOrder.get(i).isEmpty() ? "" + i : childrenOrder.get(i)), children.get(i));

        Map<Double, String> sortedOrderMap = new TreeMap<>(order);
        ArrayList<String> sortedChildrenIdList = new ArrayList<>(sortedOrderMap.values());

        for (int i = 0; i < sortedChildrenIdList.size(); i++) {
            String childId = sortedChildrenIdList.get(i);
            String stringId = parentId + (i + 1);

            String root = MqlUtil.mqlCommand(ctx, "print bus " + childId + " select attribute[Content Data];");
            String contentDelimiter = "attribute[Content Data] =";
            String content = root.substring(root.indexOf(contentDelimiter) + contentDelimiter.length() + 1);
            Map<String, String> child = row(ctx, childId, "attribute[Title]", "type");

            if (mode.equals("test"))
                mainDocumentPart.addStyledParagraphOfText("Level1", "[" + child.get("attribute[Title]") + "] ");

            if (content.length() == 0)
                content = child.get("attribute[Title]");


            if (child.get("type").equals("Comment")) {
                // nothing
            } else
                content = stringId + " " + content;

            content = content.replaceAll("\n", "");
            content = content.replaceAll(Pattern.quote("</p>"), "\n");

            byte[] bytes = null;
            int startImgTag = content.indexOf("<img");
            if (startImgTag != -1) {
                int endImgTag = content.indexOf(">", startImgTag);
                String imageTag = content.substring(startImgTag, endImgTag);
                String DATA_START = "base64,";
                int startData = imageTag.indexOf(DATA_START);
                int endData = imageTag.indexOf("\"", startData);
                String imageData = imageTag.substring(startData + DATA_START.length(), endData);
                bytes = Base64.getDecoder().decode(imageData);
                content = content.replaceFirst("<img.*?>", "\nimage\n");
            }
            content = content.replaceAll("<.*?>", "");

            content = StringEscapeUtils.unescapeHtml4(content);

            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.equals("image") && bytes != null) {
                    addImageToPackage(wordMLPackage, bytes);
                } else
                    mainDocumentPart.addStyledParagraphOfText(child.get("type").equals("Chapter") ? "Level1" : "OtherLevel", line);
            }


            if (!mode.equals("test") && !mode.equals("without_discussions")) {
                List<String> discussions = list(ctx, childId, "from[Thread].to.from[Message].to.id");
                for (String discussionId: discussions){

                    Map<String, String> firstMessage = row(ctx, discussionId,
                            "owner",
                            "description",
                            "attribute[Subject]:subject");
                    if (firstMessage != null) {
                        mainDocumentPart.addStyledParagraphOfText("MessageTitle", "Дискуссия: " + firstMessage.get("subject"));
                        mainDocumentPart.addStyledParagraphOfText("Message", firstMessage.get("owner") + ": " + firstMessage.get("description"));
                    }

                    List<String> messageIdList = list(ctx, discussionId, "from[Reply].to.id");
                    for (String messageId : messageIdList) {
                        Map<String, String> message = row(ctx, messageId, "owner");
                        String description = scalar(ctx, messageId, "description");
                        mainDocumentPart.addStyledParagraphOfText("Message", message.get("owner") + ": " + description);
                    }
                }

            }

            rec(ctx, wordMLPackage, mainDocumentPart, childId, stringId + ".", mode);
        }

    }

    @GET
    @Path("/requirements_test")
    public Response getRequirementsTest(@javax.ws.rs.core.Context HttpServletRequest request,
                                        @QueryParam("objectId") String objectId) {
        return getRequirements(request, objectId, "test");
    }

    @GET
    @Path("/requirements_without_discussions")
    public Response getRequirementsWithoutDiscussions(@javax.ws.rs.core.Context HttpServletRequest request,
                                        @QueryParam("objectId") String objectId) {
        return getRequirements(request, objectId, "without_discussions");
    }

    @GET
    @Path("/requirements")
    public Response getRequirements(@javax.ws.rs.core.Context HttpServletRequest request,
                                    @QueryParam("objectId") String objectId,
                                    @QueryParam("mode") String mode) {
        try {
            matrix.db.Context ctx = authenticate(request);

            if (!objectId.contains("."))
                objectId = findScalar(ctx, "*", objectId, "id");

            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            ObjectFactory factory = Context.getWmlObjectFactory();

            PageDimensions page = new PageDimensions();
            SectPr.PgMar pgMar = page.getPgMar();
            pgMar.setLeft(BigInteger.valueOf((long) (565 * 2.5f)));
            pgMar.setRight(BigInteger.valueOf((long) (565 * 1f)));
            pgMar.setBottom(BigInteger.valueOf((long) (565 * 2f)));
            pgMar.setTop(BigInteger.valueOf((long) (565 * 2f)));
            SectPr sectPr = Context.getWmlObjectFactory().createSectPr();
            wordMLPackage.getMainDocumentPart().getJaxbElement().getBody().setSectPr(sectPr);
            sectPr.setPgMar(pgMar);

            createFooter(wordMLPackage, factory);

            addStyle(wordMLPackage, "MainTitle", "Times New Roman", 16, JcEnumeration.CENTER, false, false, 0);
            addStyle(wordMLPackage, "Level1", "Times New Roman", 14, JcEnumeration.BOTH, true, false, 1.25f);
            addStyle(wordMLPackage, "OtherLevel", "Times New Roman", 14, JcEnumeration.BOTH, false, false, 1.25f);
            addStyle(wordMLPackage, "Message", "Times New Roman", 14, JcEnumeration.BOTH, false, true, 1.25f);
            addStyle(wordMLPackage, "MessageTitle", "Times New Roman", 14, JcEnumeration.BOTH, true, true, 1.25f);

            String title = scalar(ctx, objectId, "attribute[Title]");
            wordMLPackage.getMainDocumentPart().addStyledParagraphOfText("MainTitle", title);

            rec(ctx, wordMLPackage, wordMLPackage.getMainDocumentPart(), objectId, "", mode == null ? "default" : mode);

            return word(wordMLPackage, title);
        } catch (Exception e) {
            return error(e);
        } finally {
            finish(request);
        }
    }


    public static void addStyle(WordprocessingMLPackage wordMLPackage,
                                String styleName,
                                String fontName,
                                int fontSize,
                                JcEnumeration jcEnumeration,
                                boolean bold,
                                boolean italic,
                                float firstLineSM) {
        ObjectFactory factory = Context.getWmlObjectFactory();

        Style style = factory.createStyle();
        style.setType("paragraph");
        style.setStyleId(styleName);
        Style.Name n = factory.createStyleName();
        n.setVal(styleName);
        style.setName(n);

        RPr rpr = new RPr();
        RFonts runFont = new RFonts();
        runFont.setAscii(fontName);
        runFont.setHAnsi(fontName);
        rpr.setRFonts(runFont);

        BooleanDefaultTrue boldVal = new BooleanDefaultTrue();
        boldVal.setVal(bold);
        rpr.setB(boldVal);

        BooleanDefaultTrue italicVal = new BooleanDefaultTrue();
        italicVal.setVal(italic);
        rpr.setI(italicVal);

        HpsMeasure size = new HpsMeasure();
        size.setVal(BigInteger.valueOf(fontSize * 2));
        rpr.setSz(size);
        style.setRPr(rpr);

        PPr ppr = factory.createPPr();
        Jc justification = factory.createJc();
        justification.setVal(jcEnumeration); //JcEnumeration.BOTH
        ppr.setJc(justification);

        PPrBase.Ind indention = factory.createPPrBaseInd();
        indention.setFirstLine(BigInteger.valueOf((int) (565 * firstLineSM)));
        ppr.setInd(indention);
        style.setPPr(ppr);

        wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement().getStyle().add(style);
    }

    private void createFooter(WordprocessingMLPackage wordMLPackage, ObjectFactory factory) throws InvalidFormatException {
        FooterPart footerPart = new FooterPart();
        footerPart.setPackage(wordMLPackage);
        Ftr ftr = factory.createFtr();
        P paragraph = factory.createP();
        R run = factory.createR();
        FldChar fldchar = factory.createFldChar();
        fldchar.setFldCharType(STFldCharType.BEGIN);
        run.getContent().add(fldchar);
        paragraph.getContent().add(run);
        run = factory.createR();
        Text txt = new Text();
        txt.setSpace("preserve");
        txt.setValue(" PAGE   \\* MERGEFORMAT ");
        run.getContent().add(factory.createRInstrText(txt));
        paragraph.getContent().add(run);
        FldChar fldcharend = factory.createFldChar();
        fldcharend.setFldCharType(STFldCharType.END);
        R run3 = factory.createR();
        run3.getContent().add(fldcharend);
        paragraph.getContent().add(run3);

        PPr ppr = factory.createPPr();
        Jc justification = factory.createJc();
        justification.setVal(JcEnumeration.CENTER);
        ppr.setJc(justification);
        paragraph.setPPr(ppr);

        ftr.getContent().add(paragraph);
        footerPart.setJaxbElement(ftr);
        Relationship relationship = wordMLPackage.getMainDocumentPart().addTargetPart(footerPart);
        List<SectionWrapper> sections = wordMLPackage.getDocumentModel().getSections();
        SectPr sectPr = sections.get(sections.size() - 1).getSectPr();
        if (sectPr == null) {
            sectPr = factory.createSectPr();
            wordMLPackage.getMainDocumentPart().addObject(sectPr);
            sections.get(sections.size() - 1).setSectPr(sectPr);
        }
        FooterReference footerReference = factory.createFooterReference();
        footerReference.setId(relationship.getId());
        footerReference.setType(HdrFtrRef.DEFAULT);
        sectPr.getEGHdrFtrReferences().add(footerReference);
    }

    private void addImageToPackage(WordprocessingMLPackage wordMLPackage,
                                          byte[] bytes) throws Exception {
        BinaryPartAbstractImage imagePart =
                BinaryPartAbstractImage.createImagePart(wordMLPackage, bytes);
        Random random = new Random();
        int docPrId = random.nextInt(100000);
        int cNvPrId = random.nextInt(100000);
        Inline inline = imagePart.createImageInline("Filename hint",
                "Alternative text", docPrId, cNvPrId, false);

        P paragraph = addInlineImageToParagraph(inline);

        wordMLPackage.getMainDocumentPart().addObject(paragraph);
    }

    private P addInlineImageToParagraph(Inline inline) {
        ObjectFactory factory = new ObjectFactory();
        P paragraph = factory.createP();
        R run = factory.createR();
        paragraph.getContent().add(run);
        Drawing drawing = factory.createDrawing();
        run.getContent().add(drawing);
        drawing.getAnchorOrInline().add(inline);
        return paragraph;
    }

}
