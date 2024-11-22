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
import javax.sql.DataSource;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.util.WorkflowUtil;

/**
 *
 * @author User
 */
public class FormatEmailContent extends DefaultHashVariablePlugin {

    public static final String PLUGIN_NAME = "istw-d-sign-format-emailcontent";

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
        return "Hash Variable to get formated emailcontent";
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
        syntax.add("formatingEmailContent.SignType");
        syntax.add("formatingEmailContent.LastSignedBy");
        return syntax;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getPrefix() {
        return "formatingEmailContent";
    }

    @Override
    public String processHashVariable(String variableKey) {

        //LogUtil.info(getClassName(), "processHashVariable: formatingEmailContent,  variableKey: " + variableKey);

        String resultStr = "";


        if (variableKey.startsWith("SignType")) {
            String signTypeValue = "";
            if (variableKey.contains("[") && variableKey.contains("]")) {
                signTypeValue = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
            }

            switch (signTypeValue) {
                case "":
                    resultStr = "";
                    break;
                case "DrawSignature":
                    resultStr = "Draw Signature";
                    break;
                case "ImageSignature":
                    resultStr = "Image Signature";
                    break;
                default:
                    break;
            }
        } else if (variableKey.startsWith("LastSignedBy")) {

            String recordId = "";
            if (variableKey.contains("[") && variableKey.contains("]")) {
                recordId = variableKey.substring(variableKey.indexOf("[") + 1, variableKey.indexOf("]"));
            }
            
            //LogUtil.info(getClassName(), "processHashVariable: formatingEmailContent ==> recordId : " +recordId);
            
            Connection con = null;
            PreparedStatement psSignedBy = null;
            ResultSet rsSignedBy = null;
            try {
                //select all signed by
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                con = ds.getConnection();

                String querySignedBy = "select id,c_adjust_by,c_approver_lv_1,c_signed_by from app_fd_ds_document_order with (nolock) where id=?";
                psSignedBy = con.prepareStatement(querySignedBy);
                psSignedBy.setString(1, recordId);
                rsSignedBy = psSignedBy.executeQuery();

                try {

                    //LogUtil.info(getClassName(), "document info :");

                    if (rsSignedBy.next()) {

                        String c_adjust_by = rsSignedBy.getString("c_adjust_by");
                        //LogUtil.info(getClassName(), "doc c_adjust_by : " + c_adjust_by);
                        
                        String c_approver_lv_1 = rsSignedBy.getString("c_approver_lv_1");
                        //LogUtil.info(getClassName(), "doc c_approver_lv_1 : " + c_approver_lv_1);

                        String c_signed_by = rsSignedBy.getString("c_signed_by");
                        //LogUtil.info(getClassName(), "doc c_signed_by : " + c_signed_by);
                        
                        if(!c_signed_by.equals("")){
                            ArrayList<String> adjuster = new ArrayList<>(Arrays.asList(c_adjust_by.split(";")));
                            ArrayList<String> approver = new ArrayList<>(Arrays.asList(c_approver_lv_1.split(";")));                            
                            ArrayList<String> signedby = new ArrayList<>(Arrays.asList(c_signed_by.split(";")));
                            for (String signbypointer : signedby) {
                                if (!signbypointer.equals("")) {
                                    if (adjuster.size() > 0) {
                                        resultStr += signbypointer + " (Adjust By)<br/>";
                                        adjuster.remove(signbypointer);
                                    } else if (approver.size() > 0) {
                                        resultStr += signbypointer + " (Approve By)<br/>";
                                        approver.remove(signbypointer);
                                    }
                                }
                            }                        
                        }else{
                             resultStr = "-";
                        }
                        
                    }

                } catch (Exception e) {
                    LogUtil.error(getClass().getName(), e, "Error : " + e.getMessage());
                } finally {
                    if (rsSignedBy != null) {
                        rsSignedBy.close();
                    }
                }
            } catch (Exception ex) {
                LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
            } finally {
                try {
                    if (psSignedBy != null) {
                        psSignedBy.close();
                    }

                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException ex) {
                    LogUtil.error(getClass().getName(), ex, "Error : " + ex.getMessage());
                }
            }            
        }

        return resultStr;
    }

}
