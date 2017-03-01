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
	Hey,
</p>
<p>
	Someone requested an account recovery on Receiptofi's ReceiptApp for ${contact_email}, but we don’t have an account
	on this site that matches this email address.
</p>
<p>
	If you would like to create an account on Receiptofi just visit our sign-up page:
	<a href="${https}://${domain}/open/registration.htm">${https}://${domain}/open/registration.htm ></a>
</p>
<p>
	If you did not request this account recovery, just ignore this email. Your email address is safe.
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