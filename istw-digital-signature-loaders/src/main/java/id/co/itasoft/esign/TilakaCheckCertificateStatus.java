package id.co.itasoft.esign;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import id.co.itasoft.tilaka.library.ConstData;
import id.co.itasoft.tilaka.library.response.ResponseCheckCertificateStatus;
import id.co.itasoft.tilaka.library.services.TilakaApiServices;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.json.JSONException;
import org.json.JSONObject;

public class TilakaCheckCertificateStatus extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - Check Tilaka Certificate Status";

    @Override
    public String renderTemplate(FormData fd, Map map) {
        return "";
    }

    @Override
    public String getName() {
        return pluginName;

    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return pluginName;
    }

    @Override
    public String getLabel() {
        return pluginName;
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        WorkflowUserManager workflowUserManager = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        if (!workflowUserManager.isCurrentUserAnonymous()) {
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
            } catch (SQLException ex) {
                LogUtil.error(getClassName(), ex, ex.getMessage());
            }

            try (TilakaApiServices apiService = new TilakaApiServices(ConstData.tilaka_adapter_api_hostname_url)) {
                try {
                    apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id,
                            ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));
                    String tilakaUserName = request.getParameter("user_identifier");
                    ResponseCheckCertificateStatus objouput = apiService.CheckCertificateStatus(tilakaUserName);
                    LogUtil.info(this.getClass().getName(), objouput.toString());

                    JSONObject oujson = new JSONObject();
                    if (objouput.isSuccess()) {
                        oujson.put("status", objouput.getStatus());
                        oujson.put("message", objouput.getMessage());
                        oujson.put("response_code", objouput.getResponseCode());
                        List<Map<String, Object>> certDataList = objouput.getCertData();

                        String status = null;
                        String serialNumber = null;
                        String subjectDn = null;
                        String startActiveDate = null;
                        String expiryDate = null;
                        String currentDate = java.time.LocalDate.now().toString(); // Get current date in YYYY-MM-DD format
                        String statusCode = String.valueOf(objouput.getStatus());
                        LogUtil.info(this.getClass().getName(), "Certificate Status Code: " + statusCode);

                        if (certDataList != null && !certDataList.isEmpty()) {
                            Map<String, Object> certData = certDataList.get(0);
                            status = certData.get("status").toString();
                            serialNumber = certData.get("serialnumber").toString();
                            subjectDn = certData.get("subject_dn").toString();
                            startActiveDate = certData.get("start_active_date").toString();
                            expiryDate = certData.get("expiry_date").toString();
                        }

                        try {
                            // Add columns if they do not exist
                            String addColumnsQuery = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_certificate_status' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_certificate_status VARCHAR(255); " +
                                    "END; " +
                                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_certificate_serial_number' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_certificate_serial_number VARCHAR(255); " +
                                    "END; " +
                                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_subject_certificate' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_subject_certificate VARCHAR(255); " +
                                    "END; " +
                                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_start_active_date_certificate' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_start_active_date_certificate DATETIME; " +
                                    "END; " +
                                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_certificate_expiry_date' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_certificate_expiry_date DATETIME; " +
                                    "END; " +
                                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_last_check_certificate' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_last_check_certificate DATE; " +
                                    "END; " +
                                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_certificate_status_code' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_certificate_status_code VARCHAR(255); " +
                                    "END;";
                            try (PreparedStatement ps = con.prepareStatement(addColumnsQuery)) {
                                ps.executeUpdate();
                            }

                            String insertOrUpdateQuery = "MERGE app_fd_ds_tilaka_users AS target " +
                                    "USING (SELECT ? AS user_identifier, ? AS c_certificate_status, ? AS c_certificate_serial_number, ? AS c_subject_certificate, ? AS c_start_active_date_certificate, ? AS c_certificate_expiry_date, ? AS c_last_check_certificate, ? AS c_certificate_status_code) AS source " +
                                    "ON (target.c_user_tilaka = source.user_identifier) " +
                                    "WHEN MATCHED THEN " +
                                    "UPDATE SET target.c_certificate_status = source.c_certificate_status, target.c_certificate_serial_number = source.c_certificate_serial_number, target.c_subject_certificate = source.c_subject_certificate, target.c_start_active_date_certificate = source.c_start_active_date_certificate, target.c_certificate_expiry_date = source.c_certificate_expiry_date, target.c_last_check_certificate = source.c_last_check_certificate, target.c_certificate_status_code = source.c_certificate_status_code " +
                                    "WHEN NOT MATCHED THEN " +
                                    "INSERT (c_user_tilaka, c_certificate_status, c_certificate_serial_number, c_subject_certificate, c_start_active_date_certificate, c_certificate_expiry_date, c_last_check_certificate, c_certificate_status_code) " +
                                    "VALUES (source.user_identifier, source.c_certificate_status, source.c_certificate_serial_number, source.c_subject_certificate, source.c_start_active_date_certificate, source.c_certificate_expiry_date, source.c_last_check_certificate, source.c_certificate_status_code);";
                            try (PreparedStatement ps = con.prepareStatement(insertOrUpdateQuery)) {
                                ps.setString(1, tilakaUserName);
                                ps.setString(2, status);
                                ps.setString(3, serialNumber);
                                ps.setString(4, subjectDn);
                                ps.setString(5, startActiveDate);
                                ps.setString(6, expiryDate);
                                ps.setString(7, currentDate);
                                ps.setString(8, statusCode);
                                ps.executeUpdate();
                            }
                        } catch (SQLException e) {
                            LogUtil.error(this.getClass().getName(), e, e.getMessage());
                        }
                    } else {
                        oujson.put("response_code", objouput.getResponseCode());
                        oujson.put("message", objouput.getMessage());
                    }

                    oujson.write(response.getWriter());

                } catch (IOException e) {
                    LogUtil.error(this.getClass().getName(), e, e.getMessage());
                } catch (JSONException e) {
                    LogUtil.error(this.getClass().getName(), e, e.getMessage());
                } catch (Exception e) {
                    LogUtil.error(this.getClass().getName(), e, e.getMessage());
                }
            }

        }
    }
}