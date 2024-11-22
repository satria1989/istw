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
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.model.WorkflowProcessResult;
import org.joget.workflow.model.service.WorkflowManager;

/**
 *
 * @author User
 */
public class TilakaDocReject extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - TilakaDocReject";

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

    private void debugMessage(String msg) {
        boolean debug = true;
        if (debug) {
            LogUtil.info("" + getClassName(), msg);
        }
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Connection con = null;
        PreparedStatement psDSDSTilakaIDSigns = null;

        try {
            WorkflowUserManager workflowUserManager = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
            if (!workflowUserManager.isCurrentUserAnonymous()) {
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                con = ds.getConnection();
                String id = request.getParameter("id");

                debugMessage("tilaka_request_id : " + id);

                //check is need hit check sign document status
                String queryDSTilakaSigns = "update app_fd_ds_document_order set c_status='rejected', c_statusWv='rejected' where id=? and c_type_request='external'";
                psDSDSTilakaIDSigns = con.prepareStatement(queryDSTilakaSigns);
                psDSDSTilakaIDSigns.setString(1, id);
                psDSDSTilakaIDSigns.executeUpdate();

                queryDSTilakaSigns = "update app_fd_ds_tilaka_api_signs set c_status='REJECTED' where c_tilaka_request_id=? and c_tilaka_id=(select top 1 c_user_tilaka from app_fd_ds_tilaka_users where id=?)";
                psDSDSTilakaIDSigns = con.prepareStatement(queryDSTilakaSigns);
                psDSDSTilakaIDSigns.setString(1, id);
                psDSDSTilakaIDSigns.setString(2, workflowUserManager.getCurrentUsername());
                psDSDSTilakaIDSigns.executeUpdate();

                queryDSTilakaSigns = "update app_fd_ds_tilaka_api_signs set c_status='ABORTED' where c_tilaka_request_id=? and c_status='AWAITING'";
                psDSDSTilakaIDSigns = con.prepareStatement(queryDSTilakaSigns);
                psDSDSTilakaIDSigns.setString(1, id);
                psDSDSTilakaIDSigns.executeUpdate();

                //// send mail
                PreparedStatement psDSSendEmailToTilakaData = null;
                ResultSet rsDSSendEmailToTilakaData = null;
                try {
                    String queryDSSendEmailToTilakaData = "select tas.c_tilaka_request_id, do.c_doc_id as doc_ext_id, do.c_name as doc_name, do.c_description as doc_description, "
                            + "do.c_filedoc as doc_filename, string_agg(tu.id, ';') within group (order by tas.c_sequence asc) as approver_ids, do.createdBy as doc_requester "
                            + "from app_fd_ds_tilaka_api_signs tas with (nolock) "
                            + "inner join app_fd_ds_tilaka_users tu with (nolock) on tu.c_user_tilaka = tas.c_tilaka_id "
                            + "inner join app_fd_ds_document_order do with (nolock) on do.id=tas.c_tilaka_request_id "
                            + "where do.id = ? "
                            + "group by tas.c_tilaka_request_id,do.c_doc_id, do.c_name,do.c_description,do.c_filedoc, do.createdBy ";

                    psDSSendEmailToTilakaData = con.prepareStatement(queryDSSendEmailToTilakaData);
                    psDSSendEmailToTilakaData.setString(1, id);
                    rsDSSendEmailToTilakaData = psDSSendEmailToTilakaData.executeQuery();

                    if (rsDSSendEmailToTilakaData.next()) {
                        
                        String c_tilaka_request_id = rsDSSendEmailToTilakaData.getString("c_tilaka_request_id");
                        String doc_ext_id = rsDSSendEmailToTilakaData.getString("doc_ext_id");
                        String doc_name = rsDSSendEmailToTilakaData.getString("doc_name");
                        String doc_description = rsDSSendEmailToTilakaData.getString("doc_description");
                        String doc_filename = rsDSSendEmailToTilakaData.getString("doc_filename");
                        String approver_ids = rsDSSendEmailToTilakaData.getString("approver_ids");
                        String doc_requester = rsDSSendEmailToTilakaData.getString("doc_requester");

                        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");

                        String processDefId = "istwDigitalSign:latest:sendEmailOnRejectProcess";

                        Map variables = new HashMap();
                        variables.put("tilaka_request_id", c_tilaka_request_id);
                        variables.put("doc_ext_id", doc_ext_id);
                        variables.put("doc_name", doc_name);
                        variables.put("doc_description", doc_description);
                        variables.put("doc_filename", doc_filename);
                        variables.put("doc_approveby", approver_ids);

                        String sendEmailOnRejectUuid = UuidGenerator.getInstance().getUuid();

                        WorkflowProcessResult result = workflowManager.processStart(processDefId, null, variables, doc_requester, sendEmailOnRejectUuid, false);
                        debugMessage("Processing istwDigitalSign:latest:sendEmailOnRejectProcess - Record: " + sendEmailOnRejectUuid + " - Status: " + result.getProcess().getInstanceId());

                    }
                } catch (SQLException e) {
                    LogUtil.error(getClassName(), e, e.getMessage());
                } finally {
                    if (rsDSSendEmailToTilakaData != null) {
                        try {
                            rsDSSendEmailToTilakaData.close();
                        } catch (SQLException ex) {
                            LogUtil.error(getClassName(), ex, ex.getMessage());
                        }
                    }
                    if (psDSSendEmailToTilakaData != null) {
                        try {
                            psDSSendEmailToTilakaData.close();
                        } catch (SQLException ex) {
                            LogUtil.error(getClassName(), ex, ex.getMessage());
                        }
                    }
                }

            }

        } catch (SQLException ex) {
            LogUtil.error(getClassName(), ex, ex.getMessage());
        } finally {
            if (psDSDSTilakaIDSigns != null) {
                try {
                    psDSDSTilakaIDSigns.close();
                } catch (SQLException ex) {
                    LogUtil.error(getClassName(), ex, ex.getMessage());
                }
            }

            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LogUtil.error(getClassName(), ex, ex.getMessage());
                }
            }
            String urlInboxApprovalExt = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                    + request.getContextPath() + "/web/userview/istwDigitalSign/v/_/inbox_approval_ext";
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", urlInboxApprovalExt);
            // Optionally, you could write a brief HTML response to inform the client about the redirect
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h1>Redirecting...</h1>");
            response.getWriter().println("<p>If you are not redirected automatically, follow the <a href='" + urlInboxApprovalExt + "'>link</a>.</p>");
            response.getWriter().println("</body></html>");
            response.getWriter().flush();
        }

    }
}
