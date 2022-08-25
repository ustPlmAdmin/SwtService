define("DS/SWTActivities/controller", ['DS/SWTUtils/api', 'DS/SWTUtils/ui', "DS/ENO6WPlugins/jQuery", "DS/SWTActivities/Chart.min"], function (api, ui, $) {
    return function () {
        if (window.singleInit != null) return;
        window.singleInit = {};

        let colors = {
            PRIVATE: 'rgb(255, 159, 64)',//orange
            IN_WORK: 'rgb(75, 192, 192)', //green
            RELEASED: 'rgb(255, 99, 132)', //red
            OBSOLETE: 'rgb(153, 102, 255)',//purple
            FROZEN: 'rgb(0,66,235)',//blue
            grey: 'rgb(201, 203, 207)',
        };

        window.config = {
            type: 'bar',
            data: {
                labels: [
                    'Monday',
                    'Tuesday',
                    'Wednesday',
                    'Thursday',
                    'Friday',
                ],
                datasets: [{
                    label: 'SWTActivity',
                    data: [
                        1,
                        3,
                        9,
                        16,
                        25,
                    ],
                }]
            },
            options: {
                responsive: false,
                title: {
                    display: false,
                    text: 'Chart'
                },
                tooltips: {
                    mode: 'index',
                    intersect: false,
                },
                hover: {
                    mode: 'nearest',
                    intersect: true
                },
                scales: {
                    xAxes: [{
                        stacked: true,
                    }],
                    yAxes: [{
                        stacked: true
                    }]
                },
                onClick: handleClick
            }
        };

        var ctx = document.getElementById('canvas').getContext('2d')
        window.myLine = window.myLine || new Chart(ctx, config)

        function handleClick(evt) {
            var activeElement = window.myLine.getElementAtEvent(evt);
            if (activeElement.length) {
                let dayIndex = activeElement[0]._index
                let events = $("#events")
                events.empty()
                events.append('<div class="panel-title">' + $("#activities").children("option:selected").text()
                    + ' at ' + config.data.labels[dayIndex] + '</div>')
                window.events[dayIndex].forEach(function (event) {
                    events.append($("<div>")
                        .addClass("panel-row")
                        .css("border-left", "10px solid " + colors[event.current])
                        .text(event["attribute[PLMEntity.V_Name]"] + " (" + event.name + ")"));
                })

                $("#panel").css("display", "inherit")
            } else {
                $("#panel").css("display", "none")
            }
        }

        let msInDay = 1000 * 60 * 60 * 24

        api.getJson("users", null, function (data) {
            window.users = data.users
            window.groups = data.groups
            ui.autocomplete("chart_user", window.users)
            ui.autocomplete("export_user", window.users)
            ui.autocomplete("export_group", window.groups)
        })

        let today = (new Date()).toISOString().split("T")[0]

        $("#chart_from").val(today)
        $("#chart_to").val(today)
        $("#export_user_from").val(today)
        $("#export_user_to").val(today)
        $("#export_group_from").val(today)
        $("#export_group_to").val(today)

        $("#chart_form").submit(function (e) {
            e.preventDefault()
            $("#panel").css("display", "none")
            let user = $("#chart_user").val()
            if (window.users.indexOf(user) === -1) {
                alert("Type valid user login")
                return;
            }
            if ($("#chart_activity").children("option:selected").prop("disabled") === true) {
                alert("Please select activity")
                return;
            }
            let from = new Date($("#chart_from").val());
            let to = new Date($("#chart_to").val());

            if (from.getTime() === to.getTime() || from.getTime() > to.getTime()) {
                alert("Please select valid date interval")
                return;
            }

            to.setHours(23);

            $("#chart_submit").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            api.getJson("user/" + $("#chart_activity").children("option:selected").val(),
                {
                    name: user,
                    from: from.getTime(),
                    to: to.getTime(),
                }, function (chart) {
                    $("#chart_submit").css('background-color', 'green').prop('disabled', false).text("Search Activities")

                    window.config.data.labels = chart.labels
                    if (chart.datasets != null)
                        chart.datasets.forEach(function (dataset) {
                            dataset.backgroundColor = colors[dataset.label]
                        })
                    window.config.data.datasets = chart.datasets
                    window.events = chart.events

                    window.myLine.update()
                }, function (e) {
                    $("#chart_submit").css('background-color', 'red').prop('disabled', false).text("Error")
                })
        })

        $("#export_user_form").submit(function (e) {
            e.preventDefault()

            let user = $("#export_user").val()
            if (window.users.indexOf(user) === -1) {
                alert("Please select user login")
                return;
            }

            let from = new Date($("#export_user_from").val());
            let to = new Date($("#export_user_to").val());

            if (from.getTime() === to.getTime() || from.getTime() > to.getTime()) {
                alert("Please select valid date interval")
                return;
            }

            to.setHours(23);
            $("#export_user_submit").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            api.download(user + ".xls", "activity_export", {
                from: from.getTime(),
                to: to.getTime(),
                name: user,
            }, function () {
                $("#export_user_submit").css('background-color', 'green').prop('disabled', false).text("Save Excel")
            }, function () {
                $("#export_user_submit").css('background-color', 'red').prop('disabled', false).text("Error")
            })
        })

        $("#export_group_form").submit(function (e) {
            e.preventDefault()

            let group = $("#export_group").val()
            if (window.groups.indexOf(group) === -1) {
                alert("Please please type group name")
                return;
            }

            let from = new Date($("#export_group_from").val());
            let to = new Date($("#export_group_to").val());

            if (from.getTime() === to.getTime() || from.getTime() > to.getTime()) {
                alert("Please select valid date interval")
                return;
            }

            to.setHours(23);
            $("#export_group_submit").css('background-color', 'gray').prop('disabled', true).text("Please wait...")
            api.download(group + ".xls", "group_activity_export", {
                from: from.getTime(),
                to: to.getTime(),
                name: group,
            }, function () {
                $("#export_group_submit").css('background-color', 'green').prop('disabled', false).text("Save Excel")
            }, function () {
                $("#export_group_submit").css('background-color', 'red').prop('disabled', false).text("Error")
            })
        })
    }
});
