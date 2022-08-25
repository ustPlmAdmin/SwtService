var dCAContent;
var dCAApproval;
var URL_CREATE_CA = "../resources/cae/create";
var URL_ADD_REFERENCE_STRUCTURE = "../resources/cae/addreferencestructure";
var URL_ADD_DRAWINGS = "../resources/cae/adddrawings";
var URL_CHECK_CA = "../resources/cae/checkca";
var URL_SET_PROPERTIES = "../resources/cae/setproperties";
var URL_PROMOTE_CA = "../resources/cae/promoteca";
var URL_CA_CONTENT = "../common/emxIndentedTable.jsp?" +
    "sortColumnName=Name&SuiteDirectory=enterprisechangemgt" +
    "&showPageHeader=true&selectHandler=highlight3DCAAffectedItem" +
    "&treeLabel=Test&program=CAEChangeAction:getCAProposedAndRealizedChanges" +
    "&toolbar=CAEContentToolbar&selection=multiple" +
    "&jsTreeID=null&table=CAEAffectedItemSummary&freezePane=ProductTitle,Name&objectId=";
var URL_GET_STRUCTURE = '../resources/cae/checkstructure';
var URL_ADD_ELEMENTS_TO_CA = '../resources/cae/add';
var isChrome = /Chrome/.test(navigator.userAgent) && /Google Inc/.test(navigator.vendor);
var CHANGE_ACTION_ICON_PATH = '../common/images/iconSmallChangeAction.png';
var CHANGE_ACTION_ICON_TAG = '<img src="' + CHANGE_ACTION_ICON_PATH + '"></img>';
var EMX_TREE_HREF = '../common/emxTree.jsp?objectId=';


$(document).ready(function () {
    $.widget("ui.dialog", $.extend({}, $.ui.dialog.prototype, {
        _title: function (title) {
            if (!this.options.title) {
                title.html("&#160;");
            } else {
                title.html(this.options.title);
            }
        }
    }));

    $("#ca_estimated_start").datepicker();
    $("#ca_due_date").datepicker();

    var hrf = window.location.href;
    var src = "../common/emxVPLMLogon.jsp?op=properties&toolbar=CAEReferenceToolbar&" + hrf.substring(1 + hrf.indexOf("?"));
    $('#contentFrame').attr('src', src);

    var hgt = window.innerHeight - 20;
    var wdt = window.innerWidth - 20;
    if (!isChrome)
        $('#ca_content_frame_fs').addClass('child');

    // change action content and prd tree dialog
    dCAContent = $("#dialog_ca_content").dialog({
        autoOpen: false,
        width: wdt,
        height: hgt,
        modal: false,
        buttons: {
            "Next": function () {
                dCAContent.dialog("close");
                dCAApproval.dialog("open");
            },
            Cancel: function () {
                dCAContent.dialog("close");
            }
        },
        close: function () {
            form[0].reset();
        }
    });
    $("#dialog_ca_content").css({'overflow': 'hidden'});

    // change action properties dialog
    dCAApproval = $("#dialog_ca_approval").dialog({
        autoOpen: false,
        width: wdt,
        height: hgt,
        modal: false,
        buttons: {
            "Back": function () {
                showCAContent();
                dCAApproval.dialog("close");
            },
            "Finish": function () {
                setProperties();
            },
            Cancel: function () {
                dCAApproval.dialog("close");
            }
        },
        close: function () {
            form[0].reset();
        }
    });

    // don't know todo
    var form = dCAContent.find("form").on("submit", function (event) {
        event.preventDefault();
    });

    $('#ca_add_drawing_filter_reserved').attr('disabled', 'true');
    $('#ca_add_drawing_filter_all').attr('disabled', 'true');
});

function createCA() {
    $.ajax({
        type: "GET",
        url: URL_CHECK_CA,
        data: {
            "objectId": $("#prd_id").attr("value")
        },
        dataType: "json",
        success: function (data) {
            if (data.hasCA) {
                if (data.caCurrent != "Prepare" && data.caCurrent != "In Work" && data.caCurrent != "Cancelled") {
                    var href = EMX_TREE_HREF + data.caId;
                    showMessageWithLink("Info", "CA already started!", href, getCATextLink(data.caName, data.title, data.caCurrent));
                } else {
                    openCAForm(data);
                }
            } else if (data.type != 'Drawing' && data.type != '3DShape') {
                confirmMessage("Create CA?", "There is no active CA. Would you like to create a new one?", createChangeAction);
            } else {
                showNotice("There is no active Change Action related to this object!");
            }
        }
    });
}

function getCATextLink(caName, caTitle, caCurrent) {
    if (caTitle != undefined && caTitle != null && caTitle != '')
        caTitle = ' / ' + caTitle;
    else
        caTitle = '';
    var ca = caName + caTitle
        + ' (' + caCurrent + ')';
    return ca;
}

function createChangeAction() {
    $.ajax({
        type: "POST",
        url: URL_CREATE_CA,
        data: {
            "objectId": $("#prd_id").attr("value")
        },
        dataType: "json",
        success: function (data) {
            openCAForm(data);
            if (data.isReserved == true)
                showMessage('Warning', 'Change Action is created! There may be problems with Change Action start ' +
                    'because its content is reserved by another user!');
        },
        error: onError
    });
}

function onError(e) {
    showErrorNotice(e.responseText);
}

function add3dStructure() {
    confirmMessage("Add children?", "All child elements will be added. Are you sure?", addReferenceStructure);
}

function addReferenceStructure(mode) {
    var dataObjects = {
        "prdIds": getSelectedContentIds().join(','),
        "sCAId": $("#ca_id").attr("value"),
        mode: mode
    };
    $.ajax({
        type: "POST",
        url: URL_ADD_REFERENCE_STRUCTURE,
        data: dataObjects,
        dataType: "json",
        success: function (data) {
            if (data.Message && data.Message.indexOf('Reserved') >= 0) {
                confirmMessage('Error', data.ReservedNumber + ' elements couldn\'t be added, because they are locked by another user. Add only those, which are possible to add?',
                    addReferenceStructure, "force,lock".indexOf(mode) >= 0 ? "force,filterReserved" : "filterReserved");
            } else {
                reloadContentTable();
                reloadTree();
                if (data.Message) {
                    showNotice(data.Message);
                }
            }
        },
        error: function (data) {
            if (data.responseText.indexOf('This operation is already declared') >= 0 || data.responseText.indexOf('It is already connected ') >= 0) {
                confirmMessage('Error', 'Some of elements couldn\'t be added. Add only those, which are possible to add?',
                    addReferenceStructure, "force,lock".indexOf(mode) >= 0 ? "force,filterReserved" : "force");
            } else
                showErrorNotice(data.responseText);
        }
    });
}

function jsonToSet(json, set) {
    if (set == null || set == undefined)
        set = [];
    if (set[json.id] == null || set[json.id] == undefined) {
        set[json.id] = {
            id: json.id,
            type: json.type,
            problemType: json.problemType
        };
    }
    if (json.children != null && json.children != undefined) {
        for (var i = 0; i < json.children.length; i++)
            set = jsonToSet(json.children[i], set);
    }
    return set;
}

function openCAForm(data) {
    $('#ca_id').attr('value', data.caId);
    $('#ca_name').attr('value', data.caName);
    $('#ca_current').attr('value', data.caCurrent);

    // prd tree
    buildTree();

    //props
    var href = URL_CA_CONTENT + data.caId;
    $('#ca_content_frame').attr('src', href);
    $('#ca_title').val(data.title);
    $('#ca_description').val(data.description);
    $('#ca_due_date').val(data.estimated);
    $('#ca_estimated_start').val(data.startDate);

    $('#ca_severity_high').prop('checked', data.severity == 'High');
    $('#ca_severity_medium').prop('checked', data.severity == 'Medium');
    $('#ca_severity_low').prop('checked', data.severity == 'Low' || data.severity == null);
    $('#ca_start_now').hide();

    $('#start_type_custom').attr('selected', data.startType == 'custom');
    $('#start_type_template').attr('selected', data.startType == 'template');

    var emptyRev = !Boolean(data.revision).valueOf();
    $('#revision_empty').attr('selected', emptyRev);
    $('#revision_o').attr('selected', !emptyRev);

    var inputs = ['drw_checker', 'drw_tcontr', 'drw_lead', 'dmu_lead', 'drw_ncontr', 'drw_approver', 'route_template'];
    for (var i = 0; i < inputs.length; i++) {
        clearInput(inputs[i]);
        if (data[inputs[i] + '_ids'] && data[inputs[i]])
            addSearchElements(inputs[i], Array(data[inputs[i] + '_ids']), Array(data[inputs[i]]));
    }
    $('#drw_ncontr').val('normal_control');
    $('#drw_ncontr_ids').val('normal_control');

    loadDepartments()

    var form = $('#ca_properties_form');
    var labelsToTranslate = ['drw_checker', 'drw_tcontr', 'drw_lead', 'dmu_lead', 'revision', 'drw_ncontr', 'drw_approver'];
    for (var i = 0; i < labelsToTranslate.length; i++) {
        var labels = $('label[for=' + labelsToTranslate[i] + ']')
        if (labels.length)
            $(labels[0]).attr('title', i18n(labelsToTranslate[i]))
    }

    onStartTypeChanged()

    try {
        clearSelect('ca_followers');
        addSearchElements('ca_followers', data.followersIds.split(","), data.followers.split(","))
    } catch (e) {
    }
    showCAContent();
}

var departments = []

function loadDepartments() {
    $.ajax({
        type: "GET",
        url: '/3dspace/sw/group_select',
        success: function (data) {
            if(departments.length < 1) {
                departments = data
                data.forEach(function (group) {
                    $('#departments').append($('<option/>').attr({'value': group.name}).text(group.title))
                })
            }
        }
    });
}

function addDepartmentAsFollower() {
    var selected = $('#departments').find(':selected').val();
    departments.forEach(function (group) {
        if (group.name === selected) {
            group.members.forEach(function (member) {
                if ($('#ca_followers option[value="' + member.id + '"]').length === 0)
                    addSearchElements("ca_followers", [member.id], [member.name])
            })
        }
    })
}

function reloadContentTable() {
    $('#ca_content_frame').attr('src', $('#ca_content_frame').attr('src'));
}

function reloadTree() {
    $('#prd_structure_tree').jstree(true).refresh();
}

function setProperties() {
    confirmMessage('Start CA?', 'Start Change Action?', startCA, '', setPropertiesAction, 'Start', 'Save and close');
}

function validateForm() {
    var valid = checkLength($("#ca_title"), "Title", 3, 128);
    valid = valid && checkLength($("#ca_description"), "Description", 3, 4096);
    valid = valid && isInputFilled("ca_title");
    valid = valid && isInputFilled("ca_description");

    if (valid && $('#ca_start_now')[0].checked) {
        var startType = $('#start_type').find(':selected').attr('name');
        if (valid && 'template' === startType) {
            valid = valid && isObjectInputFilled("route_template");
        } else if (valid && 'custom' === startType) {
            valid = valid && isObjectInputFilled("drw_checker");
            valid = valid && isObjectInputFilled("drw_tcontr");
            valid = valid && isObjectInputFilled("drw_lead");
            valid = valid && isObjectInputFilled("dmu_lead");

            if (String($('#revision').find(':selected').attr('name')).length > 0) {
                valid = valid && isObjectInputFilled("drw_ncontr");
                valid = valid && isObjectInputFilled("drw_approver");
            }
        }
    }
    return valid;
}

function isInputFilled(id) {
    var object = $('#' + id);
    var isFilled = (String(object.val()).length > 0);
    object.toggleClass('ui-state-error', !isFilled);
    return isFilled;
}

function isObjectInputFilled(id) {
    return isInputFilled(id) && isInputFilled(id + '_ids');
}

function checkLength(o, n, min, max) {
    var hasProblem = (max && o.val().length > max) || o.val().length < min;
    o.toggleClass("ui-state-error", hasProblem);
    return !hasProblem
}

function setPropertiesAction(startCa) {
    $('#ca_start_now')[0].checked = Boolean(startCa).valueOf();

    if (validateForm()) {
        var sCAId = $("#ca_id").attr("value");
        var formData = $("#ca_properties_form").serialize();
        var reqData = formData + "&sCAId=" + sCAId + '&start_type=' + $('#start_type').find(':selected').attr('name');
        $.ajax({
            type: "POST",
            url: URL_SET_PROPERTIES,
            data: reqData,
            dataType: "json",
            success: function (data) {
                reloadContentTable();
                dCAApproval.dialog("close");
                if ($('#ca_start_now')[0].checked)
                    showNotice("CA Started successfully!");
                else
                    showNotice("Change Action is saved. You can start it later!");
            },
            error: onError
        });
    } else {
        showErrorNotice('Not all required fields are filled!');
    }
}

function startCA() {
    setPropertiesAction(true);
}

function popup(url, windowName) {
    var newWindow = window.open(url, windowName, 'height=630,width=850');
    if (window.focus) {
        newWindow.focus()
    }
    return false;
}

function addGroup(selectName) {
    var sURL = '../common/emxIndentedTable.jsp?selection=single&customize=false'
        + '&Export=false&multiColumnSort=false&PrinterFriendly=false&showPageURLIcon=false'
        + '&showRMB=false&showClipboard=false&objectCompare=false&submitLabel=emxFramework.Common.'
        + 'Done&cancelLabel=emxFramework.Common.Cancel&cancelButton=true&displayView=details'
        + '&table=APPGroupSummary&header=emxComponents.AddGroups.SelectGroups&program=emxGroupUt'
        + 'il%3AgetGroupSearchResults&toolbar=APPGroupSearchToolbar&memberType=Group&targetLocation=popup'
        + '&submitURL=../CAE/CAEPostGroupSearch.jsp?selectName=' + selectName;
    popup(sURL, 'Search group');
}

function addPerson(selectName, filter) {
    var sURL = '../common/emxFullSearch.jsp?field=TYPES=type_Person&table=AEFGeneralSearchResults&selection=multiple&hideHeader=true&submitURL=../CAE/CAEPostSearch.jsp?selectName=' + selectName;
    if (filter)
        sURL += '&includeOIDprogram=' + filter;
    showChooser(sURL, 850, 630);
}

function clearInput(selectName) {
    $('#' + selectName).val('');
    $('#' + selectName + "_ids").val('');
}

function addPersonAsFollower() {
    var selectName = "ca_followers";
    var followerHidden = document.getElementById(selectName + "_ids");
    var sURL = '../common/emxFullSearch.jsp?field=TYPES=type_Person&excludeOID=' + followerHidden.value + '&table=AEFGeneralSearchResults&selection=multiple&hideHeader=true&submitURL=../CAE/CAEPostSearch.jsp?selectName=' + selectName;
    showChooser(sURL, 850, 630);
}

function removeFollower() {
    removeChosenElements("ca_followers");
}

function addRouteTemplate() {
    var selectName = "route_template";
    var ReviewersHidden = document.getElementById(selectName + "_ids");
    var sURL = '../common/emxFullSearch.jsp?field=TYPES=type_RouteTemplate:ROUTE_BASE_PURPOSE=Approval:CURRENT=policy_RouteTemplate.state_Active:LATESTREVISION=TRUE&table=APPECRouteTemplateSearchList&includeOIDprogram=emxRouteTemplate:getRouteTemplateIncludeIDs&selection=single&excludeOID=' + ReviewersHidden.value + '&hideHeader=true&submitURL=../CAE/CAEPostSearch.jsp?selectName=' + selectName;
    showChooser(sURL, 850, 630);
}

function addPersonAsReviewer() {

    var selectName = "route_template";
    var followerHidden = document.getElementById(selectName + "_ids");
    var sURL = '../common/emxFullSearch.jsp?field=TYPES=type_Person&excludeOID=' + followerHidden.value + '&table=AEFGeneralSearchResults&selection=multiple&hideHeader=true&submitURL=../CAE/CAEPostSearch.jsp?selectName=' + selectName;
    showChooser(sURL, 850, 630);
}

function removeReviewer() {
    removeChosenElements("route_template");
}

function getSelectedContentIds() {
    var ids = $(frames['ca_content_frame'].contentDocument).find('#treeBodyTable').find('tr.mx_rowSelected');
    var result = [];
    for (var i = 0; i < ids.length; i++) {
        var id = ids[i].getAttribute('o');
        if (id != null && id != undefined && id != '')
            result.push(id);
    }
    return result;
}

function addDrawings() {
    confirmMessage("Add drawing?", "Drawings for the selected items will be added. Are you sure?", addDrawingsAction);
}

function addDrawingsAction(mode) {

    var dataObjects = {
        "prdIds": getSelectedContentIds().join(','),
        "sCAId": $("#ca_id").attr("value"),
        mode: mode
    };
    $.ajax({
        type: "POST",
        url: URL_ADD_DRAWINGS,
        data: dataObjects,
        dataType: "json",
        success: function (data) {
            reloadContentTable();
            reloadTree();
            if (data.Message)
                showNotice(data.Message)
        },
        error: function (data, textStatus, xhr) {
            if (data.responseText.indexOf('Reserved') >= 0)
                confirmMessage('Error', 'Some elements couldn\'t be added, because they are locked by another user. Add only those, which are possible to add?',
                    addDrawingsAction, "filterReserved");
            else
                showErrorNotice(data.responseText);
        }
    });
}

function showCAContent() {
    setTitle();
    dCAContent.dialog("open");
}

function setTitle() {
    var caName = $("#ca_name").attr("value");
    var caCurrent = $("#ca_current").attr("value");
    var caTitle = $("#ca_title").val();
    var caId = $("#ca_id").attr("value");
    var title = '<a href="' + EMX_TREE_HREF + caId + '">' + CHANGE_ACTION_ICON_TAG + '&nbsp; '
        + getCATextLink(caName, caTitle, caCurrent) + '</a>';
    dCAContent.dialog({title: title});
    dCAApproval.dialog({title: title});
}

function buildTree() {
    $('#prd_structure_tree').jstree("destroy");
    $('#prd_structure_tree').jstree({
        'core': {
            'data': {
                url: URL_GET_STRUCTURE,
                data: {
                    "sCAId": $('#ca_id').val()
                },
                "check_callback": true
            }
        },
        'checkbox': {
            'three_state': false
        },
        "contextmenu": {
            items: function (node) {
                var actions = {}
                if (node.data.showMenu == true)
                    actions.addToCA = {
                        label: 'Add to CA',
                        icon: '../common/images/iconActionAdd.png',
                        action: function (node) {
                            addElementsToCA($('#prd_structure_tree').jstree(true).get_node(node.reference).id);
                        }
                    };
                if (node.data.latest != node.data.objectId)
                    actions.addLatest = {
                        label: 'Add latest',
                        icon: '../common/images/I_ReplaceByNewPart.png',
                        action: function (node) {
                            addElementsToCA($('#prd_structure_tree').jstree(true).get_node(node.reference).data.latest);
                        }
                    };
                return actions;
            },
            select_node: false

        },
        'plugins': ['checkbox', 'contextmenu']
    });
    $("#prd_structure_tree").bind(
        "select_node.jstree", setDisabledButtons
    );
    $("#prd_structure_tree").bind(
        "deselect_node.jstree", setDisabledButtons
    );
    setDisabledButtons();
}

function setDisabledButtons() {
    var toDisableButtons = ['#button_add_elements'];
    var isDisabled = getSelectedIds().length == 0;
    for (var i = 0; i < toDisableButtons.length; i++) {
        var button = $(toDisableButtons[i]);
        if (isDisabled) {
            button.addClass('buttonDisabled');
            button.attr('onclick', null);
        } else {
            button.removeClass('buttonDisabled');
            button.attr('onclick', 'addSelectedToCA()');
        }
    }
}

function addSelectedToCA(params) {
    var elements = getSelectedIds();
    if (elements.length > 0) {
        addElementsToCA(elements.join(','));
    }
}

function addElementsToCA(ids) {
    $.ajax({
        type: "POST",
        url: URL_ADD_ELEMENTS_TO_CA,
        data: {
            "objectId": $("#prd_id").attr("value"),
            "sCAId": $("#ca_id").attr("value"),
            "ids": ids
        },
        dataType: "json",
        success: function (data, textStatus, xhr) {
            if (data.Message)
                showNotice(data.Message)
            reloadContentTable();
            reloadTree();
        },
        error: function (data, textStatus, xhr) {
            showErrorNotice(data.responseText);
        }
    });
}

function getSelectedIds() {
    return $("#prd_structure_tree").jstree("get_selected");
}

function onStartTypeChanged() {
    var selectedValue = $('#start_type').find(':selected').attr('name');
    var showRouteTemplate = false;
    var showCustomApprovers = false;

    if ('template' === selectedValue)
        showRouteTemplate = true;
    else if ('custom' === selectedValue)
        showCustomApprovers = true;

    var rt = $('.route_template_start');
    for (var i = 0; i < rt.length; i++) {
        $(rt[i]).toggleClass('hidden_row', !showRouteTemplate);
    }

    var custom = $('.custom_start');
    for (var i = 0; i < custom.length; i++) {
        $(custom[i]).toggleClass('hidden_row', !showCustomApprovers);
    }
    onRevChanged()
}

function onRevChanged() {
    var showAdditionalApprover = String($('#revision').find(':selected').attr('name')).length > 0;
    var additional = $('.additional_approver');
    for (var i = 0; i < additional.length; i++) {
        $(additional[i]).toggleClass('h_row', !showAdditionalApprover);
    }
}

function addFromTable(selectName, tableName, program, method) {
    var sURL = '../common/emxIndentedTable.jsp?selection=single&customize=false'
        + '&Export=false&multiColumnSort=false&PrinterFriendly=false&showPageURLIcon=false'
        + '&showRMB=false&showClipboard=false&objectCompare=false&submitLabel=emxFramework.Common.'
        + 'Done&cancelLabel=emxFramework.Common.Cancel&cancelButton=true&displayView=details'
        + '&table=' + tableName + '&program=' + program + ':' + method
        + '&targetLocation=popup'
        + '&submitURL=../CAE/CAEPostGroupSearch.jsp?selectName=' + selectName;
    popup(sURL, 'Search');
}