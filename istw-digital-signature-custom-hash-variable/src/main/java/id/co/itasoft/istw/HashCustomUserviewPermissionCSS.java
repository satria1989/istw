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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.workflow.util.WorkflowUtil;

/**
 *
 * @author User
 */
public class HashCustomUserviewPermissionCSS extends DefaultHashVariablePlugin {
    
    public static final String PLUGIN_NAME = "istw-d-signature-hide-menu-by-css";

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
        return "Hash Variable to hide menu by permission css";
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
        syntax.add("cssmenus.hidemenus");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "cssmenus";
    }

    @Override
    public String processHashVariable(String variableKey) {
        String resultStr = "";
        if (WorkflowUtil.isCurrentUserAnonymous()) {
            return "";
        }

        //begin doc ready

        String queryParams = null;
        if (variableKey.contains("[") && variableKey.contains("]")) {
            queryParams = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
        }

        Map<String, String[]> parameters = null;
        if (queryParams != null && !queryParams.isEmpty()) {
            parameters = StringUtil.getUrlParams(queryParams);

            //put all parameters to plugin properties
            getProperties().putAll(parameters);
        }


        Connection con = null;
        PreparedStatement psMenus = null, psUserGroup = null, psPermission = null;
        ResultSet rsMenus = null, rsUserGroup = null, rspermission = null;
        try {
            //select all menus
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();

            String queryMenus = "select id,c_name,c_url from app_fd_ds_menus with (nolock) ";
            psMenus = con.prepareStatement(queryMenus);
            rsMenus = psMenus.executeQuery();
            String menu_id = "";

            String menu_url = "";
            try {
                LinkedHashMap<String, Boolean> menuurls = new LinkedHashMap<String, Boolean>();
                //LogUtil.info(getClassName(),"menu_urls:");
                boolean hasRequestSubmissionRole = true; 
                String requestSubmissionURL ="";
                //first, hide all url menu 
                while (rsMenus.next()) {
                    menu_id = rsMenus.getString(1);

                    menu_url = rsMenus.getString(3);
                    menuurls.put(menu_url, false);
                    //LogUtil.info(getClassName(),"menu_urls: " + menu_url);
                    if(menu_id.equals("MENU-001")){
                        requestSubmissionURL = menu_url;
                    }
                }

                //showing menu urls by username
                String currentUser = WorkflowUtil.getCurrentUsername();


                String queryUserGroup = "select c_group_id from app_fd_ds_user_groups with (nolock) where c_user_id = "
                        + "(select id from app_fd_ds_users with (nolock) where id = ?)";
                psUserGroup = con.prepareStatement(queryUserGroup);
                psUserGroup.setString(1, currentUser);
                rsUserGroup = psUserGroup.executeQuery();

                String userGroup = "";



                while (rsUserGroup.next()) {
                    userGroup = rsUserGroup.getString(1);
                    //LogUtil.info(getClassName(),"userGroup is:"+userGroup);
                    String queryPermission = "select c_url from app_fd_ds_group_permission gtn with (nolock) "
                            + "left join app_fd_ds_menus mtn with (nolock) on gtn.c_menu_id=mtn.id "
                            + "where gtn.c_group_id = ? and gtn.c_access='full_access' ";

                    psPermission = con.prepareStatement(queryPermission);
                    psPermission.setString(1, userGroup);
                    rspermission = psPermission.executeQuery();

                    try {
                        //LogUtil.info(getClassName(),"and has permission to:");
                        while (rspermission.next()) {
                            menu_url = rspermission.getString(1);
                            menuurls.put(menu_url, true);
                            //LogUtil.info(getClassName(),"permission: "+menu_url);
                        }

                    } catch (Exception e) {
                        LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                    } finally {
                        if (rspermission != null) {
                            rspermission.close();
                        }
                    }
                }
        
                if (WorkflowUtil.isCurrentUserInRole("ROLE_ADMIN")) {
                    menuurls.put("/jw/web/userview/istwDigitalSign/v/_/setting_menu", true);
                    menuurls.put("/jw/web/userview/istwDigitalSign/v/_/setting_group", true);
                    menuurls.put("/jw/web/userview/istwDigitalSign/v/_/setting_user_groups", true);
                    menuurls.put("/jw/web/userview/istwDigitalSign/v/_/setting_tilaka_env", true);
                    LogUtil.info(getClassName(), "user is role admin");
                }                

                Set<String> keys = menuurls.keySet();

                for (String key : keys) {
                    
                    if (menuurls.get(key)) {

                    } else {
                        resultStr += "#category-container li ul li.menu a[href^='" + key + "'], #category-container li.category a[href^='" + key + "']{\n";
                        resultStr += "display:none !important;\n";
                        resultStr += "visibility:hidden !important;\n";
                        if(key.equals(requestSubmissionURL)){
                            hasRequestSubmissionRole = false;
                        }
                        resultStr += "}\n";
                    }
                    
                }
                if(!hasRequestSubmissionRole){
                    resultStr += "button[data-href='signature_request']{\n";
                        resultStr += "display:none !important;\n";
                        resultStr += "visibility:hidden !important;\n";                    
                    resultStr += "}\n";
                }

            } catch (Exception e) {
                LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
            } finally {
                if (rspermission != null) {
                    rspermission.close();
                }
                if (rsUserGroup != null) {
                    rsUserGroup.close();
                }
                if (rsMenus != null) {
                    rsMenus.close();
                }
            }
        } catch (Exception ex) {
            LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
        } finally {
            try {
                if (psMenus != null) {
                    psMenus.close();
                }

                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            }
        }

        //end doc ready

        return resultStr;
    }

}
