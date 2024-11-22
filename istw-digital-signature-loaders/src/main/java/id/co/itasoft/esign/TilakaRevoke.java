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
import id.co.itasoft.tilaka.library.response.ResponseRequestRevokeCertificate;
import id.co.itasoft.tilaka.library.services.TilakaApiServices;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TilakaRevoke extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - Check Tilaka Revoke";

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
                    String tilakaReasons = request.getParameter("user_reason");
                    ResponseRequestRevokeCertificate objouput = apiService.requestRevokeCertificate(tilakaUserName, tilakaReasons);

                    JSONObject oujson = new JSONObject();
                    if (objouput.isSuccess()) {
                        oujson.put("message", objouput.getMessage());
                        oujson.put("response_code", objouput.getResponseCode());

                        // Check if data is a JSONArray or JSONObject
                        Object data = objouput.getData();
                        if (data instanceof JSONArray) {
                            oujson.put("data", (JSONArray) data);
                        } else if (data instanceof JSONObject) {
                            oujson.put("data", (JSONObject) data);
                        } else {
                            oujson.put("data", JSONObject.NULL);
                        }

                    } else {
                        oujson.put("response_code", objouput.getResponseCode());
                        oujson.put("message", objouput.getMessage());

                        // Check if data is a JSONArray or JSONObject
                        Object data = objouput.getData();
                        if (data instanceof JSONArray) {
                            oujson.put("data", (JSONArray) data);
                        } else if (data instanceof JSONObject) {
                            oujson.put("data", (JSONObject) data);
                        } else {
                            oujson.put("data", JSONObject.NULL);
                        }
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