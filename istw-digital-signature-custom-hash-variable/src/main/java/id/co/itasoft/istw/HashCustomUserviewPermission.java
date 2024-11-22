/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.itasoft.istw;

import java.util.ArrayList;
import java.util.Collection;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.workflow.util.WorkflowUtil;

/**
 *
 * @author User
 */
public class HashCustomUserviewPermission extends DefaultHashVariablePlugin {
    
    public static final String PLUGIN_NAME = "istw-d-signature-hide-menu-by-access";

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
        return "Hash Variable to hide menu by permission script";
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
        syntax.add("scriptmenus.hidemenus");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "scriptmenus";
    }

    @Override
    public String processHashVariable(String variableKey) {
        String resultStr = "";
        if (WorkflowUtil.isCurrentUserAnonymous()) {
            return "";
        }


        //begin doc ready
        resultStr += "$(document).ready(function(){\n";
        if (WorkflowUtil.isCurrentUserInRole("ROLE_USER")) {
            resultStr += "    document.querySelectorAll('li.category').forEach(function(li) {\n"
                    + "        const aElement = li.querySelector('a');\n"
                    + "        if (aElement && aElement.textContent.trim() === \"User Maintenance\") {\n"
                    + "            li.remove();\n"
                    + "        }\n"
                    + "    });\n";
        }
        //end doc ready
        resultStr += "});\n";

        return resultStr;
    }

}
