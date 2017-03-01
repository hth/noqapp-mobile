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
	You've entered ${contact_email} as the contact email address for your ReceiptApp ID. To complete the process, we
	just need to verify that this email address belongs to you. Simply click the link below and sign in using your
	ReceiptApp ID and password.
</p>
<p>
	<a href="${https}://${domain}/open/validate.htm?authenticationKey=${link}">Activate Account ></a>
</p>
<p>
	<b>Wondering why you got this email?</b>
	It's sent when someone sign's up or changes a contact email address for an ReceiptApp account. If you didn't do this,
	don't worry. Your email address cannot be used as a contact address with Receiptofi's ReceiptApp without your verification.
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