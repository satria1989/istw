package id.co.itasoft.esign;

import com.google.gson.Gson;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FileUtil;
import org.joget.apps.form.service.FormPdfUtil;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import com.google.zxing.WriterException;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;


import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PDFFileDownloadCustom extends DataListActionDefault {

    private static final String MESSAGE_PATH = "message/datalist/PDFFileDownloadCustom";
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PDFFileDownloadCustom.class);

    private PdfReader document;

    private PdfStamper stamper;

    Properties properties = new Properties();

    private PdfSmartCopy copier;

    private String appPrefix = "document";

    @Override
    public String getLinkLabel() {
        return getPropertyString("label");
    }

    @Override
    public String getHref() {
        return getPropertyString("href");
    }

    @Override
    public String getTarget() {
        return "get";
    }

    @Override
    public String getHrefParam() {
        return getPropertyString("hrefParam");
    }

    @Override
    public String getHrefColumn() {
        String recordIdColumn = getPropertyString("recordIdColumn");

        debugMessage( "getHrefColumn :" + recordIdColumn);
        if ("id".equalsIgnoreCase(recordIdColumn) || recordIdColumn.isEmpty())
            return getPropertyString("hrefColumn");
        return recordIdColumn;
    }

    @Override
    public String getConfirmation() {
        return getPropertyString("confirmation");
    }

    @Override
    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        if (!getPropertyString("appPrefix").isEmpty()) {
            this.appPrefix = getPropertyString("appPrefix");
        }

        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();

        if (rowKeys != null && rowKeys.length > 0) {
            try {
                HttpServletResponse response = WorkflowUtil.getHttpServletResponse();
                singlePdf(request, response, rowKeys[0]);
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Fail to generate PDF for " + Arrays.toString(rowKeys));
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return "PDF File Download Signature Custom";
    }

    @Override
    public String getVersion() {
        return "7.0.2";
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.esign.PDFFileDownloadCustom.pluginDesc", getClassName(), "message/datalist/PDFFileDownloadCustom");
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.esign.PDFFileDownloadCustom.pluginLabel", getClassName(), "message/datalist/PDFFileDownloadCustom");
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        debugMessage( "getProperties");
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.awt.Font[] fonts = ge.getAllFonts();
        JSONArray arr = new JSONArray();
        try {
            JSONObject obj = new JSONObject();
            obj.accumulate("value", "AdineKirnberg.ttf");
            obj.accumulate("label", "Adine Kirnberg");
            arr.put(obj);
            obj = new JSONObject();
            obj.accumulate("value", "LemonTuesday.ttf");
            obj.accumulate("label", "Lemon Tuesday");
            arr.put(obj);
        } catch (JSONException ex) {
            Logger.getLogger(PDFFileDownloadCustom.class.getName()).log(Level.SEVERE, (String) null, (Throwable) ex);
        }
        for (java.awt.Font font : fonts) {
            try {
                JSONObject obj = new JSONObject();
                obj.accumulate("value", font.getFontName());
                obj.accumulate("label", font.getFontName() + " (" + font.getFamily() + ")");
                arr.put(obj);
            } catch (JSONException ex) {
                Logger.getLogger(PDFFileDownloadCustom.class.getName()).log(Level.SEVERE, (String) null, (Throwable) ex);
            }
        }
        return AppUtil.readPluginResource(getClassName(), "/properties/datalist/PDFFileDownloadCustom.json", (Object[]) new String[]{arr.toString()}, true, "message/datalist/PDFFileDownloadCustom");
    }

    // 1
    public byte[] getPdf(String primaryKeyValue) throws IOException {
        debugMessage( "getPdf");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String formDefId = getPropertyString("formDefId");
        String attachmentFieldID = getPropertyString("attachmentField");
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        String fileName = "";
        Form form = null;
        String tableName = null;
        try {
            if (appDef != null && formDefId != null &&
                    !formDefId.isEmpty() && primaryKeyValue != null &&
                    !primaryKeyValue.isEmpty()) {
                FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
                if (formDef != null) {
                    String json = formDef.getJson();
                    form = (Form) formService.createElementFromJson(json);
                    if (form != null && form.getLoadBinder() != null) {
                        tableName = form.getPropertyString("tableName");
                        FormData formData = new FormData();
                        FormRowSet rows = form.getLoadBinder().load((Element) form, primaryKeyValue, formData);
                        if (rows != null && !rows.isEmpty()) {
                            FormRow row = (FormRow) rows.get(0);
                            fileName = row.getProperty(attachmentFieldID);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "");
        }
        String decodedFileName = fileName;
        try {
            decodedFileName = URLDecoder.decode(fileName, "UTF8");
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
        }
        debugMessage( "decodeFileName :" + decodedFileName);
        debugMessage( "tableName :" + tableName);
        debugMessage( "primaryKey :" + primaryKeyValue);
        File file = FileUtil.getFile(decodedFileName, tableName, primaryKeyValue);
        FileInputStream fileIS = new FileInputStream(file);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fileIS.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        byte[] fileContent = outputStream.toByteArray();
        return fileContent;
    }

    public byte[] processPDF(String rowKey) throws Exception {
        debugMessage( "PROCESSPDF");
        byte[] pdf = getPdf(rowKey);
        if (getPropertyString("signatureStamping").equalsIgnoreCase("true"))
            pdf = addSignatureImage(pdf, rowKey);
        if (getPropertyString("qrCodeStamping").equalsIgnoreCase("true"))
            pdf = addQrCodeImage(pdf, rowKey);
        if (getPropertyString("documentTrail").equalsIgnoreCase("true"))
            pdf = addDocumentTrailToAgreement(pdf, rowKey);
        pdf = addWatermark(pdf, rowKey);
        return pdf;
    }

    public byte[] addWatermark(byte[] pdf, String id) throws Exception {
        debugMessage( "addWatermark :" + id);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            this.document = new PdfReader(pdf);
            try {
                this.stamper = new PdfStamper(this.document, os);
                internalWriteWatermark(id);
                this.stamper.close();
            } catch (Exception e) {
                throw new IOException("Unable to write watermark", e);
            }
        } finally {
            if (this.document != null)
                this.document.close();
        }
        return os.toByteArray();
    }

    private void internalWriteWatermark(String id) throws IOException {
        debugMessage( "INTERNALWRITEWATERMARK");
        WorkflowAssignment wfAssignment = null;
        wfAssignment = new WorkflowAssignment();
        wfAssignment.setProcessId(id);
        for (int i = 1; i <= this.stamper.getReader().getNumberOfPages(); i++) {
            if (i != 0) {
                PdfContentByte canvas = this.stamper.getOverContent(i);
                if (getProperty("headerWatermark") != null) {
                    String headerWatermark = getPropertyString("headerWatermark");
                    headerWatermark = AppUtil.processHashVariable(headerWatermark, wfAssignment, null, null);
                    writeHeader(canvas, i, headerWatermark);
                }
                if (getProperty("bodyWatermark") != null) {
                    String bodyWatermark = getPropertyString("bodyWatermark");
                    bodyWatermark = AppUtil.processHashVariable(bodyWatermark, wfAssignment, null, null);
                    writeBody(canvas, i, bodyWatermark);
                }
                if (getProperty("footerWatermark") != null) {
                    String footerWatermark = getPropertyString("footerWatermark");
                    footerWatermark = AppUtil.processHashVariable(footerWatermark, wfAssignment, null, null);
                    writeFooter(canvas, i, footerWatermark);
                }
            }
        }
    }

    public static Color hex2Rgb(String colorStr) {
        return new Color(Integer.valueOf(colorStr.substring(1, 3), 16).intValue(), Integer.valueOf(colorStr.substring(3, 5), 16).intValue(), Integer.valueOf(colorStr.substring(5, 7), 16).intValue());
    }


    public byte[] addDocumentTrailToAgreement(byte[] agreement, String primaryKeyValue) throws Exception {
        debugMessage( "addDOcumentTrail");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] lastPage = generateDocumentTrailPage(primaryKeyValue);
        Document finalDoc = new Document();
        PdfCopy copy = new PdfCopy(finalDoc, os);
        finalDoc.open();
        PdfReader reader = new PdfReader(agreement);
        for (int i = 0; i < reader.getNumberOfPages(); )
            copy.addPage(copy.getImportedPage(reader, ++i));
        PdfReader pdfReader = new PdfReader(lastPage);
        for (int j = 0; j < pdfReader.getNumberOfPages(); )
            copy.addPage(copy.getImportedPage(pdfReader, ++j));
        finalDoc.close();
        reader.close();
        pdfReader.close();
        return os.toByteArray();
    }


    public byte[] generateDocumentTrailPage(String id) {
        debugMessage( "generateDocumentTrailPage :: " + id);
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String formDefId = getPropertyString("trailFormDefId");
        Boolean hideEmptyValueField = null;
        if (getPropertyString("trailHideEmptyValueField").equals("true"))
            hideEmptyValueField = Boolean.valueOf(true);
        Boolean showNotSelectedOptions = null;
        if (getPropertyString("trailShowNotSelectedOptions").equals("true"))
            showNotSelectedOptions = Boolean.valueOf(true);
        Boolean repeatHeader = null;
        if ("true".equals(getPropertyString("trailRepeatHeader")))
            repeatHeader = Boolean.valueOf(true);
        Boolean repeatFooter = null;
        if ("true".equals(getPropertyString("trailRepeatFooter")))
            repeatFooter = Boolean.valueOf(true);
        String css = null;
        if (!getPropertyString("trailFormatting").isEmpty())
            css = getPropertyString("trailFormatting");
        String header = null;
        if (!getPropertyString("trailHeaderHtml").isEmpty()) {
            header = getPropertyString("trailHeaderHtml");
            header = AppUtil.processHashVariable(header, null, null, null);
        }
        String footer = null;
        if (!getPropertyString("trailFooterHtml").isEmpty()) {
            footer = getPropertyString("trailFooterHtml");
            footer = AppUtil.processHashVariable(footer, null, null, null);
        }
        String formDefIdHeader = getPropertyString("trailHeaderFormDefId");
        WorkflowAssignment wfAssignment = null;
        wfAssignment = new WorkflowAssignment();
        wfAssignment.setProcessId(id);
        if (!formDefIdHeader.isEmpty()) {
            header = FormPdfUtil.getSelectedFormHtml(formDefIdHeader, id, appDef, null, hideEmptyValueField);
            repeatHeader = Boolean.valueOf(true);
        }
        String content = FormPdfUtil.getSelectedFormHtml(formDefId, id, appDef, wfAssignment, Boolean.valueOf(true));
        return FormPdfUtil.createPdf(content, header, footer, css, showNotSelectedOptions, repeatHeader, repeatFooter);
    }


    public byte[] addSignatureImage(byte[] pdf, String primaryKeyValue) throws Exception {
        debugMessage( "ADD SIGNATURE");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            PdfReader reader = new PdfReader(pdf);
            PdfStamper stamper = new PdfStamper(reader, os);
            PdfContentByte over = null;
            List<Map> ann = getAnnotationData(primaryKeyValue);
            for (int i = 0; i < ann.size(); i++) {
                Map row = ann.get(i);
                String rowPage = (String) row.get("c_annotateData");
                String rowData = (String) row.get("c_selectedSignature");
                String rowName = (String) row.get("c_name");
                String dateModified = (String) row.get("dateModified");
                rowName = rowName.trim();

                debugMessage( "addSignatureImage - c_annotateData : " + rowPage);
                debugMessage( "addSignatureImage - c_selectedSignature : " + rowData);
                debugMessage( "addSignatureImage - rowName : " + rowName);
                debugMessage("addSignatureImage - dateModified : " + dateModified);

                debugMessage("Annotation[" + i + "] AnnotationData [" + rowPage + "] Selected Signature [" + rowData + "] Email [" + rowName + "]");
                List<Map> signatures = getSignatureData(primaryKeyValue, rowName);
                if (signatures.isEmpty()) {
                    debugMessage("No signature found");
                } else {
                    Map<String, String> signature = signatures.get(0);
                    debugMessage("Signature found [" + signature.toString() + "]");
                    String signatureData = signature.get("c_signature");
                    String signatureText = null;
                    String signatureImageName = null;
                    String signatureId = null;
                    String font = null;
                    String signatureName = signature.get("c_name");
                    String SignatureType = signature.get("signature_type");
                    if (SignatureType.equalsIgnoreCase("DrawSignature")) {
                        debugMessage("Draw Signature Found");
                    } else if (SignatureType.equalsIgnoreCase("TextSignature")) {
                        debugMessage("Text Signature Found");
                        signatureText = signature.get("c_text_signature");
                        font = signature.get("c_font");
                    } else if (SignatureType.equalsIgnoreCase("ImageSignature")) {
                        debugMessage("Image Signature Found");
                        signatureImageName = signature.get("c_image_signature");
                        signatureId = signature.get("id");
                    } else {
                        debugMessage("No Signature Found");
                    }
                    JSONArray markers = new JSONArray(rowPage);
                    String[] selectedSignature = rowData.split(",");
                    for (String Signature : selectedSignature) {
                        JSONObject marker = markers.getJSONObject(Integer.parseInt(Signature) - 1);
                        debugMessage(marker.toString());
                        int page = Integer.parseInt(marker.getString("pageNumber"));
                        float left = 0.0F;
                        float top = 0.0F;
                        float bottom = 0.0F;
                        float height = 0.0F;
                        float width = 0.0F;
                        try {
                            left = Float.parseFloat(marker.getString("left"));
                            top = Float.parseFloat(marker.getString("top"));
                            height = Float.parseFloat(marker.getString("height")) * Float.parseFloat(marker.getString("scaleY"));
                            width = Float.parseFloat(marker.getString("width")) * Float.parseFloat(marker.getString("scaleX"));
                            debugMessage("Marker Dimension - Left[" + left + "] Top[" + top + "] Height[" + height + "] Width[" + width + "]");
                        } catch (Exception ex) {
                            LogUtil.error(PDFFileDownloadCustom.class.getName(), ex, "Cannot get marker dimension - " + marker.toString());
                            debugMessage("Marker Dimension Error");
                        }
                        debugMessage(reader.getPageSize(page).getHeight() + " - " + top);
                        bottom = reader.getPageSize(page).getHeight() - top;
                        Image img = null;
                        if (SignatureType.equalsIgnoreCase("DrawSignature")) {
                            debugMessage("Signature Image - Generating Draw Signature");
                            if (signatureData != null && !signatureData.isEmpty()) {
                                byte[] signatureImage = getSignatureAsImageBytes(signatureData, Float.toString(width), Float.toString(height), dateModified);
                                img = Image.getInstance(signatureImage);
                                img.setDpi(300, 300);
                            }
                        } else if (SignatureType.equalsIgnoreCase("TextSignature")) {
                            debugMessage("Signature Image - Generating Type Signature");
                            if (signatureText != null) {
                                byte[] stylishSign = getStylishSignatureAsImageBytes(signatureText, Float.toString(width), Float.toString(height), font);
                                img = Image.getInstance(stylishSign);
                                img.setDpi(300, 300);
                            }
                        } else if (SignatureType.equalsIgnoreCase("ImageSignature")) {
                            debugMessage("Signature Image - Generating Image Signature");

                            if (signatureId != null) {
                                byte[] stylishSign = getImageAsImageBytes(signatureId, signatureImageName, Float.toString(width), Float.toString(height), signatureName, dateModified);
                                img = Image.getInstance(stylishSign);
                                img.setDpi(300, 300);
                            }
                        }
                        if (img != null) {
                            debugMessage("Signature Image - Placing");
                            img.setAbsolutePosition(left, bottom - img.getHeight());
                            float newHeight = img.getHeight();
                            float newWidth = img.getWidth();
                            debugMessage("Image Height: " + newHeight);
                            debugMessage("Image Width: " + newWidth);
                            img.scaleToFit(newWidth, newHeight);

                            over = stamper.getOverContent(page);

                            over.addImage(img);
                            debugMessage( "Done Placing");
                        } else {
                            debugMessage("Signature Image - Missing");
                            debugMessage( "Signature image missing");
                        }
                    }
                }
            }
            stamper.close();
        } finally {
            if (this.document != null)
                this.document.close();
        }
        return os.toByteArray();
    }

    public byte[] addQrCodeImage(byte[] pdf, String primaryKeyValue) throws Exception {
        debugMessage("ADD Qr Signature");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            String appId = appDef.getAppId();
            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
            String serverName = request.getServerName();
            int port = request.getServerPort();
            String scheme = request.getScheme();
            String context = request.getContextPath();
            String url = scheme + "://" + serverName + ":" + port + context + "/web/userview/" + appId + "/v/_/detail_document_req?activityId1=&request_id=" + primaryKeyValue;
            PdfReader reader = new PdfReader(pdf);
            PdfStamper stamper = new PdfStamper(reader, os);
            List<Map> ann = getQrAnnotationData(primaryKeyValue);
            for (Map row : ann) {
                String rowPage = (String) row.get("c_qr_annotation_data");
                debugMessage("QR Annotation Data: " + rowPage);
                JSONObject document = new JSONObject(rowPage);
                JSONArray pages = document.getJSONArray("pages");
                debugMessage("Pages: " + pages.toString());
                for (int j = 0; j < pages.length(); j++) {
                    JSONObject page = pages.getJSONObject(j);
                    JSONArray objects = page.getJSONArray("objects");
                    for (int k = 0; k < objects.length(); k++) {
                        JSONObject object = objects.getJSONObject(k);
                        if ("labeledRect".equals(object.getString("type"))) {
                            int pageNumber = j + 1;
                            float pageHeight = reader.getPageSize(pageNumber).getHeight();
                            float left = (float) object.getDouble("left");
                            float top = (float) object.getDouble("top");
                            float width = (float) object.getDouble("width");
                            float height = (float) object.getDouble("height");
                            debugMessage("QR Code Dimensions: Left[" + left + "] Top[" + top + "] Width[" + width + "] Height[" + height + "]");
                            float scaleX = (float) object.getDouble("scaleX");
                            float scaleY = (float) object.getDouble("scaleY");
                            debugMessage("QR Code Scale: X[" + scaleX + "] Y[" + scaleY + "]");
                            float scaledWidth = width * scaleX;
                            float scaledHeight = height * scaleY;

                            BufferedImage qrCodeImage = generateQrCodeImage(url, (int) scaledWidth, (int) scaledHeight);

                            float newLeft = left + (scaledWidth - qrCodeImage.getWidth()) / 2;
                            float newBottom = pageHeight - top - scaledHeight - (scaledHeight - qrCodeImage.getHeight()) / 2;

                            qrCodeImage = processQrCodeImage(qrCodeImage, (int) scaledWidth, (int) scaledHeight);

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(qrCodeImage, "png", baos);
                            Image img = Image.getInstance(baos.toByteArray());

                            img.setDpi(300, 300);

                            img.setAbsolutePosition(newLeft, newBottom);
                            img.scaleToFit(scaledWidth, scaledHeight);

                            PdfContentByte over = stamper.getOverContent(pageNumber);
                            over.addImage(img);

                            // Add "Digitally Signed" text centered below the QR code
                            over.beginText();
                            over.setFontAndSize(BaseFont.createFont(), 12);
                            float textWidth = over.getEffectiveStringWidth("Digitally Signed", false);
                            float textX = newLeft + (scaledWidth - textWidth) / 2;
                            over.setTextMatrix(textX, newBottom - 15); // Adjust the position as needed
                            over.showText("Digitally Signed");
                            over.endText();
                            debugMessage("QR Code Placed");
                        }
                    }
                }
            }
            stamper.close();
        } finally {
            if (this.document != null) {
                this.document.close();
            }
        }
        return os.toByteArray();
    }

    private BufferedImage generateQrCodeImage(String data, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // High error correction level
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private BufferedImage processQrCodeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        // Convert to ARGB to support transparency
        BufferedImage newImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        // Remove white background
        for (int x = 0; x < newImage.getWidth(); x++) {
            for (int y = 0; y < newImage.getHeight(); y++) {
                int color = newImage.getRGB(x, y);
                if (color == Color.WHITE.getRGB()) {
                    newImage.setRGB(x, y, 0x00ffffff); // ARGB for transparent
                }
            }
        }

        // Resize image if necessary
        java.awt.Image scaledInstance = newImage.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledInstance, 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }


    private byte[] getImageAsImageBytes(String id, String imageName, String widthString, String heightString, String signatureName, String dateModified) throws Exception {
        int width = Integer.parseInt((new DecimalFormat("#")).format(Double.parseDouble(widthString)));
        int height = Integer.parseInt((new DecimalFormat("#")).format(Double.parseDouble(heightString)));
        String formDefId = "FormsignAgreement";
        ApplicationContext ac = AppUtil.getApplicationContext();
        AppService appService = (AppService) ac.getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        FormData formData = new FormData();
        formData.setPrimaryKeyValue(id);
        Form loadForm = appService.viewDataForm(appDef.getId(), appDef.getVersion().toString(), formDefId, null, null, null, formData, null, null);

        Element elImg = FormUtil.findElement("image_signature", (Element) loadForm, formData);
        File srcImg = FileUtil.getFile(imageName, "ds_master_signatures", signatureName);

        BufferedImage image = ImageIO.read(srcImg);
        BufferedImage newImage = resizeAndConvertToTransparentBackground(image, width, height);
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int x = 0;
        int y = 0;
        int targetWidth = width;
        int targetHeight = height;
        if (width > height) {
            targetWidth = (int) (newImage.getWidth() / (double) newImage.getHeight() * height);
            x = (width - targetWidth) / 2;
        } else if (height > width) {
            targetHeight = (int) (newImage.getHeight() / (double) newImage.getWidth() * width);
            y = (height - targetHeight) / 2;
        }
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(newImage, x, y, targetWidth, targetHeight, null);

        // Format dateModified to DD MM YYYY
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSS");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy");
        Date date = inputFormat.parse(dateModified);
        String formattedDate = outputFormat.format(date);

        // Add formatted dateModified text below the image
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(formattedDate);
        int textX = (width - textWidth) / 2;
        int textY = y + targetHeight + fm.getHeight();

        // Ensure the text is within the image bounds
        if (textY + fm.getDescent() > height) {
            textY = height - fm.getDescent();
        }

        g2d.drawString(formattedDate, textX, textY);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.setUseCache(false);
        ImageIO.write(scaledImage, "png", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        return imageInByte;
    }

    private static BufferedImage resizeAndConvertToTransparentBackground(BufferedImage image, int newWidth, int newHeight) {
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return newImage;
    }

    private List<Map> getAnnotationData(String primaryKey) {
        debugMessage("getAnnotationData ........");
        List<Map> list = new ArrayList<>();
        Connection con = null;
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();
            if (!con.isClosed()) {
                // Retrieve c_hide_adjustby_signature, c_adjust_by, and c_approver_lv_1
                PreparedStatement stmtDocOrder = con.prepareStatement("SELECT c_hide_adjust_by_signature, c_adjust_by, c_approver_lv_1 FROM app_fd_ds_document_order WHERE id = ?");
                stmtDocOrder.setObject(1, primaryKey);
                ResultSet rsDocOrder = stmtDocOrder.executeQuery();
                boolean hideAdjustBySignature = false;
                Set<String> adjustBySet = new HashSet<>();
                Set<String> approverLv1Set = new HashSet<>();
                if (rsDocOrder.next()) {
                    hideAdjustBySignature = "yes".equalsIgnoreCase(rsDocOrder.getString("c_hide_adjust_by_signature"));
                    adjustBySet.addAll(Arrays.asList(rsDocOrder.getString("c_adjust_by").split(";")));
                    approverLv1Set.addAll(Arrays.asList(rsDocOrder.getString("c_approver_lv_1").split(";")));
                }

                // Retrieve annotation data
                PreparedStatement stmt = con.prepareStatement("SELECT DISTINCT anno.*, aggr.c_annotateData, aggr.c_selectedSignature, aggr.c_name FROM app_fd_ds_doc_annotation anno with (nolock) " +
                        " INNER JOIN app_fd_ds_doc_agreements aggr with (nolock) ON anno.c_request_id = aggr.c_request_id and anno.c_name = aggr.c_name" +
                        " WHERE anno.c_request_id = ? AND aggr.c_request_id = ?");
                stmt.setObject(1, primaryKey);
                stmt.setObject(2, primaryKey);
                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    String name = rs.getString("c_name");
                    if (hideAdjustBySignature && adjustBySet.contains(name) && !approverLv1Set.contains(name)) {
                        continue; // Skip this signature
                    }
                    Map<Object, Object> map = new HashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String key = meta.getColumnName(i);
                        String value = rs.getString(key);
                        debugMessage("SIGNATURE VALUES :" + value);
                        map.put(key, value);
                    }
                    list.add(map);
                }
            }
        } catch (Exception ex) {
            LogUtil.error(PDFFileDownloadCustom.class.getName(), ex, "");
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException sQLException) {
            }
        }
        return list;
    }

    private List<Map> getQrAnnotationData(String primaryKey) {
        debugMessage("GetQRAnnotation ........");
        List<Map> list = new ArrayList<>();
        Connection con = null;
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();
            if (!con.isClosed()) {
                PreparedStatement stmt = con.prepareStatement("SELECT qr_anno.* FROM app_fd_ds_qr_pos qr_anno with (nolock) WHERE qr_anno.c_request_id = ?");
                stmt.setObject(1, primaryKey);
                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<Object, Object> map = new HashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String key = meta.getColumnName(i);
                        String value = rs.getString(key);
                        debugMessage("QR VALUES :" + value);
                        map.put(key, value);
                    }
                    list.add(map);
                }
            }
        } catch (Exception ex) {
            LogUtil.error(PDFFileDownloadCustom.class.getName(), ex, "");
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException sQLException) {
            }
        }
        return list;
    }

    private void debugMessage(String message) {
        if ("true".equalsIgnoreCase(getPropertyString("debugMode")))
            LogUtil.info(""+PDFFileDownloadCustom.class.getName(), "DEBUG MODE: " + message);
    }

    private List<Map> getSignatureData(String primaryKey, String name) {
        debugMessage( "getSignatureData");
        List<Map> list = new ArrayList<>();
        Connection con = null;
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();
            if (!con.isClosed()) {
                PreparedStatement stmt = con.prepareStatement("select anno.*,ddo.c_signature_type as signature_type,dsms.c_text_signature,dsms.c_image_signature,dsms.c_signature,dsms.c_font, ddo.dateModified "
                        + "from app_fd_ds_doc_annotation anno with (nolock) "
                        + "left join app_fd_ds_document_order ddo with (nolock) on ddo.id=anno.c_request_id "
                        + "left join app_fd_ds_master_signatures dsms with (nolock) on dsms.id=anno.c_name "
                        + "where anno.c_request_id = ? and anno.c_name = ?");
                stmt.setObject(1, primaryKey);
                stmt.setObject(2, name);
                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<Object, Object> map = new HashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String key = meta.getColumnName(i);
                        String value = rs.getString(key);
                        map.put(key, value);
                    }
                    list.add(map);
                }
            }
        } catch (Exception ex) {
            LogUtil.error(PDFFileDownloadCustom.class.getName(), ex, "");
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException sQLException) {
            }
        }
        return list;
    }


    private byte[] getStylishSignatureAsImageBytes(String name, String widthString, String heightString, String fontName)  {
        debugMessage( "getStylishSignatureAsImageBytes");
        byte[] imageInByte = null;
        try {
            float fontSize = Float.parseFloat(getPropertyString("stylishFontSize"));
            if (fontName.isEmpty()) {
                fontName = getPropertyString("stylishFont");
            }
            int fontFormat = 0;
            if (fontName.equalsIgnoreCase("Ampunsuhu") || fontName.equalsIgnoreCase("Somelove")) {
                fontFormat = 1;
                fontName = fontName + ".otf";
            } else {
                fontName = fontName + ".ttf";
            }

            int width = Integer.parseInt((new DecimalFormat("#")).format(Double.parseDouble(widthString)));
            int height = Integer.parseInt((new DecimalFormat("#")).format(Double.parseDouble(heightString)));

            BufferedImage offscreenImage = new BufferedImage(width, height, 2);
            Graphics2D g2 = offscreenImage.createGraphics();
            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Color transparentColor = new Color(0, 0, 0, 0);
            g2.setColor(transparentColor);
            g2.fillRect(0, 0, width, height);
            g2.setRenderingHints(rh);
            g2.setColor(Color.black);
            PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
            debugMessage( "fontName :" + fontName);

            InputStream font = pluginManager.getPluginResource(getClassName(), "/fonts/"+fontName);
            InputStream font2 = pluginManager.getPluginResource(getClassName(), "/fonts/" + fontName);

            java.awt.Font fontCustom = java.awt.Font.createFont(fontFormat, font).deriveFont(fontSize);
            int maxSize = calculateMaxFontSize(g2, fontCustom, name, width, height);
            String size = Integer.toString(maxSize);
            float floatValue = Float.parseFloat(size);
            java.awt.Font customFontFinal = java.awt.Font.createFont(fontFormat, font2).deriveFont(floatValue);

            FontMetrics metrics = g2.getFontMetrics(customFontFinal);
            g2.setFont(customFontFinal);
            int x = (width - metrics.stringWidth(name)) / 2;
            int y = (height - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(name, x, y);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.setUseCache(false);
            ImageIO.write(offscreenImage, "png", baos);
            baos.flush();
            imageInByte = baos.toByteArray();
        }catch (Exception ex) {
            LogUtil.error(getClassName(), ex, ex.getMessage());
        }

        return imageInByte;
    }

    private static BaseFont createBaseFont(InputStream inputStream) throws Exception {
        byte[] fontData = readFontData(inputStream);

        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontData, null);
    }

    private static byte[] readFontData(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        return buffer;
    }

    private static java.awt.Font convertToAwtFont(Font font) throws Exception {
        BaseFont baseFont = font.getBaseFont();
        String fontName = baseFont.getPostscriptFontName();
        int fontStyle = font.getStyle();
        int fontSize = (int) font.getSize();

        return new java.awt.Font(fontName, fontStyle, fontSize);
    }


    private int calculateMaxFontSize(Graphics2D g2, java.awt.Font font, String text, int maxWidth, int maxHeight) {
        int textWidth, textHeight, size = 1;

        do {
            font = font.deriveFont((float) size);
            g2.setFont(font);
            FontMetrics metrics = g2.getFontMetrics();
            textWidth = metrics.stringWidth(text);
            textHeight = metrics.getHeight();
            size++;
            if (textWidth >= maxWidth || textHeight >= maxHeight) {
                break;
            }
        } while (true);

        return size - 1;
    }

    public byte[] getSignatureAsImageBytes(String data, String widthString, String heightString, String dateModified) throws Exception {
        byte[] imageInByte;
        try {
            int width = Integer.parseInt((new DecimalFormat("#")).format(Double.parseDouble(widthString)));
            int height = Integer.parseInt((new DecimalFormat("#")).format(Double.parseDouble(heightString)));
            BufferedImage originalImage = this.convertJsonToImage(data, width, height);

            // Calculate the new dimensions to maintain the aspect ratio
            double aspectRatio = (double) originalImage.getWidth() / originalImage.getHeight();
            int newWidth, newHeight;
            if (width / height > aspectRatio) {
                newHeight = height;
                newWidth = (int) (height * aspectRatio);
            } else {
                newWidth = width;
                newHeight = (int) (width / aspectRatio);
            }

            // Calculate starting points to center the image both horizontally and vertically
            int startX = (width - newWidth) / 2;
            int startY = (height - newHeight) / 2;

            // Create a new image with the correct aspect ratio and fill background to clear
            BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, width, height);

            // Draw the resized and centered image
            g2d.setComposite(AlphaComposite.Src);
            g2d.drawImage(originalImage, startX, startY, newWidth, newHeight, null);

            // Format dateModified to DD MM YYYY
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSS");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy");
            Date date = inputFormat.parse(dateModified);
            String formattedDate = outputFormat.format(date);

            // Add formatted dateModified text below the signature
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(formattedDate);
            int textX = (width - textWidth) / 2;
            int textY = startY + newHeight + fm.getHeight();

            // Ensure the text is within the image bounds
            if (textY + fm.getDescent() > height) {
                textY = height - fm.getDescent();
            }

            g2d.drawString(formattedDate, textX, textY);

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", baos);
            baos.flush();
            imageInByte = baos.toByteArray();
            baos.close();
        } catch (Exception e) {
            LogUtil.error(this.getClassName(), e, "");
            throw e;
        }
        return imageInByte;
    }

    public BufferedImage convertJsonToImage(String jsonString, int width, int height) {
        Gson gson = new Gson();
        PDFFileDownloadCustom.SignatureLine[] signatureLines = gson.fromJson(jsonString, PDFFileDownloadCustom.SignatureLine[].class);

        double minX = (double) Integer.MAX_VALUE;
        double minY = (double) Integer.MAX_VALUE;
        double maxX = 0;
        double maxY = 0;
        for (PDFFileDownloadCustom.SignatureLine line : signatureLines) {
            minX = Math.min(minX, Math.min(line.lx, line.mx));
            minY = Math.min(minY, Math.min(line.ly, line.my));
            maxX = Math.max(maxX, Math.max(line.lx, line.mx));
            maxY = Math.max(maxY, Math.max(line.ly, line.my));
        }

        double signatureWidth = maxX - minX;
        double signatureHeight = maxY - minY;
        double scaleX = width / (double) signatureWidth;
        double scaleY = height / (double) signatureHeight;
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (signatureWidth * scale);
        int scaledHeight = (int) (signatureHeight * scale);

        int startX = (width - scaledWidth) / 2;
        int startY = (height - scaledHeight) / 2;

        BufferedImage offscreenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = offscreenImage.createGraphics();

        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, width, height);

        g2.setComposite(AlphaComposite.Src);
        g2.setColor(Color.black);
        g2.setStroke(new BasicStroke(2.0F));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (PDFFileDownloadCustom.SignatureLine line : signatureLines) {
            int scaledLx = (int) ((line.lx - minX) * scale) + startX;
            int scaledLy = (int) ((line.ly - minY) * scale) + startY;
            int scaledMx = (int) ((line.mx - minX) * scale) + startX;
            int scaledMy = (int) ((line.my - minY) * scale) + startY;
            g2.drawLine(scaledLx, scaledLy, scaledMx, scaledMy);
        }

        g2.dispose();
        return offscreenImage;
    }



    private void writeHeader(PdfContentByte canvas, int pageNumber, String content) throws IOException {
        Phrase phrase = new Phrase();
        Font font = new Font(
                Integer.parseInt(getPropertyString("headerFontFamily")),
                Float.parseFloat(getPropertyString("headerFontSize")),
                Integer.parseInt(getPropertyString("headerFontStyle")),
                hex2Rgb(getPropertyString("headerFontColor"))
        );
        phrase.setFont(font);
        phrase.add(content);
        Rectangle rectangle = this.document.getPageSizeWithRotation(pageNumber);
        float pageWidth = this.document.getPageSize(pageNumber).getWidth();
        float pageHeight = this.document.getPageSize(pageNumber).getHeight();
        canvas.saveState();
        PdfGState gs1 = new PdfGState();
        gs1.setFillOpacity(Float.parseFloat(getPropertyString("headerFontOpacity")));
        canvas.setGState(gs1);
        if (rectangle.getWidth() == pageWidth && rectangle.getHeight() == pageHeight) {
            float pageCentreXPosition = pageWidth / 2.0F;
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreXPosition, pageHeight - 20.0F, 0.0F);
        } else if (rectangle.getHeight() >= rectangle.getWidth()) {
            float pageCentreXPosition = pageWidth / 2.0F;
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreXPosition, pageHeight - 20.0F, 0.0F);
        } else {
            float pageCentreXPosition = pageHeight / 2.0F;
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreXPosition, pageWidth - 20.0F, 0.0F);
        }
        canvas.restoreState();
    }

    private void writeBody(PdfContentByte canvas, int pageNumber, String content) throws IOException {
        Phrase phrase = new Phrase();
        Font font = new Font(
                Integer.parseInt(getPropertyString("bodyFontFamily")),
                Float.parseFloat(getPropertyString("bodyFontSize")),
                Integer.parseInt(getPropertyString("bodyFontStyle")),
                hex2Rgb(getPropertyString("bodyFontColor"))
        );
        phrase.setFont(font);
        phrase.add(content);
        Rectangle rectangle = this.document.getPageSizeWithRotation(pageNumber);
        float pageWidth = this.document.getPageSize(pageNumber).getWidth();
        float pageHeight = this.document.getPageSize(pageNumber).getHeight();
        float pageCentreXPosition = pageWidth / 2.0F;
        float pageCentreYPosition = pageHeight / 2.0F;
        canvas.saveState();
        PdfGState gs1 = new PdfGState();
        gs1.setFillOpacity(Float.parseFloat(getPropertyString("bodyFontOpacity")));
        canvas.setGState(gs1);
        if (rectangle.getWidth() == pageWidth && rectangle.getHeight() == pageHeight) {
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreXPosition, pageCentreYPosition, 320.0F);
        } else if (rectangle.getHeight() >= rectangle.getWidth()) {
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreXPosition, pageCentreYPosition, 305.0F);
        } else {
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreYPosition, pageCentreXPosition, 320.0F);
        }
        canvas.restoreState();
    }

    private void writeFooter(PdfContentByte canvas, int pageNumber, String content) throws IOException {
        Phrase phrase = new Phrase();
        Font font = new Font(
                Integer.parseInt(getPropertyString("footerFontFamily")),
                Float.parseFloat(getPropertyString("footerFontSize")),
                Integer.parseInt(getPropertyString("footerFontStyle")),
                hex2Rgb(getPropertyString("footerFontColor"))
        );
        phrase.setFont(font);
        phrase.add(content);
        Rectangle rectangle = this.document.getPageSizeWithRotation(pageNumber);
        float pageWidth = this.document.getPageSize(pageNumber).getWidth();
        float pageHeight = this.document.getPageSize(pageNumber).getHeight();
        canvas.saveState();
        PdfGState gs1 = new PdfGState();
        gs1.setFillOpacity(Float.parseFloat(getPropertyString("footerFontOpacity")));
        canvas.setGState(gs1);
        if (rectangle.getWidth() == pageWidth && rectangle.getHeight() == pageHeight) {
            float pageCentreXPosition = pageWidth / 2.0F;
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreXPosition, 10.0F, 0.0F);
        } else if (rectangle.getHeight() >= rectangle.getWidth()) {
            float pageCentreXPosition = pageWidth / 2.0F;
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreXPosition, 10.0F, 0.0F);
        } else {
            float pageCentreXPosition = pageHeight / 2.0F;
            ColumnText.showTextAligned(canvas, 1, phrase, pageCentreXPosition, 10.0F, 0.0F);
        }
        canvas.restoreState();
    }

    protected void singlePdf(HttpServletRequest request, HttpServletResponse response, String rowKey) throws Exception, IOException, ServletException {
        byte[] processedPdf = processPDF(rowKey);
        writeResponse(request, response, processedPdf, rowKey + ".pdf", "application/pdf");
    }

    protected void multiplePdfs(HttpServletRequest request, HttpServletResponse response, String[] rowKeys) throws Exception, IOException, ServletException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(baos);


        try {
            int counter = 1;
            for (String id : rowKeys) {

                byte[] processedPdf = processPDF(id);

                zip.putNextEntry(new ZipEntry(counter + "-" + id + ".pdf"));
                zip.write(processedPdf);
                zip.closeEntry();

                counter++;
            }

            zip.finish();
            writeResponse(request, response, baos.toByteArray(), getLinkLabel() + ".zip", "application/zip");
        } finally {
            baos.close();
            zip.flush();
        }
    }


    protected void writeResponse(HttpServletRequest request, HttpServletResponse response, byte[] bytes, String filename, String contentType) throws IOException, ServletException {
        ServletOutputStream servletOutputStream = response.getOutputStream();
        File file = null;
        try {
            String name = URLEncoder.encode(filename, "UTF8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=" + name + "; filename*=UTF-8''" + name);
            response.setContentType(contentType + "; charset=UTF-8");

            if (bytes.length > 0) {
                response.setContentLength(bytes.length);
                servletOutputStream.write(bytes);
            }
            file = new File(filename);
        } finally {
            servletOutputStream.flush();
            servletOutputStream.close();
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    public class SignatureLine {
        double lx;
        double ly;
        double mx;
        double my;

        public double getLx() {
            return this.lx;
        }

        public void setLx(double lx) {
            this.lx = lx;
        }

        public double getLy() {
            return this.ly;
        }

        public void setLy(double ly) {
            this.ly = ly;
        }

        public double getMx() {
            return this.mx;
        }

        public void setMx(double mx) {
            this.mx = mx;
        }

        public double getMy() {
            return this.my;
        }

        public void setMy(double my) {
            this.my = my;
        }
    }

}
