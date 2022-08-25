<%@ page contentType="text/html; charset=utf-8" language="java" import="java.sql.*" errorPage="" %>
<%@page import = "com.matrixone.apps.domain.*,com.matrixone.apps.domain.util.*,matrix.util.*,matrix.db.Context,com.matrixone.servlet.Framework,com.matrixone.apps.framework.ui.*"%>
<%@page import = "java.util.*,java.text.SimpleDateFormat,java.math.BigDecimal,java.util.Calendar,java.text.*"%>
<%@page import = "java.util.List"%>
<%@page import = "java.util.Arrays"%>
<%@page import="matrix.db.JPO"%>
<%@page import="com.matrixone.apps.domain.util.FrameworkException"%>
<%@page import="com.matrixone.apps.domain.util.*"%>
<%@page import="matrix.util.MatrixException"%>
<%@page import="java.lang.StringBuffer"%>
<%@page import="java.io.*"%>
<%
/////////////////////////////////////// String resources  
	HashMap hmMonths = new HashMap();
	hmMonths.put("0","января");
	hmMonths.put("1","февраля");
	hmMonths.put("2","марта");
	hmMonths.put("3","апреля");
	hmMonths.put("4","мая");
	hmMonths.put("5","июня");
	hmMonths.put("6","июля");
	hmMonths.put("7","августа");
	hmMonths.put("8","сентября");
	hmMonths.put("9","октября");
	hmMonths.put("10","ноября");
	hmMonths.put("11","декабря");
	
	String EXPRDELIM = "LONGDELIMITER";
//////////////////////////////////////////////// Date variables initialization	
	Calendar cal = Calendar.getInstance();
	java.util.Date currentTime = cal.getTime();
	int iStartMonth=0;
	int iEndMonth=0;
	int iEffectiveMonth=0;
	int iContractMonth=0;
	String sStartMonth="";
	String sEndMonth="";
	String sEffectiveMonth="";
	String sContractMonth="";
	int iStartDay=0;
	int iEndDay=0;
	int iEffectiveDay=0;
	int iContractDay=0;
	int iStartYear=0;
	int iEndYear=0;
	int iEffectiveYear=0;
	int iContractYear=0;
	java.util.Date ContractDate = new java.util.Date();
	java.util.Date EstimatedStartDate = new java.util.Date();
	java.util.Date EstimatedEndDate = new java.util.Date();
	DateFormat dateformat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss aa");
	DateFormat dfShort = new SimpleDateFormat("dd.MM.yyyy");
	DateFormat dfShortTime = new SimpleDateFormat("dd.MM.yyyy / HH:mm");
////////////////////////////////////////////////Get request params
	matrix.db.Context context = Framework.getFrameContext(session);
	String sOID = com.matrixone.apps.domain.util.Request.getParameter(request, "objectId");
	String sRouteId="";
	String sCurrentUserId=PersonUtil.getPersonObjectID(context);
////////////////////////////////////////////////	
DomainObject doRoute=new DomainObject();
doRoute = DomainObject.newInstance(context,sOID);
// Getting Route details
String sRouteName = doRoute.getInfo(context,"name");
String sRouteStatus=doRoute.getInfo(context,"attribute[Route Status]"); //emxFramework.Range.Route_Status.VALUE
	sRouteStatus=sRouteStatus.replaceAll(" ","_"); //spaces removal
String sRkey="emxFramework.Range.Route_Status."+sRouteStatus;
sRouteStatus=i18nNow.getI18nString(sRkey,"emxFrameworkStringResource","ru");
String sRouteCurrentLevel=doRoute.getInfo(context,"attribute[Current Route Node]");
//Constructing all the route tasks maplist
				StringList busSelects = new StringList();
				StringList relSelects = new StringList();
				busSelects.add("name");
				busSelects.add("id");
				relSelects.add("id[connection]");
				relSelects.add("attribute[Title]");
				relSelects.add("attribute[Route Node ID]");
				relSelects.add("attribute[Route Action]");
				relSelects.add("attribute[Route Instructions]");
				relSelects.add("attribute[Route Sequence]");
				relSelects.add("attribute[Parallel Node Procession Rule]");
				relSelects.add("attribute[Scheduled Completion Date]");
				relSelects.add("attribute[Actual Completion Date]");

				busSelects.add("name");
				busSelects.add("id");
				busSelects.add("current");
				busSelects.add("revision");
				busSelects.add("from[Project Task].to.name");
				busSelects.add("from[Project Task].to.id");
				busSelects.add("owner");
				busSelects.add("attribute[Route Node ID]");
				busSelects.add("attribute[Approval Status]");
				busSelects.add("current");
				busSelects.add("attribute[Actual Completion Date]");
				busSelects.add("attribute[Comments]");
				busSelects.add("originated");

				MapList mlTasks = doRoute.getRelatedObjects(context, "Route Node", "Person", busSelects, relSelects, false, true, (short)1, "", "", 0); //Route Nodes
				MapList mlRouteTasks = doRoute.getRelatedObjects(context, "Route Task", "*", busSelects, null, true, false, (short)1, null, "", 0); // Inbox Tasks
				mlTasks.sort("attribute[Route Sequence]", "ascending", "Integer");
				String sRouteCurrent = doRoute.getInfo(context, "current");
// Constructing previous revisions tasks maplist
int iHRevision=1; //Highest revision of any inbox task in Route.
for (int l=0;l<mlRouteTasks.size();l++)
{
	Hashtable htInboxTask= (Hashtable)mlRouteTasks.get(l);
	String sRevision = (String)htInboxTask.get("revision");
	if (Integer.parseInt(sRevision)>iHRevision) iHRevision=Integer.parseInt(sRevision);
}
/////Now We need to found all the previous revision tasks and construct history of them
MapList mlPreviousTasks=new MapList();
HashMap mOldTask=new HashMap();
for (int l=0;l<mlRouteTasks.size();l++) //passing all the currently connected tasks
{
	Hashtable htInboxTask= (Hashtable)mlRouteTasks.get(l);
	String sRevision = (String)htInboxTask.get("revision");
	String sTaskId = (String)htInboxTask.get("id");
	DomainObject sCurrentTask = DomainObject.newInstance(context,sTaskId);
	MapList mlAllRevisions=sCurrentTask.getRevisionsInfo(context, busSelects, new StringList());

	for (int i=0;i<mlAllRevisions.size();i++) //passing all revisions for filtering old revisions
	{
		HashMap hmTask = (HashMap) mlAllRevisions.get(i);
		String sRev = (String)hmTask.get("revision");
		//if (Integer.parseInt(sRev)<iHRevision) //filtering all old revisions
		mlPreviousTasks.add(hmTask);
	}
}
mlPreviousTasks.sort("originated", "descending", "Date");
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv=Content-Type content="text/html; charset=utf-8">

<style>
<!--
 /* Font Definitions */

@font-face
	{font-family:Times;
	panose-1:2 2 6 3 5 4 5 2 3 4;
	mso-font-charset:204;
	mso-generic-font-family:roman;
	mso-font-pitch:variable;
	mso-font-signature:-536859905 -1073711039 9 0 511 0;}
 /* Style Definitions */
 p.MsoNormal, li.MsoNormal, div.MsoNormal
	{mso-style-unhide:no;
	mso-style-qformat:yes;
	mso-style-parent:"";
	margin:0in;
	margin-bottom:.0001pt;
	mso-pagination:widow-orphan;
	font-size:10.0pt;
	font-family:"Times New Roman","serif";
	mso-fareast-font-family:"Times New Roman";
	mso-fareast-language:EN-US;}
h1
	{mso-style-unhide:no;
	mso-style-qformat:yes;
	mso-style-link:"Заголовок 1 Знак";
	mso-style-next:"Основной текст";
	margin-top:0in;
	margin-right:.5in;
	margin-bottom:.5in;
	margin-left:0in;
	text-align:center;
	mso-pagination:widow-orphan lines-together;
	page-break-after:avoid;
	mso-outline-level:1;
	tab-stops:1.75in;
	font-size:28.0pt;
	mso-bidi-font-size:10.0pt;
	font-family:"Times New Roman","serif";
	mso-font-kerning:0pt;
	mso-fareast-language:EN-US;
	font-weight:normal;}
h2
	{mso-style-unhide:no;
	mso-style-qformat:yes;
	mso-style-parent:"Основной текст";
	mso-style-link:"Заголовок 2 Знак";
	mso-style-next:"Основной текст";
	margin-top:6.0pt;
	margin-right:0in;
	margin-bottom:6.0pt;
	margin-left:0in;
	text-align:center;
	mso-pagination:widow-orphan lines-together;
	page-break-after:avoid;
	mso-outline-level:2;
	font-size:18.0pt;
	mso-bidi-font-size:10.0pt;
	font-family:"Times New Roman","serif";
	mso-fareast-language:EN-US;
	mso-bidi-font-weight:normal;}
h3
	{mso-style-unhide:no;
	mso-style-qformat:yes;
	mso-style-parent:"Основной текст";
	mso-style-link:"Заголовок 3 Знак";
	mso-style-next:"Основной текст";
	margin-top:6.0pt;
	margin-right:0in;
	margin-bottom:6.0pt;
	margin-left:0in;
	mso-pagination:widow-orphan lines-together;
	page-break-after:avoid;
	mso-outline-level:3;
	border:none;
	mso-border-top-alt:solid windowtext 3.0pt;
	padding:0in;
	mso-padding-alt:1.0pt 0in 0in 0in;
	font-size:14.0pt;
	mso-bidi-font-size:10.0pt;
	font-family:"Times New Roman","serif";
	mso-fareast-language:EN-US;
	mso-bidi-font-weight:normal;}
p.MsoBodyText, li.MsoBodyText, div.MsoBodyText
	{mso-style-priority:99;
	mso-style-link:"Основной текст Знак";
	margin-top:0in;
	margin-right:0in;
	margin-bottom:6.0pt;
	margin-left:0in;
	mso-pagination:widow-orphan;
	font-size:10.0pt;
	font-family:"Times New Roman","serif";
	mso-fareast-font-family:"Times New Roman";
	mso-fareast-language:EN-US;}
span.1
	{mso-style-name:"Заголовок 1 Знак";
	mso-style-unhide:no;
	mso-style-locked:yes;
	mso-style-link:"Заголовок 1";
	mso-ansi-font-size:28.0pt;}
span.a
	{mso-style-name:"Основной текст Знак";
	mso-style-priority:99;
	mso-style-unhide:no;
	mso-style-locked:yes;
	mso-style-link:"Основной текст";}
span.2
	{mso-style-name:"Заголовок 2 Знак";
	mso-style-unhide:no;
	mso-style-locked:yes;
	mso-style-link:"Заголовок 2";
	mso-ansi-font-size:18.0pt;
	font-weight:bold;
	mso-bidi-font-weight:normal;}
span.3
	{mso-style-name:"Заголовок 3 Знак";
	mso-style-unhide:no;
	mso-style-locked:yes;
	mso-style-link:"Заголовок 3";
	mso-ansi-font-size:14.0pt;
	font-weight:bold;
	mso-bidi-font-weight:normal;}

span.a0
	{mso-style-name:"Название Знак";
	mso-style-unhide:no;
	mso-style-locked:yes;
	mso-style-link:Название;
	mso-ansi-font-size:24.0pt;
	font-family:"Book Antiqua","serif";
	mso-ascii-font-family:"Book Antiqua";
	mso-hansi-font-family:"Book Antiqua";}
p.a1, li.a1, div.a1
	{mso-style-name:"Заголовок Таблиц";
	mso-style-unhide:no;
	mso-style-qformat:yes;
	margin:0in;
	margin-bottom:.0001pt;
	mso-pagination:widow-orphan lines-together;
	font-size:12.0pt;
	mso-bidi-font-size:10.0pt;
	font-family:"Times New Roman","serif";
	mso-fareast-font-family:"Times New Roman";
	mso-fareast-language:EN-US;
	font-weight:bold;
	mso-bidi-font-weight:normal;}
@page WordSection1
	{size:841.9pt 595.3pt;
	mso-page-orientation:landscape;
	margin:.5in .5in .5in .5in;
	mso-header-margin:35.4pt;
	mso-footer-margin:35.4pt;
	mso-paper-source:0;
	box-shadow: 0 0 10px rgba(0,0,0,0.5);}
div.WordSection1
	{page:WordSection1;
	max-width: 297mm;
	}
div.A4A {
		width: 297mm;   /* ширина */
        height: 210mm; /* высота */
		max-height: 210mm;
        border:1px solid black;
        background-color: white;  /* цвет фона в блоке */
        font-family:  "Times New Roman"; /* нужный шрифт */
        font-size: 14pt;
        line-height: 1.5
}
@media print{@page {size: landscape}}
-->
</style>
</head>

<body lang=RU style='tab-interval:35.4pt'>
<div class="WordSection1">
<h2>Отчёт о выполнении маршрута | Номер маршрута: <%=sRouteName%></h2>


<p class=MsoBodyText>&nbsp;</p>
<div style='mso-element:para-border-div;border:none;border-top:solid windowtext 3.0pt;
padding:1.0pt 0in 0in 0in'>
<table>
<tr>
<td><h3>Состояние маршрута | <span class=MsoBodyText style="font-weight:bold;">Задание: <%=sRouteStatus%>&nbsp</span></h3></td>
<%
if (!sRouteCurrentLevel.equals(""))
{
%>
<td><h3>|Текущий № задач: <%=sRouteCurrentLevel%></h3></td>
<%
}
%>
</tr>
</table>
</div>
<table class=MsoTableGrid border=1 cellspacing=0 cellpadding=0
 style='border-collapse:collapse;border:none;mso-border-alt:solid windowtext .5pt;
 mso-yfti-tbllook:1184;mso-padding-alt:0in 5.4pt 0in 5.4pt'>
 <tr style='mso-yfti-irow:0;mso-yfti-firstrow:yes'>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>№</p>
  </td>
  <td width=145 valign=top style='width:108.9pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Ф.И.О.</p>
  </td>
  <td width=222 valign=top style='width:166.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Должность</p>
  </td>
  <td width=91 valign=top style='width:68.35pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Состояние</p>
  </td>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Итер.</p>
  </td>
  <td width=335 valign=top style='width:251.15pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Комментарий</p>
  </td>
  <td width=114 valign=top style='width:85.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Дата ответа</p>
  </td>
  <td width=126 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1><span class=MsoBodyText>Код подписи</span></p>
  </td>
  <%
//////////////////////////////////////////    Full formated Name of assignees
  for (int i=0;i<mlTasks.size();i++)
		{
		Hashtable hmTaskNode = (Hashtable)mlTasks.get(i);
		String sMemberId=(String)hmTaskNode.get("id");
		String sNodeId=(String)hmTaskNode.get("id[connection]");
		String sRouteNodeId=(String)hmTaskNode.get("attribute[Route Node ID]");
		DomainObject doMember=DomainObject.newInstance(context,sMemberId);
		String sMemberTitle = doMember.getInfo(context,"attribute[Title]");
		String sMemberFirstName = doMember.getInfo(context,"attribute[First Name]");
			String sMemberFirstNameL="";
			if (!"".equals(sMemberFirstName)&&sMemberFirstName!=null) sMemberFirstNameL=sMemberFirstName.substring(0, 1);
		String sMemberLastName = doMember.getInfo(context,"attribute[Last Name]");
/*		String sMemberMiddleName = doMember.getInfo(context,"attribute[Middle Name]");
			String sMemberMiddleNameL="";
			if (!"".equals(sMemberMiddleName)&&sMemberMiddleName!=null)  sMemberMiddleNameL=sMemberMiddleName.substring(0, 1);*/
  %>
 </tr>
 <tr style='mso-yfti-irow:1'>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=(String)hmTaskNode.get("attribute[Route Sequence]") %></p>
  </td>
  <td width=145 valign=top style='width:108.9pt;border:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sMemberFirstNameL%>. <%=sMemberLastName%></p>
  </td>
  <td width=222 valign=top style='width:166.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sMemberTitle%></p>
  </td>
 <%
//////////////////////////////////   Determining the State of tasks
			String sNodeState="Не начато";
 			String sTaskComments="";
 			String sTaskCompletion="";
 			String sTaskNodeId="";
 			String sRevision="";
 			String sId="";
 			java.util.Date taskCompletionDate = new java.util.Date();
/////////Seaarching for created Tasks to get their atributes and Current status
			for (int k=0;k<mlRouteTasks.size();k++)
			{
				Hashtable htTask =(Hashtable) mlRouteTasks.get(k);
				sTaskNodeId=(String)htTask.get("attribute[Route Node ID]");
				//String sDEBUGTaskId=(String)htTask.get("id");

				if (sRouteNodeId.equals(sTaskNodeId))
				{
					String sTaskCurrent=(String)htTask.get("current");
					sRevision=(String)htTask.get("revision");
					sId=(String)htTask.get("id");
					if (sTaskCurrent.equals("Assigned"))sNodeState="На исполнении";
					else
					{
					String sTaskStatus =(String)htTask.get("attribute[Approval Status]");
					if (sTaskStatus.equalsIgnoreCase("Approve")) sNodeState="Утвердил";
					else if (sTaskStatus.equalsIgnoreCase("Reject")) sNodeState="Отклонил";
					else if (sTaskStatus.equalsIgnoreCase("Ignore")) sNodeState="Воздержался";
					else sNodeState="";
					sTaskComments=(String)htTask.get("attribute[Comments]");
					sTaskCompletion=(String)htTask.get("attribute[Actual Completion Date]");
					if (sTaskCompletion!=null&&!sTaskCompletion.equals(""))
							{taskCompletionDate=dateformat.parse(sTaskCompletion);
							sTaskCompletion=dfShort.format(taskCompletionDate);}
					}
				}
 			}

%>
  <td width=91 valign=top style='width:68.35pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sNodeState%></p>
  </td>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sRevision%></p>
  </td>
  <td width=335 valign=top style='width:251.15pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sTaskComments%></p>
  </td>
  <td width=114 valign=top style='width:85.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sTaskCompletion%></p>
  </td>
  <td width=126 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><span class=MsoBodyText style="font-size: 8pt;"><%=sId%></span></p>
  </td>
 <%

		}
 %>
</table>

<p class=MsoBodyText>&nbsp;</p>
<div style='mso-element:para-border-div;border:none;border-top:solid windowtext 3.0pt;
padding:1.0pt 0in 0in 0in'>
<h3>История маршрута</h3>
</div>
<table class=MsoTableGrid border=1 cellspacing=0 cellpadding=0
 style='border-collapse:collapse;border:none;mso-border-alt:solid windowtext .5pt;
 mso-yfti-tbllook:1184;mso-padding-alt:0in 5.4pt 0in 5.4pt'>
 <tr style='mso-yfti-irow:0;mso-yfti-firstrow:yes'>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>№</p>
  </td>
  <td width=145 valign=top style='width:108.9pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Ф.И.О.</p>
  </td>
  <td width=222 valign=top style='width:166.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Должность</p>
  </td>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Итер.</p>
  </td>
  <td width=426 valign=top style='width:319.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Комментарий</p>
  </td>
  <td width=120 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Действие</p>
  </td>
  <td width=120 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Дата получения</p>
  </td>
  <td width=120 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Дата ответа</p>
  </td>
  <td width=126 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1><span class=MsoBodyText>Код подписи</span></p>
  </td>
 </tr>
 <%
	String sNodeState="Не начато";
	String sTaskComments="";
	String sTaskCompletion="";
	String sTaskOrigination="";
	String sTaskNodeId="";
	String sTaskStatus="";
	String sRevision="";
	String sId="";
	java.util.Date taskCompletionDate = new java.util.Date();
	java.util.Date taskOriginationDate = new java.util.Date();
 for (int i=0;i<mlPreviousTasks.size();i++)
		{
	 HashMap hmTask=(HashMap)mlPreviousTasks.get(i);
			String sNodeId=(String)hmTask.get("attribute[Route Node ID]");
			String sSequence=DomainRelationship.getAttributeValue(context, sNodeId,"Route Sequence");
		String sMemberName=(String)hmTask.get("owner");
		String sOwnerId=PersonUtil.getPersonObjectID(context,sMemberName);
		DomainObject doMember=DomainObject.newInstance(context,sOwnerId);
		String sMemberTitle = doMember.getInfo(context,"attribute[Title]");
		String sMemberFirstName = doMember.getInfo(context,"attribute[First Name]");
			String sMemberFirstNameL="";
			if (!"".equals(sMemberFirstName)&&sMemberFirstName!=null) sMemberFirstNameL=sMemberFirstName.substring(0, 1);
		String sMemberLastName = doMember.getInfo(context,"attribute[Last Name]");
/*		String sMemberMiddleName = doMember.getInfo(context,"attribute[Middle Name]");
			String sMemberMiddleNameL="";
			if (!"".equals(sMemberMiddleName)&&sMemberMiddleName!=null)  sMemberMiddleNameL=sMemberMiddleName.substring(0, 1);*/
			sRevision=(String)hmTask.get("revision");
			sId=(String)hmTask.get("id");
			sTaskComments=(String)hmTask.get("attribute[Comments]");
			sTaskStatus =(String)hmTask.get("attribute[Approval Status]");
			if (sTaskStatus.equalsIgnoreCase("Approve")) sNodeState="Утвердил";
			else if (sTaskStatus.equalsIgnoreCase("Reject")) sNodeState="Отклонил";
			else if (sTaskStatus.equalsIgnoreCase("Ignore")) sNodeState="Воздержался";
			else sNodeState="";
			sTaskComments=(String)hmTask.get("attribute[Comments]");
			sTaskCompletion=(String)hmTask.get("attribute[Actual Completion Date]");
			sTaskOrigination=(String)hmTask.get("originated");
			if (sTaskCompletion!=null&&!sTaskCompletion.equals(""))
					{taskCompletionDate=dateformat.parse(sTaskCompletion);
					sTaskCompletion=dfShort.format(taskCompletionDate);}
			if (sTaskOrigination!=null&&!sTaskOrigination.equals(""))
					{taskOriginationDate=dateformat.parse(sTaskOrigination);
					sTaskOrigination=dfShort.format(taskOriginationDate);}
 %>
 <tr style='mso-yfti-irow:1'>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sSequence%></p>
  </td>
  <td width=145 valign=top style='width:108.9pt;border:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sMemberFirstNameL%>. <%=sMemberLastName%></p>
  </td>
  <td width=222 valign=top style='width:166.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sMemberTitle%></p>
  </td>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sRevision%></p>
  </td>
  <td width=426 valign=top style='width:319.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sTaskComments%></p>
  </td>
  <td width=120 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sNodeState%></p>
  </td>
  <td width=120 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sTaskOrigination%></p>
  </td>
  <td width=120 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sTaskCompletion%></p>
  </td>
  <td width=126 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><span class=MsoBodyText style="font-size: 8pt;"><%=sId%></span></p>
  </td>
 </tr>
 <%
 }
 %>
</table>
<p></p>

<div style='mso-element:para-border-div;border:none;border-top:solid windowtext 3.0pt;
padding:1.0pt 0in 0in 0in'>
<h3>Объекты маршрута</h3>
</div>
<table class=MsoTableGrid border=1 cellspacing=0 cellpadding=0
 style='border-collapse:collapse;border:none;mso-border-alt:solid windowtext .5pt;
 mso-yfti-tbllook:1184;mso-padding-alt:0in 5.4pt 0in 5.4pt'>
 <tr style='mso-yfti-irow:0;mso-yfti-firstrow:yes'>
  <td width=145 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Имя</p>
  </td>
  <td width=222 valign=top style='width:166.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Тип</p>
  </td>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Ревизия</p>
  </td>
  <td width=426 valign=top style='width:319.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Описание</p>
  </td>
  <td width=120 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Владелец</p>
  </td>
  <td width=145 valign=top style='width:108.9pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=a1>Код Объекта</p>
  </td>
 </tr>


<%	
    
	
	List<String> lSelectors = Arrays.asList("to[Object Route].from.name",
											"to[Object Route].from.id",
											"to[Object Route].from.type",
											"to[Object Route].from.revision",
											"to[Object Route].from.description",
											"to[Object Route].from.owner"
											);
	
	String sSelectExpr = String.join("+"+EXPRDELIM+"+",lSelectors);
	
	StringList slObjectsInfo = doRoute.getInfoList(context, String.format("evaluate[%s]",sSelectExpr));
	
	for(String sObjectInfo: slObjectsInfo.toList()){
		if(null==sObjectInfo ||"".equals(sObjectInfo)){
			continue;
		}
		String[] saObjectInfo = sObjectInfo.split(EXPRDELIM);

		
		String sObjectName = saObjectInfo[0];
		
		String sObjectId = saObjectInfo[1];
		
		String sRAWObjectType = saObjectInfo[2];
		sRAWObjectType = sRAWObjectType.replaceAll(" ","_");
		String sObjectType =  EnoviaResourceBundle.getProperty(context, "Framework",
                String.format("emxFramework.Type.%s", sRAWObjectType), context.getLocale().getLanguage());
	
				
		String sObjectRevision = saObjectInfo[3];
		
		String sObjectDescription = saObjectInfo[4];
		
		String sRAWObjectOwner = saObjectInfo[5];		
		String sOwnerId=PersonUtil.getPersonObjectID(context,sRAWObjectOwner);
		DomainObject doMember=DomainObject.newInstance(context,sOwnerId);
		String sMemberTitle = doMember.getInfo(context,"attribute[Title]");
		String sMemberFirstName = doMember.getInfo(context,"attribute[First Name]");
		String sMemberFirstNameL="";
		if (!"".equals(sMemberFirstName)&&sMemberFirstName!=null) sMemberFirstNameL=sMemberFirstName.substring(0, 1);
		String sMemberLastName = doMember.getInfo(context,"attribute[Last Name]");
/*		String sMemberMiddleName = doMember.getInfo(context,"attribute[Middle Name]");
		String sMemberMiddleNameL="";
		if (!"".equals(sMemberMiddleName)&&sMemberMiddleName!=null)  sMemberMiddleNameL=sMemberMiddleName.substring(0, 1);	*/
		String sObjectOwner = String.format("%s. %s",sMemberFirstNameL,sMemberLastName);
			
	
		%>
  <tr style='mso-yfti-irow:1'>
  <td width=145 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sObjectName%></p>
  </td>
  <td width=222 valign=top style='width:166.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sObjectType%></p>
  </td>
  <td width=145 valign=top style='width:20pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sObjectRevision%></p>
  </td>
  <td width=426 valign=top style='width:319.5pt;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sObjectDescription%></p>
  </td>
  <td width=120 valign=top style='width:1.25in;border:solid windowtext 1.0pt;padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sObjectOwner%></p>
  </td>
  <td width=145 valign=top style='width:108.9pt;border:solid windowtext 1.0pt;
  padding:0in 5.4pt 0in 5.4pt'>
  <p class=MsoBodyText><%=sObjectId%></p>
  </td>
 </tr>
		<%
	}
    
	
%>

</table>

<p></p>
<table>
<td colspan=3>
<%
DomainObject doCurrentUser=DomainObject.newInstance(context,sCurrentUserId);
String sUserTitle = doCurrentUser.getInfo(context,"attribute[Title]");
String sUserFirstName = doCurrentUser.getInfo(context,"attribute[First Name]");
	String sUserFirstNameL="";
	if (!"".equals(sUserFirstName)&&sUserFirstName!=null) sUserFirstNameL=sUserFirstName.substring(0, 1);
String sUserLastName = doCurrentUser.getInfo(context,"attribute[Last Name]");
/*String sUserMiddleName = doCurrentUser.getInfo(context,"attribute[Middle Name]");
	String sUserMiddleNameL="";
	if (!"".equals(sUserMiddleName)&&sUserMiddleName!=null)  sUserMiddleNameL=sUserMiddleName.substring(0, 1);*/
%>

<p class=MsoBodyText>Распечатал: <%=sUserFirstNameL%>. <%=sUserLastName%>. Дата отчета: <%=dfShortTime.format(currentTime)%></p>
</td>
</tr>
<tr>
<td>
<p class=MsoBodyText style="width:220pt;font-size: 8pt;">Код маршрута: <%=sOID%><br />
<%
cal = Calendar.getInstance();
%>
</td>
<td class=MsoBodyText style="font-size: 8pt;"><strong>Итер</strong> - номер попытки выполнения марш. При старте каждой последующей попытки прохождения всего процесса - создаются новые итерации задач исполнителей. Значение поля говорит о том, сколько раз данная задача была выдана исполнителю</td>
<td class=MsoBodyText style="font-size: 8pt;"><strong>№</strong> - порядковый номер задачи. Задачи с одинаковым № и Итер. выдаются на исполнение одновременно. По завершению задач с одинаковым № - выдаются задачи с последующим №</td>
</tr>
</table>


</div>
</body>
</html>