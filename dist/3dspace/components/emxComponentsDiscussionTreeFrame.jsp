<%--  emxComponentsDiscussionTreeFrame.jsp  --  Include Tree Frame page for discussion .

  Copyright (c) 1992-2018 Dassault Systemes.
  All Rights Reserved.
  This program contains proprietary and trade secret information of MatrixOne,Inc.
  Copyright notice is precautionary only and does not evidence any actual or intended publication of such program

  static const char RCSID[] = $Id: emxComponentsDiscussionTreeFrame.jsp.rca 1.14 Wed Oct 22 16:18:38 2008 przemek Experimental przemek $
 --%>
<%@ include file = "../emxUICommonAppInclude.inc"%>
<%@ include file = "emxComponentsCommonUtilAppInclude.inc"%>
 <%@include file = "../emxTagLibInclude.inc"%>
 <emxUtil:localize id="i18nId" bundle="emxComponentsStringResource" locale='<%= XSSUtil.encodeForHTML(context, request.getHeader("Accept-Language")) %>' />
<%
  String objectId = emxGetParameter(request,"objectId");
  String suiteKey = emxGetParameter(request,"suiteKey");

  Message message           = (Message) DomainObject.newInstance(context,DomainConstants.TYPE_MESSAGE);
  message.setId(objectId);
  String RELATIONSHIP_PRIMARY_DISCUSSION = PropertyUtil.getSchemaProperty(context, "relationship_PrimaryDiscussion");
  boolean hasObject = message.hasRelatedObjects(context, RELATIONSHIP_PRIMARY_DISCUSSION, false);
  
  String jsTreeID = emxGetParameter(request,"jsTreeID");

  
  
  String acceptLanguage = request.getHeader("Accept-Language");
  String I18NResourceBundle = "emxComponentsStringResource";

  StringBuffer pageHeaderBuf = new StringBuffer(100);
  pageHeaderBuf.append(i18nNow.getI18nString("emxComponents.Common.DiscussionHeader", I18NResourceBundle, acceptLanguage)).append(" ").
  				append(message.getInfo(context, message.SELECT_MESSAGE_SUBJECT)).append(" : ").
  				append(i18nNow.getI18nString("emxComponents.RouteTemplate.Properties",I18NResourceBundle,acceptLanguage));
				

				
	StringBuffer subHeaderBuf = new StringBuffer(900);
	subHeaderBuf.append(i18nNow.getI18nString("emxComponents.Discussion.subTitle","emxComponentsStringResource",acceptLanguage)).append(" ");
	
	// my change
	String selPrefix = "to[" + (hasObject ? RELATIONSHIP_PRIMARY_DISCUSSION : DomainObject.RELATIONSHIP_MESSAGE) + "].from.to[" + DomainObject.RELATIONSHIP_THREAD + "].from.";

	StringList isReqOrSubreqSelects = new StringList(1);
	String subReqSelect = selPrefix + "to[Sub Requirement].from.name";
	isReqOrSubreqSelects.addElement(subReqSelect);
	Map isReqOrSubreqResponse = message.getInfo(context, isReqOrSubreqSelects);
	boolean isSubreq = !(isReqOrSubreqResponse.get(subReqSelect) == null ||
			((String)isReqOrSubreqResponse.get(subReqSelect)).isEmpty());

	for (int level = 0; level >= 0; level++){

		if (isSubreq && level == 0)
			selPrefix += "to[Sub Requirement].from.";
		else
			selPrefix += "to[Specification Structure].from.";
		
		StringList selects = new StringList(2);
		selects.addElement(selPrefix + "name");
		selects.addElement(selPrefix + "id");
		
		Map structMap = message.getInfo(context, selects);
		String structName = (String)structMap.get(selPrefix + "name"); 
		String structId = (String)structMap.get(selPrefix + "id"); 
		if (structName != null && !structName.isEmpty()) {
			subHeaderBuf.append(" ")
				.append("<a href=\"javascript:emxShowModalDialog('../common/emxTree.jsp?objectId=").append(structId)
				.append("&jsTreeID=").append(XSSUtil.encodeForJavaScript(context, jsTreeID))
				.append("&suiteKey=").append(XSSUtil.encodeForJavaScript(context, suiteKey))
				.append("')\" alt=\"Lanka\">").append(structName).append("</a> ");
		} else {
			break;
		}
	}
	
	
	
	
StringList objectSelects = new StringList(2);
	objectSelects.addElement(DomainObject.SELECT_DESCRIPTION);
	Map objMap = message.getInfo(context, objectSelects);
	String sInfoDesc = (String)objMap.get(DomainObject.SELECT_DESCRIPTION);
	sInfoDesc = sInfoDesc.replaceAll("\\<.*?\\>", "");  

	subHeaderBuf.append(i18nNow.getI18nString("emxComponents.Common.Description","emxComponentsStringResource",acceptLanguage)).append(" : ").
  			   append("<img src=\"../common/images/iconSmallDiscussion.gif\" align=\"absmiddle\" title=\"").append(sInfoDesc.trim()).append("\" />");
 

%>

<html>
	<head>
		<title>ENOVIA</title>
		<script language="JavaScript" src="../common/scripts/emxUIConstants.js"></script>
		<script language="JavaScript" src="../common/scripts/emxUICore.js"></script>
		<script language="JavaScript" src="../common/scripts/emxUICoreMenu.js"></script>
		<script language="JavaScript" src="../common/scripts/emxUIToolbar.js"></script>
		<script language="JavaScript" src="../emxUIFilterUtility.js"></script>
		<script language="JavaScript" src="../common/scripts/emxUIActionbar.js"></script>
		<script language="JavaScript" src="../common/scripts/emxUIModal.js"></script>
		<script language="JavaScript" src="../common/scripts/emxNavigatorHelp.js"></script>
		<script language="JavaScript" src="../emxUIPageUtility.js"></script>
		<script language="JavaScript" src="../common/scripts/emxUIBottomPageJavaScriptInclude.js"></script>
		<script language="javascript" src="emxUIComponentsDiscussion.js"></script>
		<script language="Javascript">    
			addStyleSheet("emxUIDefault");    
			addStyleSheet("emxUIToolbar");    
			addStyleSheet("emxUIMenu");    
			addStyleSheet("emxUIDOMLayout");
			addStyleSheet("emxUIComponentsDiscussion");    
		</script>
		<script>
			  var STR_DELETE = "<emxUtil:i18nScript localize="i18nId">emxComponents.Common.Delete</emxUtil:i18nScript>";
			  var STR_PUBLICREPLY = "<emxUtil:i18nScript localize="i18nId">emxComponents.Common.PublicReply</emxUtil:i18nScript>";
			  var STR_PRIVATEREPLY = "<emxUtil:i18nScript localize="i18nId">emxComponents.Common.PrivateReply</emxUtil:i18nScript>";
		</script>
<script language="JavaScript">
		    var localDiscussionTree = null;
			var discussion = new jsDiscussion("emxUIComponentsDiscussion.css");
			
			function createDiscussionReply(strNodeID,messageId, DiscType) {
   // emxShowModalDialog("emxComponentsDiscussionCreateDialogFS.jsp?suiteKey=eServiceSuiteComponents&functionality=CreateDiscussion&heading=emxComponents.Discussion.Reply&help=emxhelpdiscussionreplycreate&objectId=" + messageId + "&jsTreeID=" + strNodeID + "&DiscType=" + DiscType,600,500,true);
   emxShowModalDialog("../common/emxForm.jsp?form=APPCreateDiscussion&suiteKey=Components&formHeader=emxComponents.Discussion.Reply&HelpMarker=emxhelpdiscussionreplycreate&mode=edit&showPageURLIcon=false&Export=false&findMxLink=false&objectId=" + messageId + "&DiscType=" + DiscType + "&postProcessURL=../components/emxComponentsDiscussionCreateProcess.jsp&submitAction=doNothing&preProcessJavaScript=clearMessageInDiscussionReply",600,500,true);
  }
  // function to get confirmation to delete a reply
			function deleteReply(messageId) {
    if(confirm("<emxUtil:i18nScript localize="i18nId">emxComponents.Discussion.DeleteReplyAttachConfirm</emxUtil:i18nScript>")){
    	submitWithCSRF("emxComponentsDiscussionDeleteProcess.jsp?deleteType=reply&objectId="+messageId+ "&objID=<%=XSSUtil.encodeForURL(context, objectId)%>",findFrame(getTopWindow(),'discussionTreeDisplay'));      
    }
    return;
  }
  // function to show the Subsriptions screen
			function showSubscription(messageId) {
    emxShowModalDialog("emxSubscriptionDialog.jsp?objectId=" + messageId,600,500,true);
    return;
  }
  // function to show attachments
			function showAttachments(messageId) {
    //emxShowModalDialog("../common/emxTable.jsp?program=emxDiscussion:getDiscussionAttachmentsList&table=APPDocumentSummary&suiteKey=Components&selection=multiple&sortColumnName=Name&sortDirection=ascending&header=emxComponents.Common.DiscussionAttachments&HelpMarker=emxhelpdiscussion&parentRelName=relationship_MessageAttachments&toolbar=APPDiscussionDocumentSummaryToolBar&objectId="+messageId,750,450,true);
    showModalDialog("../common/emxTable.jsp?program=emxDiscussion:getDiscussionAttachmentsList&table=APPDocumentSummary&suiteKey=Components&selection=multiple&sortColumnName=Name&sortDirection=ascending&header=emxComponents.Common.DiscussionAttachments&HelpMarker=emxhelpdiscussion&parentRelName=relationship_MessageAttachments&toolbar=APPDiscussionDocumentSummaryToolBar&objectId="+messageId,750,450,true);
  }
			function updateFields(sView, sSort) {
			    try{
	        		document.getElementById("APPDiscussionView").value = sView;
			        document.getElementById("APPDiscussionSort").value = sSort;
			       // doViewAction(objMainToolbar);
			    } catch (Err) {
  }
  }

			function doViewAction(objMainToolbar) {
			    if (objMainToolbar != null)	{
			        var sView = document.getElementById("APPDiscussionView").value;
			        var sShow = document.getElementById("APPDiscussionShow").value;
			        var toolbarItem = "";
			        for (var itr = 0; itr < objMainToolbar.items.length-2; itr++) {
			            objMainToolbar.items[itr].enable();
			            var objClassName = objMainToolbar.items[itr].element.className;
			            if (objClassName == "icon-and-text-button" || objClassName == "icon-and-text-button") {
			                //toolbarItem = objMainToolbar.items[itr].element.innerHTML;
			                toolbarItem = objMainToolbar.items[itr].text;
			            }
  
			            var publicKey = "<emxUtil:i18n localize="i18nId">emxComponents.Common.PublicReply</emxUtil:i18n>";
			            var privateKey = "<emxUtil:i18n localize="i18nId">emxComponents.Common.PrivateReply</emxUtil:i18n>";
			            /*if (toolbarItem.indexOf("...") != -1)
			            {
			                toolbarItem = toolbarItem.substring(0, toolbarItem.indexOf("..."));
			            }*/
  
			            if (toolbarItem != "" && sView == "Threaded" && (toolbarItem.indexOf(publicKey) != -1 || toolbarItem.indexOf(privateKey) != -1)) {
			                objMainToolbar.items[itr].disable();
			            }
			            /*else if (toolbarItem != "" && sView == "Flat" && publicKey.indexOf(toolbarItem) != -1 && sShow == "Private")
			            {
			                objMainToolbar.items[itr].disable();
			            }*/
			            toolbarItem = "";
		        	}   

		        	var iCount = document.getElementById("APPDiscussionShow").options.length;
			        if (sView == "Threaded") {
		        	    if (iCount == 3){
			                document.getElementById("APPDiscussionShow").options[2] = null;
			                //document.getElementById("APPDiscussionSort").disabled = true;
		            	}
		        	} else {
			            if (iCount < 3) {
			                document.getElementById("APPDiscussionShow").options[2] = new Option("<emxUtil:i18n localize="i18nId">emxComponents.Discussion.Private</emxUtil:i18n>", "Private");
			                document.getElementById("APPDiscussionSort").disabled = false;          
			            }
		        	}
	    		}
			}						
		</script>
		<script>
			function loadRelatedFrames() {
				findFrame(getTopWindow(), "discussionTreeDisplay").location.href="../components/emxComponentsDiscussionTreeContainer.jsp?objectId=<%=XSSUtil.encodeForURL(context, objectId)%>&jsTreeID=<%=XSSUtil.encodeForURL(context, jsTreeID)%>&suiteKey=<%=XSSUtil.encodeForURL(context, suiteKey)%>";
				var phd = document.getElementById("pageHeadDiv");
				var dpb = document.getElementById("divPageBody");
				if(phd && dpb){
					var ht = phd.clientHeight;
					if(ht <= 0){
						ht = phd.offsetHeight;
					}
					dpb.style.top = ht + "px";
				}				
			}
		</script>
	</head>
	<body class='no-footer'onload="loadRelatedFrames();">
		<div id="pageHeadDiv">
			<form name="mx_filterselect_hidden_form">
				<table>
					<tr>
							<td class="page-title"> 
								<h2 id="ph"><%=pageHeaderBuf.toString()%></h2>
								<h3 id="sph"><%=subHeaderBuf.toString()%> </h3>
							</td>					
					</tr>								
				</table>		  
			    <jsp:include page = "../common/emxToolbar.jsp" flush="true">
				    <jsp:param name="toolbar" value="APPDiscussionViewToolBar"/>
				    <jsp:param name="objectId" value="<%=objectId%>"/>
				    <jsp:param name="suiteKey" value="<%=XSSUtil.encodeForJavaScript(context, suiteKey)%>"/>
				    <jsp:param name="PrinterFriendly" value="false"/>
				    <jsp:param name="export" value="false"/>
				    <jsp:param name="helpMarker" value="emxhelpdiscussiondetails"/>
			    </jsp:include>
				<input type="hidden" name="txtTempDiscussion" id="txtTempDiscussion" value="../components/emxComponentsDiscussionTreeContainer.jsp?objectId=<%=XSSUtil.encodeForHTMLAttribute(context, objectId)%>&jsTreeID=<%=XSSUtil.encodeForHTMLAttribute(context, jsTreeID)%>&suiteKey=<%=XSSUtil.encodeForHTMLAttribute(context, suiteKey)%>" />
		  	</form>		    
		</div> <!-- #pageHeadDiv -->
		<div id='divPageBody'>
			<div>
				<iframe name='discussionTreeDisplay' id='discussionTreeDisplay' width='99%' height='95%' frameborder='0' border='0'></iframe>
			</div>
		</div>
		<div id='divPageFoot'>
		</div>
	</body>
</html>
