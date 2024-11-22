package id.co.itasoft.esign;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.workflow.model.WorkflowProcessResult;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

public class AbortDocument extends DataListActionDefault {

    public String getName() {
        return "ISTW - Abort Document";
    }

    public String getVersion() {
        return "7.0.0";
    }

    public String getDescription() {
        return "Abort Document ISTW Digital Signature";
    }

    public String getLinkLabel() {
        String label = getPropertyString("label");
        if (label == null || label.isEmpty()) {
            label = "Abort";
        }
        return label;
    }

    public String getHref() {
        return getPropertyString("href");
    }

    public String getTarget() {
        return getPropertyString("target");
    }

    public String getHrefParam() {
        return getPropertyString("hrefParam");
    }

    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }

    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = "Are you sure want to abort the selected document?";
        }
        return confirm;
    }

    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {

        DataListActionResult result = new DataListActionResult();

        if (rowKeys != null && rowKeys.length > 0) {
            for (String key : rowKeys) {

                String reqType = getRequestType(key);
                if (reqType != null && reqType.equals("external")) {
                    setExternalDocumentAbortStatus(key);
                } else if (reqType != null && reqType.equals("internal")) {
                    ProcessDetail pDtlInfo = getProcessDetailFromRecordId(key, key);
                    if (pDtlInfo != null) {
                        setDocumentAbortStatus(pDtlInfo, key);
                    }
                }

            }
            Map<String, String> variables = new HashMap<String, String>();
        }

        return result;
    }

    class ProcessDetail {

        public String processId = "";
        public String activityId = "";
        public String createdBy = "";
        public String c_adjust_by = "";
        public String c_approver_lv_1 = "";
        public String activityName = "";

        public ProcessDetail(String processId, String activityId, String createdBy, String c_adjust_by, String c_approver_lv_1, String activityName) {
            this.processId = processId;
            this.activityId = activityId;
            this.createdBy = createdBy;
            this.c_adjust_by = c_adjust_by;
            this.c_approver_lv_1 = c_approver_lv_1;
            this.activityName = activityName;
        }

    }

    private void debugMessage(String message) {
        boolean debugMode = false;
        if (debugMode) {
            LogUtil.info("" + AbortDocument.class.getName(), "DEBUG MODE: " + message);
        }
    }

    private String getRequestType(String key) {
        String result = "";
        debugMessage("====== Getting getRequestType =======");

        ApplicationContext appContext = AppUtil.getApplicationContext();
        DataSource ds = (DataSource) appContext.getBean("setupDataSource");
        String query = "select c_type_request from app_fd_ds_document_order with (nolock) where id = ?";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = ds.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, key);
            rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getString("c_type_request");
            } else {
                LogUtil.info(getClassName(), "request type not found for id : " + key);
            }
        } catch (SQLException ex) {
            LogUtil.error(this.getClass().getName(), ex, "Error: " + ex.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                LogUtil.error(this.getClass().getName(), e, "Error closing resources: " + e.getMessage());
            }
        }
        return result;

    }

    private ProcessDetail getProcessDetailFromRecordId(String originProcessId, String key) {
        debugMessage("====== Getting Process Id by Origin Process Id =======");

        ApplicationContext appContext = AppUtil.getApplicationContext();
        DataSource ds = (DataSource) appContext.getBean("setupDataSource");
        String query = "SELECT top 1 wpl.processId, sact.Id AS activityId, createdBy, "
                + "CASE "
                + "        WHEN CHARINDEX(';', dsdo.c_adjust_by) > 0 THEN LEFT(dsdo.c_adjust_by, CHARINDEX(';', dsdo.c_adjust_by) - 1) "
                + "        ELSE dsdo.c_adjust_by "
                + "    END AS c_adjust_by,  "
                + "CASE "
                + "        WHEN CHARINDEX(';', dsdo.c_approver_lv_1) > 0 THEN LEFT(dsdo.c_approver_lv_1, CHARINDEX(';', dsdo.c_approver_lv_1) - 1) "
                + "        ELSE dsdo.c_approver_lv_1 "
                + "    END AS c_approver_lv_1, sact.Name AS activityName FROM wf_process_link wpl with (nolock) "
                + "INNER JOIN SHKActivities sact with (nolock) ON wpl.processId = sact.ProcessId  "
                + "INNER JOIN app_fd_ds_document_order dsdo with (nolock) on dsdo.id=wpl.originProcessId "
                + "WHERE originProcessId = ? "
                + "and (sact.Name = 'Set Doc Assignatories' OR sact.Name = 'Adjust' OR sact.Name = 'Approval Layer') "
                + "order by activityId desc";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ProcessDetail pInfoDTl = null;
        try {
            con = ds.getConnection();
            ps = con.prepareStatement(query);
            ps.setString(1, originProcessId);
            rs = ps.executeQuery();
            if (rs.next()) {
                pInfoDTl = new ProcessDetail(rs.getString("processId"), rs.getString("activityId"), rs.getString("createdBy"),
                        rs.getString("c_adjust_by"), rs.getString("c_approver_lv_1"), rs.getString("activityName"));
                debugMessage("Process ID: " + pInfoDTl.processId);
                debugMessage("detected activity ID: " + pInfoDTl.activityId);
            } else {
                debugMessage("No Process ID found for Origin Process ID: " + originProcessId);
            }
        } catch (SQLException ex) {
            LogUtil.error(this.getClass().getName(), ex, "Error: " + ex.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                LogUtil.error(this.getClass().getName(), e, "Error closing resources: " + e.getMessage());
            }
        }
        return pInfoDTl;
    }

    public void setExternalDocumentAbortStatus(String key) {

        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "update app_fd_ds_document_order SET c_status = 'aborted', c_statusWv = 'aborted' WHERE id = ?";
        Connection con = null;
        try {
            con = ds.getConnection();
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, key);

            ps.executeUpdate();

            //////// send mail
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
                psDSSendEmailToTilakaData.setString(1, key);
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

                    String processDefId = "istwDigitalSign:latest:sendEmailOnAbortProcess";

                    Map variables = new HashMap();
                    variables.put("tilaka_request_id", c_tilaka_request_id);
                    variables.put("doc_ext_id", doc_ext_id);
                    variables.put("doc_name", doc_name);
                    variables.put("doc_description", doc_description);
                    variables.put("doc_filename", doc_filename);
                    variables.put("doc_approveby", approver_ids);

                    String sendEmailOnAbortUuid = UuidGenerator.getInstance().getUuid();

                    WorkflowProcessResult result = workflowManager.processStart(processDefId, null, variables, doc_requester, sendEmailOnAbortUuid, false);
                    debugMessage("Processing istwDigitalSign:latest:sendEmailOnAbortProcess - Record: " + sendEmailOnAbortUuid + " - Status: " + result.getProcess().getInstanceId());

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

        } catch (SQLException ex) {
            LogUtil.error(this.getClass().getName(), ex, "Error: " + ex.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    LogUtil.error(this.getClass().getName(), e, "Error closing connection: " + e.getMessage());
                }
            }
        }

    }

    public void setDocumentAbortStatus(ProcessDetail pDtlInfo, String key) {
        debugMessage("====== Update Status And Aborting ... =======");
        debugMessage("Process ID: " + pDtlInfo.processId);
        debugMessage("Activity ID: " + pDtlInfo.activityId);
        debugMessage("Record ID: " + key);
        ApplicationContext ac = AppUtil.getApplicationContext();
        WorkflowManager workflowManager = (WorkflowManager) ac.getBean("workflowManager");

        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "UPDATE app_fd_ds_document_order SET c_status = 'aborted', c_statusWv = 'aborted' WHERE id = ?";
        Connection con = null;
        try {
            con = ds.getConnection();
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, key);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {

                try {
                    String userToAuth = pDtlInfo.activityName.equals("Set Doc Assignatories") ? pDtlInfo.createdBy : (pDtlInfo.activityName.equals("Adjust") ? pDtlInfo.c_adjust_by : pDtlInfo.c_approver_lv_1);
                    workflowManager.assignmentForceComplete("ds_process", pDtlInfo.processId, pDtlInfo.activityId, userToAuth);
                    debugMessage("assignmentForceComplete force complete : " + pDtlInfo.activityId);
                } catch (Exception ex) {
                    LogUtil.error(this.getClass().getName(), ex, "Error: " + ex.getMessage());
                }

            }
        } catch (SQLException ex) {
            LogUtil.error(this.getClass().getName(), ex, "Error: " + ex.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    LogUtil.error(this.getClass().getName(), e, "Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    public String getLabel() {
        return "Abort Document";
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        String json = AppUtil.readPluginResource(getClassName(), "/properties/AbortDocument.json", null, true, "messages/AbortDocument");
        return json;
    }
}
