define("DS/SWTSpecExcelCompRSW/controller", ['DS/SWTUtils/api', "DS/ENO6WPlugins/jQuery"], function (api, $) {
    return function () {
        if (window.singleInit != null) return;
        window.singleInit = {};

        $("#export").on("click", function () {
            $("#message").text("Выгрузка начата...")
            $("#export").css('background-color', 'gray').prop('disabled', true)
            let objectName = $("#object_name").val()
            api.download(objectName + ".xls", "spec_schema_xls_comp_rsw", {
                name: objectName,
                mode: $("#mode").children("option:selected").val(),
            }, function () {
                $("#message").text("Выгружено успешно")
                $("#export").css('background-color', 'green').prop('disabled', false)
            }, function () {
                $("#export").css('background-color', 'green').prop('disabled', false)
                $("#message").text("Сборка не найдена")
            })
        })
    }
});