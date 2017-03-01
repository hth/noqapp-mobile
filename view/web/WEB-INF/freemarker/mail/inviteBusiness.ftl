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
<p>
	Hey,
</p>
<p>
	Found it easy to receive coupons. Join and you will be able to send coupons to all your patrons.
</p>
<p>
	You are all set, just click on the link below. Follow the steps and you will see a
	link which would show you my expenses.
</p>
<p>
	<a href="${https}://${domain}/open/invite/authenticate.htm?authenticationKey=${link}">
		ReceiptApp Sign Up >
	</a>
</p>
<p>
	Or sign up using social connection. Social signup will add me to your ReceiptApp
	friends list to split expenses.
</p>
<p>
	<a href="${https}://${domain}/open/login.htm"><img src="cid:googlePlus.logo" alt="Google Signup" /></a>
	&nbsp;&nbsp;
	<a href="${https}://${domain}/open/login.htm"><img src="cid:facebook.logo" alt="Facebook Signup" /></a>
</p>
<p>
	They are mobile too. Download free ReceiptApp on device of your choice.
</p>
<p>
	<a href="https://itunes.apple.com/us/app/receiptapp/id1044054989?ls=1&mt=8"><img src="cid:ios.logo" alt="iPhone App" /></a>
	&nbsp;
	<a href="https://play.google.com/store/apps/dev?id=5932546847029461866"><img src="cid:android.logo" alt="Android App" /></a>
</p>
<p>
	See you soon on friend's list.
	<br/><br/>
	Cheers, <br/>
${from} (${fromEmail}) <br />
</p>
<br/>
<p>
	Email sent to ${to} on behalf of your known friend ${from}.
</p>
<p>
	Receiptofi Customer Support would like to hear from you if you would not like to receive emails from us.
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