let app = angular.module('AngularApp', [
    'ngRoute',
    'ngMaterial',
]);

function loader(scriptPath) {

    if (document.querySelector('script[src="' + scriptPath + '"]') != null)
        return null;

    return {
        load: function ($q) {
            var result = $q.defer();
            var script = document.createElement("script")
            script.async = "async"
            script.type = "text/javascript"
            script.src = scriptPath
            script.onload = script.onreadystatechange = function (_, isAbort) {
                if (!script.readyState || /loaded|complete/.test(script.readyState)) {
                    if (isAbort)
                        result.reject()
                    else
                        result.resolve()
                }
            };
            script.onerror = function () {
                result.reject()
            };
            document.head.appendChild(script)
            return result.promise
        }
    };
}

function loadScriptAsync(scriptPath, success, error) {
    let old = document.querySelector('script[src="' + scriptPath + '"]')
    if (old == null) {
        let script = document.createElement('script')
        script.async = "async"
        script.type = "text/javascript"
        script.src = scriptPath
        script.onload = success
        script.onerror = error
        document.head.appendChild(script)
    } else {
        success()
    }
}

function loadScript(scriptList, success, error) {
    if (typeof scriptList === "string")
        scriptList = [scriptList]
    for (var i = 0; i < scriptList.length; i++) {
        let old = document.querySelector('script[src="' + scriptList[i] + '"]')
        if (old == null) {
            let script = document.createElement('script')
            script.type = "text/javascript"
            script.src = scriptList[i]
            if (i === scriptList.length - 1) {
                script.onload = success
                script.onerror = error
            }
            document.head.appendChild(script)
        } else {
            success()
        }
    }
}


app.config(function ($routeProvider, $controllerProvider, $locationProvider) {
    app.register = $controllerProvider.register;
    app.routeProvider = $routeProvider;
    $locationProvider.hashPrefix('');
    $routeProvider
        .when('/', {redirectTo: '/projects'})
        .when('/projects', {
            templateUrl: 'projects/index.html',
            controller: "projects",
            resolve: loader("projects/controller.js")
        })
        .when('/projects/:user_token', {
            templateUrl: 'projects/index.html',
            controller: "projects",
            resolve: loader("projects/controller.js")
        })
        .when('/project/:project_code', {
            templateUrl: "project/index.html",
            controller: "project",
            resolve: loader("project/controller.js")
        })
        .when('/project/:project_code/task/:task_id', {
            templateUrl: "task/index.html",
            controller: "task",
            resolve: loader("task/controller.js")
        })
        .otherwise({redirectTo: '/'});
});

function controller(controllerId, callback) {
    app.register(controllerId, callback);
}

app.controller('MainController', function ($rootScope, $scope, $mdSidenav, $mdDialog, $location, $http, $routeParams) {
    $scope.header_title = "awd";

    console.log($routeParams)

    $scope.setTitle = function (title) {
        $scope.header_title = title
    }

    $scope.main_button_label = null
    $scope.main_button_icon = null
    $scope.main_button_callback = null

    $scope.mainButton = function (label, icon, callback) {
        $scope.main_button_label = label
        $scope.main_button_icon = icon
        $scope.main_button_callback = callback
    }


    $scope.task_types = {
        task: "Задача",
        bug: "Проблема",
    }

    $scope.open = function (route) {
        $location.path(route)
    };

    $scope.api = function (url, params, success, error) {
        $http({
            method: 'POST',
            url: "api/" + url,
            headers: {
                "Content-Type": "application/json",
                "Auth-Token": store.get("user_token"),
            },
            data: params
        }).then(function (response) {
            if (response.data.message != null) {
                if (error != null)
                    error(response.data.message)
            } else {
                if (success != null)
                    success(response.data)
            }
        }, function (response) {
            if (response.status === 401)
                $scope.login(url, params, success, error)
        })
    }

    $scope.user_login = store.get("user_login")

    $scope.users = users;
    $scope.login = function () {
        $mdDialog.show({
            templateUrl: 'login.html',
            scope: $scope.$new(),
            controller: function ($scope, $mdDialog) {
                $scope.user_password = ""
                $scope.sign_in_message = ""
                $scope.sing_in_pending = false
                $scope.sign_in_success = false

                $scope.user_email = ""

                $scope.isEmail = function () {
                    const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
                    return re.test(String($scope.user_email).toLowerCase());
                }

                $scope.login_button = function () {
                    $scope.sing_in_pending = true
                    $scope.sign_in_message = ""

                    $scope.api("user_login.php", {
                        user_email: $scope.user_email,
                    }, function () {
                        $scope.sing_in_pending = false
                        $scope.sign_in_success = true
                        store.set("user_login", $scope.user_email.split("@")[0])
                    })
                }
            }
        })
    }

    $scope.contacts = []

    function eng_to_rus(str) {
        str = str.toLowerCase()
        let eng = "`qwertyuiop[]asdfghjkl;'\\zxcvbnm,."
        let rus = "ёйцукенгшщзхъфывапролджэ\\ячсмитьбю"
        let result = ""
        for (let i in str) {
            let index = rus.indexOf(str[i])
            if (index === -1) {
                result += str[i]
            } else {
                result += eng[index]
            }
        }
        return result
    }

    Object.keys($scope.users).forEach(function (user_login) {
        $scope.contacts.push({
            img: "user/" + user_login + ".jpg",
            name: $scope.users[user_login],
            email: user_login + "@unitski.com",
            name_lowercase: $scope.users[user_login].toLowerCase() + eng_to_rus($scope.users[user_login]),
            user_login: user_login,
        })
    })


    $scope.login_to_contacts = function (user_login_list) {
        if (user_login_list == null) return [];
        return $scope.contacts.filter(contact => user_login_list.indexOf(contact.user_login) !== -1)
    }

    $scope.contactsSearch = function (searchText) {
        searchText = searchText.toLowerCase() || ""
        return $scope.contacts.filter(contact => contact.name_lowercase.indexOf(searchText) !== -1)
    }

    $scope.contacts_to_str = function (contacts) {
        return contacts.map(function (item) {
            return item.user_login;
        }).join(",")
    }

    $scope.user_logo = function (user_login) {
        return "user/" + user_login + ".jpg"
    }


    $scope.logout = function () {
        store.clearAll()
        document.location.reload(true)
    }

    $scope.priority_logo = function (priority) {
        if (priority == 2)
            return "priority-high.svg";
        if (priority == 1)
            return "priority-normal.svg";
        if (priority == 0)
            return "priority-low.svg";
    }

    $scope.type_logo = function (type) {
        if (type === "task")
            return "img/task.png"
        if (type === "bug")
            return "img/bug.png"
    }

    $scope.statusStyle = function (task_status) {
        var color = ""
        if (task_status === "opened")
            color = "gray"
        if (task_status === "testing")
            color = "orange"
        if (task_status === "reopened")
            color = "red"
        return {'background-color': color}
    }

    $scope.statuses = {
        "opened": "Открыт",
        "in_work": "В работе",
        "testing": "Тестироание",
        "reopened": "Переоткрыт",
        "closed": "Закрыт",
    }


});


