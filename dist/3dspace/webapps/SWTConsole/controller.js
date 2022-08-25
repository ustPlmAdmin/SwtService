define("DS/SWTConsole/controller", ['DS/SWTUtils/api', "DS/ENO6WPlugins/jQuery"], function (api, $) {
    return function () {
        if (window.singleInit != null) return;
        window.singleInit = {};

        api.getJson("catalogs", null, function (data) {
            $("#catalogs>option:not(:disabled)").remove()
            $(data).each(function () {
                $("#catalogs").append($("<option>").attr('value', this).text(this))
            })
        })

        let timer;

        $("#export").on("click", function () {

            if ($("#catalogs").children("option:selected").prop("disable") === true){
                alert("Please select catalog")
                return;
            }

            let catalog = $("#catalogs").children("option:selected").val();
            let start = new Date();
            if (timer != null) {
                if (!confirm("Остановить предыдущую выгрузку?")) return;
                clearInterval(timer)
            }

            timer = setInterval(function () {
                let diffTime = new Date(new Date() - start)
                $("#message").text(
                    "Generating time: " + (diffTime.getHours() - 3) +
                    ":" + (diffTime.getMinutes() < 10 ? '0' : '') + diffTime.getMinutes() +
                    ":" + (diffTime.getSeconds() < 10 ? '0' : '') + diffTime.getSeconds())
            }, 1000)

            $("#export").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            api.download(catalog + ".json", "catalog", {
                name: catalog
            }, function (data) {
                $("#export").css('background-color', 'green').prop('disabled', false).text("Export")
                clearInterval(timer)
                timer = null
            }, function () {
                $("#export").css('background-color', 'red').prop('disabled', false).text("Error")
                clearInterval(timer)
                timer = null
            })
        })
        let timer2;

        $("#object_export").on("click", function () {

            let objectName = $("#object_name").val()
            if (objectName == null){
                alert("Enter product name")
                return;
            }

            let start = new Date()
            timer2 = setInterval(function () {
                let diffTime = new Date(new Date() - start)
                $("#object_message").text(
                    "Generating time: " + (diffTime.getHours() - 3) +
                    ":" + (diffTime.getMinutes() < 10 ? '0' : '') + diffTime.getMinutes() +
                    ":" + (diffTime.getSeconds() < 10 ? '0' : '') + diffTime.getSeconds())
            }, 1000)

            $("#object_export").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            api.download(objectName + ".json", "spec_schema", {
                name: objectName
            }, function (data) {
                $("#object_export").css('background-color', 'green').prop('disabled', false).text("Type product name")
                clearInterval(timer2)
                timer2 = null
            }, function () {
                $("#object_export").css('background-color', 'red').prop('disabled', false).text("Error")
                clearInterval(timer2)
                timer2 = null
            })
        })

        $("#ca_delete_rels").on("click", function () {

            let ca_name = $("#ca_name").val()
            if (ca_name == null){
                alert("Enter product name")
                return;
            }

            $("#ca_delete_rels").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            api.getJson("ca_delete_rels", {
                name: ca_name
            }, function (data) {
                alert("Deleted " + data.deleted);
                $("#ca_delete_rels").css('background-color', 'green').prop('disabled', false).text("Delete")
            }, function () {
                $("#ca_delete_rels").css('background-color', 'red').prop('disabled', false).text("Error")
            })
        })

        $("#ca_delete_docs").on("click", function () {

            let ca_name = $("#ca_name_delete_docs").val()
            if (ca_name == null){
                alert("Enter product name")
                return;
            }

            $("#ca_delete_docs").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            api.getJson("ca_delete_docs", {
                name: ca_name
            }, function (data) {
                alert("Deleted " + data.deleted);
                $("#ca_delete_docs").css('background-color', 'green').prop('disabled', false).text("Delete")
            }, function () {
                $("#ca_delete_docs").css('background-color', 'red').prop('disabled', false).text("Error")
            })
        })

        $("#step_prepare").on("click", function () {

            let prd_name = $("#step_prd_name").val()
            if (prd_name == null){
                alert("Enter product name")
                return;
            }

            $("#step_prepare").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            $("#step_restore").css('background-color', 'gray').prop('disabled', true)
            api.getJson("step_prepare", {
                name: prd_name
            }, function () {
                $("#step_prepare").css('background-color', 'green').prop('disabled', false).text("Подготовить")
                $("#step_restore").css('background-color', 'green').prop('disabled', false)
            }, function () {
                $("#step_prepare").css('background-color', 'red').prop('disabled', false).text("Error")
                $("#step_restore").css('background-color', 'green').prop('disabled', false)
            })
        })

        $("#step_restore").on("click", function () {

            let prd_name = $("#step_prd_name").val()
            if (prd_name == null){
                alert("Enter product name")
                return;
            }

            $("#step_restore").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            $("#step_prepare").css('background-color', 'gray').prop('disabled', true)
            api.getJson("step_restore", {
                name: prd_name
            }, function () {
                $("#step_restore").css('background-color', 'green').prop('disabled', false).text("Восстановить")
                $("#step_prepare").css('background-color', 'green').prop('disabled', false)
            }, function () {
                $("#step_restore").css('background-color', 'red').prop('disabled', false).text("Error")
                $("#step_prepare").css('background-color', 'green').prop('disabled', false)
            })
        })

        $("#sign_pdf").on("click", function () {

            let ca_name = $("#ca").val()
            if (ca_name == null){
                alert("Enter product name")
                return;
            }

            $("#sign_pdf").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            api.getJson("sign_pdf", {
                name: ca_name
            }, function () {
                $("#sign_pdf").css('background-color', 'green').prop('disabled', false).text("Signed")
            }, function () {
                $("#sign_pdf").css('background-color', 'red').prop('disabled', false).text("Error")
            })
        })

    }
});