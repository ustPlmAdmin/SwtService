<%--  emxComponentsDiscussionTree.jsp  --  Include Tree page for discussion .

  Copyright (c) 1992-2018 Dassault Systemes.
  All Rights Reserved.
  This program contains proprietary and trade secret information of MatrixOne,Inc.
  Copyright notice is precautionary only and does not evidence any actual or intended publication of such program

  static const char RCSID[] = $Id: emxComponentsDiscussionTree.jsp.rca 1.8 Wed Oct 22 16:18:16 2008 przemek Experimental przemek $
 --%>

<%@ page import="com.matrixone.apps.domain.*" %>

<%
String jsTreeID = null;
String objectId = null;
String suiteKey = null;
%>
<%@ include file = "../components/emxComponentsDiscussionDetail.inc" %>
<script language="javascript" src="../common/scripts/emxUIConstants.js"></script>
<script language="JavaScript" src="../common/scripts/emxUICore.js"></script>
<script language="JavaScript" src="../common/scripts/emxUICoreMenu.js"></script>
<script language="JavaScript" src="../common/scripts/emxUICalendar.js"></script>
<script language="JavaScript" src="../common/scripts/emxUIModal.js"></script>
<script language="JavaScript" src="../common/scripts/emxUIPopups.js"></script>
<script language="javascript" src="../components/emxUIComponentsDiscussion.js"></script>
<script language="javascript" src="../emxUIPageUtility.js"></script>

<script type="text/javascript">
  addStyleSheet("emxUIDefault");
  addStyleSheet("emxUIProperties");
  //addStyleSheet("emxUICalendar10_5");
  addStyleSheet("emxUIToolbar");
  addStyleSheet("emxUIMenu");
</script>
<html>
<head>
  <title>Discussion</title>
  <link rel="STYLESHEET" type="text/css" href="emxUIComponentsDiscussion.css" />
</head>

<%

  String sTimeZone =(String)session.getValue("timeZone");
  String loggedInUser = context.getUser();
  String formName = emxGetParameter(request, "form");
  String baseID = emxGetParameter(request, "objectId");
  DomainObject baseObj = new DomainObject(baseID);
  String baseName = (String) baseObj.getInfo(context, DomainObject.SELECT_NAME);
//new code for deciding the context user whether he is a public user or private user..
    DomainObject BaseObject   = DomainObject.newInstance(context,baseID);
    String strType = (String)BaseObject.getInfo(context,BaseObject.SELECT_TYPE);
	String[] args = new String[1];
	args[0] = objectId;
	Boolean userFlag =(Boolean)JPO.invoke(context, "emxDiscussion", args, "isPrivateUser", args, Boolean.class);
//end of code..

%>

<body onload="loadTree()">
<%
	if (formName == null)
	{
%>
Loading Discussion...
<%
	}
	else
	{
		String acceptLanguage = request.getHeader("Accept-Language");
		String I18NResourceBundle = "emxComponentsStringResource";
		String subTitle = i18nStringValue("emxComponents.Discussion.subTitle",I18NResourceBundle,acceptLanguage);
		String launch = i18nStringValue("emxFramework.Toolbar.Launch","emxFrameworkStringResource",acceptLanguage);
%>
		<table border="0" cellpadding="0" cellspacing="2" width="100%">
		<tr>
				<td class="pageSubTitle">
					<%=XSSUtil.encodeForHTML(context, subTitle)%><a href="javascript:emxShowModalDialog('../common/emxTree.jsp?objectId=<%=XSSUtil.encodeForHTML(context, baseID)%>&jsTreeID=<%=XSSUtil.encodeForHTML(context, jsTreeID)%>&suiteKey=<%=XSSUtil.encodeForHTML(context, suiteKey)%>')">:<%=XSSUtil.encodeForHTML(context, baseName)%> </a>
				</td>
		 </tr>
		 <tr>
              <td>
					<a href="javascript:launchDiscussion('<%=XSSUtil.encodeForHTML(context, objectId)%>', '<%=XSSUtil.encodeForHTML(context, jsTreeID)%>', '<%=XSSUtil.encodeForHTML(context, suiteKey)%>')"><img src="../common/images/iconActionNewWindow.png" border="0" title="<%=launch%>" /></a>
					<a href="javascript:launchDiscussion('<%=XSSUtil.encodeForHTML(context, objectId)%>', '<%=XSSUtil.encodeForHTML(context, jsTreeID)%>', '<%=XSSUtil.encodeForHTML(context, suiteKey)%>')"><%=launch%></a>
			 </td>
         </tr>
		</table>
<%
	}
%>
</body>
<%

  String timeZone =(String)session.getValue("timeZone");

  com.matrixone.apps.common.Person person = (com.matrixone.apps.common.Person) DomainObject.newInstance(context, DomainConstants.TYPE_PERSON);

  // get the logged-in user's name
   loggedInUser = person.getPerson(context).getName(context);

%>

<script language="javascript">

  function loadTree() {

	//get reference to discussion object
    //var discussion = parent.discussion;
	var discussion = null;
	var localDiscussionTree = null;
	if ("<%=XSSUtil.encodeForJavaScript(context, formName)%>" != "null")
	{
		discussion = new jsDiscussion("emxUIComponentsDiscussion.css");
		discussion.formName = "<%=XSSUtil.encodeForJavaScript(context, formName)%>";
	}
	else
	{
		discussion = parent.discussion;
		localDiscussionTree = parent.localDiscussionTree;
	}
    //set the display frame
    discussion.displayFrame = self.name;
	discussion.viewMode = "<%=XSSUtil.encodeForJavaScript(context, sDiscussionView)%>";
	discussion.showMode = "<%=XSSUtil.encodeForJavaScript(context, sDiscussionShow)%>";
	discussion.sortMode = "<%=XSSUtil.encodeForJavaScript(context, sDiscussionSort)%>";
	discussion.boolVal = "<%=userFlag%>";

    //discussion.createRoot("Do you know about the important changes that need to be made?", "There are some very important changes that have to be made before shipping.  First, we need to ensure that all of the bugs are fixed.  This is VERY important.
    //There were far too many bugs in the first ship.", "Michael Smith", "2/2/2001");
    <%
    String sDynamicURLEnabled = EnoviaResourceBundle.getProperty(context,"emxFramework.DynamicURLEnabled");
    if(!"false".equals(sDynamicURLEnabled)) {
     strSubject = UINavigatorUtil.formatEmbeddedURL(context, strSubject, "NO", "YES", request.getHeader("Accept-Language"));
     strMessage = UINavigatorUtil.formatEmbeddedURL(context, strMessage, "NO", "YES", request.getHeader("Accept-Language"));
    }
            String tempSubject=FrameworkUtil.findAndReplace(strSubject, "\n", "<br />");
	String tempMessage=FrameworkUtil.findAndReplace(strMessage, "\n", "<br />");

    %>
/* Removal of displaytime='true' by Yukthesh,Infosys for Bug # 293306*/
    discussion.createRoot("<%=tempSubject%>", "<%=tempMessage%>", "<%=strOwner%>", "<emxUtil:lzDate displaytime='true' localize='i18nId' tz='<%=sTimeZone%>' format='<%=DateFrm %>' ><%=strDate%></emxUtil:lzDate>","<%=messageId%>","<%=strPolicy1%>","<%=showSubscription%>","<%=strAttachments%>");

<framework:mapListItr mapList="<%= messageList %>" mapName="messageMap">
    //create the discussion tree root
<%
    strCount                      = (String)messageMap.get(Message.SELECT_MESSAGE_COUNT);

    String sMessage               = findReplace((String)messageMap.get(Message.SELECT_DESCRIPTION));
    String sSubject               = findReplace((String)messageMap.get(Message.SELECT_MESSAGE_SUBJECT));
    String sSelectOriginateddate  = (String)messageMap.get(Message.SELECT_ORIGINATED);
    String sSelectId              = (String)messageMap.get(Message.SELECT_ID);
	String strPolicy			  = (String)messageMap.get(Message.SELECT_POLICY);
	String sAttachments			  = (String)messageMap.get(SELECT_ATTACHMENTS);
    String sObjectIds             = "";
    String streeIds               = "";

    DomainObject domainObject = DomainObject.newInstance(context);
    domainObject.setId(sSelectId);

    String replyOwner = domainObject.getInfo(context, domainObject.SELECT_OWNER);
	//show the delete link only if the user is the owner of the "Reply" and there are no replies to this "Reply"
    boolean showDelete = replyOwner.equals(loggedInUser) && strCount.equals("0");
    // Below line  Added for Bug324966
	String sreplyOwner           = person.getDisplayName(context,replyOwner);

    if(Integer.parseInt(strCount) != 0){
      sObjectIds    = "emxComponentsDiscussionReplyDetails.jsp?objectId="+sSelectId;
      streeIds      = jsTreeID;
    }
    if(!"false".equals(sDynamicURLEnabled)) {
        sSubject = UINavigatorUtil.formatEmbeddedURL(context, sSubject, "NO", "YES", request.getHeader("Accept-Language"));
        sMessage = UINavigatorUtil.formatEmbeddedURL(context, sMessage, "NO", "YES", request.getHeader("Accept-Language"));
        }
		String rSubject=FrameworkUtil.findAndReplace(sSubject, "\n", "<br />");
		String rMessage=FrameworkUtil.findAndReplace(sMessage, "\n", "<br />");

%>
  /* Removal of displaytime='true' and  Replacement of the 3rd Parameter "sSelectOwner" with "strOwner" by Yukthesh,Infosys for Bug # 293306*/
  /* Below Code is changed for Bug324966(Replacement of the 3rdParameter "strOwner with "sreplyOwner"*/

  discussion.root.addChild("<%=rSubject%>","<%=rMessage%>","<%=sreplyOwner%>","<emxUtil:lzDate displaytime='true' localize='i18nId' tz='<%=sTimeZone%>' format='<%=DateFrm %>' ><%=sSelectOriginateddate%></emxUtil:lzDate>","<%=sSelectId%>","<%=showDelete%>","<%=sObjectIds%>","<%=streeIds%>","<%=strPolicy%>","<%=showSubscription%>","<%=sAttachments%>");
    //now add the data
    //discussion.root.addChild("I've been updated.", "We have contacted QA and are working on a new plan.", "Mike Jordan", "2/4/2001", "LoadDiscussionChildren.htm", "msg2");
    //discussion.root.addChild("I got the same email", "I suggested that we get together with all the groups and decide on a course of action together!", "Erin Cooper", "2/5/2001");
</framework:mapListItr>
    //draw the discussion tree
    //discussion.draw();
    discussion.refresh();
  }


  if (parent != null)
  {
	    parent.updateFields("<%=strLastViewFilterSelection%>", "<%=strLastSortFilterSelection%>");
  }

	if ("<%=XSSUtil.encodeForJavaScript(context, formName)%>" != "null")
	{
		loadTree();
	}

</script>
</html>
