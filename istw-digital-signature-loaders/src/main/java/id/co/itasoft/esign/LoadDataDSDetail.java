/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.itasoft.esign;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author User
 */
public class LoadDataDSDetail extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - Load Data DS Detail";
    
    @Override
    public String renderTemplate(FormData fd, Map map) {
        return "";
    }

    @Override
    public String getName() {
        return pluginName;
        
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return pluginName;
    }

    @Override
    public String getLabel() {
        return pluginName;
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
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        WorkflowUserManager workflowUserManager = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");       

        JSONObject mainObj = new JSONObject();
            
        /*Cek Hanya User Yang Sudah Login Bisa Mengakses Ini*/
        if (!workflowUserManager.isCurrentUserAnonymous()) {
        
        
            if (request.getParameterMap().containsKey("username")) {

                String username = request.getParameter("username");

                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                StringBuilder query = new StringBuilder();
                query
                        .append("SELECT "
                                + "    a.username, "
                                + "    a.email,"
                                + "    CASE "
                                + "        WHEN COUNT(dsms.id) = 0 THEN 'false'"
                                + "        WHEN MAX(dsms.c_image_signature) IS NOT NULL AND MAX(dsms.c_image_signature) <> '' AND "
                                + "             MAX(dsms.c_signature) IS NOT NULL AND MAX(dsms.c_signature) <> '' THEN 'true' "
                                + "        ELSE 'false' "
                                + "    END AS signature_result "
                                + "FROM "
                                + "    dir_user a with (nolock) "
                                + "LEFT JOIN "
                                + "    app_fd_ds_master_signatures dsms with (nolock) ON dsms.id = a.username "
                                + "WHERE "
                                + "    a.username = ? "
                                + "GROUP BY "
                                + "    a.username, a.email");
                try (
                        Connection con = ds.getConnection();
                        PreparedStatement ps = con.prepareStatement(query.toString())) {
                    ps.setString(1, username);

                    try (ResultSet rs = ps.executeQuery()) {

                        ResultSetMetaData rsmd = rs.getMetaData();
                        int columnCount = rsmd.getColumnCount();


                        JSONObject valObject = new JSONObject();
                        if (rs.next()) {
                            
                            for (int i = 1; i <= columnCount; i++) {
                                valObject.put(rsmd.getColumnLabel(i), rs.getString(rsmd.getColumnLabel(i)));
                            }

                        }
                        mainObj.put("data", valObject);
                        mainObj.write(response.getWriter());


                    } catch (JSONException ex) {
                        LogUtil.error(this.getClass().getName(), ex, "Error : " + ex.getMessage());
                    }

                } catch (SQLException e) {
                    LogUtil.error(this.getClass().getName(), e, "Error : " + e.getMessage());
                }
            }
        }
        else{
            
            try {
                mainObj.put("status", false);
                mainObj.put("message", "You Must Login First.");
                mainObj.write(response.getWriter());  
            } catch (JSONException ex) {
                LogUtil.error(this.getClass().getName(), ex, "Error : " + ex.getMessage());
            }          
        }
    }
}