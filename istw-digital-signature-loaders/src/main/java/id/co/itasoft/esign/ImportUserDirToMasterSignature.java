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
import org.joget.commons.util.UuidGenerator;



/**
 *
 * @author User
 */
public class ImportUserDirToMasterSignature extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - Import User Dir";
    
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

        String output = "not ok";
        
        String uuid = "";
            
        /*Cek Hanya User Yang Sudah Login Bisa Mengakses Ini*/
        if (!workflowUserManager.isCurrentUserAnonymous()) {
        
       


                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                StringBuilder query = new StringBuilder();
                query
                    .append("SELECT ")
                    .append("a.username ")
                    .append("FROM dir_user a with (nolock) where a.username not in (select id from app_fd_ds_users with (nolock) ) ");

                try (
                        Connection con = ds.getConnection();
                        PreparedStatement ps = con.prepareStatement(query.toString())) {
                        
                        PreparedStatement ps2 = null;

                    try (ResultSet rs = ps.executeQuery()) {



                        
                        while (rs.next()) {
                            
                            String datausername = rs.getString("username");
                            uuid = UuidGenerator.getInstance().getUuid();
                            String sqlinsert = "insert into app_fd_ds_users(id,dateCreated,dateModified,createdBy,createdByName,modifiedBy,modifiedByName,c_status,c_image_signature) "
                                    + "values('"+datausername+"',SYSDATETIME(),SYSDATETIME(),'admin','Admin admin','admin','Admin admin','active','') ";
                            
                            ps2 = con.prepareStatement(sqlinsert);
                            ps2.executeUpdate();
                            
                            String sqlinsert2 = "insert into app_fd_ds_user_groups(id,dateCreated,dateModified,createdBy,createdByName,modifiedBy,modifiedByName,c_user_id,c_group_id) "
                                    + "values('"+uuid+"',SYSDATETIME(),SYSDATETIME(),'admin','Admin admin','admin','Admin admin','"+datausername+"','G-000001') ";
                            
                            ps2 = con.prepareStatement(sqlinsert2);
                            ps2.executeUpdate();                            

                        }

                       output = "ok";
                       response.getWriter().print(output);

                    } catch (SQLException ex) {
                        LogUtil.error(this.getClass().getName(), ex, "Error : " + ex.getMessage());
                    }

                } catch (SQLException e) {
                    LogUtil.error(this.getClass().getName(), e, "Error : " + e.getMessage());
                }

        }
        else{
            response.getWriter().print(output);      
        }
    }
}