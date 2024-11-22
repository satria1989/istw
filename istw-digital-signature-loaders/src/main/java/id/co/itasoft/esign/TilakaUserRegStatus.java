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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import id.co.itasoft.tilaka.library.ConstData;
import id.co.itasoft.tilaka.library.response.ResponseUserRegStatus;
import id.co.itasoft.tilaka.library.services.TilakaApiServices;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author User
 */
public class TilakaUserRegStatus extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - Tilaka User Registration Status";

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
                LogUtil.info(getClassName(), "Successfully retrieved Tilaka settings from database");

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
                LogUtil.info(getClassName(), "Successfully retrieved Tilaka action paths from database");
            } catch (SQLException ex) {
                LogUtil.error(getClassName(), ex, ex.getMessage());
            }

            try (TilakaApiServices apiService = new TilakaApiServices(ConstData.tilaka_adapter_api_hostname_url)) {
                try {
                    apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id,
                            ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));
                    String userRegistrationId = (request.getParameter("registration_id") != null) ? request.getParameter("registration_id") : "";
                    ResponseUserRegStatus objouput = apiService.UserRegStatus(userRegistrationId);

                    JSONObject oujson = new JSONObject();
                    if (objouput.isSuccess()) {
                        oujson.put("message", objouput.getMessage());
                        oujson.put("response_code", objouput.getResponseCode());
                        oujson.put("data", objouput.getRegData());
                        String tilakaName = objouput.getRegData().get("tilaka_name").toString();
                        String status = objouput.getRegData().get("status").toString();
                        String currentDate = java.time.LocalDate.now().toString();
                        try {
                            // Add c_user_tilaka column if it does not exist
                            String addUserTilakaColumnQuery = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_user_tilaka' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_user_tilaka VARCHAR(255); " +
                                    "END;";
                            try (PreparedStatement ps = con.prepareStatement(addUserTilakaColumnQuery)) {
                                ps.executeUpdate();
                            }

                            // Add c_tilaka_status column if it does not exist
                            String addTilakaStatusColumnQuery = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_tilaka_status' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_tilaka_status VARCHAR(255); " +
                                    "END;";
                            try (PreparedStatement ps = con.prepareStatement(addTilakaStatusColumnQuery)) {
                                ps.executeUpdate();
                            }

                            String addLastCheckRegStatusColumnQuery = "IF NOT EXISTS (SELECT * FROM sys.columns WHERE name = 'c_last_check_reg_status' AND object_id = OBJECT_ID('app_fd_ds_tilaka_users')) " +
                                    "BEGIN " +
                                    "ALTER TABLE app_fd_ds_tilaka_users ADD c_last_check_reg_status DATE; " +
                                    "END;";
                            try (PreparedStatement ps = con.prepareStatement(addLastCheckRegStatusColumnQuery)) {
                                ps.executeUpdate();
                            }

                            String insertOrUpdateQuery = "MERGE app_fd_ds_tilaka_users AS target " +
                                    "USING (SELECT ? AS registration_id, ? AS c_user_tilaka, ? AS c_tilaka_status, ? AS c_last_check_reg_status) AS source " +
                                    "ON (target.c_registration_id = source.registration_id) " +
                                    "WHEN MATCHED THEN " +
                                    "UPDATE SET target.c_user_tilaka = source.c_user_tilaka, target.c_tilaka_status = source.c_tilaka_status, target.c_last_check_reg_status = source.c_last_check_reg_status " +
                                    "WHEN NOT MATCHED THEN " +
                                    "INSERT (c_registration_id, c_user_tilaka, c_tilaka_status, c_last_check_reg_status) " +
                                    "VALUES (source.registration_id, source.c_user_tilaka, source.c_tilaka_status, source.c_last_check_reg_status);";
                            try (PreparedStatement ps = con.prepareStatement(insertOrUpdateQuery)) {
                                ps.setString(1, userRegistrationId);
                                ps.setString(2, tilakaName);
                                ps.setString(3, status);
                                ps.setString(4, currentDate);
                                ps.executeUpdate();
                            }
                            LogUtil.info(getClassName(), "User registration status updated successfully for registration ID: " + userRegistrationId);
                        } catch (SQLException e) {
                            LogUtil.error(this.getClass().getName(), e, e.getMessage());
                        }
                    } else {
                        oujson.put("response_code", objouput.getResponseCode());
                        oujson.put("message", objouput.getMessage());
                        oujson.put("data", objouput.getRegData());
                        LogUtil.warn(this.getClass().getName(), "Failed to update user registration status for registration ID: " + userRegistrationId + ". Response: " + objouput.getMessage());
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
        LogUtil.info(getClassName(), "Ending webService method");
    }

    }



