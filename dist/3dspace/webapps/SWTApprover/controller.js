define("DS/SWTApprover/controller", ["DS/SWTUtils/api", "DS/ENO6WPlugins/jQuery", "DS/SWTUtils/ui"], function (api, $, ui) {
    return function () {
        if (window.SWTApprover != null) return;
        window.SWTApprover = {};

        $("#loading").css("display", "inherit")
        $("#data_container").css("display", "none")

        api.getJson("approve_list", {}, function (data) {
            $("#loading").css("display", "none")
            $("#data_container").css("display", "inherit")
            ui.table("my_tasks", ["name", "caname", "caowner", "cadescription"], data.my_tasks)
            ui.table("group_tasks", ["name", "caname", "caowner", "cadescription"], data.group_tasks)
            ui.table("routes", ["name", "task", "caname", "owner", "assigner", "assigned_time"], data.routes);
        })
    }
});