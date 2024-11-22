/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.itasoft.esign;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import id.co.itasoft.tilaka.library.ConstData;
import id.co.itasoft.tilaka.library.response.ResponseUserRegStatus;
import id.co.itasoft.tilaka.library.response.ResponseCheckCertificateStatus;
import id.co.itasoft.tilaka.library.services.TilakaApiServices;
import java.io.BufferedReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author User
 */
public class IsTilakaUserReady extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - isTilakaUserReady";

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

    private void debugMessage(String str) {
        LogUtil.info("" + getClassName(), str);
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        WorkflowUserManager workflowUserManager = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        try {
            JSONObject resultJSON = new JSONObject();
            if (!workflowUserManager.isCurrentUserAnonymous()) {
                Connection con = null;
                PreparedStatement psDSTTilakaSettings = null;
                ResultSet rdDSTilakaSettings = null;
                ////////////////
                StringBuilder jsonString = new StringBuilder();
                BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
                }

                JSONArray jray = new JSONArray(jsonString.toString());

                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;

                try {

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

                    //////
                    StringBuilder query = new StringBuilder();

                    query.append("SELECT du.id, tu.c_user_tilaka, tu.c_tilaka_status, tu.c_last_check_reg_status, ");
                    query.append("tu.c_certificate_status, tu.c_certificate_serial_number, tu.c_subject_certificate, ");
                    query.append("tu.c_start_active_date_certificate, tu.c_certificate_expiry_date, ");
                    query.append("tu.c_last_check_certificate, tu.c_certificate_status_code, tu.c_registration_id ");
                    query.append("FROM app_fd_ds_users du with (nolock) ");
                    query.append("LEFT JOIN app_fd_ds_tilaka_users tu with (nolock) ON tu.id = du.id ");
                    query.append("WHERE du.id IN ("); // For 'a', 'b', 'c'

                    for (int i = 0; i < jray.length(); i++) {
                        //debugMessage("jray["+i+"] : "+jray.getString(i));
                        if (i <= 0) {
                            query.append("?");
                        } else {
                            query.append(", ?");
                        }
                    }
                    query.append(")");

                    preparedStatement = con.prepareStatement(query.toString()); // Create a prepared statement

                    for (int i = 0; i < jray.length(); i++) {
                        preparedStatement.setString(i + 1, jray.getString(i));
                    }

                    resultSet = preparedStatement.executeQuery(); // Execute the query and get the result set

                    LocalDate currentDate = LocalDate.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    String formattedDate = currentDate.format(formatter);

                    boolean isTilakaUserReady = true;

                    // Process the result set
                    while (resultSet.next()) {
                        String userId = resultSet.getString("id");
                        String userTilaka = resultSet.getString("c_user_tilaka");
                        String tilakaStatus = resultSet.getString("c_tilaka_status");
                        String lastCheckRegStatus = resultSet.getString("c_last_check_reg_status");
                        String certificateStatus = resultSet.getString("c_certificate_status");
                        String certificateSerialNumber = resultSet.getString("c_certificate_serial_number");
                        String subjectCertificate = resultSet.getString("c_subject_certificate");
                        String startActiveDate = resultSet.getString("c_start_active_date_certificate");
                        String expiryDate = resultSet.getString("c_certificate_expiry_date");
                        String lastCheckCertificate = resultSet.getString("c_last_check_certificate");
                        String certificateStatusCode = resultSet.getString("c_certificate_status_code");
                        String registration_id = resultSet.getString("c_registration_id");

                        // Output or process retrieved data
                        //System.out.println("User ID: " + userId + ", Tilaka: " + userTilaka + ", Certificate Status: " + certificateStatus);
                        //check has tilaka user
                        if (userTilaka == null) {
                            resultJSON.put("success", (Boolean.FALSE));
                            resultJSON.put("messages", "tilaka username not found for user : " + userId);
                            isTilakaUserReady = false;
                            break;
                        }
                        if (!formattedDate.equals(lastCheckRegStatus) || tilakaStatus == null) {
                            //do hit userregstatus

                            try (TilakaApiServices apiService = new TilakaApiServices(ConstData.tilaka_adapter_api_hostname_url)) {

                                apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id,
                                        ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));
                                String userRegistrationId = (registration_id != null) ? registration_id : "";
                                ResponseUserRegStatus objouput = apiService.UserRegStatus(userRegistrationId);

                                if (objouput.isSuccess()) {

                                    String tilakaName = objouput.getRegData().get("tilaka_name").toString();
                                    String status = objouput.getRegData().get("status").toString();
                                    //String currentDate = java.time.LocalDate.now().toString();
                                    try {

                                        String insertOrUpdateQuery = "MERGE app_fd_ds_tilaka_users AS target "
                                                + "USING (SELECT ? AS registration_id, ? AS c_user_tilaka, ? AS c_tilaka_status, ? AS c_last_check_reg_status) AS source "
                                                + "ON (target.c_registration_id = source.registration_id) "
                                                + "WHEN MATCHED THEN "
                                                + "UPDATE SET target.c_user_tilaka = source.c_user_tilaka, target.c_tilaka_status = source.c_tilaka_status, target.c_last_check_reg_status = source.c_last_check_reg_status "
                                                + "WHEN NOT MATCHED THEN "
                                                + "INSERT (c_registration_id, c_user_tilaka, c_tilaka_status, c_last_check_reg_status) "
                                                + "VALUES (source.registration_id, source.c_user_tilaka, source.c_tilaka_status, source.c_last_check_reg_status);";
                                        try (PreparedStatement ps = con.prepareStatement(insertOrUpdateQuery)) {
                                            ps.setString(1, userRegistrationId);
                                            ps.setString(2, tilakaName);
                                            ps.setString(3, status);
                                            ps.setString(4, formattedDate);
                                            ps.executeUpdate();
                                        }
                                        if (!status.equals("S")) {
                                            resultJSON.put("success", (Boolean.FALSE));
                                            String appendInfo = "UNKNOWN TYPE STATUS FOR : ";
                                            switch (status) {
                                                case "F":
                                                    appendInfo = "Failed to register for dukcapil (there is data that is not appropriate, "
                                                            + "for example the NIK is not found in the dukcapil database)";
                                                    break;
                                                case "E":
                                                    appendInfo = "Dukcapil error (e.g. failed when connecting to dukcapil)";
                                                    break;
                                                case "B":
                                                    appendInfo = "User is still in the verification stage (KYC)";
                                                    break;
                                                case "D":
                                                    appendInfo = "User is still in the formular filling stage (has not yet filled in the PIN form)";
                                                    break;
                                                default:
                                                    appendInfo += status;
                                                    break;
                                            }

                                            resultJSON.put("messages", "registration info for : " + userId + ", " + appendInfo);
                                            isTilakaUserReady = false;
                                            break;
                                        }
                                    } catch (SQLException e) {
                                        LogUtil.error(this.getClass().getName(), e, e.getMessage());
                                        resultJSON.put("success", (Boolean.FALSE));
                                        resultJSON.put("messages", "SQLException in registration info for : " + userId + ", please check error log on server.");
                                        isTilakaUserReady = false;
                                        break;
                                    }
                                } else {
                                    LogUtil.info(getClassName(), "objouput.getRegData()" + objouput.getRegData().toString());

                                    resultJSON.put("success", (Boolean.FALSE));
                                    resultJSON.put("messages", "failed to get registration info for : " + userId + ", response_code : " + objouput.getResponseCode()
                                            + ", info : " + objouput.getMessage());
                                    isTilakaUserReady = false;
                                    break;
                                }

                            } catch (Exception e) {
                                LogUtil.error(getClassName(), e, e.getMessage());
                                resultJSON.put("success", (Boolean.FALSE));
                                resultJSON.put("messages", "exception in registration info for : " + userId + ", please check error log on server.");
                                isTilakaUserReady = false;
                                break;
                            }
                        } else {
                            if (!tilakaStatus.equals("S")) {
                                resultJSON.put("success", (Boolean.FALSE));
                                String appendInfo = "UNKNOWN TYPE STATUS FOR : ";
                                switch (tilakaStatus) {
                                    case "F":
                                        appendInfo = "Failed to register for dukcapil (there is data that is not appropriate, "
                                                + "for example the NIK is not found in the dukcapil database)";
                                        break;
                                    case "E":
                                        appendInfo = "Dukcapil error (e.g. failed when connecting to dukcapil)";
                                        break;
                                    case "B":
                                        appendInfo = "User is still in the verification stage (KYC)";
                                        break;
                                    case "D":
                                        appendInfo = "User is still in the formular filling stage (has not yet filled in the PIN form)";
                                        break;
                                    default:
                                        appendInfo += tilakaStatus;
                                        break;
                                }

                                resultJSON.put("messages", "registration info for : " + userId + ", " + appendInfo);
                                isTilakaUserReady = false;
                                break;
                            } else if (tilakaStatus == null) {
                                resultJSON.put("success", (Boolean.FALSE));
                                resultJSON.put("messages", "registration status not found for : " + userId);
                                isTilakaUserReady = false;
                                break;
                            }
                        }

                        if (!formattedDate.equals(lastCheckCertificate) || certificateStatusCode == null) {
                            //do hit user cert status
                            try (TilakaApiServices apiService = new TilakaApiServices(ConstData.tilaka_adapter_api_hostname_url)) {

                                apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id,
                                        ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));
                                String tilakaUserName = userTilaka;
                                ResponseCheckCertificateStatus objouput = apiService.CheckCertificateStatus(tilakaUserName);
                                LogUtil.info(this.getClass().getName(), objouput.toString());

                                //JSONObject oujson = new JSONObject();
                                if (objouput.isSuccess()) {
                                    //oujson.put("status", objouput.getStatus());
                                    //oujson.put("message", objouput.getMessage());
                                    //oujson.put("response_code", objouput.getResponseCode());
                                    List<Map<String, Object>> certDataList = objouput.getCertData();

                                    Map<String, Object> certData = certDataList.get(0);
                                    String status = certData.get("status").toString();
                                    String serialNumber = certData.get("serialnumber").toString();
                                    String subjectDn = certData.get("subject_dn").toString();
                                    startActiveDate = certData.get("start_active_date").toString();
                                    expiryDate = certData.get("expiry_date").toString();

                                    String statusCode = String.valueOf(objouput.getStatus());
                                    LogUtil.info(this.getClass().getName(), "Certificate Status Code: " + statusCode
                                    );
                                    try {

                                        String insertOrUpdateQuery = "MERGE app_fd_ds_tilaka_users AS target "
                                                + "USING (SELECT ? AS user_identifier, ? AS c_certificate_status, ? AS c_certificate_serial_number, ? AS c_subject_certificate, ? AS c_start_active_date_certificate, ? AS c_certificate_expiry_date, ? AS c_last_check_certificate, ? AS c_certificate_status_code) AS source "
                                                + "ON (target.c_user_tilaka = source.user_identifier) "
                                                + "WHEN MATCHED THEN "
                                                + "UPDATE SET target.c_certificate_status = source.c_certificate_status, target.c_certificate_serial_number = source.c_certificate_serial_number, target.c_subject_certificate = source.c_subject_certificate, target.c_start_active_date_certificate = source.c_start_active_date_certificate, target.c_certificate_expiry_date = source.c_certificate_expiry_date, target.c_last_check_certificate = source.c_last_check_certificate, target.c_certificate_status_code = source.c_certificate_status_code "
                                                + "WHEN NOT MATCHED THEN "
                                                + "INSERT (c_user_tilaka, c_certificate_status, c_certificate_serial_number, c_subject_certificate, c_start_active_date_certificate, c_certificate_expiry_date, c_last_check_certificate, c_certificate_status_code) "
                                                + "VALUES (source.user_identifier, source.c_certificate_status, source.c_certificate_serial_number, source.c_subject_certificate, source.c_start_active_date_certificate, source.c_certificate_expiry_date, source.c_last_check_certificate, source.c_certificate_status_code);";
                                        try (PreparedStatement ps = con.prepareStatement(insertOrUpdateQuery)) {
                                            ps.setString(1, tilakaUserName);
                                            ps.setString(2, status);
                                            ps.setString(3, serialNumber);
                                            ps.setString(4, subjectDn);
                                            ps.setString(5, startActiveDate);
                                            ps.setString(6, expiryDate);
                                            ps.setString(7, formattedDate);
                                            ps.setString(8, statusCode);
                                            ps.executeUpdate();
                                        }
                                        if (!statusCode.equals("3")) {
                                            resultJSON.put("success", (Boolean.FALSE));
                                            String appendInfo = "UNKNOWN TYPE CERTIFICATE STATUS FOR : ";
                                            switch (statusCode) {
                                                case "0":
                                                    appendInfo = "there is no certificate information";
                                                    break;
                                                case "1":
                                                    appendInfo = "Certificate registration is still in the verification/validator process";
                                                    break;
                                                case "2":
                                                    appendInfo = "registered certificate (has been issued) & requires user confirmation";
                                                    break;
                                                case "4":
                                                    appendInfo = "Certificate registration is rejected (final) by the verifier/validator.";
                                                    break;
                                                default:
                                                    appendInfo += statusCode;
                                                    break;
                                            }

                                            resultJSON.put("messages", "certificate info for : " + userId + ", " + appendInfo);
                                            isTilakaUserReady = false;
                                            break;
                                        }
                                    } catch (SQLException e) {
                                        LogUtil.error(this.getClass().getName(), e, e.getMessage());
                                        resultJSON.put("success", (Boolean.FALSE));
                                        resultJSON.put("messages", "SQLException in cerfitate info for : " + userId + ", please check error log on server.");
                                        isTilakaUserReady = false;
                                        break;
                                    }
                                } else {
                                    //oujson.put("response_code", objouput.getResponseCode());
                                    //oujson.put("message", objouput.getMessage());
                                    resultJSON.put("success", (Boolean.FALSE));
                                    resultJSON.put("messages", "cerfitate info for : " + userId + ", response_code:" + objouput.getResponseCode() + ", message : " + objouput.getMessage());
                                    isTilakaUserReady = false;
                                    break;
                                }
                            } catch (Exception e) {
                                LogUtil.error(getClassName(), e, e.getMessage());
                                resultJSON.put("success", (Boolean.FALSE));
                                resultJSON.put("messages", "exception in cerfitate info for : " + userId + ", please check error log on server.");
                                isTilakaUserReady = false;
                                break;
                            }
                        } else {
                            if (!certificateStatusCode.equals("3")) {
                                resultJSON.put("success", (Boolean.FALSE));
                                String appendInfo = "UNKNOWN TYPE CERTIFICATE STATUS FOR : ";
                                switch (certificateStatusCode) {
                                    case "0":
                                        appendInfo = "there is no certificate information";
                                        break;
                                    case "1":
                                        appendInfo = "Certificate registration is still in the verification/validator process";
                                        break;
                                    case "2":
                                        appendInfo = "registered certificate (has been issued) & requires user confirmation";
                                        break;
                                    case "4":
                                        appendInfo = "Certificate registration is rejected (final) by the verifier/validator.";
                                        break;
                                    default:
                                        appendInfo += certificateStatusCode;
                                        break;
                                }

                                resultJSON.put("messages", "certificate info for : " + userId + ", " + appendInfo);
                                isTilakaUserReady = false;
                                break;
                            } else if (certificateStatusCode == null) {
                                resultJSON.put("success", (Boolean.FALSE));
                                resultJSON.put("messages", "certificate not found for : " + userId);
                                isTilakaUserReady = false;
                                break;
                            }
                        }

                        //cert status 3 = aktid
                        //JSONObject
                        // Handle other fields as needed
                    }
                    if (isTilakaUserReady) {
                        resultJSON.put("success", (Boolean.TRUE));
                        resultJSON.put("messages", "all tilaka user is ready!");
                    }

                } catch (SQLException e) {
                    LogUtil.error(getClassName(), e, e.getMessage());
                    resultJSON.put("success", (Boolean.FALSE));
                    resultJSON.put("messages", "isTilakaUserReady sql error. please try again.");
                } finally {
                    // Ensure resources are closed in the correct order
                    try {
                        if (resultSet != null) {
                            resultSet.close();
                        }
                        if (preparedStatement != null) {
                            preparedStatement.close();
                        }
                        if (con != null) {
                            con.close();
                        }
                    } catch (SQLException e) {
                        LogUtil.error(getClassName(), e, e.getMessage());
                        resultJSON.put("success", (Boolean.FALSE));
                        resultJSON.put("messages", "isTilakaUserReady sql error. please try again.");
                    }
                }
                resultJSON.write(response.getWriter());

            } else {
                new JSONObject("{\"success\": false, \"messages\":\"invalid login\"}").write(response.getWriter());
            }
        } catch (JSONException ex) {
            LogUtil.error(getClassName(), ex, ex.getMessage());
            try {
                new JSONObject("{\"success\": false, \"messages\":\"isTilakaUserReady JSON error, please try again.\"}").write(response.getWriter());
            } catch (JSONException ex1) {
                LogUtil.error(getClassName(), ex1, ex1.getMessage());
            }
        }
    }
}
