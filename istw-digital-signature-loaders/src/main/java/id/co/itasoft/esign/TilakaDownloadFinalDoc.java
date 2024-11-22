/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.itasoft.esign;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.plugin.base.PluginWebSupport;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author User
 */
public class TilakaDownloadFinalDoc extends Element implements PluginWebSupport {

    public static String pluginName = "ISTW - DS - Tilaka Download";

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
        String encodedUrl = request.getParameter("url"); // Example of an encoded string
        String decodedurl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString());

        String final_doc = request.getParameter("final_doc");
        if (final_doc != null && !final_doc.isEmpty()) {
            
            String docId = request.getParameter("id");

            String scheme = request.getScheme(); // "http" or "https"

            // Get the server name (hostname)
            String serverName = request.getServerName(); // e.g., "www.example.com"

            // Get the server port
            int serverPort = request.getServerPort(); // e.g., 80 for http, 443 for https

            // Build the full URL (optional)
            decodedurl = scheme + "://" + serverName + ":" + serverPort + "/jw/web/client/app/istwDigitalSign/form/download/FormTilakaApiCalls/"+docId+"/"+final_doc+".?attachment=false";

        }

        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", decodedurl);

    }
}
