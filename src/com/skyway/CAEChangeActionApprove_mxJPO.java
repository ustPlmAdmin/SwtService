package com.skyway;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeAction;
import com.iga.cae.CAUtil;
import com.igatec.kit.common.IGAUtil;
import com.matrixone.apps.common.InboxTask;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.*;

import com.mql.MqlService;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.StringList;

public class CAEChangeActionApprove_mxJPO extends SkyService {
    public static final String CA_TO_ROUTE = "Object Route";

    public static final String ROUTE_TYPE = "Route";

    public static final String APPROVER_TYPE_ATTRIBUTE = "IGADrwApprover";

    public static final String PERSON_SIGNATURE_ATTRIBUTE_SELECT = "attribute[IGAPersonSignature]";

    public static final String STATE_SELECT = "current";

    public static final String NAME_SELECT = "name";

    public static final String LAST_NAME_SELECT = "attribute[Last Name]";

    public static final String STATE_START_SELECT = "current.start";

    public static final String COMPLETE_STATE = "Complete";

    public static final SimpleDateFormat ENO_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");

    public Map test(Context context, String[] args) throws Exception {
        Map<Object, Object> result = new HashMap<>();
        Map argsMap = (Map) JPO.unpackArgs(args);
        String caId = (String) argsMap.get("id");
        result.put("Message", "");
        return result;
    }

    public int changeActionToApproveInvoke(Context context, String[] args) throws Exception {
        String caId = args[0];
        Logger logger = signAllDocumentsInCA(context, caId);
        int status = 0;
        if (logger.hasErrors()) {
            status = 1;
            MQLCommand.exec(context, "error $1", new String[]{logger.getErrors("Error: ", "\\n")});
        }
        return status;
    }

    public Logger signAllDocumentsInCA(Context context, String caId) throws Exception {
        Logger logger = new Logger();
        ChangeAction ca = new ChangeAction(caId);
        String sCAOwner = ca.getInfo(context, "owner");
        StringList routeSelects = new StringList();
        routeSelects.add("id");
        routeSelects.add("attribute[Route Status].value");
        routeSelects.add("to[Route Task].from.attribute[Approval Status].value");
        routeSelects.add("owner");
        routeSelects.add("attribute[Route Completion Action]");
        routeSelects.add("attribute[CAERevision]");
        routeSelects.add("modified");

        StringList relSelects = new StringList();
        String routeWhere = "";
        String relWhere = "";
        MapList routes = ca.getRelatedObjects(context, "Object Route", "Route", routeSelects, relSelects, false, true, (short) 1, routeWhere, relWhere, 0);
        Route route = null;
        for (Object o : routes) {
            Map m = (Map) o;
            String sOwner = (String) m.get("owner");
            String sRouteCompletionAction = (String) m.get("attribute[Route Completion Action]");
            String sTaskStatuses = String.join(",", (Iterable<? extends CharSequence>) IGAUtil.objToStringList(m, "to[Route Task].from.attribute[Approval Status].value"));
            if ("Promote Connected Object".equals(sRouteCompletionAction) && sCAOwner
                    .equals(sOwner) &&
                    !sTaskStatuses.toLowerCase().contains("reject")) {
                route = new Route((String) m.get("id"));
                break;
            }
        }
        if (route == null) {
            logger.addError("There are no any Route or more than one active Route in the Change Action!");
            return logger;

        }
        StringList taskSelects = new StringList() {
            {
                this.add("id");
                this.add("current");
                this.add("current.start");
                this.add("name");
            }
        };
        StringList taskRelSelects = new StringList();

        MapList tasksML = route.getRouteTasks(context, taskSelects, taskRelSelects, null, false);
        ArrayList<CAEPDFSignature_mxJPO.Signature> signatures = new ArrayList<>();
        int i = 0;
        for (Object taskObj : tasksML) {
            Date completeDate;
            String taskId = (String) ((Map) taskObj).get("id");
            String taskState = (String) ((Map) taskObj).get("current");
            String taskName = (String) ((Map) taskObj).get("name");
            if (!"Complete".equals(taskState))
                logger.addError("Task " + taskName + " is not " + "Complete");
            InboxTask task = new InboxTask(taskId);
            try {
                String completeDateStr = (String) ((Map) taskObj).get("current.start");
                completeDate = ENO_DATE_FORMAT.parse(completeDateStr);
            } catch (Exception ex) {
                completeDate = dateFormat.parse(ca.getInfo(context, "modified"));
                logger.addError("Task " + taskName + " has no valid complete date");
            }
            String approverTypeStr = task.getAttributeValue(context, "IGADrwApprover");
            CAEPDFSignature_mxJPO.DrwRole approverType = getApproverType(approverTypeStr);
            if (approverTypeStr == null)
                continue;
            String approverId = task.getTaskAssigneeId(context);
            DomainObject approver = new DomainObject(approverId);
            StringList approverSelects = new StringList() {
                {
                    this.add("attribute[IGAPersonSignature]");
                    this.add("attribute[Last Name]");
                }
            };
            Map approverInfo = approver.getInfo(context, approverSelects);
            String approverSignatureStr = (String) approverInfo.get("attribute[IGAPersonSignature]");
            BufferedImage signature = null;
            if (approverSignatureStr != null)
                try {
                    signature = CAEPDFSignature_mxJPO.decodeBase64ToImage(approverSignatureStr);
                    signature = CAEPDFSignature_mxJPO.trimImage(signature, true, true);
                } catch (Exception exception) {
                }
            if (signature == null)
                logger.addError("Approver " + approverTypeStr + " (id " + approverId + ") doesn't have valid signature uploaded");
            String lastName = (String) approverInfo.get("attribute[Last Name]");
            if (lastName == null || "".equals(lastName)) {
                logger.addError("Approver " + approverTypeStr + " (id " + approverId + ") doesn't have valid attribute Last Name");
                lastName = "";
            }

    /*        if (i == 0)
                approverTypeStr = "drw_approver";
            if (i == 1)
                approverTypeStr = "drw_lead";
            if (i == 2)
                approverTypeStr = "drw_checker";
            if (i == 3)
                approverTypeStr = "drw_tcontr";
            if (i == 4)
                approverTypeStr = "drw_ncontr";
                i++;

            approverType = getApproverType(approverTypeStr);*/
            signatures.add(new CAEPDFSignature_mxJPO.Signature(approverType, lastName, signature, (new SimpleDateFormat("dd.MM.yy"))

                    .format(completeDate)));
        }

        String caOwnerName = ca.getOwner(context).getName();
        String caOwnerSignatureStr = PersonUtil.getPersonObject(context, caOwnerName).getInfo(context, "attribute[IGAPersonSignature]");
        BufferedImage caOwnerSignature = null;
        if (caOwnerSignatureStr != null)
            try {
                caOwnerSignature = CAEPDFSignature_mxJPO.decodeBase64ToImage(caOwnerSignatureStr);
                caOwnerSignature = CAEPDFSignature_mxJPO.trimImage(caOwnerSignature, true, true);
            } catch (Exception exception) {
            }
        if (caOwnerSignature == null)
            logger.addError("CA owner (name " + caOwnerName + ") doesn't have valid signature uploaded");
        String sCALetter = ca.getInfo(context, "attribute[CAERevision]");
        CAInfo caInfo = new CAInfo(ca.getName(), (new SimpleDateFormat("dd.MM.yy")).format(
                dateFormat.parse(ca.getInfo(context, "modified"))
        ), caOwnerSignature, sCALetter);
        CAEPDFSignature_mxJPO signerService = new CAEPDFSignature_mxJPO(caInfo, signatures, logger);
        MapList docs = CAUtil.getProposedAndRealizedChanges(context, ca);
        Set<Map> docsSet = new HashSet();
        for (Object obj : docs) {
            Map map = (Map) obj;
            if (map.get("type").equals("ARCHDocument"))
                docsSet.add(map);
        }

        for (Object docObj : docsSet) {
            logger.totalDocs++;
            String docId = (String) ((Map) docObj).get("id");
            query(context, "mod bus " + docId + " current IN_WORK");
            signerService.signDocument(context, docId);
            query(context, "mod bus " + docId + " current RELEASED");
        }
        if (!logger.hasErrors()) {
            String signerRoles = "document owner";
            for (CAEPDFSignature_mxJPO.Signature sig : signatures) {
                signerRoles = signerRoles + String.format(", %s (%s)", new Object[]{sig.lastName, sig.role});
            }
            logger.addWarning("The documents were approved successfully. Total documents number: " + logger.totalDocs + ". " + logger.signedDocs + " documents were stamped");
            logger.addWarning("The documents were signed by following persons: " + signerRoles);
        }
        if (logger.hasErrors()) {
            MQLCommand.exec(context, "error $1", new String[]{logger.getErrors("Error: ", "\\n")});
        } else {
        }
        return logger;
    }

    private CAEPDFSignature_mxJPO.DrwRole getApproverType(String getApproverTypeStr) {
        if ("drw_approver".equals(getApproverTypeStr))
            return CAEPDFSignature_mxJPO.DrwRole.APPROVER;
        if ("drw_checker".equals(getApproverTypeStr))
            return CAEPDFSignature_mxJPO.DrwRole.CHECKER;
        if ("drw_ncontr".equals(getApproverTypeStr))
            return CAEPDFSignature_mxJPO.DrwRole.NCONTR;
        if ("drw_tcontr".equals(getApproverTypeStr))
            return CAEPDFSignature_mxJPO.DrwRole.TCONTR;
        if ("drw_lead".equals(getApproverTypeStr))
            return CAEPDFSignature_mxJPO.DrwRole.LEAD;
        return null;
    }

    public static class Logger {
        private List<String> errors = new LinkedList<>();

        private List<String> warnings = new LinkedList<>();

        public int totalDocs;

        public int signedDocs;

        public void addError(String message) {
            this.errors.add(message);
        }

        public void addWarning(String message) {
            this.warnings.add(message);
        }

        public boolean hasErrors() {
            return !this.errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !this.warnings.isEmpty();
        }

        public String getErrors(String prefics, String suffix) {
            StringBuilder sb = new StringBuilder();
            for (String error : this.errors)
                sb.append(prefics + error + suffix);
            return sb.toString();
        }

        public String getWarnings(String prefics, String suffix) {
            StringBuilder sb = new StringBuilder();
            for (String warning : this.warnings)
                sb.append(prefics + warning + suffix);
            return sb.toString();
        }
    }

    public static class CAInfo {
        private final String name;

        private final String completeDate;

        private final BufferedImage ownerSignature;

        private final String letter;

        public CAInfo(String name, String completeDate, BufferedImage ownerSignature, String letter) {
            this.name = name;
            this.completeDate = completeDate;
            this.ownerSignature = ownerSignature;
            this.letter = letter;
        }

        public String getName() {
            return this.name;
        }

        public String getCompleteDate() {
            return this.completeDate;
        }

        public BufferedImage getOwnerSignature() {
            return this.ownerSignature;
        }

        public String getLetter() {
            return this.letter;
        }
    }
}
