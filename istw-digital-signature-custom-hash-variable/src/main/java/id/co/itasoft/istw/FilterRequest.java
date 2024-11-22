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

public class FilterRequest extends DefaultHashVariablePlugin {
    public static final String PLUGIN_NAME = "istw-digital-signature-filter-request";

    @Override
    public String getPrefix() {
        return "filterRequest";
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
        String query = "SELECT c_user_id AS userId, c_group_id AS groupId FROM app_fd_ds_user_groups with (nolock) WHERE c_user_id = ? AND c_group_id = 'G-000001'";
        try {
            con = ds.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, currentUser);
            rs = ps.executeQuery();

            while (rs.next()) {
                String userId = rs.getString("userId");
                String groupId = rs.getString("groupId");
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

        return result.isEmpty() ? "No matching records found" : result;
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
        return "Fetch User and Group ID Plugin";
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