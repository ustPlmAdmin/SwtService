define("DS/SWTOrders/controller", ['DS/SWTUtils/api', "DS/ENO6WPlugins/jQuery"], function (api, $) {
    return function () {
        if (window.singleInit != null) return;
        window.singleInit = {};

        $("#loading").css("display", "inherit")
        $("#data_container").css("display", "none")

        api.getJson("order_tasks", {}, function (data) {
            $("#loading").css("display", "none")
            $("#data_container").css("display", "inherit")
            if (data.tasks != null) {
                data.tasks.forEach(function (item) {
                    let tr = $("<tr>");
                    tr.append($("<td>").append($("<a>").attr("href", api.url(item.task_id)).text(item.name)))
                    tr.append($("<td>").text(item.order))
                    tr.append($("<td>").text(item.title))
                    tr.append($("<td>").append($("<a>").attr("href", api.url(item.route_id)).text(item.route)))
                    tr.append($("<td>").append($("<a>").attr("href", api.url(item.result_id)).text(item.result)))
                    tr.append($("<td>").text(item.start))
                    tr.append($("<td>").text(item.finish))
                    $("#order_tasks").append(tr);
                })
            }
        })

        $("#create_route").on("click", function () {
            $("#create_route").css('background-color', 'gray').prop('disabled', true)
            api.getJson("create_routes", {}, function (data) {
                document.location.reload()
            }, function () {
                $("#create_route").css('background-color', 'red').prop('disabled', false)
            })
        })
    }
});