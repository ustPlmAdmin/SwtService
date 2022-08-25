<?php

include "basic.php";

$project_code = get_required("project_code");
$task_id = get_int("task_id");
$all = get_bool("all", false);
$task_statuses = get("task_statuses");

$response["project"] = selectRow("select * from projects where project_code = '$project_code'");
if ($response["project"] == null)
    error("project dont exist");

if ($task_statuses != null)
    $response["tasks"] = select("select * from tasks where project_code = '$project_code'"
        . " and task_status in ('" . implode("', '", explode(",", $task_statuses)) . "')"
        . " order by task_order");

if ($task_id != null) {
    $response["task"] = selectRow("select * from tasks where task_id = $task_id");
    $response["comments"] = select("select * from comments where task_id = $task_id order by comment_created desc");
    $response["files"] = select("select * from files where task_id = $task_id");
    $response["managers"] = selectList("select distinct user_login from members where project_code = '$project_code' and task_id = $task_id and member_role = 'manager'");
    $response["workers"] = selectList("select distinct user_login from members where project_code = '$project_code' and task_id = $task_id and member_role = 'worker'");
    $response["member_role"] = scalar("select member_role from members where project_code = '$project_code' and task_id = $task_id and user_login = '$user_login'");
} else {
    $response["managers"] = select("select task_id, user_login from members where project_code = '$project_code' and member_role = 'manager'");
    $response["workers"] = select("select task_id, user_login from members where project_code = '$project_code' and member_role = 'worker'");
}


echo json_encode_readable($response);