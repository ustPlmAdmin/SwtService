<?php

include_once "db.php";

echo json_encode(selectList("select user_login from users"));