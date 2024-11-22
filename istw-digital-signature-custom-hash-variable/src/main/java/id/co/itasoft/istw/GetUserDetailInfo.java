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
import java.util.Arrays;
import java.util.Collection;
import javax.sql.DataSource;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;

/**
 * tidak dipakai
 *
 * @author User
 */
public class GetUserDetailInfo extends DefaultHashVariablePlugin {

    public static final String PLUGIN_NAME = "istw-d-signature-get-userinfo";

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
        return "Get Detail Info of digital signature user";
    }

    @Override
    public String getPrefix() {
        return "dsignature";
    }

    private void debugMessage(String out) {
        boolean debug_mode = false;
        if (debug_mode) {
            LogUtil.info(PLUGIN_NAME, out);
        }
    }

    @Override
    public String processHashVariable(String variableKey) {

        String resultStr = "false";

        if (variableKey.startsWith("hasSignature")) {
            String userId = "";
            if (variableKey.contains("[") && variableKey.contains("]")) {
                userId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
            }

            debugMessage("userId : " + userId);

            Connection con = null;
            PreparedStatement psHasSignature = null;
            ResultSet rsHasSignature = null;
            try {
                //select user id sinature
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                con = ds.getConnection();

                String queryHasSignature = "SELECT CASE "
                        + "WHEN COUNT(*) = 0 THEN 'false'"
                        + "WHEN MAX(c_image_signature) IS NOT NULL AND MAX(c_image_signature) <> '' AND "
                        + "MAX(c_signature) IS NOT NULL AND MAX(c_signature) <> '' THEN 'true' "
                        + "ELSE 'false' "
                        + "END AS result "
                        + "FROM app_fd_ds_master_signatures with (nolock) "
                        + "WHERE id = ?";
                psHasSignature = con.prepareStatement(queryHasSignature);
                psHasSignature.setString(1, userId);
                rsHasSignature = psHasSignature.executeQuery();

                try {
                    if (rsHasSignature.next()) {
                        debugMessage("result : " + rsHasSignature.getString("result"));
                        resultStr = rsHasSignature.getString("result");
                    }
                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsHasSignature != null) {
                        rsHasSignature.close();
                    }
                }
            } catch (Exception ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            } finally {
                try {
                    if (psHasSignature != null) {
                        psHasSignature.close();
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
        syntax.add("dsignature.hasSignature");
        return syntax;
    }

}
