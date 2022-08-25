define("DS/SWTApproverPdo/controller", ["DS/SWTUtils/api", "DS/ENO6WPlugins/jQuery", "DS/SWTUtils/ui"], function (api, $, ui) {
    return function () {
        if (window.SWTApproverPdo != null) return;
        window.SWTApproverPdo = {};

        $("#loading").css("display", "inherit")
        $("#data_container").css("display", "none")

        api.getJson("approve_pdo_list", {}, function (data) {
            $("#loading").css("display", "none")
            $("#data_container").css("display", "inherit")
            ui.table("routes", ["name", "task_name", "ca_name", "route_owner", "task_originated", "task_assigner", "task_comment"], data.routes)
            ui.table("ca", ["prd_title", "ca_name", "ca_current", "ca_owner", "route_name", "route_responsible", "route_current", "route_task_comment"], data.ca)
        })
    }
});