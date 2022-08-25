<?php

include_once "basic.php";

$task_id = get("task_id");
$product_id = get_required("product_id");
$product_name = get_required("product_name");

insertRowAndGetId("products", array(
    "task_id" => $task_id,
    "product_id" => $product_id,
    "product_name" => $product_name,
));