<?php

include_once "basic.php";

$project_code = get_required("project_code");
$task_id = get_required("task_id");
$task_title = get_required("task_title");
$task_description = get_required("task_description");
$task_days = get_required("task_days");
$task_type = get_required("task_type");
$managers = get_required("managers");
$workers = get_required("workers");

updateWhere("tasks", array(
    "task_updated" => time(),
    "task_days" => $task_days,
    "task_title" => $task_title,
    "task_description" => $task_description,
    "task_type" => $task_type,
), array(
    "project_code" => $project_code,
    "task_id" => $task_id,
));

if (strpos($user_login, $managers) === false)
    $managers .= "," . $user_login;

query("delete from members where project_code = '$project_code' and task_id = '$task_id'");

foreach (explode(",", $managers) as $manager) {
    insertRow("members", array(
        "project_code" => $project_code,
        "task_id" => $task_id,
        "user_login" => $manager,
        "member_role" => "manager",
    ));
}

foreach (explode(",", $workers) as $worker) {
    insertRow("members", array(
        "project_code" => $project_code,
        "task_id" => $task_id,
        "user_login" => $worker,
        "member_role" => "worker",
    ));
}

send(selectList("select distinct user_login from members where task_id = $task_id"),
    "Задача изменена",
    "Название: $task_title <br> "
    . " Подробности смотрите по ссылке: <a href='http://localhost/project/$project_code/task/$task_id'>http://localhost/project/$project_code/task/$task_id</a>");