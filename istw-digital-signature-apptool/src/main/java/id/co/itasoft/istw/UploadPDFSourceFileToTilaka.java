package id.co.itasoft.istw;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//import java.util.*;
import org.joget.plugin.base.DefaultApplicationPlugin;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.ServletException;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FileUtil;
import org.joget.apps.form.service.FormService;
import org.joget.workflow.model.WorkflowAssignment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import id.co.itasoft.tilaka.library.ConstData;
import id.co.itasoft.tilaka.library.response.ResponseUpload;
import id.co.itasoft.tilaka.library.services.TilakaApiServices;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import javax.sql.DataSource;
import org.joget.apps.app.model.EnvironmentVariable;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import java.util.List;
import org.joget.apps.app.dao.EnvironmentVariableDao;

public class UploadPDFSourceFileToTilaka extends DefaultApplicationPlugin {

    public static String pluginName = "ISTW - DS - Upload PDFSource To Tilaka";

    private PdfReader document;

    @Override
    public Object execute(Map properties) {

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

        WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");

        //get current record id
        String recordId = appService.getOriginProcessId(assignment.getProcessId());

        debugMessage("recordId : " + recordId);

        byte[] processedPdf;
        Connection con = null;
        PreparedStatement psDSTTilakaSettings = null;
        ResultSet rdDSTilakaSettings = null;

        try {

            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();

            String queryDSTilakaSettings = "select id,c_env_value from app_fd_ds_tilaka_env_vars with (nolock) ";
            psDSTTilakaSettings = con.prepareStatement(queryDSTilakaSettings);
            rdDSTilakaSettings = psDSTTilakaSettings.executeQuery();

            while (rdDSTilakaSettings.next()) {

                String id = rdDSTilakaSettings.getString("id");

                switch (id) {
                    case "tilaka_adapter_api_hostname_url":
                        ConstData.tilaka_adapter_api_hostname_url = rdDSTilakaSettings.getString("c_env_value");
                        break;
                    case "tilaka_api_base_url":
                        ConstData.tilaka_api_base_url = rdDSTilakaSettings.getString("c_env_value");
                        break;
                    case "tilaka_channel_id":
                        ConstData.tilaka_channel_id = rdDSTilakaSettings.getString("c_env_value");
                        break;
                    case "tilaka_client_secret":
                        ConstData.tilaka_client_secret = rdDSTilakaSettings.getString("c_env_value");
                        break;
                    default:
                        break;
                }

            }

            queryDSTilakaSettings = "select id,c_action_path from app_fd_ds_tilaka_act_paths with (nolock) ";
            psDSTTilakaSettings = con.prepareStatement(queryDSTilakaSettings);
            rdDSTilakaSettings = psDSTTilakaSettings.executeQuery();

            while (rdDSTilakaSettings.next()) {

                String id = rdDSTilakaSettings.getString("id");

                switch (id) {
                    case "tilaka_action_auth":
                        ConstData.tilaka_action_auth = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_checkakundsexist":
                        ConstData.tilaka_action_checkakundsexist = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_checkcertstatus":
                        ConstData.tilaka_action_checkcertstatus = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_checksignstatus":
                        ConstData.tilaka_action_checksignstatus = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_executesign":
                        ConstData.tilaka_action_executesign = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_generateuuid":
                        ConstData.tilaka_action_generateuuid = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_register":
                        ConstData.tilaka_action_register = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_requestrevokecert":
                        ConstData.tilaka_action_requestrevokecert = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_requestsign":
                        ConstData.tilaka_action_requestsign = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_upload":
                        ConstData.tilaka_action_upload = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_userregstatus":
                        ConstData.tilaka_action_userregstatus = rdDSTilakaSettings.getString("c_action_path");
                        break;
                    default:
                        break;
                }

            }

            ///////
            processedPdf = processPDF(recordId);

            uploadFiletoTilaka(processedPdf, recordId, "application/pdf", con);

        } catch (Exception ex) {
            LogUtil.error(UploadPDFSourceFileToTilaka.class.getName(), ex, ex.getMessage());
        } finally {
            if (rdDSTilakaSettings != null) {
                try {
                    rdDSTilakaSettings.close();
                } catch (SQLException ex) {
                    LogUtil.error(UploadPDFSourceFileToTilaka.class.getName(), ex, ex.getMessage());
                }
            }
            if (psDSTTilakaSettings != null) {
                try {
                    psDSTTilakaSettings.close();
                } catch (SQLException ex) {
                    LogUtil.error(UploadPDFSourceFileToTilaka.class.getName(), ex, ex.getMessage());
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LogUtil.error(UploadPDFSourceFileToTilaka.class.getName(), ex, ex.getMessage());
                }
            }
        }

        return null;
    }

    public File convertBytesToFile(byte[] bytes, String filePath) {
        File file = new File(filePath);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
            return file;
        } catch (IOException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return null;
        }
    }

    protected void uploadFiletoTilaka(byte[] bytes, String recordId, String contentType, Connection con) throws IOException, ServletException {
        try {
            if (bytes.length > 0) {
                PreparedStatement psDSTTilakaUploadResp = null;
                try {

                    // Panggil metode untuk mengkonversi byte array ke file
                    File pdfFile = convertBytesToFile(bytes, recordId + ".pdf");
                    if (pdfFile != null) {
                        debugMessage("tilaka_adapter_api_hostname_url : " + ConstData.tilaka_adapter_api_hostname_url);
                        debugMessage("tilaka_api_base_url : " + ConstData.tilaka_api_base_url);
                        debugMessage("tilaka_channel_id : " + ConstData.tilaka_channel_id);
                        debugMessage("tilaka_client_secret : " + ConstData.tilaka_client_secret);

                        debugMessage("tilaka_action_auth : " + ConstData.tilaka_action_auth);
                        debugMessage("tilaka_action_checkakundsexist : " + ConstData.tilaka_action_checkakundsexist);
                        debugMessage("tilaka_action_checkcertstatus : " + ConstData.tilaka_action_checkcertstatus);
                        debugMessage("tilaka_action_checksignstatus : " + ConstData.tilaka_action_checksignstatus);
                        debugMessage("tilaka_action_executesign : " + ConstData.tilaka_action_executesign);
                        debugMessage("tilaka_action_generateuuid : " + ConstData.tilaka_action_generateuuid);
                        debugMessage("tilaka_action_register : " + ConstData.tilaka_action_register);
                        debugMessage("tilaka_action_requestrevokecert : " + ConstData.tilaka_action_requestrevokecert);
                        debugMessage("tilaka_action_requestsign : " + ConstData.tilaka_action_requestsign);
                        debugMessage("tilaka_action_upload : " + ConstData.tilaka_action_upload);
                        debugMessage("tilaka_action_userregstatus : " + ConstData.tilaka_action_userregstatus);
                        //do upload file to tilaka
                        ResponseUpload respUpload = null;
                        try (TilakaApiServices apiService = new TilakaApiServices(ConstData.tilaka_adapter_api_hostname_url)) {

                            try {
                                apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id,
                                        ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));

                                respUpload = apiService.upoadFiles(pdfFile);
                                debugMessage("resUploadToTilaka = " + respUpload.toString());

                                String queryDSTilakaUploadResp = "INSERT INTO app_fd_ds_tilaka_api_calls (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName, "
                                        + "c_last_response_code, c_last_action, c_last_response_data, c_last_action_status, c_filename) "
                                        + "VALUES (?, SYSDATETIME(), SYSDATETIME(), 'admin', 'Admin', 'admin', 'admin', ?, ?, ?, ?, ?)";
                                psDSTTilakaUploadResp = con.prepareStatement(queryDSTilakaUploadResp);
                                psDSTTilakaUploadResp.setString(1, recordId);
                                psDSTTilakaUploadResp.setString(2, respUpload.getResponseCode());
                                psDSTTilakaUploadResp.setString(3, "tilaka_action_upload");
                                if (respUpload.getSuccess()) {
                                    debugMessage("json to save : " + respUpload.getJsonString());
                                    psDSTTilakaUploadResp.setString(4, respUpload.getJsonString());
                                    psDSTTilakaUploadResp.setString(5, "OK");
                                    psDSTTilakaUploadResp.setString(6, respUpload.getFilename());
                                } else {
                                    psDSTTilakaUploadResp.setString(4, "");
                                    psDSTTilakaUploadResp.setString(5, "FAIL");
                                    psDSTTilakaUploadResp.setString(6, null);
                                }

                                psDSTTilakaUploadResp.executeUpdate();

                            } catch (IOException e) {
                                LogUtil.error(getClass().getName(), e, "Error on Upload to Tilaka : " + e.getMessage());

                                String queryDSTilakaUploadResp = "INSERT INTO app_fd_ds_tilaka_api_calls (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName, "
                                        + "c_last_response_code, c_last_action, c_last_response_data, c_last_action_status) "
                                        + "VALUES (?, SYSDATETIME(), SYSDATETIME(), 'admin', 'Admin', 'admin', 'admin', ?, ?, ?, ?)";
                                psDSTTilakaUploadResp = con.prepareStatement(queryDSTilakaUploadResp);
                                psDSTTilakaUploadResp.setString(1, recordId);
                                psDSTTilakaUploadResp.setString(2, "UNKNOWN");
                                psDSTTilakaUploadResp.setString(3, "tilaka_action_upload");

                                psDSTTilakaUploadResp.setString(4, e.getMessage());
                                psDSTTilakaUploadResp.setString(5, "FAIL");
                                psDSTTilakaUploadResp.executeUpdate();
                            } catch (Exception ex) {
                                LogUtil.error(getClass().getName(), ex, "Error on Upload to Tilaka : " + ex.getMessage());

                                String queryDSTilakaUploadResp = "INSERT INTO app_fd_ds_tilaka_api_calls (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName, "
                                        + "c_last_response_code, c_last_action, c_last_response_data, c_last_action_status) "
                                        + "VALUES (?, SYSDATETIME(), SYSDATETIME(), 'admin', 'Admin', 'admin', 'admin', ?, ?, ?, ?)";
                                psDSTTilakaUploadResp = con.prepareStatement(queryDSTilakaUploadResp);
                                psDSTTilakaUploadResp.setString(1, recordId);
                                psDSTTilakaUploadResp.setString(2, "UNKNOWN");
                                psDSTTilakaUploadResp.setString(3, "tilaka_action_upload");

                                psDSTTilakaUploadResp.setString(4, ex.getMessage());
                                psDSTTilakaUploadResp.setString(5, "FAIL");
                                psDSTTilakaUploadResp.executeUpdate();
                            }
                        }
                        if (respUpload != null) {
                            //save response data
                        }
                        //end do upload
                        pdfFile.delete();
                    } else {
                        LogUtil.error(getClass().getName(), new NullPointerException("Failed to created file"), "Output PDF Object is Null");
                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, e.getMessage());
                } finally {
                    if (psDSTTilakaUploadResp != null) {
                        try {
                            psDSTTilakaUploadResp.close();
                        } catch (SQLException ex) {
                            LogUtil.error(UploadPDFSourceFileToTilaka.class.getName(), ex, ex.getMessage());
                        }
                    }
                }
            } else {
                //file is empty
                LogUtil.error(getClass().getName(), new NullPointerException("File is empty"), "Output PDF File is Empty");
            }

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
        }
    }

    public String getName() {
        return pluginName;
    }

    public String getVersion() {
        return "1.0.0";
    }

    public String getDescription() {
        return pluginName;
    }

    public String getLabel() {
        return pluginName;
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getPropertyOptions() {
        return "";
    }

    // 1
    public byte[] getPdf(String primaryKeyValue) throws IOException {
        debugMessage("getPdf");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String formDefId = "FormDocumentUploads";
        String attachmentFieldID = "filedoc";
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        String fileName = "";
        Form form = null;
        String tableName = null;
        try {
            if (appDef != null && formDefId != null
                    && !formDefId.isEmpty() && primaryKeyValue != null
                    && !primaryKeyValue.isEmpty()) {
                FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
                if (formDef != null) {
                    String json = formDef.getJson();
                    form = (Form) formService.createElementFromJson(json);
                    if (form != null && form.getLoadBinder() != null) {
                        tableName = "ds_document_order";
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
        debugMessage("decodeFileName :" + decodedFileName);
        debugMessage("tableName :" + tableName);
        debugMessage("primaryKey :" + primaryKeyValue);
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
        debugMessage("PROCESSPDF");
        byte[] pdf = getPdf(rowKey);
        pdf = addQrCodeImage(pdf, rowKey);
        return pdf;
    }

    public byte[] addQrCodeImage(byte[] pdf, String primaryKeyValue) throws Exception {
        debugMessage("ADD Qr Signature");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            String appId = appDef.getAppId();

            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

            String variableqrUrlTemplate = "qrUrlTemplate"; // Replace with your app variable name
            String variableqrUrlTemplateValue = "";

            if (appDef != null) {
                EnvironmentVariableDao environmentVariableDao = (EnvironmentVariableDao) AppUtil.getApplicationContext().getBean("environmentVariableDao");
                Collection<EnvironmentVariable> environmentVariableList = environmentVariableDao.getEnvironmentVariableList(null, appDef, null, null, null, null);

                if (environmentVariableList != null && environmentVariableList.size() > 0) {

                    for (EnvironmentVariable e : environmentVariableList) {
                        debugMessage("ENV Name: " + e.getId() + ", ENV Value: " + e.getValue());
                        if (variableqrUrlTemplate.equals(e.getId())) {
                            variableqrUrlTemplateValue = e.getValue();
                            break;
                        }

                    }

                }
            }

            // Retrieve the variable by name
            String url = variableqrUrlTemplateValue + primaryKeyValue;
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
                            
                            debugMessage("generateQrCodeImage : " + url + ", scaledWidth: " + scaledWidth+", scaledHeight: "+scaledHeight);
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

    private java.util.List<Map> getQrAnnotationData(String primaryKey) {
        debugMessage("GetQRAnnotation ........");
        java.util.List<Map> list = new ArrayList<>();
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
            LogUtil.error(getClass().getName(), ex, "");
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException sQLException) {
            }
        }
        return list;
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

    private void debugMessage(String message) {
        boolean debug = true;
        if (debug) {
            LogUtil.info("" + UploadPDFSourceFileToTilaka.class.getName(), "DEBUG MODE: " + message);
        }
    }

}
