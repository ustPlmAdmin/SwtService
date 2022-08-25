<?php
error_reporting(12);

$user_email = "m.gaiduk@sw-tech.by";
$user_token = "yuyl";

include_once "mail.php";
echo 1;
echo send_mail($user_email, "Михаил Гайдук", array($user_email => "Михаил Гайдук"),
    "Вход в систему", "Для входа в систему перейдите по ссылке: <a href='http://localhost/#/projects/$user_token'>http://localhost/#/projects/$user_token</a>");

echo 1;