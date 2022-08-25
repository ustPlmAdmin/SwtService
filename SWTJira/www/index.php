
<!DOCTYPE html>
<html ng-app="AngularApp">
<head>
    <meta charset="utf8">
    <title>Менеджер задач</title>
    <link rel="shortcut icon" href="img/favicon.png" type="image/png">
    <link rel="stylesheet" href="angular/css/angular-material.min.css">
    <link rel="stylesheet" href="angular/css/flex.css">
    <link rel="stylesheet" href="custom.css">
</head>
<body ng-controller="MainController">
<script src="angular/js/angular.min.js"></script>
<!--angular-animate for ngClass, ngHide, ngIf, ngInclude, ngMessage, ngMessages, ngModel, ngRepeat, ngShow, ngSwitch, ngView-->
<script src="angular/js/angular-animate.min.js"></script>
<!--angular-aria for ngModel, ngDisabled, ngRequired, ngChecked, ngReadonly, ngValue, ngShow, ngHide, ngDblclick, ngClick-->
<script src="angular/js/angular-aria.min.js"></script>
<!--angular-route for working with url-->
<script src="angular/js/angular-route.min.js"></script>
<!---->
<script src="store/store.legacy.min.js"></script>
<!--angular-main-controller for auto loading js scripts-->
<script src="angular/js/angular-material.min.js"></script>
<!--angular-material for using material design. Docs: https://material.angularjs.org/ -->
<script src="chart/Chart.min.js"></script>
<script src="qr/qrcode.js"></script>
<script src="sortable/Sortable.min.js"></script>

<!--angular-material for using material design. Docs: https://material.angularjs.org/ -->
<?php
include_once "api/db.php";
header("Content-type: text/html");

$users = array();
foreach (select("select user_login, user_name from users") as $row)
    $users[$row["user_login"]] = $row["user_name"];
?>

<script>
    users = <?= json_encode($users)?>
</script>

<script src="controller.js"></script>
<!--angular-material for using material design. Docs: https://material.angularjs.org/ -->


<div class="row header align-center-center">
    <img class="logo" src="img/ust-logo.png" ng-click="open('/projects')">
    <h2>{{header_title}}</h2>
    <div class="flex"></div>
    <div class="flex"></div>

    <md-button class="md-raised md-primary" ng-if="main_button_callback != null" ng-click="main_button_callback($event)" >
        <md-icon md-svg-icon="{{main_button_icon}}"></md-icon>
        {{main_button_label}}
    </md-button>

    <md-menu md-position-mode="target-right target">
        <md-button aria-label="Open demo menu" class="md-icon-button" ng-click="$mdMenu.open($event)">
            <img src="{{user_logo(user_login)}}" class="mid-user-logo" onerror="this.onerror = null; this.src = 'img/user.svg'"/>
        </md-button>
        <md-menu-content width="4">
            <md-menu-item>
                <md-button ng-click="logout()">
                    <div layout="row" flex>
                        <img src="img/logout.svg" class="header-menu-item"/>
                        <p flex>Выход</p>
                    </div>
                </md-button>
            </md-menu-item>
        </md-menu-content>
    </md-menu>
</div>
<ng-view></ng-view>
</body>
</html>
