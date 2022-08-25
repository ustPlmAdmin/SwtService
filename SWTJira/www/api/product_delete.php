<?php

include_once "basic.php";

$task_id = get_required("task_id");
$product_id = get_required("product_id");

query("delete from products where task_id = $task_id and product_id = '$product_id'");