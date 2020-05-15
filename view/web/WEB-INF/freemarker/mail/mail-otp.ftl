<#assign ftlDateTime = .now>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>Email</title>

    <style type="text/css">
        @import url('https://fonts.googleapis.com/css?family=Roboto:100,300,300i,400,400i,500,700,900');

        body {
            background: #f1f1f1;
            font-size: 14px;
            font-family: 'Roboto', sans-serif;
            color: #222;
        }

        .email-style {
            background: #f1f1f1;
            padding: 10px 5px;
        }

        .content-txt {
            font-size: 16px;
        }

        @media only screen and (max-width: 368px) {
            .content-txt {
                font-size: 14px;
            }

            .content-txt p {
                font-size: 14px;
            }
        }
    </style>

</head>

<body>

<div class="email-style"
        style="background:#f1f1f1;  font-size: 14px; padding:10px 5px; font-family: 'Roboto', sans-serif; color: #222">

    <div class="content-txt" style="font-size:16px; padding:0 15px; font-family: 'Roboto', sans-serif;">
        <div class="logo" style="width:220px; padding:15px 0 10px 0">
            <#include "../NoQApp.svg">
        </div>

        <p style="padding-bottom:15px; font-family: 'Roboto', sans-serif;"><strong>Dear ${profileName},</strong></p>
        <p style="padding-bottom:10px; font-family: 'Roboto', sans-serif;">
            To confirm your ${message}, please enter the OTP below in your screen
            <span style="padding: 15px; display: block; font-size: x-large; color: black; font-weight: bold; font-family: 'Roboto', sans-serif">${mailOTP}</span>
            Code is case sensitive. This code expires in 15 minutes.
        </p>
        <p style="padding-bottom:10px; font-family: 'Roboto', sans-serif;">
            <strong>Wondering why you got this email?</strong>
            It's sent when someone sign's up or changes a contact email address for a NoQueue account. If you didn't do this,
            don't worry. Your email address cannot be used as a contact address with NoQueue without your verification.
        </p>
        <p style="font-family: 'Roboto', sans-serif;">
            Thanks, <br/>
            <strong>NoQueue Customer Support</strong>
        </p>
        <br/><br/><br/>
    </div>

    <div class="qr-footer" style="font-size:12px; background:#dadada; padding:15px;">
        TM and Copyright &copy; 2020 NoQueue. Mumbai, Maharashtra, India.<br/>
        All Rights Reserved &nbsp; | &nbsp; <a href="${parentHost}/#/pages/privacy"
                style="color:#222; text-decoration:none">Privacy Policy</a> &nbsp; |
        &nbsp; <a href="${parentHost}/#/pages/terms" style="color:#222; text-decoration:none">Terms</a><br/>
        S:${ftlDateTime?iso("PST")}
    </div>
</div>


</body>
</html>
