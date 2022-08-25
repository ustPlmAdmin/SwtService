<?php

include_once "basic.php";

$task_id = get_required("task_id");
$file_hash = get_required("file_hash");

query("delete from files where task_id = $task_id and file_hash = '$file_hash'");