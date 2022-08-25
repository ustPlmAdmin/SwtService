<?php

include_once "basic.php";

$comment_id = get_required("comment_id");

if (scalar("select user_login from comments where comment_id = $comment_id") == $user_login)
    query("delete from comments where comment_id = $comment_id");
else
    error("dont have permissions");