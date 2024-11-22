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

/**
 *
 * @author User
 */
public class DigiSignData extends DefaultHashVariablePlugin {

    public static final String PLUGIN_NAME = "istw-d-sign-digiSignData";

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
        return "Hash Variable to get CurrentActivityId";
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
        syntax.add("digiSignData.CurrentActivityId");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "digiSignData";
    }
    
    private void debugMessage(String msg){
        boolean isDebug  = false;
        if(isDebug){
            LogUtil.info(""+getClassName(), "processHashVariable: digiSignData,  variableKey: " + msg);
        }
    }

    @Override
    public String processHashVariable(String variableKey) {

        debugMessage( "processHashVariable: digiSignData,  variableKey: " + variableKey);
        String resultStr = "";

        if (variableKey.startsWith("CurrentActivityId")) {

            String recordId = "";
            if (variableKey.contains("[") && variableKey.contains("]")) {
                recordId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
            }

            debugMessage( "processHashVariable: digiSignData ==> recordId : " +recordId);
            Connection con = null;
            PreparedStatement psDigiSignData = null;
            ResultSet rsDigiSignData = null;
            try {
                //select all signed by
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                con = ds.getConnection();

                String querySignedBy = "SELECT  a.id, sact.Id AS activityId "
                        + "FROM app_fd_ds_document_order a with (nolock) "
                        + "INNER JOIN wf_process_link wpl with (nolock) ON wpl.originProcessId = a.id "
                        + "INNER JOIN SHKActivities sact with (nolock) ON wpl.processId = sact.ProcessId "
                        + "WHERE (sact.Name = 'Adjust' and a.id = ? ) OR (sact.Name = 'Approval Layer' and a.id = ? ) "
                        + "AND c_status = 'Awaiting' "
                        + "order by activityId desc";
                psDigiSignData = con.prepareStatement(querySignedBy);
                psDigiSignData.setString(1, recordId);
                psDigiSignData.setString(2, recordId);
                rsDigiSignData = psDigiSignData.executeQuery();

                try {
                    if (rsDigiSignData.next()) {
                        resultStr = rsDigiSignData.getString("activityId");
                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsDigiSignData != null) {
                        rsDigiSignData.close();
                    }
                }
            } catch (Exception ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            } finally {
                try {
                    if (psDigiSignData != null) {
                        psDigiSignData.close();
                    }

                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException ex) {
                    LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
                }
            }
        }
        
        debugMessage("resultStr : "+resultStr);

        return resultStr;
    }

}
