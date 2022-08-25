define("DS/SWTSpecExcelComp/controller", ['DS/SWTUtils/api', "DS/ENO6WPlugins/jQuery"], function (api, $) {
    return function () {
        if (window.singleInit != null) return;
        window.singleInit = {};

        $("#export").on("click", function () {
            $("#message").text("Выгрузка начата...")
            $("#export").css('background-color', 'gray').prop('disabled', true)
            let objectName = $("#object_name").val()
            api.download(objectName + ".xls", "spec_schema_xls_comp", {
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
        $("#search").on("click", function () {
            $("#text").text("Идёт поиск...")
            $("#search").css('background-color', 'gray').prop('disabled', true)
            let detail = $("#detail").val()
            api.getJson("detail_search", {
                name: detail,
            }, function (response) {
                $("#text").text("Выгружено успешно")
                $("#search").css('background-color', 'green').prop('disabled', false)
                $("#response").text(response)

            }, function () {
                $("#search").css('background-color', 'green').prop('disabled', false)
                $("#text").text("Сборки не найдены")
            })
        })
    }
});