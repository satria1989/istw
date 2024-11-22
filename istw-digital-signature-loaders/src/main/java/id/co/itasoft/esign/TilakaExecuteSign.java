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
import javax.sql.DataSource;

import id.co.itasoft.tilaka.library.ConstData;
import id.co.itasoft.tilaka.library.response.ResponseExecuteSign;
import id.co.itasoft.tilaka.library.services.TilakaApiServices;
import java.util.HashMap;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.workflow.model.WorkflowProcessResult;
import org.joget.workflow.model.service.WorkflowManager;

/**
 *
 * @author User
 */
public class TilakaExecuteSign {

    public static String pluginName = "ISTW - DS - Tilaka ExecuteSign";

    public static void startExecuteSignDoc(String tilaka_request_id, String tilakaId) {

        ////// set variabel berikut ambil dari database ///////
        Connection con = null;
        PreparedStatement psDSTTilakaSettings = null, psDSDSTilakaIDSigns = null, psDSDSTilakaSignExecuted = null;
        ResultSet rsDSTilakaSettings = null, rsDSTilakaIDSigns = null;

        try {

            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();

            String queryDSTilakaSettings = "select id,c_env_value from app_fd_ds_tilaka_env_vars with (nolock) ";
            psDSTTilakaSettings = con.prepareStatement(queryDSTilakaSettings);
            rsDSTilakaSettings = psDSTTilakaSettings.executeQuery();

            while (rsDSTilakaSettings.next()) {

                String id = rsDSTilakaSettings.getString("id");

                switch (id) {
                    case "tilaka_adapter_api_hostname_url":
                        ConstData.tilaka_adapter_api_hostname_url = rsDSTilakaSettings.getString("c_env_value");
                        break;
                    case "tilaka_api_base_url":
                        ConstData.tilaka_api_base_url = rsDSTilakaSettings.getString("c_env_value");
                        break;
                    case "tilaka_channel_id":
                        ConstData.tilaka_channel_id = rsDSTilakaSettings.getString("c_env_value");
                        break;
                    case "tilaka_client_secret":
                        ConstData.tilaka_client_secret = rsDSTilakaSettings.getString("c_env_value");
                        break;
                    default:
                        break;
                }

            }

            queryDSTilakaSettings = "select id,c_action_path from app_fd_ds_tilaka_act_paths with (nolock) ";
            psDSTTilakaSettings = con.prepareStatement(queryDSTilakaSettings);
            rsDSTilakaSettings = psDSTTilakaSettings.executeQuery();

            while (rsDSTilakaSettings.next()) {

                String id = rsDSTilakaSettings.getString("id");

                switch (id) {
                    case "tilaka_action_auth":
                        ConstData.tilaka_action_auth = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_checkakundsexist":
                        ConstData.tilaka_action_checkakundsexist = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_checkcertstatus":
                        ConstData.tilaka_action_checkcertstatus = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_checksignstatus":
                        ConstData.tilaka_action_checksignstatus = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_executesign":
                        ConstData.tilaka_action_executesign = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_generateuuid":
                        ConstData.tilaka_action_generateuuid = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_register":
                        ConstData.tilaka_action_register = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_requestrevokecert":
                        ConstData.tilaka_action_requestrevokecert = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_requestsign":
                        ConstData.tilaka_action_requestsign = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_upload":
                        ConstData.tilaka_action_upload = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    case "tilaka_action_userregstatus":
                        ConstData.tilaka_action_userregstatus = rsDSTilakaSettings.getString("c_action_path");
                        break;
                    default:
                        break;
                }

            }

            //////////////////////////////////////////////////////////
            try (TilakaApiServices apiService = new TilakaApiServices(ConstData.tilaka_adapter_api_hostname_url)) {

                if (tilakaId == null) {
                    LogUtil.info(TilakaExecuteSign.class.getName(), "tilaka id is null on request_id: " + tilaka_request_id);

                } else {
                    String queryDSTilakaSignExecuted = "";
                    try {
                        queryDSTilakaSignExecuted = "update app_fd_ds_tilaka_api_signs set c_status = 'EXECUTING-FAILED' where c_tilaka_request_id=? and c_tilaka_id=?";
                        psDSDSTilakaSignExecuted = con.prepareStatement(queryDSTilakaSignExecuted);
                        psDSDSTilakaSignExecuted.setString(1, tilaka_request_id);
                        psDSDSTilakaSignExecuted.setString(2, tilakaId);
                        psDSDSTilakaSignExecuted.executeUpdate();

                        apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id,
                                ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));

                        ResponseExecuteSign objouput = apiService.executeSign(tilaka_request_id, tilakaId);

                        if (objouput.getStatus() != null && objouput.getStatus().equals("DONE")) {
                            //
                            String queryDSOrderSeqUpdate = "update app_fd_ds_tilaka_sign_seq set c_next_sequence = cast(cast(c_next_sequence AS int) + 1 AS varchar) "
                                    + "where id=?";
                            psDSDSTilakaSignExecuted = con.prepareStatement(queryDSOrderSeqUpdate);
                            psDSDSTilakaSignExecuted.setString(1, tilaka_request_id);
                            psDSDSTilakaSignExecuted.executeUpdate();
                            //// send mail
                            PreparedStatement psDSSendEmailToTilakaData = null;
                            ResultSet rsDSSendEmailToTilakaData = null;
                            try {
                                String queryDSSendEmailToTilakaData = "select tas.c_tilaka_request_id, do.c_doc_id as doc_ext_id, do.c_name as doc_name, do.c_description as doc_description, "
                                        + "do.c_filedoc as doc_filename, string_agg(tu.id, ';') within group (order by tas.c_sequence asc) as approver_ids, do.createdBy as doc_requester "
                                        + "from app_fd_ds_tilaka_api_signs tas with (nolock) "
                                        + "inner join app_fd_ds_tilaka_users tu with (nolock) on tu.c_user_tilaka = tas.c_tilaka_id "
                                        + "inner join app_fd_ds_document_order do with (nolock) on do.id=tas.c_tilaka_request_id "
                                        + "where do.id = ? "
                                        + "group by tas.c_tilaka_request_id,do.c_doc_id, do.c_name,do.c_description,do.c_filedoc, do.createdBy ";

                                psDSSendEmailToTilakaData = con.prepareStatement(queryDSSendEmailToTilakaData);
                                psDSSendEmailToTilakaData.setString(1, tilaka_request_id);
                                rsDSSendEmailToTilakaData = psDSSendEmailToTilakaData.executeQuery();

                                if (rsDSSendEmailToTilakaData.next()) {
                                    String c_tilaka_request_id = rsDSSendEmailToTilakaData.getString("c_tilaka_request_id");
                                    String doc_ext_id = rsDSSendEmailToTilakaData.getString("doc_ext_id");
                                    String doc_name = rsDSSendEmailToTilakaData.getString("doc_name");
                                    String doc_description = rsDSSendEmailToTilakaData.getString("doc_description");
                                    String doc_filename = rsDSSendEmailToTilakaData.getString("doc_filename");
                                    String approver_ids = rsDSSendEmailToTilakaData.getString("approver_ids");
                                    String doc_requester = rsDSSendEmailToTilakaData.getString("doc_requester");

                                    WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");

                                    String processDefId = "istwDigitalSign:latest:sendEmailToTilakaApproverProcess";

                                    Map variables = new HashMap();
                                    variables.put("tilaka_request_id", c_tilaka_request_id);
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
                                LogUtil.error(TilakaExecuteSign.class.getName(), e, e.getMessage());
                            } finally {
                                if (rsDSSendEmailToTilakaData != null) {
                                    try {
                                        rsDSSendEmailToTilakaData.close();
                                    } catch (SQLException ex) {
                                        LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
                                    }
                                }
                                if (psDSSendEmailToTilakaData != null) {
                                    try {
                                        psDSSendEmailToTilakaData.close();
                                    } catch (SQLException ex) {
                                        LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
                                    }
                                }
                            }
                        }
                        if (objouput.getStatus() != null && (objouput.getStatus().equals("DONE") || objouput.getStatus().equalsIgnoreCase("FAILED"))) {
                            queryDSTilakaSignExecuted = "update app_fd_ds_tilaka_api_signs set c_status = ? where c_tilaka_request_id=? and c_tilaka_id=?";
                            psDSDSTilakaSignExecuted = con.prepareStatement(queryDSTilakaSignExecuted);
                            psDSDSTilakaSignExecuted.setString(1, objouput.getStatus().toUpperCase());
                            psDSDSTilakaSignExecuted.setString(2, tilaka_request_id);
                            psDSDSTilakaSignExecuted.setString(3, tilakaId);
                            psDSDSTilakaSignExecuted.executeUpdate();
                        } else {
                            queryDSTilakaSignExecuted = "update app_fd_ds_tilaka_api_signs set c_status = 'EXECUTING-FAILED' where c_tilaka_request_id=? and c_tilaka_id=?";
                            psDSDSTilakaSignExecuted = con.prepareStatement(queryDSTilakaSignExecuted);
                            psDSDSTilakaSignExecuted.setString(1, tilaka_request_id);
                            psDSDSTilakaSignExecuted.setString(2, tilakaId);
                            psDSDSTilakaSignExecuted.executeUpdate();
                        }

                    } catch (IOException e) {
                        LogUtil.error(TilakaExecuteSign.class.getName(), e, e.getMessage());

                        queryDSTilakaSignExecuted = "update app_fd_ds_tilaka_api_signs set c_status = 'EXECUTING-FAILED' where c_tilaka_request_id=? and c_tilaka_id=?";
                        psDSDSTilakaSignExecuted = con.prepareStatement(queryDSTilakaSignExecuted);
                        psDSDSTilakaSignExecuted.setString(1, tilaka_request_id);
                        psDSDSTilakaSignExecuted.setString(2, tilakaId);
                        psDSDSTilakaSignExecuted.executeUpdate();
                    }

                }

            }
        } catch (SQLException ex) {
            LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
        } finally {
            if (rsDSTilakaIDSigns != null) {
                try {
                    rsDSTilakaIDSigns.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
                }
            }
            if (rsDSTilakaSettings != null) {
                try {
                    rsDSTilakaSettings.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
                }
            }
            if (psDSDSTilakaSignExecuted != null) {
                try {
                    psDSDSTilakaSignExecuted.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
                }
            }
            if (psDSDSTilakaIDSigns != null) {
                try {
                    psDSDSTilakaIDSigns.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
                }
            }
            if (psDSTTilakaSettings != null) {
                try {
                    psDSTTilakaSettings.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaExecuteSign.class.getName(), ex, ex.getMessage());
                }
            }
        }
    }

    private static void debugMessage(String message) {
        boolean debug = true;
        if (debug) {
            LogUtil.info("" + TilakaExecuteSign.class.getName(), "DEBUG MODE: " + message);
        }
    }

}
