
//=================================================================
// JavaScript explorerPanel.js
// Copyright (c) 1992-2018 Dassault Systemes.
// All Rights Reserved.
//=================================================================
// NOTE: Legacy code stay as global to avoid regression.
//       New code should be put inside of "define"
//							MM:DD:YY
//quickreview T94	  		11:05:14 	Comment RMTHandleTogglePanel
//quickreview LX6     		01:06:15 	IR-325525-3DEXPERIENCER2016 Object in list page is unselected when user click anywhere on blank area  
//quickreview LX6     		01:06:15 	IR-320132-3DEXPERIENCER2016 Deselection of object does not happen when user click on object attribute but then selection of object is enable by this action. 
//quickreview T94    		02:24:15  	FUN053186  - ENOVIA GOV TRM Single left panel for Tree and Categories
//quickreview JX5 	T94 	03:18:15	Adapt to new level of jquery-ui and jquery-dialogextend for TMC project
//quickreview ZUD      		04:24:15    IR-316230                    : R216-STP: Structure explorer disapperes after switching from thumnail view to detail view.
//quickreview JX5			05:07:15	IR-368841-3DEXPERIENCER2016x : using drag and drop to reorganize requirements, this action needs to take a lot of time.
//quickreview JX5   QYG     03:15:16    IR-428416-3DEXPERIENCER2017x Performance issue of check input on a check box on Requirement tree (CRIT TERUMO)
//quickreview QYG           05:03:16    javascript refactoring, split from RichTextEditorStructure.js
//quickreview HAT1  ZUD     05:03:16    IR-394405-3DEXPERIENCER2017x: R418-FUN053186: On Chapter strucuture view Single left panel for Tree and Categories is not displayed when user click on "Strucutre View" category.
//quickreview KIE1  ZUD     12:21:17    IR-569139-3DEXPERIENCER2018x: R420-iOS11_FUN075788: On Specification Structure view after creation of the new Object UI get distorted.
//quickreview ZUD           10:29:19    IR-676262-3DEXPERIENCER2017x: Req Management category tree gets broken after navigating through it followed by a refresh 
if(localStorage['debug.AMD']) {
	var _RMTDynatree_js = _RMTDynatree_js || 0;
	_RMTDynatree_js++;
	console.info("AMD: ENORMTCustoSB/explorerPanel.js loading " + _RMTDynatree_js + " times.");
}

define('DS/ENORMTCustoSB/explorerPanel', [
	"dummy/../../requirements/scripts/plugins/jquery.contextMenu-RMT.js", 
    'DS/ENORMTCustoSB/explorer', 
    'DS/RichEditorCusto/Util',
    'DS/ENORMTCustoSB/toolbar'], function(){
	
	if(localStorage['debug.AMD']) {
		console.info("AMD: ENORMTCustoSB/explorerPanel.js dependency loaded.");
	}
	
	// If we want to show the tree 
	if (bShowTreeExplorerAndDecorators) {
	    // If we need to create the div for the first time
	    if (!bFirstShowDynaTreeSlideContainer) {
	        createContainerForDynaTreeTimeout();

	        // Toolbar customization
	        emxUICore.instrument(emxUIToolbar, 'init', null, doCustomizeToolBarForRMT);
	        emxUICore.instrument(window, 'RefreshView', null, afterRefreshViewForRMT);
	        emxUICore.instrument(window, 'sbToolbarResize', null, afterOnResizeTimeoutForRMT);
	    }
	}

	if(!isSBEmbedded && bShowTreeExplorerAndDecorators){
		var win = getTopWindow();
		emxUICore.instrument(win, 'togglePanel', beforeTogglePanel, null);
		emxUICore.instrument(win, 'showStructureTree', beforeShowStructureTree, null);
		$(window).unload(function(){
			emxUICore.deinstrument(win, 'togglePanel');	
			emxUICore.deinstrument(win, 'showStructureTree');	
			win.jQuery(".categories LI").off("click", hideToggleIcons);
			getTopWindow().jQuery("#mydeskpanel LI").off("click", displayCategoryTree);
		});
		// ++ HAT1 ZUD: IR-394405-3DEXPERIENCER2017x:FIX
		win.jQuery(".categories LI:not(:first)[name!='li_RMTChapterStructureTreeCategory']").on("click",hideToggleIcons);
		win.jQuery(".categories LI[name='li_RMTChapterStructureTreeCategory']").on("click",showToggleIcons);
		win.jQuery(".categories LI:first[name!='li_type_Chapter']").on("click",showToggleIcons);

		function displayCategoryTree() {
			showCategoryTree();
		}
		getTopWindow().jQuery("#mydeskpanel LI").on("click", displayCategoryTree); //IR-482836-3DEXPERIENCER2018x
		// -- HAT1 ZUD: IR-394405-3DEXPERIENCER2017x:FIX
		

		// When a slide in is closed on the right or left
		emxUICore.instrument(win, 'closeSlideInPanel', function () {
			// Bug jQuery: with Firefox, need to use style.display here instead
			if (jQuery('#dynaTreeSlideContainer').length > 0 && jQuery('#dynaTreeSlideContainer')[0].style.display != 'none')
				getTopWindow().leftpos = 16;
			else
				getTopWindow().leftpos = 212;
		}, function () {
			getTopWindow().leftpos = 212;
		});
		
		$(window).unload(function(){
			emxUICore.deinstrument(win, 'closeSlideInPanel');	
		});
	}	

	emxUICore.instrument(window, 'showThumbnailsView', null, afterShowThumbnailsView);
	
	if(localStorage['debug.AMD']) {
		console.info("AMD: ENORMTCustoSB/explorerPanel.js finish.");
	}
	return {};
});

/* JS SCRIPTS for Dynatree */
//moved to RequirementAddExisting.jsp
/* - JS SCRIPTS for Dynatree - */

/** To determine if we are in the Structure Display for Requirements or Req. Spec. **/
var bShowTreeExplorerAndDecorators = false;
if (typeof urlParameters != 'undefined')
    if (urlParameters.indexOf('showTreeExplorer') >= 0) {
        bShowTreeExplorerAndDecorators = true;
    }

/* -------------------------------------- */
/* --------- DynaTree CONTAINER --------- */
/* -------------------------------------- */

var bFirstShowDynaTreeSlideContainer = false;
if (sessionStorage.getItem('show_dynaTree_RMT') == null)
    sessionStorage.setItem('show_dynaTree_RMT', 'true');

var widthContainer = 205;
var selectedEffectSideBarDyna = 'slide';
var optionsSideBarDyna = {};

function hideOrShowDynaTree() {
	
    var toSlide = '+=0';
    var timeToHide = 200;

    var dynaTreeContainer = $("#dynaTreeSlideContainer");

    var show_dynaTree_RMT = sessionStorage.getItem('show_dynaTree_RMT');
    if (show_dynaTree_RMT == 'true') {
        sessionStorage.setItem('show_dynaTree_RMT', 'false');
        toSlide = '-=' + dynaTreeContainer.width() + 'px';
    } else {
        sessionStorage.setItem('show_dynaTree_RMT', 'true');
        toSlide = '+=' + dynaTreeContainer.width() + 'px';
    }

    displayOrNoneDynaTree(true, null);

    $("#dynaTreeSlideContainer").toggle(0,
        function() {
            displayOrNoneDynaTree(false, show_dynaTree_RMT);
        });

    $("#mainSBForRMT").css('margin-left', toSlide);
    $("#mx_divBody").css('left', toSlide);
    $("#mx_divThumbnailBody").css('left', toSlide);
    var isQuickChart = $('#dashBoardSlideInFrame').length?true:false;

    //manage if the quickChart slide-in is displayed
    if(isQuickChart){
    	if(show_dynaTree_RMT=='true'){
    		$("#dashBoardSlideIn").animate({
                left: $("#dashBoardSlideIn").position().left - dynaTreeContainer.width() + 'px'
            }, timeToHide - 20);
    	}else{
    		$("#dashBoardSlideIn").animate({
    			left: $("#dashBoardSlideIn").position().left + dynaTreeContainer.width() + 'px'
            }, timeToHide - 20);
    	}    	
    }
};

function displayOrNoneDynaTree(bStart, bShowDynaTree) {
    var jSideBarSBForRMT = $('#sideBarSBForRMT');
    if (jSideBarSBForRMT.is(":visible") && !bStart && bShowDynaTree == 'true')
        jSideBarSBForRMT.hide();
    else
        jSideBarSBForRMT.show();
}

function createContainerForDynaTree() {
    var sbBody = $('body');
    var wrapperSB = $('<div/>', {
        id: 'wrapSBForRMT',
        css: {
            'margin': '0 auto'
        }
    }).prependTo(sbBody);

    var bodySB = $('<div/>', {
        id: 'bodySBForRMT',
        css: {
        }
    }).prependTo(wrapperSB);


    var widthContainerSpecial = 0;
    if (sessionStorage.getItem('show_dynaTree_RMT') == 'true') {
        bFirstShowDynaTreeSlideContainer = true;
        widthContainerSpecial = widthContainer;
        $('#mx_divBody').css('left', ($('#mx_divTree').position().left + widthContainer));
    }

    var sideBarSB = $('<div/>', {
        id: 'sideBarSBForRMT',
        css: {
            'width': widthContainer - 5 + 'px',
            'height': '100%',
            'position': 'absolute',
            'top': '0px',
            'left': '0px'
        }
    }).prependTo(bodySB);

    var mainSB = $('<div/>', {
        id: 'mainSBForRMT',
        css: {
            'margin-left': widthContainerSpecial + 'px'
        }
    }).prependTo(bodySB);

    $('form[name="emxTableForm"]').appendTo(mainSB);

    sideBarSB.append('<div class="toggler_dynaTree" style="width: ' + widthContainer +
        'px; height: 100%;">' +
        '<div id="dynaTreeSlideContainer" class="ui-widget-content ui-corner-all" style="width: ' +
        widthContainer + 'px; ' +
        'height: 100%; position: absolute; overflow-y: auto; overflow-x: hidden">' +
        '<div id="requirementTreeExplorer"> </div>' +
        '</div>' +
        '</div>');

    $("#dynaTreeSlideContainer").resizable({
        handles: 'e',
        minWidth: widthContainer,
        maxWidth: widthContainer * 2,
        ghost: true,
        helper: 'ui-resizable-helper',
        stop: function(event, ui) {
            var timeToHide = 200;
            var toSlide = '+=0';

            var newWidth = ui.size.width;
            var oldWidth = ui.originalSize.width;

            if (newWidth > oldWidth) {
                toSlide = '+=' + (newWidth - oldWidth) + 'px';
            } else if (newWidth < oldWidth) {
                toSlide = '-=' + (oldWidth - newWidth) + 'px';
            }

            $("#mainSBForRMT").animate({
                'margin-left': toSlide
            }, timeToHide);
            $("#mx_divBody").animate({
                left: toSlide
            }, timeToHide);
        }
    });

    if (widthContainerSpecial == 0) {
        $("#dynaTreeSlideContainer").toggle();
        // The container should be hidden
        $('#sideBarSBForRMT').css('display', 'none');
    }

    if (isSBEmbedded) {
        $("#requirementTreeExplorer").bind("contextmenu", function(e) {
            return false
        });
    }
    
    if(!isSBEmbedded) {
	getTopWindow().objStructureFancyTree.isActive = false; // no tree from BPS point of view
	getTopWindow().showTreePanel(); 
	
	// It means we are in our structure view
	if (getTopWindow().closePanel == undefined)
		getTopWindow().closePanel = function() {};
	
	getTopWindow().closePanel(); // always close BPS tree panel
	getTopWindow().jQuery("div#leftPanelMenu").css("pointer-events","auto"); //IR-529733-3DEXPERIENCER2018x
    var show_dynaTree_RMT = sessionStorage.getItem('show_dynaTree_RMT');
    if (show_dynaTree_RMT == 'true') {
		getTopWindow().jQuery("div#panelToggle").removeClass("closed");
		getTopWindow().jQuery("div#panelToggle").addClass("open");
    }
    else{
    	getTopWindow().isPanelOpen = false;
    }
    $("#dynaTreeSlideContainer").prepend(getTopWindow().jQuery("<div>").append(getTopWindow().jQuery("#togglecat").clone()).html());
    jQuery("button#catButton").switchClass("toggle-active", "toggle-inactive");
    jQuery("button#strucButton").switchClass("toggle-inactive", "toggle-active");
    $('#togglecat.toggle').css({
        'padding': '4px 5px 4px 8px',
        'background-color': '#f1f1f1',
        'margin': '0'
    });

    $('button.toggle-active, button.toggle-inactive').css({
    'border-color': '#288fd1',
    'background': 'transparent',
    'padding-left': '4px',
    'padding-right': '4px'
    });

    $('button.toggle-inactive').css("border-color", "transparent");

    $('button.toggle-active:hover,button.toggle-inactive:hover').css({
    "background-color": "#F5F6F7",
    "background-image": "linear-gradient(to bottom, #F5F6F7, #E2E4E3)",
    "border-color": "#B4B6BA"
    });

    $("button.toggle-active:active,button.toggle-inactive:active").css({
    "background-color": "#E7E8E9",
    "background-image": "linear-gradient(to bottom, #E7E8E9, #F0F1F2)",
    "border-color": "#5B5D5E"
    });
}

}

function adjustDynaTreeContainerEditMode() {
    $('#sideBarSBForRMT').css('margin-top', '-' + ($('#pageHeadDiv').height() + 25) + 'px'); // FIXME
}

function createContainerForDynaTreeTimeout() { // FIXME DO NOT USE TIMEOUT, CLEAN THE OTHERS
    if (typeof currentNodeDynaTree != 'undefined' && $('#mx_divBody').length == 0 || !($('#pageHeadDiv').height() >
        0) || !($('#mx_divTreeBody').height() > 0)) {
        setTimeout(function() {
            createContainerForDynaTreeTimeout();
        }, TIMEOUT_VALUE);
    } else {
        createContainerForDynaTree();
        loadDynaTreeScript();
        setTimeout(function (){showCategoryTree();});
    }
}


function afterRefreshViewForRMT() {
    customizeMassUpdateToolBarForRMT();
}

function afterOnResizeTimeoutForRMT() {
    preLoadDynaTree();
}

/* -------------------------------------- */
/* --------- DynaTree LOADING --------- */
/* -------------------------------------- */


function loadDynaTreeScript() {
    if (oXML == null) {
        setTimeout(function() {
            loadDynaTreeScript();
        }, TIMEOUT_VALUE);
    } else {
        preLoadDynaTree();
        syncDynaTreeWithSB();
    }
}

/* Allows the user to use selectable for SB */
var rowsSelectedForDynaTree = {};
var $lastSelected = undefined;

function selectableForSBRMT(divForDnD) {
		
    var selectableContentRows = $('#mx_divBody').find('#treeBodyTable > tbody, #bodyTable > tbody');

    if (divForDnD != null)
        selectableContentRows = divForDnD;

    if (selectableContentRows.selectable == undefined || editableTable.mode == "edit")
        return;

    /* Is the user check a box, we want to add it for selectable */
    $('#treeBodyTable :checkbox').change(function(event) {
        if (this.checked) {

            // We can extract the row ID
            $('#' + $(this).parent().parent().attr('id').replace(/\,/g, '\\,'),
                '#mx_divTableBody').addClass(
                'ui-selected');
            rowsSelectedForDynaTree[this.value.split('|')[3].replace(/\,/g, '\\,')] = true;
            selectableContentRows.selectable('refresh');

            setTimeout(function() {
                var mapCheckboxes = getCheckedCheckboxes();
                for (var entry in mapCheckboxes) {
                    var rowSelectedId = mapCheckboxes[entry].split('|')[3].replace(/\,/g,
                        '\\,');
                    $('#' + rowSelectedId, '#mx_divTableBody').addClass(
                        'ui-selected');
                    rowsSelectedForDynaTree[rowSelectedId] = true;
                    //refresh in a loop is too costful, refresh is done outside the loop
                    //selectableContentRows.selectable('refresh');
                }
            }, TIMEOUT_VALUE * 2);

        } else {
            $('#' + $(this).parent().parent().attr('id').replace(/\,/g, '\\,'),
                '#mx_divTableBody').removeClass(
                'ui-selected');
            delete rowsSelectedForDynaTree[this.value.split('|')[3].replace(/\,/g, '\\,')];
            selectableContentRows.selectable('refresh');
        }
    });

    /* Use for the shift selection */
    var augmentedSelectedCallback;
    augmentedSelectedCallback = function(selected, $element, event, ui) {
        if ($element.length) {
            $element.addClass('ui-selected');
            for (var i = 0; i < $element.length; i++) {
                // Sync selectable and the checkbox from BPS, and Dynatree
                var selectedRowId = $element[i].id.replace(/\,/g, '\\,');
                rowsSelectedForDynaTree[selectedRowId] = true;
                $('#' + selectedRowId,
                    '#mx_divBody').find(':checkbox').first().prop(
                    'checked', true).triggerHandler('click');
            }
        }
    };

    selectableContentRows.selectable({
        selected: function(event, ui) {
        	if (editableTable.mode != "edit") {
        		var column = document.elementFromPoint(event.clientX, event.clientY).getAttribute("position");
        		if(column == null){
        			column = document.elementFromPoint(event.clientX, event.clientY).parentElement.getAttribute("position");
        		}
            }
            var $selected = $(ui.selected),
                isShiftSelect = false;
            if (event.shiftKey && $lastSelected) {
                $selected.siblings('.ui-selectee').andSelf().each(function() {
                    var $element = $(this);
                    if ($element.is($selected) || $element.is($lastSelected)) {
                        isShiftSelect = !isShiftSelect;
                        augmentedSelectedCallback(ui.selected, $element, event, ui);
                    } else if (isShiftSelect) {
                        augmentedSelectedCallback(ui.selected, $element, event, ui);
                    }
                });
            } else {
                $lastSelected = $selected;

                for (var row in rowsSelectedForDynaTree) {
                    $('#' + row,
                        '#mx_divBody').find(':checkbox').first().prop(
                        'checked', false).triggerHandler('click');
                }

                // MAND for double click to edit
                $selected.addClass("notSelectableRMT");
                setTimeout(function() {
                    $selected.removeClass("notSelectableRMT");
                }, TIMEOUT_VALUE * 2);

                // Sync selectable and the checkbox from BPS, and Dynatree
                var selectedRowId = ui.selected.id.replace(/\,/g, '\\,');
//START IR-320132-3DEXPERIENCER2016 Deselection of object does not happen when user click on object attribute but then selection of object is enable by this action.                
                if(rowsSelectedForDynaTree[selectedRowId]==null){
                	rowsSelectedForDynaTree[selectedRowId] = true;
                    $('#' + selectedRowId,
                        '#mx_divBody').find(':checkbox').first().prop(
                        'checked', true).triggerHandler('click');
                }else{
                    delete rowsSelectedForDynaTree[selectedRowId];
                    $('#' + selectedRowId,
                        '#mx_divBody').find(':checkbox').first().prop(
                        'checked', false).triggerHandler('click');
                }
//END IR-320132-3DEXPERIENCER2016 Deselection of object does not happen when user click on object attribute but then selection of object is enable by this action.             
            }
        },
        unselected: function(event, ui) {
            // Sync selectable and the checkbox from BPS, and Dynatree
//START IR-325525-3DEXPERIENCER2016 Object in list page is unselected when user click anywhere on blank area        	
        	var column = document.elementFromPoint(event.clientX, event.clientY).getAttribute("position");
    		if(column == null){
    			column = document.elementFromPoint(event.clientX, event.clientY).parentElement.getAttribute("position");
    		}
    		if(column!=null){
    			var selectedRowId = ui.unselected.id.replace(/\,/g, '\\,');
                delete rowsSelectedForDynaTree[selectedRowId];
                $('#' + selectedRowId,
                    '#mx_divBody').find(':checkbox').first().prop(
                    'checked', false).triggerHandler('click');

                // if you are unselecting the last item selected, then disable 
                // shift+click selection
                if (!event.shiftKey && $(ui.unselected).is($lastSelected)) {
                    $lastSelected = undefined;
                }
    		}
//END IR-325525-3DEXPERIENCER2016 Object in list page is unselected when user click anywhere on blank area
        },
        cancel: 'a,input,.notSelectableRMT',
        filter: 'tr'
    });
}

/** Function who allows to cut paste a specific rows, whitout the need to be checked **/
function cutPasteOperationForDND(idObjectToPaste, idObjecToCut, strOperation, bCheckOnly) {
    var aPRR = emxUICore.selectNodes(oXML, "/mxRoot/rows//r[@id = '" + idObjectToPaste + "']");
    var ACRR = emxUICore.selectNodes(oXML, "/mxRoot/rows//r[@id = '" + idObjecToCut + "']");

    if (bCheckOnly)
        return RMTValidatePasteOperation(strOperation, aPRR, ACRR, bCheckOnly);

    ACRR[0].setAttribute('status', 'cut');
    if (RMTValidatePasteOperation(strOperation, aPRR,
        ACRR, bCheckOnly)) {
        cutForDynaTree(idObjecToCut);

        switch (strOperation) {
            case 'paste-as-child':
                pasteAsChildDynaTree(idObjectToPaste);
                break;
            case 'paste-below':
                pasteBelowDynaTree(idObjectToPaste);
                break;
            case 'paste-above':
                pasteAboveDynaTree(idObjectToPaste);
                break;
        }

        applyEdits();
        operationSelected = true;
        return true;
    }
    ACRR[0].setAttribute('status', '');
    return false;
}

var includes_typeArr;
function cancelDnDForObjectType(objectType, objectList) {
	
	if(includes_typeArr == undefined)
	{
		var sub_types = emxUICore.getXMLData("../requirements/RequirementUtil.jsp?mode=getSubTypes");
		var includes_type = $(sub_types).find('r').attr('include');
//		var excludes_type = $(sub_types).find('r1').attr('exclude');
		includes_typeArr = includes_type.split(",");
	}
		
    var objectsToDnD = emxUICore.selectNodes(oXML, "/mxRoot/rows//r[@type!='" + objectType +
    "' or not(@type)]");
    for (var i = 0; i < objectsToDnD.length; i++) 
    {
    	var type = objectsToDnD[i].getAttribute("type");
    	var id = objectsToDnD[i].getAttribute("o");
    	
    	if(includes_typeArr.indexOf(type) != -1 && type != 'Test Case' && type != 'PlmParameter' )
    	{
    		objectList.filter("[o='" + id + "']")
    				.prepend(
    					'<td id="DnDDecoratorForRMT" class="handleForDnDRMT" title='+dragAndDrop+' style="position:absolute; ' +
    					'border: solid #ccc 1px; border-radius: 5px; left: 5px; cursor:move;"><span class="ui-icon ui-icon-arrowthick-2-n-s">' +
    					'</span></td>');
    	}
    }
		
}

/* DnD */
function DnDForSBRMT() {
    // This feature is only available in view mode, and if sortable is available
    if (editableTable.mode == "edit" || jQuery().sortable == undefined) {
        return;
    }

    var selectedObjectHelper = null;

    // The helper during the DnD
    var fixHelperModified = function(e, tr) {
        var $originals = tr.children();
        var $helper = tr.clone();
        $helper.children().each(function(index) {
            $(this).width($originals.eq(index).width());
        });
        return $helper;
    };
    var objects = $("#treeBodyTable > tbody > tr:not(:first,.root-node)").find('div tr');
    cancelDnDForObjectType("PlmParameter", objects);

    var sortableContainer = $('#mx_divBody').find('#treeBodyTable > tbody, #bodyTable  > tbody');

    var oldPositionObject = null;
    var sortableObject = sortableContainer.sortable({
        opacity: 0.6,
        handle: '.handleForDnDRMT',
        axis: 'y',
        cursor: 'move',
        helper: fixHelperModified,
        items: '> tr:not(.root-node)',
        start: function(event, ui) {
            objectHelper = ui.helper;
            isDraggingObject = true;
            ui.placeholder.html('<td></td>');

            var idObjecToCut = ui.item[0].id;
            selectedObjectHelper = $('#' + idObjecToCut.replace(',', '\\,'), '#bodyTable');
            originalPosition = selectedObjectHelper.position();

            var listOfChildren = event.target.children;

            for (var i = 0; i < listOfChildren.length; i++) {
                if (listOfChildren[i].id == idObjecToCut) {
                    oldPositionObject = listOfChildren[i - 1].id;
                    break;
                }
            }
        },
        stop: function(event, ui) {
            isDraggingObject = false;

            var idObjecToCut = ui.item[0].id;
            var listOfChildren = event.target.children;
            var operationSelected = false;

            for (var i = 0; i < listOfChildren.length; i++) {
                if (listOfChildren[i].id == idObjecToCut) {

                    var modalDialogForPaste = $('<div id="dialog-message-dnd-paste" title="' +
                        strDndPasteOperation +
                        ' ' + $('.object', ui.item[0]).context.textContent + '">' +
                        '<p><span class="ui-icon ui-icon-circle-check" style="float: left; margin: 0 7px 50px 0;"></span>' +
                        strDndPasteLocation + ' ' + $('.object', ui.item[0]).context.textContent +
                        '\?' +
                        '</p>' +
                        '<p>' +
                        strDndPasteTarget + ' <span style="color:red">' + $('.object',
                            listOfChildren[i - 1]).context.textContent + '</span></b>.' +
                        '</p>' +
                        '</div>');

                    var idObjectToPaste = listOfChildren[i - 1].id;

                    // If the user don't move the object
                    if (idObjectToPaste == oldPositionObject) {
                        selectedObjectHelper.css('position', 'inherit');
                        oldPositionObject = null;
                        return false;
                    }

                    var buttonsjQueryHTMLEdit = {};
                    buttonsjQueryHTMLEdit[strDndPasteC] = function() {
                        cutPasteOperationForDND(idObjectToPaste, idObjecToCut,
                            'paste-as-child',
                            false);
                        $(this).dialog("close");
                    };

                    buttonsjQueryHTMLEdit[strDndPasteB] = function() {
                        cutPasteOperationForDND(idObjectToPaste, idObjecToCut,
                            'paste-below', false);
                        $(this).dialog("close");
                    };

                    modalDialogForPaste.dialog({
                        modal: true,
                        width: 340,
                        buttons: buttonsjQueryHTMLEdit,
                        close: function() {
                            try {
                                if (!operationSelected)
                                    $("#mx_divTreeBody").find('tbody').first().sortable(
                                        'cancel');
                                $(this).dialog('destroy');
                            } catch (e) {
                                // NOP
                            }
                            selectedObjectHelper.css('position', 'inherit');
                            syncAllRowsSB();
                        }
                    });
                }
            }
        },
        over: function(event, ui) {
        	// NOP
        }
    }).disableSelection();

    return sortableObject;
}

function syncAllRowsSB() {
	var treeRows = $("#treeBodyTable > tbody > tr:not(:first)");
	$("#bodyTable > tbody > tr:not(:first)").each(function(index) {
		var treeRow = treeRows[index];
		var newHeight = Math.max($(treeRow).height(), $(this).height());

		treeRow.setAttribute("height", newHeight);
		$(treeRow).css("height", newHeight); //IR-434537: always set both height and css height after BPS change in syncSB()
		this.setAttribute("height", newHeight);
		$(this).css("height", newHeight);
	});
}

/* Creates some elements related to DynaTree */
function preLoadDynaTree() {
    if ($('#contextualMenuDynaTree').length == 0) {
        var treeBody = $('#sideBarSBForRMT');
        treeBody
            .append(
                '<div class="menu-panel page"><div class="menu-content" style="height: 174px; border-radius:5px !important;">' +
                '<ul style="display:none;" id="contextualMenuDynaTree" class="contextMenu">' +
                '<li class="viewDetails"><a href="#viewDetails"><span></span><label>' +
                structureExplorerDetailsSB + '</label></a></li>' + '<li class="separator"></li>' +
                '<li class="edit"><a href="#edit"><span></span><label>' + structureExplorerEditSB +
                '</label></a></li>' + '<li class="separator"></li>' +
                '<li class="expandRichText"><a href="#expandRichText"><span></span><label>' +
                structureExplorerExpandSB + '</label></a></li>' + '<li class="separator"></li>' +
                '<li class="cut"><a href="#cut"><span></span><label>' + structureExplorerCutSB +
                '</label></a></li>' + '<li class="copy"><a href="#copy"><span></span><label>' +
                structureExplorerCopySB + '</label></a></li>' +
                '<li class="paste"><a href="#paste"><span></span><label>' + structureExplorerPasteSB +
                '</label></a></li>' + '<li class="delete"><a href="#delete"><span></span><label>' +
                structureExplorerDeleteSB + '</label></a></li>' + '</ul></div></div>');
    }

    var cssId = 'ui-dynatree-csss';
    if (!document.getElementById(cssId)) {
        var head = document.getElementsByTagName('head')[0];
        var csslink = document.createElement('link');
        csslink.id = cssId;
        csslink.rel = 'stylesheet';
        csslink.type = 'text/css';
        csslink.href = '../common/styles/emxUIDynaTree.css';
        csslink.media = 'all';
        head.appendChild(csslink);
    }

    var cssIdMenu = 'ui-dynatreeMenu-csss';
    if (!document.getElementById(cssIdMenu)) {
        var headMenu = document.getElementsByTagName('head')[0];
        var csslinkMenu = document.createElement('link');
        csslinkMenu.id = cssIdMenu;
        csslinkMenu.rel = 'stylesheet';
        csslinkMenu.type = 'text/css';
        csslinkMenu.href = '../requirements/styles/jquery-contextMenu.css';
        csslinkMenu.media = 'all';
        headMenu.appendChild(csslinkMenu);
    }
}

function syncDynaTreeWithSB() {
        $('#requirementTreeExplorer').append(
            '<div id="loadingDynaTreeGif" style="text-align: center; margin-top: 20px;">' +
            '<img src="../common/images/utilProgressGray.gif" /></div>');

        goDynaTree("#requirementTreeExplorer", null);

        emxUICore.instrument(emxEditableTable, 'refreshStructureWithOutSort', null,
            afterRefreshStructureWithOutSort);
        emxUICore.instrument(emxEditableTable, 'addToSelected', null, reloadTreeAfterAdd);
        $('#loadingDynaTreeGif').remove();
    }
    /* --- DynaTree Order for SB --- */

function beforeTogglePanel()
{
	var win = getTopWindow();
	if(win.isPanelOpen){
		win.isPanelOpen = false;
		
		// Only if we are currently showing the tree, and not the categories menu 
		if (jQuery('#catMenu', win.document).css('display') == 'none') {
			hideOrShowDynaTree();
		}
		win.closePanel();
	} else {
		win.isPanelOpen = true;
		// Only if we are currently showing the tree, and not the categories menu 
		if (jQuery('#catMenu', win.document).css('display') == 'none') {
			//show dynatree
			hideOrShowDynaTree();
			getTopWindow().jQuery("div#panelToggle").removeClass("closed");
			getTopWindow().jQuery("div#panelToggle").addClass("open");
		} else
			win.openPanel();
	}
	return false;
}

function hideToggleIcons() {
	try{
		getTopWindow().jQuery("div#togglecat").hide();
	}catch(e){}
}


// IR-676262-3DEXPERIENCER2017x ZUD
function showToggleIcons() {
	try{
		getTopWindow().jQuery("div#togglecat").show();
	}catch(e){}
}


function showCategoryTree()
{
	var win = getTopWindow();
	win.showCategoryTree();
	win.openPanel();
	win.isPanelOpen = true;
	hideOrShowDynaTree();
	sessionStorage.setItem('show_dynaTree_RMT', 'true');
	
	var slideInLeft = jQuery('#leftSlideIn', win.document);
	// It means a slide in is displayed on the left
	if (slideInLeft.css('left') == '0px') {
		jQuery('#pageContentDiv', win.document).css('left', slideInLeft.width());
		jQuery('#divExtendedHeaderContent', win.document).css('left', slideInLeft.width());
	}
}

function showStructureTree()
{
	
}

function beforeShowStructureTree()
{
	var win = getTopWindow();

	win.jQuery("button#catButton").removeClass("toggle-active");
	win.jQuery("button#catButton").addClass("toggle-inactive");
	win.jQuery("button#strucButton").removeClass("toggle-inactive");
	win.jQuery("button#strucButton").addClass("toggle-active");
	win.objStructureFancyTree.isActive = false;
	win.jQuery("div#catMenu").hide();
	win.jQuery("div#leftPanelTree").show();
	
	win.closePanel();
	win.jQuery("div#panelToggle").removeClass("closed");
	win.jQuery("div#panelToggle").addClass("open");
	win.isPanelOpen = true;
	sessionStorage.setItem('show_dynaTree_RMT', 'false');
	hideOrShowDynaTree();
	return false;
}

emxUICore.instrument(window, 'onResizeTimeout', null, afterOnResizeTimeout);
function afterOnResizeTimeout() {
	
	if(true == bShowTreeExplorerAndDecorators){ //check if the dynatree explorer is used
		
		var leftPanel = $('#leftPanelMenu', getTopWindow().document);
		if(leftPanel.length==1){//The bps left Panel is found  
			if(parseInt(leftPanel.css('left').replace('px',''))>=-195&&$('#sideBarSBForRMT').css('display')!='none'){
				//the left Panel is visible on screen so we need to hide it
				leftPanel.css('left','-193px');
				var divGrabber = $('#mx_divGrabber', getTopWindow().document);
				if(divGrabber.length==1){
					divGrabber.hide();
				}
				var pageContent = $('#pageContentDiv', getTopWindow().document);
				pageContent.css('left', '18px');
			}
		}
	}
}

/* Customization for the structure Explorer
$('#panelToggle', getTopWindow().document).click(function() {
    RMTHandleTogglePanel();
}); */

function RMTHandleTogglePanel() {
    var dashBoardSlideIn = $('#dashBoardSlideIn');
    var bodyWidth = $("#mx_divBody").width();
    var panelToggle = $('#panelToggle', getTopWindow().document);
    var className = panelToggle.attr('class');
    var leftPanelWidth = 0;
    var leftWidth = $('#leftPanelMenu', getTopWindow().document).width();
    if (className.contains('closed')) {
        leftPanelWidth = 0;
        var newSize = bodyWidth + leftWidth;
        $("#mx_divBody").width(newSize + 'px');
    } else if (className.contains('open')) {
        leftPanelWidth = leftWidth;
        var newSize = bodyWidth - leftWidth;
        $("#mx_divBody").width(newSize + 'px');
    }
    var PanelTogglelWidth = panelToggle.width();
    var left = getTopWindow().innerWidth - leftPanelWidth - PanelTogglelWidth - 575;
    dashBoardSlideIn.css({
        'left': left.toString() + 'px'
    });
}

// Hide the tree for this specific view
function afterShowThumbnailsView() 
{	
    var show_dynaTree_RMT = sessionStorage.getItem('show_dynaTree_RMT');
    if (show_dynaTree_RMT == 'false') {
        return;
    }

    var toSlide = '+=0';
    var timeToHide = 500;

    var dynaTreeContainer = $("#dynaTreeSlideContainer");

    var show_dynaTree_RMT = sessionStorage.getItem('show_dynaTree_RMT');
    if (show_dynaTree_RMT == 'true') {
        toSlide = '+=' + dynaTreeContainer.width() + 'px';
    } else {
        toSlide = '-=' + dynaTreeContainer.width() + 'px';
    }
     
    // IR-316230- Shift the Thibnail view only if offset for thumbnail is zero
 	if(0 == $("#mx_divThumbnailBody").offset().left)
 	{
 		$("#mx_divThumbnailBody").animate({
 			left: toSlide
 			}, timeToHide - 20);
 	}
}


if(localStorage['debug.AMD']) {
	console.info("AMD: ENORMTCustoSB/explorerPanel.js global finish.");
}
