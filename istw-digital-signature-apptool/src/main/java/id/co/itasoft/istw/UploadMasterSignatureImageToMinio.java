package id.co.itasoft.istw;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import java.util.*;
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
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.UploadObjectArgs;
import java.net.URL;
import org.joget.workflow.model.WorkflowAssignment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.joget.commons.util.UuidGenerator;

public class UploadMasterSignatureImageToMinio extends DefaultApplicationPlugin {

    public static String pluginName = "ISTW - DS - Upload MySign To Minio";

    @Override
    public Object execute(Map properties) {

        MyConfig config = new MyConfig(getPropertyString("minioUrl"), getPropertyString("minioUser"),
                getPropertyString("minioKey"), getPropertyString("minioBucket"));

        debugMessage("config.getUrlMinio() : " + config.getUrlMinio());
        debugMessage("config.getUserName() : " + config.getUserName());
        debugMessage("config.getPassword() : " + config.getPassword());
        debugMessage("config.getBucket() : " + config.getBucket());

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

        WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");

        //get current record id
        String recordId = appService.getOriginProcessId(assignment.getProcessId());

        debugMessage("recordId : " + recordId);

        MasterImageSignatureData processedImage;
        Connection con = null;
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();            
            String uuid = UuidGenerator.getInstance().getUuid();
            
            processedImage = processMasterSignImage(recordId, uuid, con);

            uploadFiletoMinio(processedImage.getBytes(), processedImage.getFileName(), config);
        } catch (Exception ex) {
            LogUtil.error(UploadMasterSignatureImageToMinio.class.getName(), ex, ex.getMessage());
        } finally{
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LogUtil.error(UploadMasterSignatureImageToMinio.class.getName(), ex, ex.getMessage());
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

    protected void uploadFiletoMinio(byte[] bytes, String filename, MyConfig config) throws IOException, ServletException {
        try {
            if (bytes.length > 0) {
                try {

                    // Panggil metode untuk mengkonversi byte array ke file
                    File masterSignFile = convertBytesToFile(bytes, filename);
                    if (masterSignFile != null) {

                        MinioClient minioClient = MinioClient.builder()
                                .endpoint(new URL(config.getUrlMinio()))
                                .credentials(config.getUserName(), config.getPassword())
                                .build();

                        boolean found
                                = minioClient.bucketExists(BucketExistsArgs.builder().bucket(config.getBucket()).build());
                        if (!found) {
                            minioClient.makeBucket(MakeBucketArgs.builder().bucket(config.getBucket()).build());
                        } else {
                            LogUtil.info(getClass().getName(), "Bucket already exists.");
                        }

                        String minioFilePath = "masterDigiSignImages";

                        minioFilePath = minioFilePath.replace("/", "");

                        ObjectWriteResponse owr = minioClient.uploadObject(
                                UploadObjectArgs.builder()
                                        .bucket(config.getBucket())
                                        .object(minioFilePath + "/" + masterSignFile.getName())
                                        .filename(masterSignFile.getAbsolutePath())
                                        .build());
                        debugMessage("ObjectWriteResponse:" + owr.etag());
                        masterSignFile.delete();
                    } else {
                        LogUtil.error(getClass().getName(), new NullPointerException("Failed to created file"), "Output Image Object is Null");
                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, e.getMessage());
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

        return AppUtil.readPluginResource(getClassName(), "/properties/uploadMySignToMinio.json", null, true, null);
    }

    // 1
    public MasterImageSignatureData getSignImage(String primaryKeyValue, String uuid, Connection con) throws IOException, SQLException {
        MasterImageSignatureData result = new MasterImageSignatureData();
        debugMessage("getSignImage");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String formDefId = "mySignatures";
        String attachmentFieldID = "image_signature";
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        String fileName = "";
        Form form = null;
        String tableName = null;

        PreparedStatement psDSUpdateMasterSignImage = null;
        try {
            if (appDef != null && formDefId != null
                    && !formDefId.isEmpty() && primaryKeyValue != null
                    && !primaryKeyValue.isEmpty()) {
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
        debugMessage("decodeFileName :" + decodedFileName);
        debugMessage("tableName :" + tableName);
        debugMessage("primaryKey :" + primaryKeyValue);
        File file = FileUtil.getFile(decodedFileName, tableName, primaryKeyValue);

        debugMessage("File Location : " + file.getAbsolutePath());

        String oldFilePath = file.getAbsolutePath();

        // New filename (without the extension)
        String newFileName = "ttd-" + uuid;

        // Extract the folder path from the old file
        String folderPath = file.getParent();

        String filethumb = folderPath + File.separator + decodedFileName + ".thumb.jpg";

        File oldFileThumb = new File(filethumb);

        // Extract the extension from the old file
        String extension = oldFilePath.substring(oldFilePath.lastIndexOf("."));

        // Construct the new file path
        String newFilePathThumb = folderPath + File.separator + newFileName + extension + ".thumb.jpg";

        // Create a File object for the new file
        File newFileThumb = new File(newFilePathThumb);

        if (oldFileThumb.renameTo(newFileThumb)) {
            debugMessage("File renamed successfully to: " + newFilePathThumb);
        } else {
            debugMessage("Failed to rename the file.");
        }

        // Construct the new file path
        String newFilePath = folderPath + File.separator + newFileName + extension;

        // Create a File object for the new file
        File newFile = new File(newFilePath);

        // Rename the old file to the new file
        if (file.renameTo(newFile)) {
            debugMessage("File renamed successfully to: " + newFilePath);
        } else {
            debugMessage("Failed to rename the file.");
        }

        result.setFileName(newFileName + extension);
        try {
            String queryDSUpdateMasterSignImage = "update app_fd_ds_master_signatures set c_image_signature = ? where id = ?";
            psDSUpdateMasterSignImage = con.prepareStatement(queryDSUpdateMasterSignImage);
            psDSUpdateMasterSignImage.setString(1, newFileName + extension);
            psDSUpdateMasterSignImage.setString(2, primaryKeyValue);

            psDSUpdateMasterSignImage.executeUpdate();
        } catch (SQLException ex) {
            LogUtil.error(getClassName(), ex, ex.getMessage());
        }

        FileInputStream fileIS = new FileInputStream(newFile);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fileIS.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        byte[] fileContent = outputStream.toByteArray();
        result.setBytes(fileContent);
        return result;
    }

    public MasterImageSignatureData processMasterSignImage(String rowKey, String uuid, Connection con) throws Exception {
        debugMessage("processMasterSignImage");
        MasterImageSignatureData image = getSignImage(rowKey, uuid, con);
        return image;
    }

    private void debugMessage(String message) {
        if ("true".equalsIgnoreCase(getPropertyString("debugMode"))) {
            LogUtil.info("" + UploadMasterSignatureImageToMinio.class.getName(), "DEBUG MODE: " + message);
        }
    }
}
