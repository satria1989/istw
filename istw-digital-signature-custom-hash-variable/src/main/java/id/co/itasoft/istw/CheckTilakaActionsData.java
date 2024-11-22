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
public class CheckTilakaActionsData extends DefaultHashVariablePlugin {

    public static final String PLUGIN_NAME = "istw-d-sign-chk-Tilaka.ActPath";

    public enum DsTilakaActionPaths {
        TILAKA_ENV_001("tilaka_action_auth", "/auth"),
        TILAKA_ENV_002("tilaka_action_generateuuid", "/generateUUID"),
        TILAKA_ENV_003("tilaka_action_register", "/registerForKycCheck"),
        TILAKA_ENV_004("tilaka_action_userregstatus", "/userregstatus"),
        TILAKA_ENV_005("tilaka_action_checkcertstatus", "/checkcertstatus"),
        TILAKA_ENV_006("tilaka_action_checkakundsexist", "/checkAkunDSExist"),
        TILAKA_ENV_007("tilaka_action_upload", "/api/v1/upload"),
        TILAKA_ENV_008("tilaka_action_requestsign", "/api/v1/requestsign"),
        TILAKA_ENV_009("tilaka_action_executesign", "/api/v1/executesign"),
        TILAKA_ENV_010("tilaka_action_checksignstatus", "/api/v1/checksignstatus"),
        TILAKA_ENV_011("tilaka_action_requestrevokecert", "/requestRevokeCertificate");

        private final String id;
        private final String actionPath;

        DsTilakaActionPaths(String id, String actionPath) {
            this.id = id;
            this.actionPath = actionPath;
        }

        public String getId() {
            return id;
        }

        public String getActionPath() {
            return actionPath;
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
        return "Hash Variable to check TilakaActPaths";
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
        syntax.add("checkTilakaActionsData.evaluateNow");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "checkTilakaActionsData";
    }

    private final boolean debug_mode = false;

    private void debugMessage(String msg) {

        if (debug_mode) {
            LogUtil.info("" + getClassName(), msg);
        }
    }

    @Override
    public String processHashVariable(String variableKey) {

        debugMessage("processHashVariable: checkTilakaActionsData,  variableKey: " + variableKey);

        if (variableKey.startsWith("evaluateNow")) {

            Connection con = null;
            PreparedStatement psDSTilakaActionPaths = null, psDSTilakaEnvsUpdate = null;
            ResultSet rsDSTilakaActionPaths = null;
            try {
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                con = ds.getConnection();
                //select all envs
                String queryDSActionPaths = "select id from app_fd_ds_tilaka_act_paths with (nolock) ";
                psDSTilakaActionPaths = con.prepareStatement(queryDSActionPaths);
                rsDSTilakaActionPaths = psDSTilakaActionPaths.executeQuery();
                try {

                    debugMessage("data path :");

                    ArrayList<String> foundIds = new ArrayList<>();

                    while (rsDSTilakaActionPaths.next()) {
                        // Retrieve by column name
                        String id = rsDSTilakaActionPaths.getString("id");

                        debugMessage("id Tilaka Env : " + id);
                        foundIds.add(id);
                    }

                    String queryDSTilakaEnvUpdate = "";

                    for (DsTilakaActionPaths dsPathData : DsTilakaActionPaths.values()) {
                        if (!foundIds.contains(dsPathData.getId())) {
                            queryDSTilakaEnvUpdate = "INSERT INTO app_fd_ds_tilaka_act_paths (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName, "
                                    + "c_method, c_action_path) "
                                    + "VALUES (?, SYSDATETIME(), SYSDATETIME(), 'admin', 'Admin', 'admin', 'admin', 'POST', ?)";
                            psDSTilakaEnvsUpdate = con.prepareStatement(queryDSTilakaEnvUpdate);
                            psDSTilakaEnvsUpdate.setString(1, dsPathData.getId());
                            psDSTilakaEnvsUpdate.setString(2, dsPathData.getActionPath());
                            psDSTilakaEnvsUpdate.executeUpdate();
                        } else {
                        }
                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsDSTilakaActionPaths != null) {
                        rsDSTilakaActionPaths.close();
                    }
                }

            } catch (Exception ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            } finally {
                try {

                    ////
                    if (rsDSTilakaActionPaths != null) {
                        rsDSTilakaActionPaths.close();
                    }

                    if (psDSTilakaEnvsUpdate != null) {
                        psDSTilakaEnvsUpdate.close();
                    }

                    if (psDSTilakaActionPaths != null) {
                        psDSTilakaActionPaths.close();
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
