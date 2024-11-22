package id.co.itasoft.istw;

import com.lowagie.text.Image;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import java.util.*;
import org.joget.plugin.base.DefaultApplicationPlugin;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletException;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FileUtil;
import org.joget.workflow.model.WorkflowAssignment;
import java.io.File;
import java.io.IOException;
import id.co.itasoft.tilaka.library.ConstData;
import id.co.itasoft.tilaka.library.body.AttachedSignature;
import id.co.itasoft.tilaka.library.body.MasterSignatureData;
import id.co.itasoft.tilaka.library.body.PdfData;
import id.co.itasoft.tilaka.library.response.AuthUrl;
import id.co.itasoft.tilaka.library.response.ResponseRequestSign;
import id.co.itasoft.tilaka.library.services.TilakaApiServices;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import org.joget.commons.util.UuidGenerator;
import org.joget.workflow.model.WorkflowProcessResult;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RequestSignDocumentOnTilaka extends DefaultApplicationPlugin {

    public static final String pluginName = "ISTW DS ReqSign Doc on Tilaka";

    @Override
    public Object execute(Map properties) {

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

        WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");

        //get current record id
        String recordId = appService.getOriginProcessId(assignment.getProcessId());

        debugMessage("recordId : " + recordId);

        String fileName;
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

            fileName = getFilename(recordId, con);
            if (fileName != null && !fileName.isEmpty()) {
                requestSignToTilaka(fileName, recordId, con);
            } else {
                Exception x = new Exception("filename is not ready/not uploaded!");
                LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), x, x.getMessage());
            }

        } catch (Exception ex) {
            LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
        } finally {
            if (rdDSTilakaSettings != null) {
                try {
                    rdDSTilakaSettings.close();
                } catch (SQLException ex) {
                    LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
                }
            }
            if (psDSTTilakaSettings != null) {
                try {
                    psDSTTilakaSettings.close();
                } catch (SQLException ex) {
                    LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
                }
            }
        }

        return null;
    }

    private List<Map> getAnnotationData(String primaryKey) {
        debugMessage("getAnnotationData ........");
        List<Map> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();
            boolean isAllTilakaUserReady = true;
            if (!con.isClosed()) {
                boolean isHideAdjustBySignature = false;
                stmt = con.prepareStatement("select * from app_fd_ds_document_order "
                        + "where id = ? "
                        + "and c_hide_adjust_by_signature = 'yes'");

                stmt.setObject(1, primaryKey);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    isHideAdjustBySignature = true;
                }
                if (!isHideAdjustBySignature) {
                    stmt = con.prepareStatement("select distinct anno.*, tuser.c_user_tilaka,"
                            + " (case "
                            + " when anno.c_approval = 'Adjusted By' then 1"
                            + " when anno.c_approval = 'Adjusted By | Approver By - 1' then 2"
                            + " when anno.c_approval = 'Approver By - 1' then 3"
                            + " else 4 end) as sort_num FROM app_fd_ds_doc_annotation anno with (nolock) "
                            + " left join app_fd_ds_tilaka_users tuser with (nolock) on tuser.id = anno.c_name"
                            + " where anno.c_request_id = ?"
                            + " order by case "
                            + " when anno.c_approval = 'Adjusted By' then 1"
                            + " when anno.c_approval = 'Adjusted By | Approver By - 1' then 2"
                            + " when anno.c_approval = 'Approver By - 1' then 3"
                            + " else 4"
                            + " end");
                }else{
                    stmt = con.prepareStatement("select distinct anno.*, tuser.c_user_tilaka,"
                            + " (case "
                            + " when anno.c_approval = 'Adjusted By' then 1"
                            + " when anno.c_approval = 'Adjusted By | Approver By - 1' then 2"
                            + " when anno.c_approval = 'Approver By - 1' then 3"
                            + " else 4 end) as sort_num FROM app_fd_ds_doc_annotation anno with (nolock) "
                            + " left join app_fd_ds_tilaka_users tuser with (nolock) on tuser.id = anno.c_name"
                            + " where anno.c_request_id = ? and (anno.c_approval = 'Adjusted By | Approver By - 1' or anno.c_approval = 'Approver By - 1')"
                            + " order by case "
                            + " when anno.c_approval = 'Adjusted By' then 1"
                            + " when anno.c_approval = 'Adjusted By | Approver By - 1' then 2"
                            + " when anno.c_approval = 'Approver By - 1' then 3"
                            + " else 4"
                            + " end");                                    
                }
                stmt.setObject(1, primaryKey);
                rs = stmt.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<Object, Object> map = new HashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String key = meta.getColumnName(i);
                        String value = rs.getString(key);

                        debugMessage("SIGNATURE VALUES :" + value);
                        map.put(key, value);

                        if (key.equals("c_annotation_data")) {
                            // Original JSON data
                            String jsonData = value;

                            // Parse the original JSON object
                            JSONObject jsonObject = new JSONObject(jsonData);
                            JSONArray pages = jsonObject.getJSONArray("pages");

                            // Prepare the output JSON array
                            JSONArray outputArray = new JSONArray();

                            // Loop through the pages
                            for (int pageNumber = 0; pageNumber < pages.length(); pageNumber++) {
                                JSONObject page = pages.getJSONObject(pageNumber);
                                JSONArray objects = page.getJSONArray("objects");

                                // Loop through the objects
                                for (int objectNumber = 0; objectNumber < objects.length(); objectNumber++) {
                                    JSONObject obj = objects.getJSONObject(objectNumber);

                                    // Create a new object for the output
                                    JSONObject outputObject = new JSONObject();
                                    outputObject.put("pageNumber", pageNumber + 1); // Page number starts from 1
                                    outputObject.put("objectNumber", objectNumber + 1); // Object number starts from 1
                                    outputObject.put("top", obj.getDouble("top"));
                                    outputObject.put("left", obj.getDouble("left"));
                                    outputObject.put("height", obj.getDouble("height"));
                                    outputObject.put("width", obj.getDouble("width"));
                                    outputObject.put("scaleX", obj.getDouble("scaleX"));
                                    outputObject.put("scaleY", obj.getDouble("scaleY"));
                                    outputObject.put("typeName", "ImageSignature");

                                    // Add to output array
                                    outputArray.put(outputObject);
                                }
                            }

                            debugMessage(outputArray.toString(4));
                            map.put("c_annotateData", outputArray.toString());
                        } else if (key.equals("c_signature_count")) {
                            int signCount = Integer.parseInt(value);
                            String c_selectedSignature = "";
                            for (int itrscount = 1; itrscount <= signCount; itrscount++) {
                                if (itrscount == 1) {
                                    c_selectedSignature += "" + itrscount;
                                } else {
                                    c_selectedSignature += "," + itrscount;
                                }
                            }
                            map.put("c_selectedSignature", c_selectedSignature);
                        } else if (key.equals("c_user_tilaka")) {
                            if (value == null || value.isEmpty()) {
                                isAllTilakaUserReady = false;
                                list = null;
                                Exception x = new Exception("Tilaka user is not ready for id : " + primaryKey);
                                LogUtil.error(getClassName(), x, x.getMessage());
                                break;
                            }
                        }
                    }
                    if (!isAllTilakaUserReady) {
                        break;
                    }
                    list.add(map);
                }
            }

        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "");
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException sQLException) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException sQLException) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException sQLException) {
            }
        }
        return list;
    }

    private List<Map> getSignatureData(String primaryKey, String name) {
        debugMessage("getSignatureData");
        List<Map> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();
            if (!con.isClosed()) {
                stmt = con.prepareStatement("select anno.*,ddo.c_signature_type as signature_type,dsms.c_text_signature,dsms.c_image_signature,dsms.c_signature,dsms.c_font, ddo.dateModified "
                        + "from app_fd_ds_doc_annotation anno with (nolock) "
                        + "left join app_fd_ds_document_order ddo with (nolock) on ddo.id=anno.c_request_id "
                        + "left join app_fd_ds_master_signatures dsms with (nolock) on dsms.id=anno.c_name "
                        + "where anno.c_request_id = ? and anno.c_name = ?");
                stmt.setObject(1, primaryKey);
                stmt.setObject(2, name);
                rs = stmt.executeQuery();
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
            LogUtil.error(getClassName(), ex, ex.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException sQLException) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException sQLException) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException sQLException) {
            }
        }
        return list;
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

    private byte[] getImageAsImageBytes(String id, String imageName, String widthString, String heightString, String signatureName, String dateModified) throws Exception {
        int width = Integer.parseInt((new DecimalFormat("#")).format(Double.parseDouble(widthString)));
        int height = Integer.parseInt((new DecimalFormat("#")).format(Double.parseDouble(heightString)));

        FormData formData = new FormData();
        formData.setPrimaryKeyValue(id);

        File srcImg = FileUtil.getFile(imageName, "ds_master_signatures", signatureName);

        if (srcImg == null) {
            Exception x = new Exception("master signature file is null, ds_master_signatures, imageName : " + imageName + ", signatureName : " + signatureName);
            LogUtil.error(getClassName(), x, x.getMessage());
        }

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

    protected void requestSignToTilaka(String fileName, String recordId, Connection con) throws IOException, ServletException {
        try {

            PreparedStatement psDSRequestDocSign = null, psDSCreateDocSignData = null, psDSSendEmailToTilakaData = null;

            ResultSet rsDSSendEmailToTilakaData = null;

            try {
                List<MasterSignatureData> msds = new ArrayList<>();

                List<PdfData> listPdf = new ArrayList<>();
                PdfData pdfData1 = new PdfData();
                pdfData1.setFilename(fileName);

                List<AttachedSignature> attachedSignatures = new ArrayList<>();

                List<Map> ann = getAnnotationData(recordId);

                if (ann == null) {
                    debugMessage("annotation is null for record Id :  " + recordId);
                    return;
                }

                debugMessage("ann size : " + ann.size());

                for (int i = 0; i < ann.size(); i++) {
                    Map row = ann.get(i);
                    String rowPage = (String) row.get("c_annotateData");
                    String rowData = (String) row.get("c_selectedSignature");
                    String rowName = (String) row.get("c_name");
                    String dateModified = (String) row.get("dateModified");
                    String user_tilaka = (String) row.get("c_user_tilaka");
                    rowName = rowName.trim();

                    debugMessage("SignatureImage - c_annotateData : " + rowPage);
                    debugMessage("SignatureImage - c_selectedSignature : " + rowData);
                    debugMessage("SignatureImage - rowName : " + rowName);
                    debugMessage("SignatureImage - dateModified : " + dateModified);
                    debugMessage("SignatureImage - user_tilaka : " + user_tilaka);

                    MasterSignatureData sign = new MasterSignatureData();

                    sign.setUserIdentifier(user_tilaka);

                    List<Map> signatures = getSignatureData(recordId, rowName);

                    byte[] signatureImageAsBytes = null;

                    if (signatures.isEmpty()) {
                        debugMessage("No signature found");
                    } else {
                        Map<String, String> signature = signatures.get(0);
                        debugMessage("Signature found [" + signature.toString() + "]");

                        String signatureImageName = null;
                        String signatureId = null;

                        String signatureName = signature.get("c_name");

                        debugMessage("Image Signature Found");
                        signatureImageName = signature.get("c_image_signature");
                        signatureId = signature.get("id");

                        JSONArray markers = new JSONArray(rowPage);
                        String[] selectedSignature = rowData.split(",");
                        for (String Signature : selectedSignature) {
                            AttachedSignature atchSign = new AttachedSignature();
                            atchSign.setUserIdentifier(user_tilaka);

                            JSONObject marker = markers.getJSONObject(Integer.parseInt(Signature) - 1);
                            debugMessage(marker.toString());
                            int page = Integer.parseInt(marker.getString("pageNumber"));
                            float left = 0.0F;
                            float top = 0.0F;
                            float height = 0.0F;
                            float width = 0.0F;
                            try {
                                left = Float.parseFloat(marker.getString("left"));
                                top = Float.parseFloat(marker.getString("top"));
                                height = Float.parseFloat(marker.getString("height")) * Float.parseFloat(marker.getString("scaleY"));
                                width = Float.parseFloat(marker.getString("width")) * Float.parseFloat(marker.getString("scaleX"));
                                debugMessage("Marker Dimension - Left[" + left + "] Top[" + top + "] Height[" + height + "] Width[" + width + "]");
                            } catch (Exception ex) {
                                LogUtil.error(getClassName(), ex, "Cannot get marker dimension - " + marker.toString());
                                debugMessage("Marker Dimension Error");
                            }

                            Image img = null;

                            debugMessage("Signature Image - Generating Image Signature");

                            if (signatureId != null) {
                                if (signatureImageAsBytes == null) {
                                    signatureImageAsBytes = getImageAsImageBytes(signatureId, signatureImageName, Float.toString(width), Float.toString(height), signatureName, dateModified);
                                }
                                img = Image.getInstance(signatureImageAsBytes);
                                img.setDpi(300, 300);
                            }

                            if (img != null) {

                                float newHeight = img.getHeight();
                                float newWidth = img.getWidth();
                                debugMessage("Image Height: " + newHeight);
                                debugMessage("Image Width: " + newWidth);

                                atchSign.setWidth((int) newWidth);
                                atchSign.setHeight((int) newHeight);
                                atchSign.setCoordinate_x((int) left);
                                atchSign.setCoordinate_y((int) top);
                                atchSign.setPage_number(page);

                                //end
                            } else {
                                debugMessage("Signature Image - Missing");
                                debugMessage("Signature image missing");
                            }
                            debugMessage("attaching signature : " + atchSign.toString());
                            attachedSignatures.add(atchSign);
                        }
                    }

                    if (signatureImageAsBytes == null) {
                        throw new Exception("bytes signature image not found for user_tilaka : " + user_tilaka);
                    }

                    String base64String = Base64.getEncoder().encodeToString(signatureImageAsBytes);

                    //sign.setSignatureImage("data:image/jpeg;base64," + base64String);
                    sign.setSignatureImage(null);
                    sign.setSequence(i + 1);

                    debugMessage("adding master signature : " + sign.toString());
                    msds.add(sign);

                }

                debugMessage("attachedSignatures : " + attachedSignatures.toString());

                pdfData1.setSignatures(attachedSignatures);

                debugMessage("pdfData : " + pdfData1.toString());

                listPdf.add(pdfData1);

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

                ResponseRequestSign respRequestSign = null;
                try (TilakaApiServices apiService = new TilakaApiServices(ConstData.tilaka_adapter_api_hostname_url)) {

                    try {
                        apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id,
                                ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));

                        ///
                        respRequestSign = apiService.requestSign(recordId, msds, listPdf);
                        debugMessage("resRequestSignToTilaka = " + respRequestSign.toString());

                        String queryDSTilakaUploadResp = "update app_fd_ds_tilaka_api_calls set c_last_response_code=?, "
                                + "c_last_action = ?, c_last_response_data = ?, c_last_action_status = ?, dateModified=SYSDATETIME() where id=?";
                        psDSRequestDocSign = con.prepareStatement(queryDSTilakaUploadResp);
                        psDSRequestDocSign.setString(1, respRequestSign.getResponseCode());
                        psDSRequestDocSign.setString(2, "tilaka_action_requestsign");
                        if (respRequestSign.getSuccess()) {
                            debugMessage("json to save : " + respRequestSign.getJsonString());
                            psDSRequestDocSign.setString(3, respRequestSign.getJsonString());
                            psDSRequestDocSign.setString(4, "OK");

                            ////
                            for (MasterSignatureData msd : msds) {
                                //insert into app_fd_ds_tilaka_api_signs
                                String uuid = UuidGenerator.getInstance().getUuid();

                                String queryDSTilakaSigns = "insert into app_fd_ds_tilaka_api_signs (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName, "
                                        + "c_tilaka_request_id, c_auth_url, c_tilaka_id, c_status, c_sequence) "
                                        + "values (?, SYSDATETIME(), SYSDATETIME(), 'admin', 'Admin', 'admin', 'admin', ?, ?, ?, ?, ?)";
                                psDSCreateDocSignData = con.prepareStatement(queryDSTilakaSigns);
                                psDSCreateDocSignData.setString(1, uuid);
                                psDSCreateDocSignData.setString(2, recordId);
                                psDSCreateDocSignData.setString(3, "");
                                psDSCreateDocSignData.setString(4, msd.getUserIdentifier());
                                psDSCreateDocSignData.setString(5, "AWAITING");
                                psDSCreateDocSignData.setString(6, "" + msd.getSequence());

                                psDSCreateDocSignData.executeUpdate();
                            }

                            String queryDSTilakaSignsSeq = "insert into app_fd_ds_tilaka_sign_seq (id, dateCreated, dateModified, "
                                    + "createdBy, createdByName, modifiedBy, modifiedByName, c_next_sequence) "
                                    + "values (?, SYSDATETIME(), SYSDATETIME(), 'admin', 'Admin', 'admin', 'admin', '1')";

                            psDSCreateDocSignData = con.prepareStatement(queryDSTilakaSignsSeq);
                            psDSCreateDocSignData.setString(1, recordId);
                            psDSCreateDocSignData.executeUpdate();

                            try {
                                String queryDSSendEmailToTilakaData = "select tas.c_tilaka_request_id, do.c_doc_id as doc_ext_id, do.c_name as doc_name, do.c_description as doc_description, "
                                        + "do.c_filedoc as doc_filename, string_agg(tu.id, ';') within group (order by tas.c_sequence asc) as approver_ids, do.createdBy as doc_requester "
                                        + "from app_fd_ds_tilaka_api_signs tas with (nolock) "
                                        + "inner join app_fd_ds_tilaka_users tu with (nolock) on tu.c_user_tilaka = tas.c_tilaka_id "
                                        + "inner join app_fd_ds_document_order do with (nolock) on do.id=tas.c_tilaka_request_id "
                                        + "where do.id = ? "
                                        + "group by tas.c_tilaka_request_id,do.c_doc_id, do.c_name,do.c_description,do.c_filedoc, do.createdBy ";

                                psDSSendEmailToTilakaData = con.prepareStatement(queryDSSendEmailToTilakaData);
                                psDSSendEmailToTilakaData.setString(1, recordId);
                                rsDSSendEmailToTilakaData = psDSSendEmailToTilakaData.executeQuery();

                                if (rsDSSendEmailToTilakaData.next()) {
                                    String tilaka_request_id = rsDSSendEmailToTilakaData.getString("c_tilaka_request_id");
                                    String doc_ext_id = rsDSSendEmailToTilakaData.getString("doc_ext_id");
                                    String doc_name = rsDSSendEmailToTilakaData.getString("doc_name");
                                    String doc_description = rsDSSendEmailToTilakaData.getString("doc_description");
                                    String doc_filename = rsDSSendEmailToTilakaData.getString("doc_filename");
                                    String approver_ids = rsDSSendEmailToTilakaData.getString("approver_ids");
                                    String doc_requester = rsDSSendEmailToTilakaData.getString("doc_requester");

                                    WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");

                                    String processDefId = "istwDigitalSign:latest:sendEmailToTilakaApproverProcess";

                                    Map variables = new HashMap();
                                    variables.put("tilaka_request_id", tilaka_request_id);
                                    variables.put("doc_ext_id", doc_ext_id);
                                    variables.put("doc_name", doc_name);
                                    variables.put("doc_description", doc_description);
                                    variables.put("doc_filename", doc_filename);
                                    variables.put("doc_approveby", approver_ids);

                                    String sendEmailToTilakaApproverUuid = UuidGenerator.getInstance().getUuid();

                                    WorkflowProcessResult result = workflowManager.processStart(processDefId, null, variables, doc_requester, sendEmailToTilakaApproverUuid, false);
                                    debugMessage("Processing istwDigitalSign:latest:sendEmailToTilakaApproverProcess - Record: " + sendEmailToTilakaApproverUuid + " - Status: " + result.getProcess().getInstanceId());

                                }
                            } catch (SQLException e) {
                                LogUtil.error(getClass().getName(), e, e.getMessage());
                            }

                        } else {
                            psDSRequestDocSign.setString(3, "");
                            psDSRequestDocSign.setString(4, "FAIL");

                            ///
                        }
                        psDSRequestDocSign.setString(5, recordId);

                        psDSRequestDocSign.executeUpdate();

                        if (respRequestSign.getSuccess()) {
                            for (AuthUrl aurl : respRequestSign.getAuth_urls()) {

                                //update query after response api retrieve
                                String queryDSTilakaSignsUpdate = "update app_fd_ds_tilaka_api_signs set dateModified=SYSDATETIME(), c_auth_url=?, c_status=? "
                                        + "where c_tilaka_request_id=? and c_tilaka_id=? ";
                                psDSRequestDocSign = con.prepareStatement(queryDSTilakaSignsUpdate);

                                psDSRequestDocSign.setString(1, aurl.getUrl());

                                psDSRequestDocSign.setString(2, "AWAITING");
                                psDSRequestDocSign.setString(3, recordId);
                                psDSRequestDocSign.setString(4, aurl.getUserIdentifier());

                                psDSRequestDocSign.executeUpdate();
                            }

                        }

                    } catch (IOException e) {
                        LogUtil.error(getClass().getName(), e, "Error on Upload to Tilaka : " + e.getMessage());
                    }
                }
                if (respRequestSign != null) {
                    //save response data
                }
                //end do upload

            } catch (Exception e) {
                LogUtil.error(getClass().getName(), e, e.getMessage());
            } finally {

                if (rsDSSendEmailToTilakaData != null) {
                    try {
                        rsDSSendEmailToTilakaData.close();
                    } catch (SQLException ex) {
                        LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
                    }
                }
                if (psDSSendEmailToTilakaData != null) {
                    try {
                        psDSSendEmailToTilakaData.close();
                    } catch (SQLException ex) {
                        LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
                    }
                }
                if (psDSRequestDocSign != null) {
                    try {
                        psDSRequestDocSign.close();
                    } catch (SQLException ex) {
                        LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
                    }
                }
                if (psDSCreateDocSignData != null) {
                    try {
                        psDSCreateDocSignData.close();
                    } catch (SQLException ex) {
                        LogUtil.error(RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
                    }
                }

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

    // 
    public String getFilename(String primaryKeyValue, Connection con) throws IOException, JSONException {
        debugMessage("getFilename");

        PreparedStatement psDSFileToRequest = null;
        ResultSet rsDSFileToRequest = null;
        try {
            String queryJsonFilename = "select id, c_filename, c_last_response_data from app_fd_ds_tilaka_api_calls with (nolock) where id=?";
            psDSFileToRequest = con.prepareStatement(queryJsonFilename);
            psDSFileToRequest.setString(1, primaryKeyValue);
            rsDSFileToRequest = psDSFileToRequest.executeQuery();
            String c_filename = "";
            if (rsDSFileToRequest.next()) {
                c_filename = rsDSFileToRequest.getString("c_filename");
            }

            return c_filename;

        } catch (SQLException ex) {
            LogUtil.error("" + RequestSignDocumentOnTilaka.class.getName(), ex, ex.getMessage());
        }

        return "";
    }

    private void debugMessage(String message) {
        boolean debug = true;
        if (debug) {
            LogUtil.info("" + RequestSignDocumentOnTilaka.class.getName(), "DEBUG MODE: " + message);
        }
    }

}
