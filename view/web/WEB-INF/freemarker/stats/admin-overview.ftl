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
            <#include "../noqueue-logo-mail.svg">
        </div>

        <p style="padding-bottom:15px; font-family: 'Roboto', sans-serif;"><strong>${businessName} Daily Summary for ${day}</strong></p>
        <p style="padding-bottom:10px; font-family: 'Roboto', sans-serif;">
            <strong>Clients/Patients Visits</strong><br/>
            Clients/Patients: ${totalClient}<br/>
            Serviced: ${totalServiced}<br/>
            No Show (Skipped): ${totalNoShow}<br/>
            Abort Visit (Self Cancelled): ${totalAbort}<br/>
            New Clients/Patients: ${newCustomer}<br/>
            <#if timeOfService??>
            Time of Begin/End Service: ${timeOfService}<br/>
            </#if>
        <br/>
        Previously Visited Clients/Patients: ${clientsPreviouslyVisitedThisBusiness}<br/>
        </p>
        <p style="padding-bottom:10px; font-family: 'Roboto', sans-serif;">
            <strong>Customer Reviews for yesterday</strong><br/>
            Total Rating: ${totalRating}<br/>
            Clients/Patients Rated: ${totalCustomerRated}<br/>
            <#if averageRating??>
            Average Rating for the day: ${averageRating}<br/>
            </#if>
            Total Hour Saved for Clients/Patients: ${totalHoursSaved}hrs<br/>
            <br/>
        </p>
        <#if timeOfServices??>
        <p>
            <strong>Time of Begin/End Service:</strong><br/>
            <#if timeOfServices?has_content>
            <#list timeOfServices?keys as key>
                &nbsp;&nbsp;${key} = ${timeOfServices[key]}<br/>
            </#list>
            <#else>
                &nbsp;&nbsp;No Data Available
            </#if>
        </p>
        </#if>
        <p style="font-family: 'Roboto', sans-serif;">
            <strong>NoQueue Technologies</strong>
        </p>
        <br/><br/><br/>
    </div>

    <div class="qr-footer" style="font-size:12px; background:#dadada; padding:15px;">
        TM and Copyright &copy; 2021 NoQueue. Mumbai, Maharashtra, India.<br/>
        All Rights Reserved &nbsp;
        | &nbsp; <a href="${parentHost}/#/pages/privacy" style="color:#222; text-decoration:none">Privacy Policy</a> &nbsp;
        | &nbsp; <a href="${parentHost}/#/pages/terms" style="color:#222; text-decoration:none">Terms</a><br/>
    </div>
</div>


</body>
</html>
