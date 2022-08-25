<%--  emxComponentsDiscussionDetail.inc  --  Include page for discussion tree.

  Copyright (c) 1992-2018 Dassault Systemes.
  All Rights Reserved.
  This program contains proprietary and trade secret information of MatrixOne,Inc.
  Copyright notice is precautionary only and does not evidence any actual or intended publication of such program

  static const char RCSID[] = $Id: emxComponentsDiscussionDetail.inc.rca 1.6 Wed Oct 22 16:18:46 2008 przemek Experimental przemek $
 --%>

<%@include file = "../emxUICommonAppInclude.inc"%>
<%@include file = "emxComponentsCommonUtilAppInclude.inc"%>


<%@ page import="java.text.*, com.matrixone.apps.framework.taglib.*, com.matrixone.apps.common.*"%>
<%!

private static final String FILTER_VIEW_EMPTY      = "";
private static final String FILTER_VIEW_FLAT       = "emxComponents.Discussion.Flat";
private static final String FILTER_VIEW_THREADED   = "emxComponents.Discussion.Threaded";
private static final String FILTER_SORT_EMPTY      = "";
private static final String FILTER_SORT_ASCENDING  = "emxComponents.Discussion.Ascending";
private static final String FILTER_SORT_DESCENDING = "emxComponents.Discussion.Descending";
private static final String FILTER_SHOW_ALL        = "emxComponents.Discussion.All";
private static final String FILTER_SHOW_PUBLIC     = "emxComponents.Discussion.Public";
private static final String FILTER_SHOW_PRIVATE    = "emxComponents.Discussion.Private";

private static final String DISCUSSION_EMPTY         = "";
private static final String DISCUSSION_FLAT          = "Flat";
private static final String DISCUSSION_THREADED      = "Threaded";
private static final String DISCUSSION_ASCENDING     = "Ascending";
private static final String DISCUSSION_DESCENDING    = "Descending";
private static final String DISCUSSION_ALL           = "All";
private static final String DISCUSSION_PUBLIC        = "Public";
private static final String DISCUSSION_PRIVATE       = "Private";


//Method to append Special characters with \ so that Javascript Understands it.
public String Replace(String s, String s1, String s2)
{
  String s3 = "";
  int i = -1;
  ArrayList arraylist = new ArrayList();
  while((i = s.indexOf(s1, i + 1)) != -1)
    {
      arraylist.add(new Integer(i));
     }
  if(arraylist.size() > 0)
  {
      int k = 0;
      for(int j = 0; j < arraylist.size(); j++)
      {
          s3 = s3 + s.substring(k, ((Integer)arraylist.get(j)).intValue()) + s2;
          k = ((Integer)arraylist.get(j)).intValue() + 1;
      }

      s3 = s3 + s.substring(k, s.length());
  } else
  {
      s3 = s;
  }
  return s3;
}
// Method to replace the Special characters that is present in the message description.
public String findReplace(String str)
{
  //To take care of special character "\".
  str = Replace(str,"\\", "\\\\");
 //Code for replacing CR with "\r", so that Javascript interprets CR properly.
  String sTempStr = str;
  String sFinal = "";
  int iBeginIndex = 0;
  while (sTempStr.indexOf(13)!=-1)
  {
    sFinal = sFinal + sTempStr.substring(iBeginIndex,sTempStr.indexOf(13))+"\\r";
    sTempStr = sTempStr.substring(sTempStr.indexOf(13)+2,sTempStr.length());
  }
  sFinal = sFinal+sTempStr;
  sFinal = "".equals(sFinal)?str:sFinal;
  //To take care of double quotes.
  //Add similar functions to replace any special characters.
  sFinal = Replace(sFinal,"\"", "\\\"");

  //take care of single quotes
  sFinal = Replace(sFinal,"\'", "\\\'");

  return sFinal;
}
static public String i18nStringValue(String text,String Bundle, String languageStr) 
{
    i18nNow loc = new i18nNow();
    return (String)loc.GetString(Bundle, languageStr, text);
}

%>
<%
 /* 
When user sees the discussion tree from properties page, objectId of the discussion 
will come from request attribute. To handle that following code is added.*/
        
    String discussionId = (String)request.getAttribute("discussionId");
    if (discussionId == null || "".equals(discussionId) || "null".equalsIgnoreCase(discussionId))
    {
        objectId = emxGetParameter(request, "objectId");
    }
    else
    {
        objectId = discussionId;
    }

    jsTreeID = emxGetParameter(request, "jsTreeID");
    suiteKey = emxGetParameter(request, "suiteKey");
  
    String sDiscussionView    = emxGetParameter(request, "discussionView");
    String sDiscussionSort    = emxGetParameter(request, "discussionSort");
    String sDiscussionShow    = emxGetParameter(request, "discussionShow");
    String sDiscussionForm    = emxGetParameter(request, "form");

    String strLastViewFilterSelection = "";
    String strLastSortFilterSelection = "";
    
    if(!(FILTER_VIEW_FLAT.equals(sDiscussionView)||FILTER_VIEW_THREADED.equals(sDiscussionView))){
        sDiscussionView = PersonUtil.getDiscussionView(context);
        if (DISCUSSION_FLAT.equals(sDiscussionView)) {
            sDiscussionView = FILTER_VIEW_FLAT;
        }
        if (DISCUSSION_THREADED.equals(sDiscussionView)) {
            sDiscussionView = FILTER_VIEW_THREADED;
        }
    }
    
    if(!(FILTER_SORT_ASCENDING.equals(sDiscussionSort)||FILTER_SORT_DESCENDING.equals(sDiscussionSort))){
        sDiscussionSort = PersonUtil.getDiscussionSort(context);
        if (DISCUSSION_ASCENDING.equals(sDiscussionSort)) {
            sDiscussionSort = FILTER_SORT_ASCENDING;
        }
        if (DISCUSSION_DESCENDING.equals(sDiscussionSort)) {
            sDiscussionSort = FILTER_SORT_DESCENDING;
        }
    }

    //
    // The values of filters that are coming to this page are string resource keys
    // so covert them to the usable values
    //
    if (FILTER_VIEW_FLAT.equals(sDiscussionView)) {
        sDiscussionView = DISCUSSION_FLAT;
        strLastViewFilterSelection = FILTER_VIEW_FLAT;
    }
    else if (FILTER_VIEW_THREADED.equals(sDiscussionView)) {
        sDiscussionView = DISCUSSION_THREADED;
        strLastViewFilterSelection = FILTER_VIEW_THREADED;
    }
    else {
        sDiscussionView = null;
        strLastViewFilterSelection = FILTER_VIEW_FLAT;
    }

    if(FILTER_SHOW_PUBLIC.equals(sDiscussionShow)) {
        sDiscussionShow = DISCUSSION_PUBLIC;
    }
    else if(FILTER_SHOW_PRIVATE.equals(sDiscussionShow)) {
        sDiscussionShow = DISCUSSION_PRIVATE;
    }
    else {
        sDiscussionShow = DISCUSSION_ALL;
    }

    if (FILTER_SORT_ASCENDING.equals(sDiscussionSort)) {
        sDiscussionSort = DISCUSSION_ASCENDING;
        strLastSortFilterSelection = FILTER_SORT_ASCENDING;
    }
    else if (FILTER_SORT_DESCENDING.equals(sDiscussionSort)) {
        sDiscussionSort = DISCUSSION_DESCENDING;
        strLastSortFilterSelection = FILTER_SORT_DESCENDING;
    }
    else {
        sDiscussionSort = null;
        strLastSortFilterSelection = FILTER_SORT_ASCENDING;
    }


    if (sDiscussionForm != null && !"".equals(sDiscussionForm) && !"null".equals(sDiscussionForm))
    {
        sDiscussionView = DISCUSSION_FLAT;
        strLastViewFilterSelection = FILTER_VIEW_FLAT;
    }
    if (sDiscussionView == null && ("".equals(sDiscussionView = PersonUtil.getDiscussionView(context)))) 
    {
        sDiscussionView = DISCUSSION_FLAT;
        strLastViewFilterSelection = FILTER_VIEW_FLAT;
    }

    String objectWhere = null;
    if (DISCUSSION_PRIVATE.equals(sDiscussionShow))
    {
        sDiscussionShow = "Private Message";
        objectWhere = Message.SELECT_POLICY + " == '" + sDiscussionShow + "'";
    }
    else if (DISCUSSION_PUBLIC.equals(sDiscussionShow))
    {
        sDiscussionShow = "Message";
        objectWhere = Message.SELECT_POLICY + " == '" + sDiscussionShow + "'";
    }
    else
    {
        sDiscussionShow = DISCUSSION_ALL;
    }
    
    if (sDiscussionSort == null && ("".equals(sDiscussionSort = PersonUtil.getDiscussionSort(context)))) 
    {
        sDiscussionSort = DISCUSSION_ASCENDING;
        strLastSortFilterSelection = FILTER_SORT_ASCENDING;
    }

    String strCount    = "";
    String strChildURL = "";

    StringList objectSelects = new StringList();

    // To check that suscribe link should be shown or not
    boolean showSubscription = false;

  //~EOF To check that suscribe link should be shown or not

    Message message = (Message) DomainObject.newInstance(context, DomainConstants.TYPE_MESSAGE);
    message.setId(objectId);

    String SELECT_ATTACHMENTS = "from[Message Attachments]";
    message.open(context);
    objectSelects.add(Message.SELECT_ID);
    objectSelects.add(Message.SELECT_POLICY);
    objectSelects.add(Message.SELECT_MESSAGE_SUBJECT);
    objectSelects.add(Message.SELECT_DESCRIPTION);
    objectSelects.add(Message.SELECT_MESSAGE_COUNT);
    objectSelects.add(Message.SELECT_OWNER);
    objectSelects.add(Message.SELECT_ORIGINATED);
    objectSelects.add(SELECT_ATTACHMENTS);

    Map parentMessMap = (Map)message.getInfo(context,objectSelects);

    String strSubject   = (String)parentMessMap.get(Message.SELECT_MESSAGE_SUBJECT);
    String strPolicy1   = (String)parentMessMap.get(Message.SELECT_POLICY);
    String strMessage   = findReplace((String)parentMessMap.get(Message.SELECT_DESCRIPTION));
    String strOwner     = com.matrixone.apps.common.Person.getDisplayName(context,(String)parentMessMap.get(Message.SELECT_OWNER));
    String strDate      = (String)parentMessMap.get(Message.SELECT_ORIGINATED);
    String strAttachments      = (String)parentMessMap.get(SELECT_ATTACHMENTS);
    String messageId    = objectId;

    objectSelects.add(Message.SELECT_ID);

    MapList messageList = null;

    if (DISCUSSION_FLAT.equals(sDiscussionView))
    {
        messageList = Message.getMessages(context, (MessageHolder)message, objectSelects, objectWhere, 0);
    }
    else if (DISCUSSION_THREADED.equals(sDiscussionView))
    {
        String strPolicy = "";
        if (!DISCUSSION_ALL.equals(sDiscussionShow)) {
            strPolicy = sDiscussionShow;
        }
        messageList = message.list(context,objectSelects,null, strPolicy, message);
    }
    
    if (messageList != null) 
    {
        messageList.sort(Message.SELECT_ORIGINATED, sDiscussionSort, "date");
    }
%>


