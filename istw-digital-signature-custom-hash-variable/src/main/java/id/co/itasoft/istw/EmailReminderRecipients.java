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
public class EmailReminderRecipients extends DefaultHashVariablePlugin {
    
    public static final String PLUGIN_NAME = "istw-d-signature-reminder-recipients";

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
        return "Hash Variable to get reminder recipients";
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
        syntax.add("emailreminder.recipients");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "emailreminder";
    }

    @Override
    public String processHashVariable(String variableKey) {
        
        //LogUtil.info(getClassName(),"processHashVariable: EmailReminderRecipients");
        
        String resultStr = "";

        

        String recordId = null;
        if (variableKey.contains("[") && variableKey.contains("]")) {
            recordId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
        }

        Connection con = null;
        PreparedStatement psRecipients = null;
        ResultSet rsRecipients = null;
        try {
            //select all recipients
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();

            String queryRecipients = "WITH SplitValues1 AS (SELECT value, ROW_NUMBER() OVER (PARTITION BY value ORDER BY (SELECT NULL)) AS row_num "
                    + "FROM string_split((SELECT CONCAT(a.c_approver_lv_1, ';', a.c_adjust_by) FROM app_fd_ds_document_order a with (nolock) WHERE id =?), ';')), "
                    + "SplitValues2 AS (SELECT value, ROW_NUMBER() OVER (PARTITION BY value ORDER BY (SELECT NULL)) AS row_num "
                    + "FROM string_split((SELECT c_signed_by FROM app_fd_ds_document_order a with (nolock) WHERE id =?), ';') "
                    + "WHERE value != ''), ExceptValues AS (SELECT s1.value, s1.row_num FROM SplitValues1 s1 LEFT JOIN SplitValues2 s2 ON s1.value = s2.value "
                    + "AND s1.row_num = s2.row_num WHERE s2.value IS NULL) SELECT STRING_AGG(value, ';') AS merged_values FROM ExceptValues;";
            psRecipients = con.prepareStatement(queryRecipients);
            psRecipients.setString(1, recordId);
            psRecipients.setString(2, recordId);
            rsRecipients = psRecipients.executeQuery();

            try {

                //LogUtil.info(getClassName(),"user recipients:");

                if (rsRecipients.next()) {
                    
                    resultStr += rsRecipients.getString("merged_values");
                    
                    //LogUtil.info(getClassName(),"user recipients : " + resultStr);
                }


            } catch (Exception e) {
                LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
            } finally {
                if (rsRecipients != null) {
                    rsRecipients.close();
                }
            }
        } catch (Exception ex) {
            LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
        } finally {
            try {
                if (psRecipients != null) {
                    psRecipients.close();
                }

                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            }
        }

      

        return resultStr;
    }

}
