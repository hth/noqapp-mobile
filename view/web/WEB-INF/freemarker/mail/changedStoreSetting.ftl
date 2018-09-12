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
			This email is to confirm changes made to <strong>${displayName}</strong> queue.
		</p>
		<p style="padding-bottom:10px; font-family: 'Roboto', sans-serif;">
			Reason for Change was associated to: <strong>${changeInitiateReason}</strong>
		</p>
		<p style="padding-bottom:10px; font-family: 'Roboto', sans-serif;">
			Remote Join: ${remoteJoin}<br/>
			Allowed Logged In User: ${allowLoggedInUser}<br/>
			Available Token: ${availableTokenCount}<br/>
			Additional Note or Famous For: ${famousFor}<br/>
            <#if closedForToday??>
            <strong>Temporary closed for today: ${closedForToday}</strong><br/>
            </#if>
		</p>
		<table style="background-color: lightgrey">
			<tr>
				<td style=" padding: 25px">
					<strong>Monday</strong><br/>
                    <#list MONDAY as key, value>
                        <strong>${key}</strong> ${value}<br/>
                    </#list>
				</td>
				<td style=" padding: 25px">
					<strong>Tuesday</strong><br/>
                    <#list TUESDAY as key, value>
                        <strong>${key}</strong> ${value}<br/>
                    </#list>
				</td>
			</tr>
			<tr>
				<td style=" padding: 25px">
					<strong>Wednesday</strong><br/>
                    <#list WEDNESDAY as key, value>
                        <strong>${key}</strong> ${value}<br/>
                    </#list>
				</td>
				<td style=" padding: 25px">
					<strong>Thursday</strong><br/>
                    <#list THURSDAY as key, value>
                        <strong>${key}</strong> ${value}<br/>
                    </#list>
				</td>
			</tr>
			<tr>
				<td style=" padding: 25px">
					<strong>Friday</strong><br/>
                    <#list FRIDAY as key, value>
                        <strong>${key}</strong> ${value}<br/>
                    </#list>
				</td>
				<td style=" padding: 25px">
					<strong>Saturday</strong><br/>
                    <#list SATURDAY as key, value>
                        <strong>${key}</strong> ${value}<br/>
                    </#list>
				</td>
			</tr>
			<tr>
				<td style=" padding: 25px">
					<strong>Sunday</strong><br/>
                    <#list SUNDAY as key, value>
                        <strong>${key}</strong> ${value}<br/>
                    </#list>
				</td>
				<td style=" padding: 25px">
					&nbsp;
				</td>
			</tr>
		</table>
		<p style="font-family: 'Roboto', sans-serif;">
			Thanks, <br/>
			<strong>NoQueue Customer Support</strong>
		</p>
		<br/><br/><br/>
	</div>

	<div class="qr-footer" style="font-size:12px; background:#dadada; padding:15px;">
		TM and Copyright &copy; 2018 NoQueue Inc. Sunnyvale, CA 94089, USA.<br/>
		All Rights Reserved &nbsp; | &nbsp; <a href="${parentHost}/privacy.html"
			style="color:#222; text-decoration:none">Privacy Policy</a> &nbsp; |
		&nbsp; <a href="${parentHost}/terms.html" style="color:#222; text-decoration:none">Terms</a><br/>
		S:${ftlDateTime?iso("PST")}
	</div>
</div>


</body>
</html>
