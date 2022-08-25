<?php

include_once "basic.php";

$task_id = get_required("task_id");
$task_status = get_required("task_status");

$statuses = array("opened" => "Открыт",
    "testing" => "Тестирование",
    "closed" => "Закрыт");

if ($statuses[$task_status] == null)
    error("code is not valid");

$old_task_status = scalar("select task_status from tasks where task_id = $task_id");

$success = update("update tasks set task_status = '$task_status' where task_id = $task_id");

if ($success == false)
    error("update fail");

send(selectList("select distinct user_login from members where task_id = $task_id"),
    "Изменен статус задачи",
    getUserName($user_login) . " изменил статус таска с " . $statuses[$old_task_status] . " на " . $statuses[$task_status]);