define("DS/SWTGroups/controller", ['DS/SWTUtils/api', 'DS/SWTUtils/ui', "DS/ENO6WPlugins/jQuery"], function (api, ui, $) {
    return function () {
        if (window.singleInit != null) return;
        window.singleInit = {};

        function error() {
            alert("Please create session in Default space as Administrator and try it again");
        }

        $("#group_name").on("keyup", function () {
            let group_name = this.value
            if (window.group_names == null)
                $("#add_group_button").css('background-color', 'green').text("Add Group")
            else if (window.group_names.indexOf(group_name) === -1) {
                $("#add_group_button").css('background-color', 'green').text("Add Group")
                $("#group_title").val("")
                $("#group_description").val("")
                $("#context").val("")
            } else {
                $("#add_group_button").css('background-color', '#799dff').text("Update Group")
                window.groups.forEach(function (group) {
                    if (group.name === group_name){
                        $("#group_title").val(group.title)
                        $("#group_description").val(group.description)
                        $("#context").val(group.context)
                        return;
                    }
                })
            }
        })

        function groupsUpdate() {
            $("#group_title").val("")
            $("#group_description").val("")
            $("#context").val("")
            $("#add_group_button").css('background-color', 'gray').prop('disabled', true).text("Add Group")
            $("#add_group_member_button").css('background-color', 'gray').prop('disabled', true)
            api.getJson("groups", null, function (data) {
                window.user_names = data.user_names
                ui.autocomplete("user_name", data.user_names)
                window.groups = data.groups
                window.group_names = []
                let groups = $("#groups");
                groups.empty();

                data.groups.forEach(function (group) {
                    window.group_names.push(group.name)

                    let line = $('<div class="layout-row line">')
                    let groupCell = $("<div class='layout-row flex-30 layout-wrap'>");

                    groupCell.append($("<div>").text(group.name + " " + group.title))
                    if (group.routes == null)
                        groupCell.append($("<a class='delete-link'>").text("X").on("click", function () {
                            api.getJson("del_group", {
                                "group_name": group.name,
                            }, function () {
                                groupsUpdate()
                            }, function () {
                                groupsUpdate()
                            })
                        }))
                    line.append(groupCell)
                    let usersCell = $("<div class='layout-row flex layout-wrap '>")
                    if (group.members != null) {
                        group.members.forEach(function (member) {
                            usersCell.append($("<div>").text(member));
                            usersCell.append($("<a class='delete-link'>").text("X").on("click", function () {
                                api.getJson("del_group_member", {
                                    "group_name": group.name,
                                    "user_name": member,
                                }, function () {
                                    groupsUpdate()
                                }, function () {
                                    groupsUpdate()
                                })
                            }))
                        })
                    }
                    line.append(usersCell)
                    groups.append(line)
                })
                ui.autocomplete("group_name", window.group_names)
                ui.autocomplete("group_name_for_member", window.group_names)

                $("#context").children("option:enabled").remove();
                data.contexts.forEach(function (context) {
                    $("#context").append($("<option>").attr('value', context).text(context))
                })

                $("#add_group_button").css('background-color', 'green').prop('disabled', false)
                $("#add_group_member_button").css('background-color', 'green').prop('disabled', false)
            }, function () {
                $("#add_group_button").css('background-color', 'red').prop('disabled', false)
                $("#add_group_member_button").css('background-color', 'red').prop('disabled', false)
            })
        }

        groupsUpdate()

        $("#add_group_form").submit(function (e) {
            e.preventDefault()

            $("#add_group_button").css('background-color', 'gray').prop('disabled', true)
            api.getJson("add_group", {
                "group_name": $("#group_name").val(),
                "group_title": $("#group_title").val(),
                "group_description": $("#group_description").val(),
                "context": $("#context").children("option:selected").text(),
            }, function (data) {
                $("#add_group_button").css('background-color', 'green').prop('disabled', false)
                $("#group_name").val("")
                $("#group_title").val("")
                $("#group_description").val("")
                groupsUpdate()
            }, function () {
                $("#add_group_button").css('background-color', 'red').prop('disabled', false)
                error();
            })
        })

        $("#add_member_form").submit(function (e) {
            e.preventDefault()

            let group_name = $("#group_name_for_member").val()
            if (window.group_names.indexOf(group_name) === -1) {
                alert("group_name is invalid")
                return;
            }
            let user_name = $("#user_name").val()
            if (window.user_names.indexOf(user_name) === -1) {
                alert("user_name is invalid")
                return;
            }
            $("#add_group_member_button").css('background-color', 'gray').prop('disabled', true)
            api.getJson("add_group_member", {
                "group_name": group_name,
                "user_name": user_name,
            }, function (data) {
                $("#add_group_member_button").css('background-color', 'green').prop('disabled', false)
                $("#group_name_for_member").val("")
                $("#user_name").val("")
                groupsUpdate()
            }, function () {
                $("#add_group_member_button").css('background-color', 'red').prop('disabled', false)
                error();
            })

        })

        $("#add_group_form").validate()
        $("#add_member_form").validate()
    }
});
