/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.itasoft.istw;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import javax.sql.DataSource;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.util.WorkflowUtil;

/**
 *
 * @author User
 */
public class HashCheckIsValidApprovalURL extends DefaultHashVariablePlugin {

    public static final String PLUGIN_NAME = "istw-d-signature-chk-valid-approve-url";

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getLabel() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return "Hash Variable to hide content with invalid aproval url";
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
    public Collection<String> availableSyntax() {
        Collection<String> syntax = new ArrayList<String>();
        syntax.add("approval.isSignedByUserLogin");
        syntax.add("approval.isTilakaUserLoginNextSequence");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "approval";
    }

    @Override
    public String processHashVariable(String variableKey) {
        String resultStr = "";
        if (WorkflowUtil.isCurrentUserAnonymous()) {
            return "";
        }

        debugMessage("variableKey : "+ variableKey);
        //begin doc ready
        if (variableKey.startsWith("isSignedByUserLogin")) {

            String urlandactId = "";
            if (variableKey.contains("[") && variableKey.contains("]")) {
                urlandactId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
            }

            String[] x = urlandactId.split(";");

            String url = x[0];
            //LogUtil.info(getClassName(), "url hcek approval : " + url);
            if (url.endsWith("/inbox_approval")) {
                String actId = x[1];
                if (actId != null && !actId.isEmpty()) {

                    Connection con = null;
                    PreparedStatement psCheck = null;
                    ResultSet rsCheck = null;

                    String actType = actId.endsWith("ds_process_adjust") ? "adjust" : "approve";

                    //LogUtil.info(getClassName(), "actId : " + actId);
                    String currentUsername = WorkflowUtil.getCurrentUsername();
                    try {
                        if (actType.equals("adjust")) {

                            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                            con = ds.getConnection();

                            String queryCheck = "SELECT  a.id, ot.createdBy "
                                    + "FROM app_fd_ds_document_order a with (nolock) "
                                    + "INNER JOIN wf_process_link wpl with (nolock) ON wpl.originProcessId = a.id "
                                    + "INNER JOIN SHKActivities sact with (nolock) ON wpl.processId = sact.ProcessId "
                                    + "JOIN SHKActivityStates ssta with (nolock) ON ssta.oid = sact.State "
                                    + "INNER JOIN SHKAssignmentsTable sass with (nolock) ON sact.Id = sass.ActivityId "
                                    + "LEFT JOIN app_fd_ds_doc_order_trail ot with (nolock) ON ot.c_request_id = a.id AND ot.createdBy = ? AND ot.c_action_name LIKE 'adjusted by%' "
                                    + "WHERE sact.Id = ?";
                            psCheck = con.prepareStatement(queryCheck);
                            psCheck.setString(1, currentUsername);
                            psCheck.setString(2, actId);
                            rsCheck = psCheck.executeQuery();

                            if (rsCheck.next()) {
                                if (rsCheck.getString("createdBy") != null && rsCheck.getString("createdBy").equals(currentUsername)) {
                                    resultStr = "1";
                                }
                            }

                        } else {
                            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                            con = ds.getConnection();

                            String queryCheck = "SELECT  a.id, ot.createdBy "
                                    + "FROM app_fd_ds_document_order a with (nolock) "
                                    + "INNER JOIN wf_process_link wpl with (nolock) ON wpl.originProcessId = a.id "
                                    + "INNER JOIN SHKActivities sact with (nolock) ON wpl.processId = sact.ProcessId "
                                    + "JOIN SHKActivityStates ssta with (nolock) ON ssta.oid = sact.State "
                                    + "INNER JOIN SHKAssignmentsTable sass with (nolock) ON sact.Id = sass.ActivityId "
                                    + "LEFT JOIN app_fd_ds_doc_order_trail ot with (nolock) ON ot.c_request_id = a.id AND ot.createdBy = ? AND ot.c_action_name LIKE 'approved by%' "
                                    + "WHERE sact.Id = ?";
                            psCheck = con.prepareStatement(queryCheck);
                            psCheck.setString(1, currentUsername);
                            psCheck.setString(2, actId);
                            rsCheck = psCheck.executeQuery();

                            if (rsCheck.next()) {
                                if (rsCheck.getString("createdBy") != null && rsCheck.getString("createdBy").equals(currentUsername)) {
                                    resultStr = "1";
                                }
                            }
                        }
                    } catch (SQLException ex) {
                        LogUtil.error(getClassName(), ex, ex.getMessage());
                    } finally {
                        if (rsCheck != null) {
                            try {
                                rsCheck.close();
                            } catch (SQLException ex) {
                                LogUtil.error(getClassName(), ex, ex.getMessage());
                            }
                        }
                        if (psCheck != null) {
                            try {
                                psCheck.close();
                            } catch (SQLException ex) {
                                LogUtil.error(getClassName(), ex, ex.getMessage());
                            }
                        }
                        if (con != null) {
                            try {
                                con.close();
                            } catch (SQLException ex) {
                                LogUtil.error(getClassName(), ex, ex.getMessage());
                            }
                        }
                    }

                }
            }

        } else if (variableKey.startsWith("isTilakaUserLoginNextSequence")) {

            String urlandactId = "";
            if (variableKey.contains("[") && variableKey.contains("]")) {
                urlandactId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
            }

            String[] x = urlandactId.split(";");

            String url = x[0];
            debugMessage("url : "+ url);
            //LogUtil.info(getClassName(), "url hcek approval : " + url);
            if (url.endsWith("/tilaka_execute_sign")) {
                String tilaka_request_id = x[1];
                debugMessage("tilaka_request_id : "+ tilaka_request_id);
                if (tilaka_request_id != null && !tilaka_request_id.isEmpty()) {

                    Connection con = null;
                    PreparedStatement psCheck = null;
                    ResultSet rsCheck = null;

                    //LogUtil.info(getClassName(), "actId : " + actId);
                    String currentUsername = WorkflowUtil.getCurrentUsername();
                    try {

                        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                        con = ds.getConnection();

                        String queryCheck = "select tu.id as value from app_fd_ds_tilaka_api_signs tas with (nolock) "
                                + "inner join app_fd_ds_tilaka_users tu with (nolock) on tu.c_user_tilaka = tas.c_tilaka_id "
                                + "inner join app_fd_ds_tilaka_sign_seq tss with (nolock) on tss.c_next_sequence = tas.c_sequence and tss.id = tas.c_tilaka_request_id "
                                + "where tu.id=? and tas.c_tilaka_request_id = ? ";
                        psCheck = con.prepareStatement(queryCheck);
                        psCheck.setString(1, currentUsername);
                        psCheck.setString(2, tilaka_request_id);
                        rsCheck = psCheck.executeQuery();

                        if (rsCheck.next()) {
                            resultStr = "1";
                            debugMessage("result joget id : "+ rsCheck.getString("value"));
                        }
                        
                        debugMessage("resultStr : "+ resultStr);

                    } catch (SQLException ex) {
                        LogUtil.error(getClassName(), ex, ex.getMessage());
                    } finally {
                        if (rsCheck != null) {
                            try {
                                rsCheck.close();
                            } catch (SQLException ex) {
                                LogUtil.error(getClassName(), ex, ex.getMessage());
                            }
                        }
                        if (psCheck != null) {
                            try {
                                psCheck.close();
                            } catch (SQLException ex) {
                                LogUtil.error(getClassName(), ex, ex.getMessage());
                            }
                        }
                        if (con != null) {
                            try {
                                con.close();
                            } catch (SQLException ex) {
                                LogUtil.error(getClassName(), ex, ex.getMessage());
                            }
                        }
                    }

                }
            }
        }

        //LogUtil.info(getClassName(), "resultStr : " + resultStr);
        //end doc ready
        return resultStr;
    }
    
    private void debugMessage(String message) {
        boolean debug = true;
        if (debug) {
            LogUtil.info("" + HashCheckIsValidApprovalURL.class.getName(), "DEBUG MODE: " + message);
        }
    }    

}
