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
import id.co.itasoft.tilaka.library.response.RespPdfData;
import id.co.itasoft.tilaka.library.response.ResponseCheckSignStatus;
import id.co.itasoft.tilaka.library.response.Status;
import id.co.itasoft.tilaka.library.services.TilakaApiServices;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author User
 */
public class TilakaCheckSignStatus {

    public static String pluginName = "ISTW - DS - Tilaka Check Signs";

    public static void startCheckSignStatus(String tilaka_request_id) {
        debugMessage("startCheckSignStatus ... ");
        Connection con = null;
        PreparedStatement psDSTTilakaSettings = null, psDSDSTilakaIDSigns = null, psDSOrderUpdate = null;
        ResultSet rsDSTilakaSettings = null, rsDSTilakaIDSigns = null;

        PreparedStatement psDSCheckSignStatus = null, psDSCheckSignCall = null;
        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();

            try (TilakaApiServices apiService = new TilakaApiServices(ConstData.tilaka_adapter_api_hostname_url)) {

                apiService.setAccessToken(apiService.getAccessToken(ConstData.tilaka_channel_id,
                        ConstData.tilaka_client_secret, ConstData.tilaka_api_base_url + ConstData.tilaka_action_auth));

                ResponseCheckSignStatus respCheckSignStatus = apiService.checkSignStatus(tilaka_request_id);
                debugMessage("respCheckSignStatus = " + respCheckSignStatus.toString());

                String queryDSTilakaCheckSignStatusCall = "update app_fd_ds_tilaka_api_calls set c_last_response_code=?, "
                        + "c_last_action = ?, c_last_response_data = ?, c_last_action_status = ?, c_presigned_url = ?, dateModified=SYSDATETIME() where id=?";
                psDSCheckSignCall = con.prepareStatement(queryDSTilakaCheckSignStatusCall);
                psDSCheckSignCall.setString(1, respCheckSignStatus.getResponseCode());
                psDSCheckSignCall.setString(2, "tilaka_action_checksignstatus");

                debugMessage("srespCheckSignStatus.isSuccess() : " + respCheckSignStatus.isSuccess());

                boolean isFailed = false;

                if (respCheckSignStatus.isSuccess()) {
                    psDSCheckSignCall.setString(3, respCheckSignStatus.getJsonString());
                    psDSCheckSignCall.setString(4, "OK");

                    for (Status status : respCheckSignStatus.getListStatus()) {

                        if (status.getStatus().equals("DONE")) {
                            String queryDSTilakaSignUpdate = "update app_fd_ds_tilaka_api_signs set c_status_sequence=?, "
                                    + "c_status = ?, c_status_num_signatures = ?, c_status_num_signatures_done = ?, dateModified=SYSDATETIME() "
                                    + "where c_tilaka_request_id=? and c_tilaka_id=? and c_status='AWAITING'";
                            psDSCheckSignStatus = con.prepareStatement(queryDSTilakaSignUpdate);
                            psDSCheckSignStatus.setString(1, "" + status.getSequence());
                            psDSCheckSignStatus.setString(2, status.getStatus());
                            psDSCheckSignStatus.setString(3, "" + status.getNumSignatures());
                            psDSCheckSignStatus.setString(4, "" + status.getNumSignaturesDone());
                            psDSCheckSignStatus.setString(5, respCheckSignStatus.getRequestId());
                            psDSCheckSignStatus.setString(6, status.getUserIdentifier());
                            psDSCheckSignStatus.executeUpdate();

                            //
                        } else {
                            String queryDSTilakaSignUpdate = "update app_fd_ds_tilaka_api_signs set c_status_sequence=?, "
                                    + "c_status = ?, dateModified=SYSDATETIME() "
                                    + "where c_tilaka_request_id=? and c_tilaka_id=?";
                            psDSCheckSignStatus = con.prepareStatement(queryDSTilakaSignUpdate);
                            psDSCheckSignStatus.setString(1, "" + status.getSequence());
                            psDSCheckSignStatus.setString(2, status.getStatus());
                            psDSCheckSignStatus.setString(3, respCheckSignStatus.getRequestId());
                            psDSCheckSignStatus.setString(4, status.getUserIdentifier());
                            psDSCheckSignStatus.executeUpdate();
                            if (!isFailed && status.getStatus().equals("FAILED")) {
                                isFailed = true;
                                String queryDSOrderUpdate = "update app_fd_ds_document_order set c_status = 'failed', dateModified=SYSDATETIME() where id=?";
                                psDSOrderUpdate = con.prepareStatement(queryDSOrderUpdate);
                                psDSOrderUpdate.setString(1, tilaka_request_id);
                                psDSOrderUpdate.executeUpdate();
                            }
                        }

                        debugMessage("status.getStatus() for " + status.getUserIdentifier() + ": " + status.getStatus());

                    }
                    String presignedUrl = "";

                    for (RespPdfData rsp : respCheckSignStatus.getListPdf()) {
                        presignedUrl = rsp.getPresignedUrl();
                    }
                    psDSCheckSignCall.setString(5, presignedUrl);
                    if (!isFailed) {
                        String queryDSOrderUpdate = "update app_fd_ds_document_order set c_status = 'done' where id=?";
                        psDSOrderUpdate = con.prepareStatement(queryDSOrderUpdate);
                        psDSOrderUpdate.setString(1, tilaka_request_id);
                        psDSOrderUpdate.executeUpdate();
                    }

                } else {
                    psDSCheckSignCall.setString(3, "");
                    psDSCheckSignCall.setString(4, "FAIL");
                    psDSCheckSignCall.setString(5, null);
                }
                psDSCheckSignCall.setString(6, tilaka_request_id);

                psDSCheckSignCall.executeUpdate();

            } catch (IOException e) {
                LogUtil.error(TilakaCheckSignStatus.class.getName(), e, e.getMessage());
            }

        } catch (SQLException ex) {
            LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
        } finally {
            if (rsDSTilakaIDSigns != null) {
                try {
                    rsDSTilakaIDSigns.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
                }
            }
            if (rsDSTilakaSettings != null) {
                try {
                    rsDSTilakaSettings.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
                }
            }

            if (psDSOrderUpdate != null) {
                try {
                    psDSOrderUpdate.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
                }
            }
            if (psDSCheckSignStatus != null) {
                try {
                    psDSCheckSignStatus.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
                }
            }
            if (psDSCheckSignCall != null) {
                try {
                    psDSCheckSignCall.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
                }
            }

            if (psDSDSTilakaIDSigns != null) {
                try {
                    psDSDSTilakaIDSigns.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
                }
            }
            if (psDSTTilakaSettings != null) {
                try {
                    psDSTTilakaSettings.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LogUtil.error(TilakaCheckSignStatus.class.getName(), ex, ex.getMessage());
                }
            }
        }

    }

    private static void debugMessage(String message) {
        boolean debug = true;
        if (debug) {
            LogUtil.info("" + TilakaCheckSignStatus.class.getName(), "DEBUG MODE: " + message);
        }
    }
}
