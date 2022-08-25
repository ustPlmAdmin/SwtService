<?php

include_once "db.php";
include_once "mail.php";

$user_email = get_required("user_email");

$user_token = hash("sha256", random_id());

updateWhere("users", array(
    "user_token" => $user_token
), array(
    "user_login" => explode("@", $user_email)[0],
));

send_mail("m.gaiduk@sw-tech.by", "Администратор платформы", array($user_email => "Уважаемый пользователь"),
    "Вход в систему", "Для входа в систему перейдите по ссылке: <a href='http://localhost/#/projects/$user_token'>http://localhost/#/projects/$user_token</a>");
