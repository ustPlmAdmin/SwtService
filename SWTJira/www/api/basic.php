<?php

include_once "db.php";
include_once "mail.php";

$auth_token = apache_request_headers()["Auth-Token"];

$user_login = scalar("select user_login from users where user_token = '$auth_token'");

if ($user_login == null)
    error("not auth", 401);

function getUserName($user_login)
{
    return scalar("select user_name from users where user_login = '$user_login'");
}

function send($user_login_list, $subject, $message)
{
    $users = select("select user_login, user_name from users where user_login in ('" . implode("','", $user_login_list) . "')");

    $user_emails = array();
    foreach ($users as $user)
        $user_emails[$user["user_login"] . "@sw-tech.by"] = $user["user_name"];

    send_mail($GLOBALS["user_login"] . "@sw-tech.by", getUserName($GLOBALS["user_login"]), $user_emails, $subject, $message);
}


function internal($method, $params = array())
{
    return http_post_json("https://3dspace-m001.sw-tech.by:444/internal/sw/$method", $params, array(
        "Auth-Token" => "Basic VGVzdDoxMjM="
    ));
}
