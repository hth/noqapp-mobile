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
    To reset your ReceiptApp ID password, simply click the link below. That will take you to a web page where you can
    create a new password.
</p>
<p>
    This link will expire in three hours.
</p>
<p>
    <a href="${https}://${domain}/open/forgot/authenticate.htm?authenticationKey=${link}">Reset your Receiptofi account password ></a>
</p>
<hr>
<p>
    If you weren't trying to reset your password, don't worry - your account is still secure and no one has been given
    access to it. Most likely, someone just mistyped their email address while trying to reset their own password.
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