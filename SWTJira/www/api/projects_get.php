<?php

include "basic.php";

foreach (internal("boards") as $board){
    scalar("select project_name from projects where project_name = '" . $board["name"] . "'");
}


$response["projects"] = select("select * from projects");
$response["members"] = select("select project_code, user_login from members where task_id is null");

echo json_encode_readable($response);