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
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import id.co.itasoft.tilaka.library.ConstData;
import id.co.itasoft.tilaka.library.body.RegisterForKycCheckData;
import id.co.itasoft.tilaka.library.response.ResponseRegisterForKycCheck;
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
public class TilakaRegisterForKycCheckKitas extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - Tilaka Register For KYC Check Kitas / Kitap";

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
                Properties prop = new Properties();
                try {
                    apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id, ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));
                    String RegistrationId = request.getParameter("registration_id");
                    String Email = request.getParameter("email");
                    String Name = request.getParameter("name");
                    String Nik = request.getParameter("nik");
                    String Ktp = request.getParameter("photo_ktp");
                    String Company = request.getParameter("company_name");
                    String ExpiredDateParam = request.getParameter("date_expired");
                    String IsApprovedParam = request.getParameter("is_approved");
                    boolean IsApproved = Boolean.parseBoolean(IsApprovedParam);
                    String ConsentText = request.getParameter("consent_text");
                    String Version = request.getParameter("version_");
                    String NationalityType = request.getParameter("nationality_type");
                    String IdentityType = request.getParameter("identity_type");
                    String CountryCode = request.getParameter("country_code");
                    String PassportNumber = request.getParameter("passport_number");
                    String PassportDateExpire = request.getParameter("passport_date_expire");
                    String Passport = request.getParameter("passport_file");
                    String IdentityDateExpire = request.getParameter("identity_date_expire");
                    String CompanySupportingDocument = request.getParameter("company_supporting_document");
                    String consentTimeStampParam = request.getParameter("consent_timestamp");
                    RegisterForKycCheckData datatoSend = new RegisterForKycCheckData();

                    datatoSend.setDateExpire(ExpiredDateParam);
                    datatoSend.setConsentTimestamp(consentTimeStampParam);

                    datatoSend.setRegistrationId(RegistrationId);
                    datatoSend.setEmail(Email);
                    datatoSend.setName(Name);
                    datatoSend.setCompanyName(Company);
                    datatoSend.setNik(Nik);
                    datatoSend.setNationalityType(NationalityType);
                    datatoSend.setIdentityType(IdentityType);
                    datatoSend.setCountryCode(CountryCode);
                    datatoSend.setPassportNumber(PassportNumber);
                    datatoSend.setPassportDateExpired(PassportDateExpire);
                    datatoSend.setPassport(Passport);
                    datatoSend.setPhotoKtp(Ktp);
                    datatoSend.setConsentText(ConsentText);
                    datatoSend.setApproved(IsApproved);
                    datatoSend.setIdentityDateExpire(IdentityDateExpire);
                    datatoSend.setCompanySupportingDocument(CompanySupportingDocument);
                    datatoSend.setVersion(Version);
                    System.out.println(datatoSend.getHashConsent(ConstData.tilaka_channel_id, ConstData.tilaka_client_secret));


                    ResponseRegisterForKycCheck objouput = apiService.RegisterForKycCheckKitas(datatoSend, ConstData.tilaka_channel_id, ConstData.tilaka_client_secret);
                    System.out.println(objouput.toString());

                    JSONObject oujson = new JSONObject();
                    if (objouput.isSuccess()) {
                        oujson.put("message", objouput.getMessage());
                        oujson.put("uuid", objouput.getData());
                        oujson.put("response_code", objouput.getResponseCode());
                        oujson.put("status", objouput.isSuccess());
                        LogUtil.info(this.getClass().getName(), "Success: " + objouput.getData());
                    } else {
                        oujson.put("response_code", objouput.getResponseCode());
                        oujson.put("message", objouput.getMessage());
                        oujson.put("uuid", objouput.getData());
                        oujson.put("status", objouput.isSuccess());
                        LogUtil.info(this.getClass().getName(), "Failed: " + objouput.getData());
                    }
                    oujson.write(response.getWriter());
                } catch (JSONException e) {
                    LogUtil.error(getClassName(), e, "JSON Parsing Error: " + e.getMessage());
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON response");
                } catch (Exception e) {
                    LogUtil.error(getClassName(), e, "Error: " + e.getMessage());
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing the request");
                }
            }
        }
    }
}


