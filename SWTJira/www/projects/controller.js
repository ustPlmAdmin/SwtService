controller("projects", function ($scope, $routeParams, $mdDialog) {
    if ($routeParams.user_token != null)
        store.set("user_token", $routeParams.user_token)

    $scope.setTitle("Менеджер задач")

    $scope.projects = []
    $scope.members = []

    $scope.update = function () {
        $scope.api("projects_get.php", {}, function (data) {
            $scope.projects = data.projects || []
            $scope.members = data.members || []
        })
    }

    $scope.projectMembers = function (project_code) {
        return $scope.members.filter(member => member.project_code === project_code)
    }

    $scope.update()

    $scope.mainButton("Новый проект", "img/add.svg", function (ev) {
        $mdDialog.show({
            templateUrl: 'projects/add_project.html',
            targetEvent: ev,
            scope: $scope.$new(),
            clickOutsideToClose: true,
            controller: function ($scope, $mdDialog) {
                $scope.project_code = ""
                $scope.project_name = ""
                $scope.project_department = ""
                $scope.managers = $scope.login_to_contacts([$scope.user_login])
                $scope.workers = []


                $scope.project_insert_pending = false
                $scope.project_insert_success = false
                $scope.project_insert_message = ""
                $scope.project_insert = function () {
                    $scope.project_insert_message = ""
                    $scope.project_insert_pending = true

                    $scope.api("project_insert.php", {
                        project_code: $scope.project_code,
                        project_name: $scope.project_name,
                        project_department: $scope.project_department,
                        workers: $scope.contacts_to_str($scope.workers),
                        managers: $scope.contacts_to_str($scope.managers),
                    }, function (data) {
                        $scope.project_insert_pending = false
                        $scope.project_insert_success = true
                        setTimeout(function () {
                            $mdDialog.hide()
                            $scope.update()
                        }, 1000)
                    }, function () {
                        $scope.project_insert_pending = false
                        $scope.project_insert_message = "Ошибка добавления"
                    })
                }

                $scope.cancel = function () {
                    $mdDialog.cancel()
                }
            }
        })
    })

})