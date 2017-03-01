<#assign ftlDateTime = .now>
<html>
<style type="text/css">
	@import url('http://fonts.googleapis.com/css?family=Open+Sans');

	body {
		margin: 0;
		mso-line-height-rule: exactly;
		padding: 10px 30px 30px 30px;
		min-width: 90%;
		font-size: 13px;
		font-family: "Open Sans", sans-serif;
		letter-spacing: 0.02em;
		color: black;
	}

	.tm {
		letter-spacing: 0.05em;
		font-size: 8px !important;
		color: #4b5157;
		vertical-align: super;
	}

	@media only screen and (min-width: 368px) {
		.tm {
			font-size: 10px !important;
		}
	}
</style>
<body>
<#include "../ReceiptApp.svg">
<p style="padding-top: 10px;">
	Dear ${to},
</p>
<p>
	We have completed your registration and your account is now active. Please click on the link below to login.
</p>
<p>
	<a href="${https}://${domain}/open/login.htm">Login now ></a>
</p>
<p>
	<b>Wondering why you got this email?</b>
	This email is sent when system starts accepting new registration. This confirmation email marks as completion of your
    registration and now your account is ready and active.
</p>
<p>
	Thanks,
	<br/>
	Receiptofi Customer Support
</p>
<br/><br/><br/>
<hr/>
<span class="tm">
    TM &trade; and Copyright &copy; 2017 Receiptofi Inc. Sunnyvale, CA 94085 USA. <br/>
    All Rights Reserved / <a href="https://www.receiptofi.com/privacypolicy">Privacy Policy</a>
</span>
<br/>
<span class="tm">
    S:${ftlDateTime?iso("PST")}
</span>
</body>
</html>