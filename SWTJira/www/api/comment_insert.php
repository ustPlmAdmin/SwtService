<?php

include_once "basic.php";

$task_id = get_required("task_id");
$comment_text = get_required("comment_text");

$success = insertRow("comments", array(
    "task_id" => $task_id,
    "user_login" => $user_login,
    "comment_text" => $comment_text,
    "comment_created" => time(),
));

if ($success == false)
    error("comment not inserted");

send(selectList("select distinct user_login from members where task_id = $task_id"),
    "Новый комментарий", getUserName($user_login) . ": " . $comment_text);