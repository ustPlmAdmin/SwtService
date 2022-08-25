//=================================================================
// JavaScript Discussion Tree
// Version 1.0
//
// Copyright (c) 1992-2018 Dassault Systemes.
// All Rights Reserved.
// This program contains proprietary and trade secret information of MatrixOne,Inc.
// Copyright notice is precautionary only
// and does not evidence any actual or intended publication of such program
//=================================================================
// History
//-----------------------------------------------------------------
// August 1, 2001 (Version 1.0)
// - Works in IE 4.0+ and Netscape 4.x. Not fully tested in Netscape 6.0/Mozilla.
//=================================================================

//=================================================================
// Part 1: Global Constants
//=================================================================
// This data in this section may be changed in order to customize
// the tab interface.
//=================================================================

//local copy of the tree
var DIR_IMAGES = "../common/images/";

var DIR_DISC = DIR_IMAGES;
var DIR_SMALL_ICONS = DIR_IMAGES;

//images
var IMG_PLUS = DIR_DISC + "iconDiscussionArrowUp.gif";
var IMG_MINUS = DIR_DISC + "iconDiscussionArrowDown.gif"
var IMG_PUBLICREPLY = DIR_DISC + "iconActionPublicReply.gif";
var IMG_PRIVATEREPLY = DIR_DISC + "iconActionPrivateReply.gif";
var IMG_ATTACHMENTS = DIR_DISC + "iconSmallPaperclipVertical.gif";
var IMG_DELETE = DIR_DISC + "iconActionDelete.gif";
var IMG_DISC_ICON = DIR_SMALL_ICONS + "iconSmallDiscussion.gif";

//indent size in pixels
var NODE_INDENT = 19;

var PRIVATE_MESSAGE = "Private Message";
var MESSAGE = "Message";
var DISCUSSION_FLAT = "Flat";
var DISCUSSION_PUBLICMODE = "Public";


//=================================================================
// Part 2: Discussion Classes and Class Methods
//=================================================================
// This section defines the objects that control the discussion control
// and should not be modified in any way.  Doing so could cause
// the discussion control to malfunction.
//=================================================================

//-----------------------------------------------------------------
// Class jsDiscussion
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This class is the base of the discussion tree.
//
// PARAMETERS
//  strStylesheet (String) - the style sheet to use for the discussion.
//-----------------------------------------------------------------

function jsDiscussion(strStylesheet) {

   //assign stylesheet (NCZ, 8/1/02)
  this.stylesheet = strStylesheet;
  
  //the root node of the tree (NCZ, 8/1/02)
  this.root = null;
  
  //scrolling information (NCZ, 8/1/02)
  this.scrollX = 0;
  this.scrollY = 0;
  
  //map of all nodes in the tree (NCZ, 8/1/02)
  this.nodes = new Array;
  
  //frame name (NCZ, 8/1/02)
  this.displayFrame = "discussionDisplay";
  
  //user preferences (NCZ, 8/1/02)
  this.expandAll = false;
  this.messageWidth = 500;
  
  //methods (NCZ, 8/1/02)
  this.draw = _jsDiscussion_draw;
  this.drawReply = _jsDiscussion_drawReply;
  this.drawMiscImages = _jsDiscussion_drawMiscImages;
  this.drawPlusMinusImage = _jsDiscussion_drawPlusMinusImage;
  this.getScrollPosition = _jsDiscussion_getScrollPosition;
  this.refresh = _jsDiscussion_refresh;
  this.createRoot = _jsDiscussion_createRoot;
  this.setScrollPosition = _jsDiscussion_setScrollPosition;
  this.toggleExpand = _jsDiscussion_toggleExpand; 
  this.drawLoadingMessage = _jsDiscussion_drawLoadingMessage;
  this.showReply = showReply;
  
  //save local copy of the tree (NCZ, 8/1/02)
  localDiscussionTree = this;

  //Discussion Filters
  this.viewMode = "";
  this.sortMode = "";
  this.showMode = "";
  this.formName = null;
  this.boolVal = "";
  
}

//-----------------------------------------------------------------
// Method jsDiscussion.draw()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods draws the tree onto the screen.
//
// PARAMETERS
//  (none)
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_draw() {

  //create string holder (NCZ, 8/1/02)
  var d = new jsDocument;

  //write the header (NCZ, 8/1/02)
  d.writeHTMLHeader(this.stylesheet);
  d.write("<body onload=\"parent.localDiscussionTree.setScrollPosition()\">");

  //draw the root, which recursively draws the rest of the tree (NCZ, 8/1/02)
  this.drawReply(d, this.root);
  d.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr><td><img src=\"");
  d.write(IMG_SPACER);
  d.write("\" width=\"1\" height=\"1\" border=\"0\" /></td></tr></table>");
  //write the footer (NCZ, 8/1/02)
  d.writeHTMLFooter();

  //draw to the frame (NCZ, 8/1/02)
  var displayFrameWin=emxUICore.findFrame(getTopWindow(),"detailsDisplay");
  if (emxUICore.findFrame(displayFrameWin,this.displayFrame))
  {
  with (emxUICore.findFrame(displayFrameWin,this.displayFrame).document) {
    open();
    write(d);
    close();
  }
}
  else
  {
    with (document) {
        open();
        write(d);
        close();
    }
  }
}

//-----------------------------------------------------------------
// Method jsDiscussion.drawReply()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods draws an individual discussion reply.
//
// PARAMETERS
//  d (jsDocument) - the document object to write to.
//  objReply (jsReply) - node to draw image for.
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_drawReply(d, objReply) {
  
  //spacer table (NCZ, 8/1/02)
  d.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr><td><img src=\"");
  d.write(IMG_SPACER);
  d.write("\" width=\"1\" height=\"1\" border=\"0\" /></td></tr></table>");

  //begin outer table (used for background) (NCZ, 8/1/02)
  d.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\"  width=\"100%\"><tr >");
  if (this.viewMode != DISCUSSION_FLAT)//change made
  {
  this.drawMiscImages(d, objReply);
  }
  var msgType = objReply.messageType;
  //alert(msgType);
  d.write("<td>");
  d.write("<div style=\"padding: 0px 0px 0px 0px;\">");
  var tempCSS = "<div class = \"messageheader\">";
  if (msgType == PRIVATE_MESSAGE)
  {
	  
    tempCSS = "<div class = \"privatemessageheader\">";
  }
  d.write(tempCSS);

  if (this.viewMode != DISCUSSION_FLAT)//change made
  {
      NODE_INDENT = 19;
      this.drawPlusMinusImage(d, objReply);
  }
  else
  {
      NODE_INDENT = 0;
  }
  
  //determine what indent needs to be added (NCZ, 8/1/02)
  d.write("<span class=\"subject\">");
  d.write(objReply.subject);
  d.write("</span>&nbsp;<span class=\"author\">");
  d.write(objReply.author);
  d.write("&nbsp;&nbsp;");
  d.write("</span>, <span class=\"date\">");
  d.write(objReply.date);
  d.write("</span>");

  tempCSS = "<div class=\"message\">";
  if (msgType == PRIVATE_MESSAGE)
  {
    d.write("<div class=\"messageType\">");
    d.write(emxUIConstants.MESSAGE_TYPE_PRIVATE);
    d.write("</div>");
    tempCSS = "<div class=\"privatemessage\">";
  }
  d.write("</div>");
  d.write("<div style=\"padding: 0px 0px 0px 0px;\">");

  d.write(tempCSS);

  d.write(objReply.message);
  d.write("<div>");
 

  if (this.viewMode != DISCUSSION_FLAT)
  {
      if (msgType != PRIVATE_MESSAGE)
      {
          d.write("<a href=\"javascript:parent.createDiscussionReply('");
          d.write(objReply.nodeID + "','" + objReply.messageId + "', 'Public Message");
          d.write("')\"> <img align=\"absmiddle\" border=\"0\" src=\"");
          d.write(IMG_PUBLICREPLY);
          d.write("\"/></a>");

          d.write(" <a href=\"javascript:parent.createDiscussionReply('");
          d.write(objReply.nodeID + "','" + objReply.messageId + "', 'Public Message");
  d.write("')\">");
          d.write(STR_PUBLICREPLY);
          d.write("</a> &nbsp;&nbsp;");
      }
 if(this.boolVal == 'true'){
      d.write("<a href=\"javascript:parent.createDiscussionReply('");
      d.write(objReply.nodeID + "','" + objReply.messageId + "', 'Private Message");
      d.write("')\"> <img align=\"absmiddle\" border=\"0\" src=\"");
      d.write(IMG_PRIVATEREPLY);
      d.write("\"/></a>");

      d.write(" <a href=\"javascript:parent.createDiscussionReply('");
      d.write(objReply.nodeID + "','" + objReply.messageId + "', 'Private Message");
      d.write("')\">");
      d.write(STR_PRIVATEREPLY);
      d.write("</a> &nbsp;&nbsp;");
		}

  }

  if (this.formName == null)
  {
  
      d.write("<a href=\"javascript:parent.showAttachments('" + objReply.messageId +"')\">");
      d.write("<img align=\"absmiddle\" border=\"0\" src=\"");
      d.write(IMG_ATTACHMENTS);
      d.write("\"/></a>");
      d.write("<a " + (objReply.attachments === "TRUE" ? ' style="font-weight:600" ' : ""));
      d.write(" href=\"javascript:parent.showAttachments('" + objReply.messageId +"')\">");
  d.write(emxUIConstants.STR_ATTACH);
      d.write("</a>&nbsp;&nbsp;");

  // var STR_DELETE is defined in emxComponentsDiscussionTreeFrame, check to show the "delete" link
      if(objReply.showDelete == "true") 
      {
        d.write("<a href=\"javascript:parent.deleteReply('" + objReply.messageId +"')\">");
        d.write("<img align=\"absmiddle\" border=\"0\" src=\"");
        d.write(IMG_DELETE);
        d.write("\"/></a>");
        d.write("</a> <a href=\"javascript:parent.deleteReply('" + objReply.messageId +"')\">");
    d.write(emxUIConstants.STR_DELETE);
        d.write("</a>&nbsp;&nbsp;");
  }
  }

    d.write("</div>");
    d.write("</div>");
    d.write("</div>");
  //end outer table (NCZ, 8/1/02)
  d.write("</td></tr></table>");
  //fun part, draw your children! (NCZ, 8/1/02)
  if (objReply.hasChildNodes && objReply.expanded){
    if (objReply.loaded) {
      for (var i=0; i < objReply.replies.length; i++)
        this.drawReply(d, objReply.replies[i]);
    } else {
      this.drawLoadingMessage(d, objReply);
    } 
  }  
}

//-----------------------------------------------------------------
// Method jsDiscussion.drawMiscImages()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods draws extra images for the specified node.
//
// PARAMETERS
//  d (jsDocument) - the document object to write to.
//  objReply (jsReply) - node to draw for.
//  iExtra (int) - extra indent for a given level (optional).
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_drawMiscImages(d, objReply, iExtra) {

  //no indent needed if root (NCZ, 8/1/02)
  if (objReply.indent > 0) {
  
    //stop at one before indent (NCZ, 8/1/02)
    var iIndents = objReply.indent;
    
    //cycle through indents (NCZ, 8/1/02)
    d.write("<td valign=\"top\" width=\"");
    d.write(iIndents);
 //   d.write((NODE_INDENT * iIndents));//commented out
    d.write("\">");
    d.write("<img src=\"");
    d.write(IMG_SPACER)
    d.write("\" width=\"");
    d.write((NODE_INDENT * (iIndents + (iExtra ? iExtra : 0))));
    d.write("\" height=\"16\" border=\"0\" />");
    d.write("</td>");   
  }
}

//-----------------------------------------------------------------
// Method jsDiscussion.drawPlusMinusImage()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods draws a plus/minus image, or no image, depending on
// the state of the node.
//
// PARAMETERS
//  d (jsDocument) - the document object to write to.
//  objReply (jsReply) - node to draw image for.
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_drawPlusMinusImage(d, objReply) {
  
  //begin cell (NCZ, 8/1/02)
//  d.write("<td valign=\"top\" width=\"19\">");
  
  //begin link (NCZ, 8/1/02)
  if (objReply.hasChildNodes) {
    d.write("<a href=\"javascript:parent.clickPlusMinus('");
    d.write(objReply.nodeID);
    d.write("', '")
    d.write(this.sortMode);
    d.write("', '");
    d.write(this.showMode);
    d.write("')\">");
  }
  
  //nasty part, figure out which graphic to use (NCZ, 8/1/02)
  d.write("<img src=\"");
  if (objReply.hasChildNodes) {
    if (objReply.expanded){
      d.write(IMG_MINUS);
    }else{
      d.write(IMG_PLUS);
    }  
  } else{
    d.write(IMG_SPACER);
  }  
    
   d.write("\" border=\"0\" width=\"10\" height=\"10\" />");
  
  if (objReply.hasChildNodes){
    d.write("</a>&nbsp;&nbsp;");
  }  
  //close up cell
  //d.write("</td>");
}

//-----------------------------------------------------------------
// Method jsDiscussion.refresh()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods refreshes the view of the tree.
//
// PARAMETERS
//  (none)
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_refresh() {

  //save the current scroll position (NCZ, 8/1/02)
  localDiscussionTree.getScrollPosition();
  
  //redraw the tree (NCZ, 8/1/02)
  this.draw();

}

//-----------------------------------------------------------------
// Method jsDiscussion.createRoot()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods creates the root node of the discussion tree.
//
// PARAMETERS
//  strSubject (String) - the subject of the message.
//  strMessage (String) - the message text.
//  strAuthor (String) - the author of the message.
//  strDate (String) - the date of the message.
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_createRoot(strSubject, strMessage, strAuthor, strDate,strMessageID, strMessageType,showSubscription,strAttachments) {

  //set the root (NCZ, 8/1/02)
  this.root = new jsReply(strSubject, strMessage, strAuthor, strDate,strMessageID, null, null,null, strMessageType, strAttachments);
  this.root.title = strSubject;
  this.root.text = strMessage;
  this.root.messageType = strMessageType;
  this.root.showSubscription = showSubscription;
  //make sure it's expanded (NCZ, 8/1/02)
  this.root.expanded = true;
  
  //set the tree (NCZ, 8/1/02)
  this.root.tree = this;
  
  //set ID (NCZ, 8/1/02)
  this.root.nodeID = "root";
  this.nodes["root"] = this.root;

}

//-----------------------------------------------------------------
// Method jsDiscussion.toggleExpand()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods expands/collapses a node's children.
//
// PARAMETERS
//  strNodeID (String) - the nodeID of the node to act on.
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_toggleExpand(strNodeID) {

  //get the node (NCZ, 8/1/02)
  var objReply = this.nodes[strNodeID];
  
  //change the expansion (NCZ, 8/1/02)
  objReply.expanded = !objReply.expanded;
  
  //refresh the tree (NCZ, 8/1/02)
  this.refresh();

}

//-----------------------------------------------------------------
// Method jsDiscussion.getScrollPosition()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods gets the scrolling position of the window and saves it
// into a local variable.
//
// PARAMETERS
//  (none)
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_getScrollPosition() {
    if (!isIE) {
        this.scrollX = frames[this.displayFrame].pageXOffset;
        this.scrollY = frames[this.displayFrame].pageYOffset;
    }
    
    
    else {
        this.scrollX = frames[this.displayFrame].document.body.scrollLeft;
        this.scrollY = frames[this.displayFrame].document.body.scrollTop;
    }
}

//-----------------------------------------------------------------
// Method jsDiscussion.setScrollPosition()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods sets the scrolling position of the window.
//
// PARAMETERS
//  (none)
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_setScrollPosition() {
  frames[this.displayFrame].scrollTo(this.scrollX, this.scrollY);
}


//-----------------------------------------------------------------
// Method jsDiscussion.drawLoadingMessage()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods draws a loading message for a node if the node's
// children have not yet been loaded.
//
// PARAMETERS
//  d (jsDocument) - the document object to write to.
//  objReply (jsReply) - node to draw image for.
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function _jsDiscussion_drawLoadingMessage(d, objReply) {

  //spacer table (NCZ, 8/1/02)
  d.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\"><tr><td><img src=\"");
  d.write(IMG_SPACER);
  d.write("\" width=\"1\" height=\"1\" border=\"0\" /></td></tr></table>");

  //begin outer table (used for background) (NCZ, 8/1/02)
  d.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" class=\"reply\" width=\"100%\"><tr>");

  //determine what indent needs to be added (NCZ, 8/1/02)
  this.drawMiscImages(d, objReply, 2);
  d.write("<td>");
  //begin inner table (NCZ, 8/1/02)
  d.write("<table border=\"0\"><tr>");
  //begin message (NCZ, 8/1/02)
  d.write("<td nowrap><img src=\"");
  d.write(IMG_LOADING); 
  d.write("\" border=\"0\" width=\"16\" height=\"16\"></td><td nowrap class=\"loading\">");
  d.write(emxUIConstants.STR_LOADING);
  d.write("</td></tr></table>"); 

  //end outer table (NCZ, 8/1/02)
  d.write("</td></tr></table>");
}

//-----------------------------------------------------------------
// Class jsReply
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This class represents a node in the discussion tree.
//
// PARAMETERS
//  strSubject (String) - the subject of the message.
//  strMessage (String) - the message text.
//  strAuthor (String) - the author of the message.
//  strDate (String) - the date of the message.
//  strExpandURL (String) - the URL to load this node's chidren (optional).
//  strID (String) - a unique identifier to use for this node (optional).
//-----------------------------------------------------------------

function jsReply (strSubject, strMessage, strAuthor, strDate, strMessageID, showDelete, strExpandURL, strID, strMessageType, strAttachments) {

  //properties (NCZ, 8/1/02)
  this.id = strID;
  this.subject = strSubject;
  this.message = strMessage;
  this.author = strAuthor;
  this.date = strDate;
  this.expandURL = strExpandURL;
  this.messageId = strMessageID;
  this.showDelete = showDelete;
  this.messageType = strMessageType;
  this.attachments = strAttachments;

  if (strExpandURL) {
    this.hasChildNodes = true;
    this.loaded = false
  } else {
    this.hasChildNodes = false;
    this.loaded = true;
  }
  
  //node ID assigned by JavaScript object itself (NCZ, 8/1/02)
  this.nodeID = "-1";
  
  //properties of replies (NCZ, 8/1/02)
  this.replies = new Array;
  this.expanded = false;
  this.parent = null;
  this.indent = 0;
  this.tree = null;
  
  //methods
  this.addChild = _jsReply_addChild;
}


//-----------------------------------------------------------------
// Method jsReply.addChild()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods adds a reply to the current reply in the discussion.
//
// PARAMETERS
//  strSubject (String) - the subject of the message.
//  strMessage (String) - the message text.
//  strAuthor (String) - the author of the message.
//  strDate (String) - the date of the message.
//  strExpandURL (String) - the URL to load the children for this reply (optional).
//  strID (String) - a unique identifier to use for this node. (optional)
//
// RETURNS
//  The jsReply object that was created.
//-----------------------------------------------------------------
   function _jsReply_addChild(strSubject, strMessage, strAuthor, strDate, strMessageID, showDelete, strExpandURL, strID, strMessageType, showSubscription, strAttachments) {

  //check for duplicates (NCZ, 8/1/02)
  //if (this.loaded && this != this.tree.root) return;
  
  //create the new node (NCZ, 8/1/02)
  var objReply = new jsReply(strSubject, strMessage, strAuthor, strDate,strMessageID, showDelete, strExpandURL, strID, strMessageType, strAttachments);
  
  //add the child to the array (NCZ, 8/1/02)
  this.replies[this.replies.length] = objReply;
  
  //set hasChildNodes flag (NCZ, 8/1/02)
  this.hasChildNodes = true;
    this.loaded = true;
  
  //set the parent (NCZ, 8/1/02)
  objReply.parent = this;
  
  //assign the tree (NCZ, 8/1/02)
  objReply.tree = this.tree;
  
  //assign expandAll (NCZ, 8/1/02)
  objReply.expanded = this.tree.expandAll;
  
  //set the indent (NCZ, 8/1/02)
  objReply.indent = this.indent + 1;

  //assign ID (NCZ, 8/1/02)
  objReply.nodeID = (this.nodeID == "-1" ? "" : this.nodeID + "_")  + String(this.replies.length - 1);

  //place into node map (NCZ, 8/1/02)
  //this.tree.nodes[strID] = objReply;
  this.tree.nodes[objReply.nodeID] = objReply;
  
  return objReply;
}


//-----------------------------------------------------------------
// Part 3: Event Handlers
//-----------------------------------------------------------------

//-----------------------------------------------------------------
// Function clickPlusMinus()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods  is the event handler for clicking the expand/collapse
// arrow in the discussion.
//
// PARAMETERS
//  strNodeID (String) - the ID of the node to act on.
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function clickPlusMinus(strNodeID, sortMode, showMode) {
  //expand the given node (NCZ, 8/1/02)
  localDiscussionTree.toggleExpand(strNodeID);
  if (showMode == MESSAGE)
  { 
      showMode = DISCUSSION_PUBLICMODE;
  }
  //refresh the screen (NCZ, 8/1/02)
  //localDiscussionTree.refresh();
  
  //check to see if data has to be loaded (NCZ, 8/1/02)
  if (localDiscussionTree.nodes[strNodeID].hasChildNodes && localDiscussionTree.nodes[strNodeID].expanded && !localDiscussionTree.nodes[strNodeID].loaded) {
    var strURL = localDiscussionTree.nodes[strNodeID].expandURL;
    var view = document.getElementById("APPDiscussionView").value;
    strURL += (strURL.indexOf('?') > -1 ? '&amp;' : '?') + "discussionShow=" + showMode + "&discussionSort=" + sortMode + "&discussionView=" + view + "&jsTreeID=" + strNodeID;   
    if(isIE){
	  frames[localDiscussionTree.displayFrame].document.location.href = frames[localDiscussionTree.displayFrame].document.location.href.substring(0, frames[localDiscussionTree.displayFrame].document.location.href.lastIndexOf("/")+1) + strURL;
    } else {
	  frames[localDiscussionTree.displayFrame].document.location.href = strURL;
    }
  }
}

//-----------------------------------------------------------------
// Function showReply()
//-----------------------------------------------------------------
// AUTHOR(S)
//  Nicholas C. Zakas (NCZ), 8/1/01
//
// EDITOR(S)
//
// DESCRIPTION
//  This methods shows the window to allow a user to reply.
//
// PARAMETERS
//  trNodeID (String) - the ID of the node to reply to.
//
// RETURNS
//  (nothing)
//-----------------------------------------------------------------
function showReply(strNodeID,messageId) {
//alert("here in js");
  //showModalDialog("emxComponentsCreateDiscussionDialog.jsp?objectId=" + objectId,600,700);
//needs to be completed when the discussion is implemented.


}

function doFilter()
{
    //doViewAction(objMainToolbar);

    var view = document.getElementById("APPDiscussionView").value;
    var sort = document.getElementById("APPDiscussionSort").value;
    var show = document.getElementById("APPDiscussionShow").value;
    var tempVar = document.getElementById("txtTempDiscussion").value;       
    
    findFrame(parent, "discussionTreeDisplay").location.href = tempVar + "&discussionView=" + view + "&discussionSort=" + sort + "&discussionShow=" + show;	
    
}

function launchDiscussion(objid, treeId, suite)
{
    emxShowModalDialog("../common/emxTree.jsp?objectId=" + objid + "&jsTreeID=" + treeId + "&suiteKey=" + suite + "&toolbar=APPDiscussionViewToolBar",750,450,true);
}
