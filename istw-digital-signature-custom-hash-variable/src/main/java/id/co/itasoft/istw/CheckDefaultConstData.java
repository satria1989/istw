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
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;

/**
 *
 * @author User
 */
public class CheckDefaultConstData extends DefaultHashVariablePlugin {

    public static final String PLUGIN_NAME = "istw-d-sign-chk-defaultData";

    public enum GroupData {
        REQUESTER("G-000001", "Requester"),
        CHECKER("G-000002", "Checker (Approval lvl 1)"),
        APPROVER("G-000003", "Approver (Approval lvl 2)"),
        ADMIN("G-000005", "Admin");

        private final String id;
        private final String groupName;

        GroupData(String id, String groupName) {
            this.id = id;
            this.groupName = groupName;
        }

        public String getId() {
            return id;
        }

        public String getGroupName() {
            return groupName;
        }

        public static GroupData getById(String id) {
            for (GroupData group : values()) {
                if (group.getId().equals(id)) {
                    return group;
                }
            }
            throw new IllegalArgumentException("No group with id " + id + " found");
        }

        public static GroupData getByGroupName(String groupName) {
            for (GroupData group : values()) {
                if (group.getGroupName().equals(groupName)) {
                    return group;
                }
            }
            throw new IllegalArgumentException("No group with group name " + groupName + " found");
        }
    }

    public enum DsMenus {
        MENU_001("MENU-001", "Pengajuan Dokumen", "/jw/web/userview/istwDigitalSign/v/_/signature_request"),
        MENU_002("MENU-002", "Inbox Pengajuan", "/jw/web/userview/istwDigitalSign/v/_/inbox_approval"),
        MENU_003("MENU-003", "Detail Dokumen", "/jw/web/userview/istwDigitalSign/v/_/detail_document_req"),
        MENU_004("MENU-004", "Admin Setup Menu", "/jw/web/userview/istwDigitalSign/v/_/setting_menu"),
        MENU_005("MENU-005", "Admin Setup Group Menu", "/jw/web/userview/istwDigitalSign/v/_/setting_group"),
        MENU_006("MENU-006", "Admin Setup User Groups", "/jw/web/userview/istwDigitalSign/v/_/setting_user_groups"),
        MENU_007("MENU-007", "My Request List", "/jw/web/userview/istwDigitalSign/v/_/my_request_list"),
        MENU_009("MENU-009", "Download Dokumen", "/jw/web/userview/istwDigitalSign/v/_/document_report"),
        MENU_010("MENU-010", "View My Unprepared Documents", "/jw/web/userview/istwDigitalSign/v/_/unprepared_documents"),
        MENU_011("MENU-011", "All Request List", "/jw/web/userview/istwDigitalSign/v/_/all_request_list"),
        MENU_012("MENU-012", "My Signature", "/jw/web/userview/istwDigitalSign/v/_/my_signatures"),
        MENU_013("MENU-013", "Dashboard", "/jw/web/userview/istwDigitalSign/v/_/dashboard"),
        MENU_014("MENU-014", "Inquiry Pengajuan", "/jw/web/userview/istwDigitalSign/v/_/inquiry_pengajuan"),
        MENU_015("MENU-015", "Admin Setup Tilaka Env", "/jw/web/userview/istwDigitalSign/v/_/setting_tilaka_env"),
        MENU_016("MENU-016", "Inbox Pengajuan Ext", "/jw/web/userview/istwDigitalSign/v/_/inbox_approval_ext"),
        MENU_017("MENU-017", "Download Dokumen External", "/jw/web/userview/istwDigitalSign/v/_/document_report_ext");

        private final String id;
        private final String cName;
        private final String cUrl;

        DsMenus(String id, String cName, String cUrl) {
            this.id = id;
            this.cName = cName;
            this.cUrl = cUrl;
        }

        public String getId() {
            return id;
        }

        public String getCName() {
            return cName;
        }

        public String getCUrl() {
            return cUrl;
        }
    }

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
        return "Hash Variable to check defaultData";
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
        syntax.add("checkDefaultConstData.evaluateNow");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "checkDefaultConstData";
    }

    private final boolean debug_mode = false;

    private void debugMessage(String msg) {

        if (debug_mode) {
            LogUtil.info(""+getClassName(), msg);
        }
    }

    @Override
    public String processHashVariable(String variableKey) {

        debugMessage("processHashVariable: checkDefaultConstData,  variableKey: " + variableKey);

        if (variableKey.startsWith("evaluateNow")) {

            Connection con = null;
            PreparedStatement psDSGroups = null, psDSGroupUpdate = null,
                    psDSGroupPermissions = null, psDSGroupPermissionsUpdate = null,
                    psDSMenus = null, psDSGroupMenusUpdate = null,
                    psDSAdmins = null, psDSAdminsUpdate = null;
            ResultSet rsDSGroups = null, rsDSGroupPermissions = null, rsDSMenus = null, rsDSAdmins = null;
            try {
                //select all groups
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                con = ds.getConnection();

                String queryDSGroups = "select id,c_group_name from app_fd_ds_groups with (nolock) ";
                psDSGroups = con.prepareStatement(queryDSGroups);
                rsDSGroups = psDSGroups.executeQuery();

                try {

                    debugMessage("data groups :");

                    Map<String, String> resultDSGroupsMap = new HashMap<>();

                    while (rsDSGroups.next()) {

                        String id = rsDSGroups.getString("id");
                        debugMessage(" id : " + id);

                        String c_group_name = rsDSGroups.getString("c_group_name");
                        debugMessage(" c_group_name : " + c_group_name);

                        resultDSGroupsMap.put(id, c_group_name);

                    }

                    String queryDSGroupUpdate = "";

                    //check all default enum groupdata
                    for (GroupData group : GroupData.values()) {

                        if (!resultDSGroupsMap.containsKey(group.getId())) {
                            queryDSGroupUpdate = "INSERT INTO app_fd_ds_groups (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName, c_group_name) "
                                    + "VALUES (?, SYSDATETIME(),  SYSDATETIME(), 'admin', 'Admin admin', 'admin', 'Admin admin', ?)";
                            psDSGroupUpdate = con.prepareStatement(queryDSGroupUpdate);
                            psDSGroupUpdate.setString(1, group.getId());
                            psDSGroupUpdate.setString(2, group.getGroupName());
                            psDSGroupUpdate.executeUpdate();
                        } else {
                            if (!resultDSGroupsMap.get(group.getId()).equals(group.getGroupName())) {
                                queryDSGroupUpdate = "UPDATE app_fd_ds_groups SET c_group_name = ? WHERE id = ?";
                                psDSGroupUpdate = con.prepareStatement(queryDSGroupUpdate);
                                psDSGroupUpdate.setString(1, group.getGroupName());
                                psDSGroupUpdate.setString(2, group.getId());
                                psDSGroupUpdate.executeUpdate();
                            }
                        }
                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsDSGroups != null) {
                        rsDSGroups.close();
                    }
                }
                
                //select all menus
                String queryDSMenus = "select id,c_name,c_url from app_fd_ds_menus with (nolock) ";
                psDSMenus = con.prepareStatement(queryDSMenus);
                rsDSMenus = psDSMenus.executeQuery();
                try {

                    debugMessage("data menus :");

                    Map<String, Map<String, String>> menuMap = new HashMap<>();

                    while (rsDSMenus.next()) {
                        // Retrieve by column name
                        String id = rsDSMenus.getString("id");
                        String c_name = rsDSMenus.getString("c_name");
                        String c_url = rsDSMenus.getString("c_url");

                        // Store in a nested map
                        Map<String, String> menuDetails = new HashMap<>();
                        menuDetails.put("id", id);
                        menuDetails.put("c_name", c_name);
                        menuDetails.put("c_url", c_url);
                        
                        debugMessage("menuDetails : "+menuDetails);

                        // Add to the main map
                        menuMap.put(id, menuDetails);
                    }

                    String queryDSMenuUpdate = "";

                    for (DsMenus dsMenuData : DsMenus.values()) {
                        if (!menuMap.containsKey(dsMenuData.getId())) {
                            queryDSMenuUpdate = "INSERT INTO app_fd_ds_menus (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName, c_name, c_url) "
                                    + "VALUES (?, SYSDATETIME(), SYSDATETIME(), 'admin', 'Admin', 'admin', 'admin', ?, ?)";
                            psDSGroupMenusUpdate = con.prepareStatement(queryDSMenuUpdate);
                            psDSGroupMenusUpdate.setString(1, dsMenuData.getId());
                            psDSGroupMenusUpdate.setString(2, dsMenuData.getCName());
                            psDSGroupMenusUpdate.setString(3, dsMenuData.getCUrl());
                            psDSGroupMenusUpdate.executeUpdate();
                        } else if (!menuMap.get(dsMenuData.getId()).get("c_name").equals(dsMenuData.getCName())
                                || !menuMap.get(dsMenuData.getId()).get("c_url").equals(dsMenuData.getCUrl())) {
                            queryDSMenuUpdate = "UPDATE app_fd_ds_menus SET c_name = ?, c_url = ? WHERE id = ?";
                            psDSGroupMenusUpdate = con.prepareStatement(queryDSMenuUpdate);
                            psDSGroupMenusUpdate.setString(1, dsMenuData.getCName());
                            psDSGroupMenusUpdate.setString(2, dsMenuData.getCUrl());
                            psDSGroupMenusUpdate.setString(3, dsMenuData.getId());
                            psDSGroupMenusUpdate.executeUpdate();
                        }
                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsDSMenus != null) {
                        rsDSMenus.close();
                    }
                }
                
                //select all with role admins
                String queryDSAdmins = "select dur.userId 'c_user_id',dug.c_group_id,dur.roleId 'role_id', du.id 'ds_user_id' from dir_user_role dur with (nolock) "
                        + "left outer join app_fd_ds_user_groups dug with (nolock) on dur.userId=dug.c_user_id "
                        + "left outer join app_fd_ds_users du with (nolock) on du.id=dur.userId "
                        + "where dur.roleId='ROLE_ADMIN' order by dur.userId asc";
                psDSAdmins = con.prepareStatement(queryDSAdmins);
                rsDSAdmins = psDSAdmins.executeQuery();

                try {

                    debugMessage("data admins :");

                    Map<String, ArrayList<String>> resultDSAdminsMap = new HashMap<>();


                    while (rsDSAdmins.next()) {

                        String c_user_id = rsDSAdmins.getString("c_user_id");
                        debugMessage(" c_user_id : " + c_user_id);

                        String c_group_id = rsDSAdmins.getString("c_group_id") == null ? "" : rsDSAdmins.getString("c_group_id");
                        debugMessage(" c_group_id : " + c_group_id);
                        if (resultDSAdminsMap.containsKey(c_user_id)) {
                            resultDSAdminsMap.get(c_user_id).add(c_group_id);
                        } else {
                            String[] firstData = {c_group_id};
                            resultDSAdminsMap.put(c_user_id, new ArrayList<>(Arrays.asList(firstData)));

                        }
                    }

                    String queryDSAdminsUpdate = "";
                 

                    UuidGenerator uuidgen = UuidGenerator.getInstance();

                    // Mengiterasi kunci dan nilai dari resultDSAdminsMap
                    for (Map.Entry<String, ArrayList<String>> entry : resultDSAdminsMap.entrySet()) {
                        String user_id = entry.getKey();
                        ArrayList<String> groupIds = entry.getValue();

                        if (!groupIds.contains("G-000005")) {
                            debugMessage(user_id+" is admin but don\"t have admin priviledge");
                            String uuid = uuidgen.getUuid();
                            queryDSAdminsUpdate = "INSERT INTO app_fd_ds_user_groups (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName, c_user_id, c_group_id) "
                                    + "VALUES (?, SYSDATETIME(),  SYSDATETIME(), 'admin', 'Admin admin', 'admin', 'Admin admin', ?, ?)";
                            psDSAdminsUpdate = con.prepareStatement(queryDSAdminsUpdate);
                            psDSAdminsUpdate.setString(1, uuid);
                            psDSAdminsUpdate.setString(2, user_id);
                            psDSAdminsUpdate.setString(3, "G-000005");
                            psDSAdminsUpdate.executeUpdate();
                        }

                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsDSAdmins != null) {
                        rsDSAdmins.close();
                    }
                }

            } catch (Exception ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            } finally {
                try {

                    if (rsDSGroups != null) {
                        rsDSGroups.close();
                    }

                    if (psDSGroupUpdate != null) {
                        psDSGroupUpdate.close();
                    }

                    if (psDSGroups != null) {
                        psDSGroups.close();
                    }
                    ////
                    if (rsDSGroupPermissions != null) {
                        rsDSGroupPermissions.close();
                    }

                    if (psDSGroupPermissionsUpdate != null) {
                        psDSGroupPermissionsUpdate.close();
                    }

                    if (psDSGroupPermissions != null) {
                        psDSGroupPermissions.close();
                    }
                    ////
                    ////
                    if (rsDSMenus != null) {
                        rsDSMenus.close();
                    }

                    if (psDSGroupMenusUpdate != null) {
                        psDSGroupMenusUpdate.close();
                    }

                    if (psDSMenus != null) {
                        psDSMenus.close();
                    }
                    ////
                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException ex) {
                    LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
                }
            }
        }

        return "";
    }

}
