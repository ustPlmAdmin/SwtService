controller("project", function ($scope, $routeParams, $mdDialog) {
    $scope.project_code = $routeParams.project_code
    $scope.project = null
    $scope.opened_tasks = []
    $scope.completed_tasks = []
    $scope.tasks = []
    $scope.managers = []
    $scope.workers = []

    $scope.update = function () {
        $scope.api("project_get.php", {
            project_code: $scope.project_code,
            task_statuses: "opened,testing,closed",
        }, function (data) {
            $scope.setTitle(data.project.project_name + " - " + data.project.project_department)
            $scope.project = data.project
            $scope.tasks = data.tasks || []
            $scope.managers = data.managers || []
            $scope.workers = data.workers || []
        }, function () {
            $scope.open('projects')
        })
    }

    $scope.update()

    $scope.getManagers = function (task_id) {
        return $scope.managers.filter(member => member.task_id === task_id)
    }

    $scope.getWorkers = function (task_id) {
        return $scope.workers.filter(member => member.task_id === task_id)
    }

    $scope.getOpened = function () {
        return $scope.tasks.filter(task => task.task_status !== "closed")
    }

    $scope.getClosed = function () {
        return $scope.tasks.filter(task => task.task_status === "closed")
    }

    $scope.mainButton("Новая задача", "img/add.svg", function (ev) {

        $mdDialog.show({
            templateUrl: 'project/add_task.html',
            targetEvent: ev,
            scope: $scope.$new(),
            clickOutsideToClose: true,
            controller: function ($scope, $mdDialog) {
                $scope.title = "Создать задачу"
                $scope.managers = $scope.login_to_contacts([$scope.user_login])
                $scope.workers = []
                $scope.task_type = "task"
                $scope.task_insert_pending = false
                $scope.task_insert_success = false
                $scope.task_insert_message = ""
                $scope.task_insert = function () {
                    $scope.task_insert_message = ""
                    $scope.task_insert_pending = true
                    $scope.api("task_insert.php", {
                        project_code: $scope.project_code,
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
                        $scope.task_insert_message = "Ошибка создания"
                    })
                }

                $scope.cancel = function () {
                    $mdDialog.cancel()
                }
            }
        })
    })

    new Sortable(document.getElementById("sortlist"), {
        animation: 150,
        ghostClass: 'blue-background-class',
        onUpdate: function (event) {

            let task = $scope.tasks[event.oldIndex]
            $scope.tasks.splice(event.oldIndex, 1)
            $scope.tasks.splice(event.newIndex, 0, task)


            $scope.api("task_order_update.php", {
                project_code: $scope.project_code,
                task_id: task.task_id,
                order_increment: event.newIndex - event.oldIndex,
            }, function () {

            })
        }
    });

})