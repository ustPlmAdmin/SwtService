controller("task", function ($scope, $routeParams, $mdDialog, $mdToast) {
    $scope.project_code = $routeParams.project_code
    $scope.project = {}
    $scope.tasks = []

    $scope.task = null
    $scope.taskFiltered = null
    $scope.managers = []
    $scope.workers = []
    $scope.comments = []
    $scope.comment_text = ""
    $scope.files = []

    $scope.searchText = ""

    $scope.update = function (task_id) {
        if (task_id == null)
            task_id = $scope.task.task_id
        $scope.api("project_get.php", {
            project_code: $scope.project_code,
            task_id: task_id,
            task_statuses: "opened,in_work,testing,reopend,closed",
        }, function (data) {
            $scope.setTitle("Задача " + data.project.project_code + "-" + data.task.task_id)

            //history.pushState(null, null, "#/project/" + data.project.project_code + "/task/" + data.task.task_id);

            $scope.project = data.project
            data.tasks = data.tasks || []

            if ($scope.tasks.length !== data.tasks.length) {
                $scope.tasks = data.tasks
                $scope.taskFiltered = data.tasks
            }
            $scope.member_role = data.member_role
            setUpdateButton(data.member_role)
            $scope.task = data.task
            $scope.comments = data.comments || []
            $scope.managers = data.managers || []
            $scope.workers = data.workers || []
            $scope.files = data.files || []
            //set hash after change task
            setQr("http://localhost/#/project/" + $scope.project_code + "/task/" + $scope.task.task_id)
        }, function () {
            $scope.open('projects')
        })
    }

    function setUpdateButton(member_role){
        if (member_role === 'manager'){
            $scope.mainButton("Изменить задачу", "img/edit.svg", function (ev) {

                $mdDialog.show({
                    templateUrl: 'project/add_task.html',
                    targetEvent: ev,
                    scope: $scope.$new(),
                    clickOutsideToClose: true,
                    controller: function ($scope, $mdDialog) {
                        $scope.title = "Изменить задачу"

                        $scope.managers = $scope.login_to_contacts($scope.managers)
                        $scope.workers = $scope.login_to_contacts($scope.workers)
                        $scope.task_type = $scope.task.task_type
                        $scope.task_title = $scope.task.task_title
                        $scope.task_description = $scope.task.task_description
                        $scope.task_days = $scope.task.task_days

                        $scope.task_insert_pending = false
                        $scope.task_insert_success = false
                        $scope.task_insert_message = ""
                        $scope.task_insert = function () {
                            $scope.task_insert_message = ""
                            $scope.task_insert_pending = true
                            $scope.api("task_update.php", {
                                project_code: $scope.project_code,
                                task_id: $scope.task.task_id,
                                task_title: $scope.task_title,
                                task_type: $scope.task_type,
                                task_description: $scope.task_description,
                                task_days: $scope.task_days,
                                managers: $scope.contacts_to_str($scope.managers),
                                workers: $scope.contacts_to_str($scope.workers),
                            }, function (data) {
                                $scope.task_insert_pending = false
                                $scope.task_insert_success = true
                                setTimeout(function () {
                                    $mdDialog.hide()
                                    $scope.update()
                                }, 1000)
                            }, function () {
                                $scope.task_insert_pending = false
                                $scope.task_insert_message = "Ошибка изменения"
                            })
                        }

                        $scope.cancel = function () {
                            $mdDialog.cancel()
                        }
                    }
                })
            })
        }else {
            $scope.mainButton(null, null, null)
        }
    }

    function setQr(data){
        var qr = qrcode(4, "L");
        qr.addData(data);
        qr.make();
        document.getElementById('qrcode').innerHTML = qr.createImgTag();
    }

    $scope.update($routeParams.task_id)

    $scope.addComment = function () {
        var comment_text = $scope.comment_text
        $scope.comment_text = ""
        $scope.api("comment_insert.php", {
            task_id: $scope.task.task_id,
            comment_text: comment_text,
        }, function (data) {
            $scope.update()
        }, function () {
            $scope.comment_text = comment_text
        })
    }

    $scope.time_to_text = function () {
        return "2 часа назад";
    }

    $scope.delComment = function (comment_id) {
        $scope.api("comment_delete.php", {
            comment_id: comment_id,
        }, function (data) {
            $scope.update()
        }, function () {
            alert("error")
        })
    }

    $scope.changeSearch = function () {
        var search = $scope.searchText.toLowerCase();
        $scope.taskFiltered = $scope.tasks.filter(task => task.task_title.toLowerCase().indexOf(search) > -1 || task.task_id.indexOf(search) > -1)
    }

    $scope.fileUpload = function () {
        var input = document.createElement("input")
        input.setAttribute("type", "file")
        input.onchange = function () {

            var formData = new FormData();
            formData.append("userfile", input.files[0])
            formData.append("task_id", $scope.task.task_id)

            var request = new XMLHttpRequest()
            request.open("POST", "http://localhost/api/file_insert.php", true)
            request.setRequestHeader("Auth-Token", store.get("user_token"))
            request.onload = function (e) {
                setTimeout(function () {
                    $scope.update()
                }, 500)
            }
            request.send(formData)
        }
        input.click()
    }

    $scope.getFiles = function (comment_id) {
        return $scope.files.filter(file => file.comment_id === comment_id)
    }

    $scope.fileIcon = function (filename) {
        return filename.split('.').pop() + ".png";
    }

    $scope.fileName = function (filename) {
        var extension = filename.split('.').pop()
        var name = filename.substr(0, filename.length - extension.length - 1)
        name = (name.length > 10 ? name.substr(0, 10) + ".." : name)
        return name + "." + extension
    }

    $scope.fileDownload = function (file) {
        window.open("api/upload/" + file.file_hash + "/" + file.file_name, '_blank');
    }

    $scope.copyTaskLink = function () {
        $mdToast.show(
            $mdToast.simple()
                .textContent('Ссылка скопирована')
                .position('bottom right')
                .hideDelay(3000))
    }

    $scope.changeStatus = function (status) {
        $scope.api("task_status_update.php", {
            task_id: $scope.task.task_id,
            task_status: status,
        }, function () {
            $scope.update()
        })
    }



})