<?php

include_once "basic.php";

$task_id = get_required("task_id");
$comment_id = get("comment_id");

$file_hash = hash_file("sha256", $_FILES['userfile']['tmp_name']);

$upload_dir = "upload/" . $file_hash;
mkdir($upload_dir);
$upload_file = $upload_dir . "/" . $_FILES['userfile']['name'];

if (!move_uploaded_file($_FILES['userfile']['tmp_name'], $upload_file))
    error("move error");

insertRow("files", array(
    "task_id" => $task_id,
    "comment_id" => $comment_id,
    "file_name" => $_FILES['userfile']['name'],
    "file_hash" => $file_hash,
));

