<?php

include_once "basic.php";

$project_code = get_required("project_code");
$project_name = get_required("project_name");
$project_department = get_required("project_department");
$managers = get_required("managers");
$workers = get_required("workers");

$success = insertRow("projects", array(
    "project_code" => $project_code,
    "project_name" => $project_name,
    "project_department" => $project_department,
    "user_login" => $user_login,
));

if ($success == false)
    error("error inserting");

foreach (explode(",", $managers) as $manager) {
    insertRow("members", array(
        "project_code" => $project_code,
        "user_login" => $manager,
        "member_role" => "manager",
    ));
}

foreach (explode(",", $workers) as $worker) {
    insertRow("members", array(
        "project_code" => $project_code,
        "user_login" => $worker,
        "member_role" => "worker",
    ));
}

send(selectList("select distinct user_login from members where project_code = '$project_code' and task_id is null"),
    "Вас пригласили в проект",
    "Приглашаю вас в проект $project_name. <br>"
    . "Дальнейшие инструкции вы получите по ссылке: <a href='http://localhost/project/$project_code'>http://localhost/project/$project_code</a>");