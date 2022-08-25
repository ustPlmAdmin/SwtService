import com.dassault_systemes.enovia.changeaction.interfaces.IChangeAction;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeAction;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.mql.MqlService;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
/**
 * Класс для отправки запросов на 1C
 * */
public class SWTUtils_mxJPO extends MqlService {

    public SWTUtils_mxJPO(Context context, String[] args) throws Exception {

    }

    public int notifyChangeActionComplete(Context context, String[] args) throws Exception {
        String actionId = args[0];

        IChangeAction mChangeAction = ChangeAction.getChangeAction(context, actionId);

        StringList finalFollowerList = new StringList();
        List followerNameList = mChangeAction.GetFollowers(context);
        Iterator followersList = followerNameList.iterator();
        while (followersList.hasNext())
            finalFollowerList.addElement((String) followersList.next());

        String caData = MqlUtil.mqlCommand(context, "print bus $1 select $2 $3 $4 $5 dump $6",
                actionId, "name", "owner", "attribute[Synopsis]", "description", "|");
        StringList caList = FrameworkUtil.split(caData, "|");
        String caName = caList.get(0);
        String caOwner = caList.get(1);
        String caTitle = caList.get(2);
        String caDescription = caList.get(3);

        String complete = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale("ru"), "emxFramework.Label.SWT.ChangeActionComplete");
        String subject = caName + " " + complete;

        List<String> tasks = list(context, mChangeAction.getLastApprovalRoute(context), "to[Route Task].from.id");
        StringList objectIdList = new StringList(tasks.get(tasks.size() - 1));

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("objectIdList", objectIdList);
        paramMap.put("toList", finalFollowerList);
        paramMap.put("subject", subject);
        paramMap.put("message", "");
        paramMap.put("fromAgent", caOwner);
        JPO.invoke(context, "emxNotificationUtil", null, "sendMail", JPO.packArgs(paramMap), Integer.class);
        return 0;
    }

    public static String post(String data) {
        try {
            URL u = new URL("http://1c-web.corp.sw-tech.by/erp/hS/PLM/Process");
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.setRequestProperty("Content-Length", data);
            conn.setRequestProperty("Authorization", "Basic " + new String(Base64.getEncoder().encode("Test:123".getBytes())));

            OutputStream os = conn.getOutputStream();
            os.write(data.getBytes());

            Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";

            return result;
        } catch (Exception ignored) {
            return "error";
        }
    }

    public int PolicyProjectTaskStateReviewPromoteAction(Context context, String[] args) throws Exception {
        String taskId = args[0];

        String dump = MqlUtil.mqlCommand(context, "print bus $1 select name $2 dump $3", taskId, "attribute[Notes]", "|");
        StringList row = FrameworkUtil.split(dump, "|");
        if (row.get(1) != null && !row.get(1).isEmpty()) {
            post("{ \"task_name\": \"" + row.get(0) + "\"}");
            //emxNotificationUtil_mxJPO.sendJavaMail(context, new StringList("p.nikitin"), null, null, "subject", null, row.get(0), "admin_platform", null);
        }
        return 0;
    }

    public int PolicyRouteStateInProcessPromoteAction(Context context, String[] args) throws Exception {
        /*String route_id = args[0];

        String dump = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", route_id, "name", "|");
        StringList row = FrameworkUtil.split(dump, "|");

        post("{ \"route_name\": \"" + row.get(0) + "\"}");*/
        //emxNotificationUtil_mxJPO.sendJavaMail(context, new StringList("m.gaiduk"), null, null, "subject", null, "wefwef" + row.get(0), "admin_platform", null);
        return 0;
    }

    /*class NotAllPartsInChangeAction extends Exception {

    }


    private void testIncluding(Context ctx, List<String> contentIds, String objectId) throws Exception {
        for (Map<String, String>  child : rows(ctx, objectId, "from[VPMInstance].to.physicalid:id", "from[VPMInstance].to.current:current")) {
            if (contentIds.indexOf(child.get("id")) == -1 || (contentIds.indexOf(child.get("id")) != -1 && child.get("current").equals("RELEASED")))
                throw new NotAllPartsInChangeAction();
            else
                testIncluding(ctx, contentIds, child.get("id"));
        }
    }

    public int CheckAllPartsAdded(Context ctx, String[] args) throws Exception {
        String objectId = args[0];

        List<String> contentIds = list(ctx, objectId, "from[Proposed Activities].to.paths[Proposed Activity.Where].path.element[0].physicalid");
        String root = null;
        for (String contentId : contentIds) {
            Map<String, String> assy = row(ctx, contentId, "type", "to[VPMInstance]:is_root");
            if (assy.get("type").equals("Kit_Product") && assy.get("is_root").equals("FALSE")) {
                root = contentId;
                break;
            }
        }
        testIncluding(ctx, contentIds, root);

        if (true)
            throw new NullPointerException();

        return 0;
    }*/

}