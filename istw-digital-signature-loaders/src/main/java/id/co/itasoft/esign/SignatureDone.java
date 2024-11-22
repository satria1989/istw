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

/**
 *
 * @author User
 */
public class SignatureDone extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - Tilaka DoneSign";

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
        //PreparedStatement psDSDSTilakaSignDone = null;
        PreparedStatement psDSDSTilakaIDSigns = null;
        ResultSet rsDSTilakaIDSigns = null;

        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            con = ds.getConnection();
            String tilaka_request_id = request.getParameter("tilaka_request_id");

            debugMessage("tilaka_request_id : " + tilaka_request_id);

            TilakaExecuteSign.startExecuteSignDoc(tilaka_request_id, request.getParameter("tilaka_id"));

            //check is need hit check sign document status
            String queryDSTilakaSigns = "select case when exists "
                    + "(select 1 from app_fd_ds_tilaka_api_signs tas with (nolock) "
                    + "inner join app_fd_ds_tilaka_sign_seq tseq with (nolock) on tseq.id = tas.c_tilaka_request_id and tas.c_sequence = tseq.c_next_sequence "
                    + "where tseq.id=? and tas.c_status!='FAILED') "
                    + "then 'true' else 'false' "
                    + "end as result ";
            psDSDSTilakaIDSigns = con.prepareStatement(queryDSTilakaSigns);
            psDSDSTilakaIDSigns.setString(1, tilaka_request_id);
            rsDSTilakaIDSigns = psDSDSTilakaIDSigns.executeQuery();

            if (rsDSTilakaIDSigns.next()) {
                //do tilaka check sign status
                debugMessage("query result : " + rsDSTilakaIDSigns.getString("result"));
                if (rsDSTilakaIDSigns.getString("result") != null && rsDSTilakaIDSigns.getString("result").equals("false")) {
                    TilakaCheckSignStatus.startCheckSignStatus(tilaka_request_id);
                }
            }

        } catch (SQLException ex) {
            LogUtil.error(getClassName(), ex, ex.getMessage());
        } finally {

            if (rsDSTilakaIDSigns != null) {
                try {
                    rsDSTilakaIDSigns.close();
                } catch (SQLException ex) {
                    LogUtil.error(getClassName(), ex, ex.getMessage());
                }
            }

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
