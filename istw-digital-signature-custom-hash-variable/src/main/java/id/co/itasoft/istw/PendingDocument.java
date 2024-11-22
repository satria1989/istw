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

public class PendingDocument extends DefaultHashVariablePlugin {
    public static final String PLUGIN_NAME = "istw-digital-signature-pending-document";

    @Override
    public String getPrefix() {
        return "pendingDocument";
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
        String query = "SELECT COUNT (*) AS count_data " +
                "FROM app_fd_ds_document_order with (nolock) " +
                "WHERE createdBy = ? AND c_status = 'New'";
        try {
            con = ds.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, currentUser);
            rs = ps.executeQuery();

            while (rs.next()) {
                if ("count_data".equalsIgnoreCase(variableKey)) {
                    result = rs.getString("count_data");
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

        return result.isEmpty() ? "Document Tidak Di temukan" : result;
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
        return "Pending Report Hash Variable Plugin";
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


