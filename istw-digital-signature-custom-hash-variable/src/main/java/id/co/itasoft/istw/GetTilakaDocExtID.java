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
public class GetTilakaDocExtID extends DefaultHashVariablePlugin {

    public static final String PLUGIN_NAME = "istw-ds-tilakadoc-ext-id";

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Get url of tilakadoc-extID";
    }

    @Override
    public String getPrefix() {
        return "tilaka";
    }

    private void debugMessage(String out) {
        boolean debug_mode = true;
        if (debug_mode) {
            LogUtil.info("" + PLUGIN_NAME, out);
        }
    }

    @Override
    public String processHashVariable(String variableKey) {

        String resultStr = "";
        if (WorkflowUtil.isCurrentUserAnonymous()) {
            return "";
        }

        if (variableKey.startsWith("getDocExtID")) {
            String tilaka_request_id = "";
            if (variableKey.contains("[") && variableKey.contains("]")) {
                tilaka_request_id = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
            }

            debugMessage("tilaka_request_id : " + tilaka_request_id);

            Connection con = null;
            PreparedStatement psDocExtID = null;
            ResultSet rsDocExtID = null;
            try {
                //select user id sinature
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                con = ds.getConnection();

                String queryAuthUrl = "select c_doc_id from app_fd_ds_document_order with (nolock) where id=? ";
                psDocExtID = con.prepareStatement(queryAuthUrl);
                psDocExtID.setString(1, tilaka_request_id);
                rsDocExtID = psDocExtID.executeQuery();

                try {
                    if (rsDocExtID.next()) {
                        debugMessage("result : " + rsDocExtID.getString("c_doc_id"));
                        resultStr = rsDocExtID.getString("c_doc_id");
                    }
                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsDocExtID != null) {
                        rsDocExtID.close();
                    }
                }
            } catch (Exception ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            } finally {
                try {
                    if (psDocExtID != null) {
                        psDocExtID.close();
                    }

                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException ex) {
                    LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
                }
            }
        }

        return resultStr;
    }

    @Override
    public String getLabel() {
        return PLUGIN_NAME;
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return null;
    }

    @Override
    public Collection<String> availableSyntax() {
        Collection<String> syntax = new ArrayList<String>();
        syntax.add("tilaka.getDocExtID");
        return syntax;
    }

}
