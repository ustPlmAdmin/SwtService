<?php

include_once "basic.php";

$project_code = get_required("project_code");
$task_id = get("task_id");
$user_login = get_required("user_login");
$member_role = get_required("member_role");

insertRowAndGetId("members", array(
    "task_id" => $task_id,
    "project_code" => $project_code,
    "user_login" => $user_login,
    "member_role" => $member_role,
));