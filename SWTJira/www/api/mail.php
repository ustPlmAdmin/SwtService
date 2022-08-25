<?php

spl_autoload_register(function ($class_name) {
    include_once $class_name . '.php';
});

use PHPMailer\PHPMailer;
use PHPMailer\SMTP;

function send_mail($from_email, $from_name, $to_email_map, $subject, $message_html)
{

    $mail = new PHPMailer;
    $mail->isSMTP();
// SMTP::DEBUG_OFF = off (for production use)
// SMTP::DEBUG_CLIENT = client messages
// SMTP::DEBUG_SERVER = client and server messages
    //$mail->SMTPDebug = SMTP::DEBUG_SERVER;
    $mail->Host = 'mail.sw-tech.by';
    $mail->Port = 25;

    $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
    $mail->SMTPAuth = true;
    $mail->Username = 'm.gaiduk';
    $mail->Password = '9375ooW10';

    $mail->setFrom($from_email, $from_name);

    foreach ($to_email_map as $user_email => $user_name)
        $mail->addAddress( $user_email, $user_name);

    $mail->CharSet = 'UTF-8';
    $mail->Subject = $subject;

    $message = file_get_contents("message.html");
    $message = str_replace("__SUBJECT__", $subject, $message);
    $message = str_replace("__MESSAGE__", $message_html, $message);
    $mail->msgHTML($message);

    return $mail->send();
}