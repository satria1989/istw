package id.co.itasoft.istw;

import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.model.service.WorkflowUserManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetTilakaUser extends DefaultHashVariablePlugin {
    public static final String PLUGIN_NAME = "istw-digital-signature-tilaka-username";

    @Override
    public String getPrefix() {
        return "tilakaUser";
    }

    @Override
    public String processHashVariable(String variableKey) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection con = null;
        String result = "";
        WorkflowUserManager workflowUserManager = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        String currentUser = workflowUserManager.getCurrentUsername();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_user_tilaka AS tilaka_user, c_registration_id AS registration_id, c_certificate_status_code AS certificate_status_code, c_certificate_status AS certificate_status " +
                "FROM app_fd_ds_tilaka_users with (nolock) " +
                "WHERE id = ?";
        try {
            con = ds.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, currentUser);
            rs = ps.executeQuery();

            while (rs.next()) {
                if ("tilaka_user".equalsIgnoreCase(variableKey)) {
                    result = rs.getString("tilaka_user");
                    LogUtil.info(this.getClass().getName(), "Tilaka User ID: " + result);
                } else if ("registration_id".equalsIgnoreCase(variableKey)) {
                    result = rs.getString("registration_id");
                    LogUtil.info(this.getClass().getName(), "Registration ID: " + result);
                } else if ("certificate_status_code".equalsIgnoreCase(variableKey)) {
                    result = rs.getString("certificate_status_code");
                    LogUtil.info(this.getClass().getName(), "Certificate Status Code: " + result);
                } else if("certificate_status".equalsIgnoreCase(variableKey)) {
                    result = rs.getString("certificate_status");
                    LogUtil.info(this.getClass().getName(), "Certificate Status: " + result);

                } else {
                    result = "";
                }
            }

        } catch (SQLException sqlException) {
            LogUtil.error(this.getClass().getName(), sqlException, "SQL Error: " + sqlException.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                LogUtil.error(this.getClass().getName(), e, "Error closing resources: " + e.getMessage());
            }
        }
        return (result == null || result.isEmpty()) ? "Tilaka User Tidak Ditemukan" : result;
    }

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
        return "Get Tilaka Username by current User";
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
}


