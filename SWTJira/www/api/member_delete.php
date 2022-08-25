<?php

include_once "basic.php";

$project_code = get("project_code");
$task_id = get("task_id");
$user_login = get_required("user_login");

if ($task_id != null) {
    query("delete from members where task_id = $task_id and user_login = '$user_login'");
} else if ($project_code != null) {
    query("delete from members where project_code = $project_code and user_login = '$user_login'");
}
