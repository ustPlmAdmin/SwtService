package com.skyway;

import com.dassault_systemes.platform.ven.jackson.core.JsonProcessingException;
import com.dassault_systemes.platform.ven.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import com.skyway.res.Resources;
import com.mql.MqlService;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.util.StringList;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

public class CAEPDFSignature_mxJPO extends SkyService {
    public static final String DOC_INV_NUMBER_SELECT = "name";

    public static final String TYPE_SELECT = "type";

    public static final String NAME_SELECT = "name";

    public static final String DRW_OWNER_NAME_SELECT = "to[ARCHReferenceObject].from.owner";

    public static final String DRW_REVISION_SELECT = "to[ARCHReferenceObject].from.revision";

    public static final String REVISION_SELECT = "revision";

    public static final String DOC_TYPE_SELECT = "attribute[ARCHDocumentType]";

    public static final String DOC_TYPE_DRAWING_STR = "drawing";

    public static final String DOC_TYPE_SPECIFICATION_STR = "specification";

    public enum DrwRole {
        CREATOR, CHECKER, TCONTR, LEAD, NCONTR, APPROVER;
    }

    public enum DocType {
        DRAWING, SPECIFICATION;
    }

    public enum TNRIdEncodeMode {
        JSON, COMPRESS, ID_ONLY;
    }

    public enum CACellsCoords {
        DRW_REVISION, CA_NAME, CA_OWNER_SIGNATURE, CA_COMPLETE_DATE, CA_LETTER, EMPTY_FIELD;
    }

    private static Map<CAEPDFSignature_mxJPO.DrwRole, Float> signerYDrw = new HashMap<CAEPDFSignature_mxJPO.DrwRole, Float>() {
        {
            this.put(CAEPDFSignature_mxJPO.DrwRole.CREATOR, 32.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.CHECKER, 27.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.TCONTR, 22.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.LEAD, 17.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.NCONTR, 12.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.APPROVER, 7.5F);
        }
    };
    private static Map<CAEPDFSignature_mxJPO.DrwRole, Float> signerYSpec = new HashMap<CAEPDFSignature_mxJPO.DrwRole, Float>() {
        {
            this.put(CAEPDFSignature_mxJPO.DrwRole.CREATOR, 27.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.CHECKER, 22.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.LEAD, 17.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.NCONTR, 12.5F);
            this.put(CAEPDFSignature_mxJPO.DrwRole.APPROVER, 7.5F);
        }
    };
    private static Map<CAEPDFSignature_mxJPO.CACellsCoords, CAEPDFSignature_mxJPO.CellCoords> drwCellsCoords = new HashMap<CAEPDFSignature_mxJPO.CACellsCoords, CAEPDFSignature_mxJPO.CellCoords>() {
        {
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.DRW_REVISION, new CAEPDFSignature_mxJPO.CellCoords(0.0F, 35.0F, 7.0F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.EMPTY_FIELD, new CAEPDFSignature_mxJPO.CellCoords(7.0F, 35.0F, 10.0F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.CA_NAME, new CAEPDFSignature_mxJPO.CellCoords(17.0F, 35.0F, 23.0F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.CA_OWNER_SIGNATURE, new CAEPDFSignature_mxJPO.CellCoords(40.0F, 35.0F, 15.0F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.CA_COMPLETE_DATE, new CAEPDFSignature_mxJPO.CellCoords(55.0F, 35.0F, 9.4F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.CA_LETTER, new CAEPDFSignature_mxJPO.CellCoords(135.5F, 25.0F, 9.4F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
        }
    };
    private static Map<CAEPDFSignature_mxJPO.CACellsCoords, CAEPDFSignature_mxJPO.CellCoords> specCellsCoords = new HashMap<CAEPDFSignature_mxJPO.CACellsCoords, CAEPDFSignature_mxJPO.CellCoords>() {
        {
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.DRW_REVISION, new CAEPDFSignature_mxJPO.CellCoords(0.0F, 30.0F, 7.0F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.EMPTY_FIELD, new CAEPDFSignature_mxJPO.CellCoords(7.0F, 30.0F, 10.0F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.CA_NAME, new CAEPDFSignature_mxJPO.CellCoords(17.0F, 30.0F, 23.0F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.CA_OWNER_SIGNATURE, new CAEPDFSignature_mxJPO.CellCoords(40.0F, 30.0F, 15.0F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.CA_COMPLETE_DATE, new CAEPDFSignature_mxJPO.CellCoords(55.0F, 30.0F, 9.4F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
            this.put(CAEPDFSignature_mxJPO.CACellsCoords.CA_LETTER, new CAEPDFSignature_mxJPO.CellCoords(135.5F, 14.8F, 9.4F, 0.5F, 1.0F, "MAIN_FORM_LEFT_BOTTOM"));
        }
    };

    private final CAEChangeActionApprove_mxJPO.CAInfo caInfo;

    private List<Signature> signatures;

    private CAEChangeActionApprove_mxJPO.Logger logger;

    private PDFont font;

    public CAEPDFSignature_mxJPO(CAEChangeActionApprove_mxJPO.CAInfo caInfo, List<Signature> signatures, CAEChangeActionApprove_mxJPO.Logger logger) {
        this.caInfo = caInfo;
        this.signatures = signatures;
        this.logger = logger;
    }

    public static BufferedImage decodeBase64ToImage(String imageString) {
        String descriptionMarker = "base64,";
        int dmPos = imageString.indexOf(descriptionMarker);
        if (dmPos > -1 && dmPos < 100)
            imageString = imageString.substring(dmPos + descriptionMarker.length());
        BufferedImage image = null;
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] imageByte = decoder.decode(imageString);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            image = ImageIO.read(bis);
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }

    public static String generateQRString(String id, String type, String name, String revision, TNRIdEncodeMode mode) throws JsonProcessingException {
        if (TNRIdEncodeMode.ID_ONLY.equals(mode))
            return id;
        if (TNRIdEncodeMode.JSON.equals(mode))
            return generateQRString(id, type, name, revision);
        if (TNRIdEncodeMode.COMPRESS.equals(mode))
            return String.format("%s#@%s#@%s#@%s", new Object[] { type, name, revision, id });
        return generateQRString(id, type, name, revision);
    }

    public static String generateQRString(final String id, final String type, final String name, final String revision) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = new HashMap<String, String>() {

        };
        return mapper.writeValueAsString(map);
    }

    public void signDocument(Context context, String docId) throws Exception {
        DocType docType;
        String sPath = context.createWorkspace();
        String format = DomainConstants.FORMAT_GENERIC;
        CommonDocument doc = new CommonDocument(docId);
        StringList docSelects = new StringList() {

        };
        docSelects.add("to[ARCHReferenceObject].from.owner");
        docSelects.add("name");
        docSelects.add("type");
        docSelects.add("revision");
        docSelects.add("attribute[ARCHDocumentType]");
        docSelects.add("to[ARCHReferenceObject].from.revision");
        Map docInfo = doc.getInfo(context, docSelects);
        String ownerLastName = "";
        DomainObject owner = null;
        try {
            owner = PersonUtil.getPersonObject(context, (String)docInfo.get("to[ARCHReferenceObject].from.owner"));
            if (owner == null)
                throw new Exception();

        } catch (Exception ex) {
            this.logger.addError("Owner (document id " + docId + ") is not defined");
            return;
        }
        StringList ownerSelects = new StringList() {

        };
        ownerSelects.add("attribute[IGAPersonSignature]");
        ownerSelects.add("attribute[Last Name]");
        Map ownerInfo = owner.getInfo(context, ownerSelects);
        String ownerSignatureStr = (String)ownerInfo.get("attribute[IGAPersonSignature]");
        BufferedImage signature = null;
        if (ownerSignatureStr != null)
            try {
                signature = decodeBase64ToImage(ownerSignatureStr);
                signature = trimImage(signature, true, true);
            } catch (Exception exception) {}
        if (signature == null)
            this.logger.addError("Owner (document id " + docId + ") doesn't have valid signature uploaded");
        String lastName = (String)ownerInfo.get("attribute[Last Name]");
        if (lastName == null || "".equals(lastName)) {
            this.logger.addError("Owner (document id " + docId + ") doesn't have valid attribute Last Name");
            lastName = "";
        }
        boolean checkinDateFound = true;
        Date checkinDate = null;
        try {
            Map histData = UINavigatorUtil.getHistoryData(context, docId);
            List actions = (List)histData.get("action");
            List times = (List)histData.get("time");
            String checkinTime = null;
            int totalEvents = actions.size();
            for (int i = totalEvents - 1; i >= 0; i--) {

                if ("checkin".equals(actions.get(i))) {
                    checkinTime = ((String)times.get(i)).split("time: ")[1];
                    break;
                }
            }
            checkinDate = CAEChangeActionApprove_mxJPO.ENO_DATE_FORMAT.parse(checkinTime);
        } catch (Exception exception) {}
        if (checkinDate == null) {
            checkinDate = new Date();
            checkinDateFound = false;
        }
        checkinDate = dateFormat.parse(scalar(context, docId, "originated"));
        Signature ownerSignature = new Signature(DrwRole.CREATOR, lastName, signature, (new SimpleDateFormat("dd.MM.yy")).format(checkinDate));
        List<Signature> docSignatures = new ArrayList<>(this.signatures);
        docSignatures.add(ownerSignature);
        String invNumber = getInvNumber(docInfo);
        String qr = generateQRString(docId, (String)docInfo

                .get("type"), (String)docInfo
                .get("name"), (String)docInfo
                .get("revision"), TNRIdEncodeMode.COMPRESS);
        BufferedImage qrCodeImg = trimImage(generateQRCodeImage(qr, 100, 100), true, false);
        String docTypeStr = (String)docInfo.get("attribute[ARCHDocumentType]");
        if ("drawing".equals(docTypeStr)) {
            docType = DocType.DRAWING;
        } else if ("specification".equals(docTypeStr)) {
            docType = DocType.SPECIFICATION;
        } else {
            return;
        }
        String docRevision = (String)docInfo.get("to[ARCHReferenceObject].from.revision");
        List<String> fileNames = new ArrayList<>();
        FileList files = doc.getFiles(context, format);
        for (Object f : files)
            fileNames.add(((matrix.db.File)f).getName());
        if (fileNames.size() == 0 || fileNames.size() > 1)
            return;
        if (!checkinDateFound)
            this.logger.addError("Can't find document (id " + docId + ") last checkin date");
        String fName = fileNames.get(0);
        String fullName = sPath + File.separator + fName;
        String canCheckout = doc.getInfo(context, "from[Active Version]");
        try {
            if ("True".equalsIgnoreCase(canCheckout)) {
                doc.checkoutFile(context, true, format, fName, sPath);
            } else {
                this.logger.addError("Document (id " + docId + ") can't be checkouted");
                return;
            }
            File file = new File(fullName);
            PDDocument pdfDoc = PDDocument.load(file);
            try {
                signPdf(pdfDoc, invNumber, qrCodeImg, docSignatures, docType, docRevision);
                pdfDoc.save(file);
                pdfDoc.close();
                doc.checkinFile(context, true, true, "", format, fName, sPath);
            } catch (Exception e){
                e.printStackTrace();
            }finally {
                //pdfDoc.close();
            }
        } catch (Exception ex) {
            this.logger.addError("Couldn't handle file in document " + docId + " (" + ex.getMessage() + ")");
        } finally {
            doc.unlock(context);
            FileUtils.deleteQuietly(new File(fullName));
        }
    }

    public String getInvNumber(Map docInfo) {
        String invNumber = (String)docInfo.get("name");
        String[] temp = invNumber.split("-");
        invNumber = (temp.length > 1) ? temp[1] : temp[0];
        return invNumber;
    }

    public static void main(String[] args) throws Exception {
        String fileNameFrom = "C:/temp/TestInSpec.pdf";
        String fileNameTo = "C:/temp/TestOutSpec.pdf";
        File file = new File(fileNameFrom);
        PDDocument doc = PDDocument.load(file);
        BufferedImage qrImg = trimImage(generateQRCodeImage("QR code content", 15, 15), true, true);
        List<Signature> signatures = new ArrayList<>();
        signatures.add(new Signature(DrwRole.NCONTR, "Some last name",
                trimImage(qrImg, true, true), "Some date"));
        CAEChangeActionApprove_mxJPO.CAInfo caInfo = new CAEChangeActionApprove_mxJPO.CAInfo("CA  looooong name", "01.01.01", qrImg, "O");
        (new CAEPDFSignature_mxJPO(caInfo, signatures, new CAEChangeActionApprove_mxJPO.Logger()))
                .signPdf(doc, "Some number",

                        trimImage(qrImg, true, true), signatures, DocType.SPECIFICATION, "A.1");
        doc.save(fileNameTo);
    }

    private void signPdf(PDDocument document, String invNumber, BufferedImage qr, List<Signature> signatures, DocType docType, String docRevision) throws Exception {
        PDFont gostFontIt;
        String fontFileName = "GostAI.ttf";
        try {
            gostFontIt = loadFontFormResource(document, "/" + fontFileName);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't find " + fontFileName + " font file");
        }
        HashMap<Integer, String> overlayGuide = new HashMap<>();
        Overlay overlay = new Overlay();
        overlay.setInputPDF(document);
        overlay.setOverlayPosition(Overlay.Position.FOREGROUND);
        PDDocument overlayDoc = new PDDocument();
        int pageNumber = document.getNumberOfPages();
        for (int i = 0; i < pageNumber; i++)
            pdfPageWork(document, overlayDoc, i, invNumber, qr, signatures, gostFontIt, docType, docRevision);
        overlay.setAllPagesOverlayPDF(overlayDoc);
        overlay.overlay(overlayGuide);
    }

    public static BufferedImage textToImage(String text, boolean isVertical) {
        int size = 50;
        Font font = new Font("Arial", 2, size);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Rectangle2D bounds = font.getStringBounds(text, frc);
        int w = (int)(bounds.getWidth() * (1 + size / 5000));
        int h = (int)bounds.getHeight();
        if (isVertical) {
            int temp = w;
            w = h;
            h = temp;
        }
        BufferedImage image = new BufferedImage(w, h, 1);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.setFont(font);
        if (isVertical) {
            g.rotate(-1.5707963267948966D);
            g.drawString(text, -h, (w - size / 5));
        } else {
            g.drawString(text, (float)bounds.getX(), (float)-bounds.getY());
        }
        g.dispose();
        return image;
    }

    public static BufferedImage generateQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    public static BufferedImage getEmptyImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, 2);
        int[] imgData = new int[width];
        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, imgData, 0, 1);
            for (int x = 0; x < width; x++)
                imgData[x] = -1;
            image.setRGB(0, y, width, 1, imgData, 0, 1);
        }
        return image;
    }

    public static BufferedImage trimImage(BufferedImage image, boolean cutSpaces, boolean makeTransparent) {
        int width = image.getWidth();
        int[] imgData = new int[width];
        BufferedImage result = new BufferedImage(width, image.getHeight(), 6);
        int firstY = -1;
        int lastY = -1;
        int firstX = width;
        int lastX = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            image.getRGB(0, y, width, 1, imgData, 0, 1);
            for (int x = 0; x < width; x++) {
                int color = imgData[x];
                if ((color & 0xFF0000) >> 16 > 248 && (color & 0xFF00) >> 8 > 248 && (color & 0xFF) > 248) {
                    if (makeTransparent)
                        color &= 0xFFFFFF;
                } else {
                    if (firstY == -1)
                        firstY = y;
                    lastY = y;
                    if (x < firstX)
                        firstX = x;
                    if (x > lastX)
                        lastX = x;
                }
                imgData[x] = color;
            }
            result.setRGB(0, y, width, 1, imgData, 0, 1);
        }
        if (cutSpaces) {
            int newW = lastX - firstX + 1;
            int newH = lastY - firstY + 1;
            imgData = new int[newW];
            BufferedImage result2 = new BufferedImage(newW, newH, 6);
            int counter = 0;
            for (int i = firstY; i < lastY + 1; i++) {
                result.getRGB(firstX, i, newW, 1, imgData, 0, 1);
                result2.setRGB(0, counter, newW, 1, imgData, 0, 1);
                counter++;
            }
            result = result2;
        }
        return result;
    }

    public static float mmToPoints(float mm) {
        float result = (float)(mm / 0.352778D);
        return result;
    }

    public static float pointsToMm(float units) {
        return (float)(units * 0.352778D);
    }

    public static PDFont loadFontFormFile(PDDocument document, String ttfFilePath) throws Exception {
        return (PDFont)PDType0Font.load(document, new FileInputStream(ttfFilePath), false);
    }

    public PDFont loadFontFormResource(PDDocument document, String resourceName) throws Exception {
        InputStream stream = null;
        stream = Resources.getStream("spec/GostAI.ttf" );
        return (PDFont)PDType0Font.load(document, stream, false);
    }

    public static void addFontToPage(PDPage page, PDFont font) {
        page.getResources().add(font);
    }

    public static void insertTextWithMaxWidth(PDPageContentStream cs, String text, float size, PDFont font, float x, float y, float rot, float maxW) throws Exception {
        float calcW = calcMmTextWidth(text, font, size);
        float scale = 1.0F;
        if (calcW > maxW)
            scale = maxW / calcW;
        insertText(cs, text, size, font, x, y, scale, 1.0F, rot);
    }

    public static void insertText(PDPageContentStream cs, String text, float size, PDFont font, float x, float y, float xScale, float yScale, float rot) throws Exception {
        cs.saveGraphicsState();
        cs.beginText();
        cs.setFont(font, mmToPoints(size));
        Matrix m = Matrix.getRotateInstance(rot, mmToPoints(x), mmToPoints(y));
        m.scale(xScale, yScale);
        cs.setTextMatrix(m);
        cs.showText(text);
        cs.endText();
        cs.restoreGraphicsState();
    }

    public static float calcMmTextWidth(String text, PDFont font, float size) throws Exception {
        return font.getStringWidth(text) * size / 1000.0F;
    }

    private void pdfPageWork(PDDocument document, PDDocument overlay, int index, String invNumber,
                             BufferedImage qrCode, List<Signature> signatures, PDFont font, DocType docType, String docRevision) throws Exception {
        PDPage inputPage = document.getPage(index);

        PDPage overlayPage = new PDPage();
        addFontToPage(inputPage, font);
        float width = inputPage.getMediaBox().getWidth();
        float height = inputPage.getMediaBox().getHeight();
        float mmWidth = pointsToMm(width);
        float mmHeight = pointsToMm(height);
        overlayPage.setMediaBox(new PDRectangle(width, height));
        PDPageContentStream content = new PDPageContentStream(overlay, overlayPage);
        float fontSize = 5.0F;
        float nameXCenter = pointsToMm(width) - 161.5F;
        float signXCenter = pointsToMm(width) - 142.5F;
        float dateXCenter = pointsToMm(width) - 130.0F;
        float signMaxW = 15.0F;
        float signMaxH = 7.0F;
        float dateMaxW = 8.0F;
        float dateMaxH = 4.0F;
        Map<DrwRole, Float> signerMap = signerYDrw;
        Map<CACellsCoords, CellCoords> otherCellsCoords = drwCellsCoords;
        if (DocType.SPECIFICATION.equals(docType)) {
            signerMap = signerYSpec;
            otherCellsCoords = specCellsCoords;
        }
        if (index == 0) {
            String str2 = docRevision;
            CellCoords cellCoords2 = otherCellsCoords.get(CACellsCoords.DRW_REVISION);
            BufferedImage bufferedImage1 = getEmptyImage(100, 20);
            PDImageXObject pDImageXObject = LosslessFactory.createFromImage(overlay, bufferedImage1);
            content.drawImage(pDImageXObject,
                    mmToPoints(cellCoords2.getAbsPaddedX(mmWidth)),
                    mmToPoints(cellCoords2.getAbsPaddedY(mmHeight)),
                    mmToPoints(cellCoords2.getPaddedWidth()), mmToPoints(3.7F));
            insertTextWithMaxWidth(content, str2, fontSize, font, cellCoords2.getAbsPaddedX(mmWidth), cellCoords2.getAbsPaddedY(mmHeight), 0.0F, cellCoords2.getPaddedWidth());
            CellCoords coords = otherCellsCoords.get(CACellsCoords.EMPTY_FIELD);
            BufferedImage emptyImg = getEmptyImage(100, 20);
            pDImageXObject = LosslessFactory.createFromImage(overlay, emptyImg);
            content.drawImage(pDImageXObject,
                    mmToPoints(coords.getAbsPaddedX(mmWidth)),
                    mmToPoints(coords.getAbsPaddedY(mmHeight)),
                    mmToPoints(coords.getPaddedWidth()), mmToPoints(3.7F));
            String str1 = this.caInfo.getName();
            CellCoords cellCoords1 = otherCellsCoords.get(CACellsCoords.CA_NAME);
            bufferedImage1 = getEmptyImage(100, 20);
            pDImageXObject = LosslessFactory.createFromImage(overlay, bufferedImage1);
            content.drawImage(pDImageXObject,
                    mmToPoints(cellCoords1.getAbsPaddedX(mmWidth)),
                    mmToPoints(cellCoords1.getAbsPaddedY(mmHeight)),
                    mmToPoints(cellCoords1.getPaddedWidth()), mmToPoints(3.7F));
            insertTextWithMaxWidth(content, str1, fontSize, font, cellCoords1.getAbsPaddedX(mmWidth), cellCoords1.getAbsPaddedY(mmHeight), 0.0F, cellCoords1.getPaddedWidth());
            str1 = this.caInfo.getCompleteDate();
            cellCoords1 = otherCellsCoords.get(CACellsCoords.CA_COMPLETE_DATE);
            bufferedImage1 = getEmptyImage(100, 20);
            pDImageXObject = LosslessFactory.createFromImage(overlay, bufferedImage1);
            content.drawImage(pDImageXObject,
                    mmToPoints(cellCoords1.getAbsPaddedX(mmWidth)),
                    mmToPoints(cellCoords1.getAbsPaddedY(mmHeight)),
                    mmToPoints(cellCoords1.getPaddedWidth()), mmToPoints(3.7F));
            insertTextWithMaxWidth(content, str1, fontSize, font, cellCoords1.getAbsPaddedX(mmWidth), cellCoords1.getAbsPaddedY(mmHeight), 0.0F, cellCoords1.getPaddedWidth());
            String letter = this.caInfo.getLetter();
            cellCoords1 = otherCellsCoords.get(CACellsCoords.CA_LETTER);
            insertTextWithMaxWidth(content, letter, fontSize, font, cellCoords1.getAbsPaddedX(mmWidth), cellCoords1.getAbsPaddedY(mmHeight), 0.0F, cellCoords1.getPaddedWidth());
            String text = this.caInfo.getCompleteDate();
            cellCoords1 = otherCellsCoords.get(CACellsCoords.CA_OWNER_SIGNATURE);
            bufferedImage1 = getEmptyImage(100, 20);
            pDImageXObject = LosslessFactory.createFromImage(overlay, bufferedImage1);
            content.drawImage(pDImageXObject,
                    mmToPoints(cellCoords1.getAbsPaddedX(mmWidth)),
                    mmToPoints(cellCoords1.getAbsPaddedY(mmHeight)),
                    mmToPoints(cellCoords1.getPaddedWidth()), mmToPoints(3.7F));
            BufferedImage signImg = this.caInfo.getOwnerSignature();
            if (signImg != null) {
                float realW = signImg.getWidth();
                float realH = signImg.getHeight();
                float sMaxH = 7.0F;
                float sMaxW = cellCoords1.getPaddedWidth();
                float rel = Math.min(sMaxH / realH, sMaxW / realW);
                PDImageXObject tempSignImg = LosslessFactory.createFromImage(overlay, signImg);
                content.drawImage(tempSignImg,
                        mmToPoints(cellCoords1.getAbsMiddleX(mmWidth) - realW * rel / 2.0F),
                        mmToPoints(cellCoords1.getAbsMiddleY(mmHeight) - realH * rel / 2.0F),
                        mmToPoints(realW * rel), mmToPoints(realH * rel));
            }
            for (DrwRole role : signerMap.keySet()) {
                bufferedImage1 = getEmptyImage(100, 20);
                pDImageXObject = LosslessFactory.createFromImage(overlay, bufferedImage1);
                content.drawImage(pDImageXObject,
                        mmToPoints(nameXCenter - 11.1F),
                        mmToPoints(((Float)signerMap.get(role)).floatValue() - 2.1F),
                        mmToPoints(22.2F),
                        mmToPoints(4.2F));
            }
            for (int i = 0; i < signatures.size(); i++) {
                DrwRole role = ((Signature)signatures.get(i)).role;
                float yCenter = 0.0F;
                if (signerMap.get(role) != null) {
                    yCenter = ((Float)signerMap.get(role)).floatValue();
                    String lastName = ((Signature)signatures.get(i)).lastName;
                    insertTextWithMaxWidth(content, lastName, fontSize, font, nameXCenter - 11.5F, yCenter + 0.5F - dateMaxH / 2.0F, 0.0F, 22.2F);
                    BufferedImage bufferedImage = ((Signature)signatures.get(i)).getSignature();
                    if (bufferedImage != null) {
                        float realW = bufferedImage.getWidth();
                        float realH = bufferedImage.getHeight();
                        float rel = Math.min(signMaxH / realH, signMaxW / realW);
                        pDImageXObject = LosslessFactory.createFromImage(overlay, bufferedImage);
                        content.drawImage(pDImageXObject,
                                mmToPoints(signXCenter - realW * rel / 2.0F),
                                mmToPoints(yCenter - realH * rel / 2.0F),
                                mmToPoints(realW * rel),
                                mmToPoints(realH * rel));
                    }
                    String date = ((Signature)signatures.get(i)).date;

                    bufferedImage1 = getEmptyImage(100, 20);
                    pDImageXObject = LosslessFactory.createFromImage(overlay, bufferedImage1);
                    content.drawImage(pDImageXObject, mmToPoints(dateXCenter - 4.8F),
                            mmToPoints(yCenter + 0.5F - dateMaxH / 2.0F),
                                    mmToPoints(8.8F), mmToPoints(3.7F));
                    insertTextWithMaxWidth(content, date, fontSize, font, dateXCenter - 4.8F, yCenter + 0.5F - dateMaxH / 2.0F, 0.0F, 8.8F);
                }
            }
        }
        int i=0;
        boolean isHorizontalA4 = (height < mmToPoints(240.0F));
        PDImageXObject tempImg = LosslessFactory.createFromImage(overlay, qrCode);
        if (isHorizontalA4) {
            content.drawImage(tempImg, mmToPoints(155.0F), height - mmToPoints(14.0F), mmToPoints(12.0F), mmToPoints(12.0F));
        } else {
            content.drawImage(tempImg, mmToPoints(2.0F), mmToPoints(155.0F), mmToPoints(12.0F), mmToPoints(12.0F));
        }
        if (isHorizontalA4) {
            insertTextWithMaxWidth(content, invNumber, fontSize, font, 6.0F, pointsToMm(height) - 18.5F, 0.0F, 23.0F);
        } else {
            insertTextWithMaxWidth(content, invNumber, fontSize, font, 18.5F, 6.0F, 1.5707964F, 23.0F);
        }
        content.close();
        overlay.addPage(overlayPage);
    }

    public static class Signature {
        public final CAEPDFSignature_mxJPO.DrwRole role;

        public final String lastName;

        BufferedImage signature;

        public final String date;

        public Signature(CAEPDFSignature_mxJPO.DrwRole role, String lastName, BufferedImage signature, String date) {
            this.role = role;
            this.lastName = lastName;
            this.signature = signature;
            this.date = date;
        }

        public BufferedImage getSignature() {
            return this.signature;
        }
    }

    private static class CellCoords {
        public static final String MAIN_FORM_LEFT_BOTTOM = "MAIN_FORM_LEFT_BOTTOM";

        private static final float CELL_HEIGHT = 5.0F;

        private static final float BOTTOM_TO_MAIN_FORM_LB = 5.0F;

        private static final float MAIN_FORM_LB_TO_RIGHT = 190.0F;

        private final float x;

        private final float y;

        private final float w;

        private final float sidePadding;

        private final float bottomPadding;

        private final String fixing;

        public CellCoords(float x, float y, float w, float sidePadding, float bottomPadding, String fixing) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.sidePadding = sidePadding;
            this.bottomPadding = bottomPadding;
            this.fixing = fixing;
        }

        public float getAbsPaddedX(float pageWidth) {
            return getAbsX(pageWidth) + this.sidePadding;
        }

        public float getAbsPaddedY(float pageHeight) {
            return getAbsY(pageHeight) + this.bottomPadding;
        }

        public float getPaddedWidth() {
            return getWidth() - 2.0F * this.sidePadding;
        }

        public float getAbsX(float pageWidth) {
            if (this.fixing.equals("MAIN_FORM_LEFT_BOTTOM"))
                return pageWidth - 190.0F + this.x;
            throw new RuntimeException("CellCoords error");
        }

        public float getAbsY(float pageHeight) {
            if (this.fixing.equals("MAIN_FORM_LEFT_BOTTOM"))
                return 5.0F + this.y;
            throw new RuntimeException("CellCoords error");
        }

        public float getAbsMiddleX(float pageWidth) {
            return getAbsX(pageWidth) + getWidth() / 2.0F;
        }

        public float getAbsMiddleY(float pageHeight) {
            return getAbsY(pageHeight) + 2.5F;
        }

        public float getWidth() {
            return this.w;
        }
    }
}
