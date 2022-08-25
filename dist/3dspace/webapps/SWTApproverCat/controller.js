define("DS/SWTApproverCat/controller", ["DS/SWTUtils/api", "DS/ENO6WPlugins/jQuery", "DS/SWTUtils/ui"], function (api, $, ui) {
    return function () {
        if (window.SWTApproverCat != null) return;
        window.SWTApproverCat = {};

        $("#loading").css("display", "inherit")
        $("#data_container").css("display", "none")

        api.getJson("approve_cat_list", {}, function (data) {
            $("#loading").css("display", "none")
            $("#data_container").css("display", "inherit")
            ui.table("my_tasks", ["route_name", "task_name", "task_originated", "task_finish", "task_assigner", "task_comment", "ca_name", "ca_owner"], data.routesWithMyActiveTasks)
            ui.table("wait_tasks", ["route_name", "task_name", "task_originated", "task_finish", "task_assigner", "task_comment", "ca_name", "ca_owner"], data.routesWithWaitingTasks)
            ui.table("history", ["route_name", "task_name", "task_originated","task_finish", "task_assigner", "task_comment", "ca_name", "ca_owner"], data.routesFinishedWithMyTasks);
        })
    }
});