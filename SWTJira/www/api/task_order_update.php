<?php

include_once "basic.php";

$project_code = get_required("project_code");
$task_id = get_required("task_id");
$order_increment = get_required("order_increment");

$task_order = scalar("select task_order from tasks where project_code = '$project_code' and task_id = $task_id");


if ($order_increment < 0) {
    $from_order = $task_order + $order_increment;
    $to_order = $task_order;
    $order_move = 1;
} else {
    $from_order = $task_order;
    $to_order = $task_order + $order_increment;
    $order_move = -1;
}

update("update tasks set task_order = task_order + $order_move"
    . " where project_code = '$project_code'"
    . " and task_id <> $task_id"
    . " and task_order >= $from_order and task_order <= $to_order");

update("update tasks set task_order = task_order + $order_increment where project_code = '$project_code' and task_id = $task_id");
