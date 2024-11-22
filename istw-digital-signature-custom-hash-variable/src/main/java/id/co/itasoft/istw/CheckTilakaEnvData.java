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
public class CheckTilakaEnvData extends DefaultHashVariablePlugin {

    public static final String PLUGIN_NAME = "istw-d-sign-chk-Tilaka.Env";

    public enum DsTilakaEnvs {
        TILAKA_ENV_001("tilaka_api_base_url"),
        TILAKA_ENV_002("tilaka_adapter_api_hostname_url"),
        TILAKA_ENV_003("tilaka_channel_id"),
        TILAKA_ENV_004("tilaka_client_secret");

        private final String id;

        DsTilakaEnvs(String id) {
            this.id = id;

        }

        public String getId() {
            return id;
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
        return "Hash Variable to check TilakaEnvs";
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
        syntax.add("checkTilakaEnvData.evaluateNow");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "checkTilakaEnvData";
    }

    private final boolean debug_mode = false;

    private void debugMessage(String msg) {

        if (debug_mode) {
            LogUtil.info("" + getClassName(), msg);
        }
    }

    @Override
    public String processHashVariable(String variableKey) {

        debugMessage("processHashVariable: checkTilakaEnvData,  variableKey: " + variableKey);

        if (variableKey.startsWith("evaluateNow")) {

            Connection con = null;
            PreparedStatement psDSTilakaEnvs = null, psDSTilakaEnvsUpdate = null;
            ResultSet rsDSTilakaEnvs = null;
            try {
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                con = ds.getConnection();
                //select all envs
                String queryDSEnvs = "select id from app_fd_ds_tilaka_env_vars with (nolock) ";
                psDSTilakaEnvs = con.prepareStatement(queryDSEnvs);
                rsDSTilakaEnvs = psDSTilakaEnvs.executeQuery();
                try {

                    debugMessage("data env vars :");

                    ArrayList<String> foundIds = new ArrayList<>();

                    while (rsDSTilakaEnvs.next()) {
                        // Retrieve by column name
                        String id = rsDSTilakaEnvs.getString("id");

                        debugMessage("id Tilaka Env : " + id);
                        foundIds.add(id);
                    }

                    String queryDSTilakaEnvUpdate = "";

                    for (DsTilakaEnvs dsMenuData : DsTilakaEnvs.values()) {
                        if (!foundIds.contains(dsMenuData.getId())) {
                            queryDSTilakaEnvUpdate = "INSERT INTO app_fd_ds_tilaka_env_vars (id, dateCreated, dateModified, createdBy, createdByName, modifiedBy, modifiedByName) "
                                    + "VALUES (?, SYSDATETIME(), SYSDATETIME(), 'admin', 'Admin', 'admin', 'admin')";
                            psDSTilakaEnvsUpdate = con.prepareStatement(queryDSTilakaEnvUpdate);
                            psDSTilakaEnvsUpdate.setString(1, dsMenuData.getId());
                            psDSTilakaEnvsUpdate.executeUpdate();
                        } else {
                        }
                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsDSTilakaEnvs != null) {
                        rsDSTilakaEnvs.close();
                    }
                }

            } catch (Exception ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            } finally {
                try {

                    ////
                    if (rsDSTilakaEnvs != null) {
                        rsDSTilakaEnvs.close();
                    }

                    if (psDSTilakaEnvsUpdate != null) {
                        psDSTilakaEnvsUpdate.close();
                    }

                    if (psDSTilakaEnvs != null) {
                        psDSTilakaEnvs.close();
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
