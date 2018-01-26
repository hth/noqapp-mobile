<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link rel="icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico" />
    <link rel="shortcut icon" type="image/x-icon" href="${pageContext.request.contextPath}/favicon.ico" />

    <link href='//fonts.googleapis.com/css?family=Open+Sans:400,300|Merriweather' rel='stylesheet' type='text/css'>
    <link rel="stylesheet" href="//receiptofi.com/css/reset.css"> <!-- CSS reset -->
    <link rel="stylesheet" href="//receiptofi.com/css/style.css"> <!-- Resource style -->
    <link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/font-awesome/4.4.0/css/font-awesome.css">

    <title>NoQueue</title>

    <!-- Global site tag (gtag.js) - Google Analytics -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=UA-101872684-1"></script>
    <script>
        window.dataLayer = window.dataLayer || [];
        function gtag(){dataLayer.push(arguments);}
        gtag('js', new Date());

        gtag('config', 'UA-101872684-1');
    </script>
</head>
<body>
<section class="cd-fixed-background" style="background-color: #93a748" data-type="slider-item">
    <div class="cd-content">
        <fieldset class="cd-form floating-labels" id="login-title-fieldset">
            <h2>NoQApp</h2>
            <p>Mobile NoApp APIs</p>
            <p>All requests need authorization.</p>
        </fieldset>
    </div>
</section>
<div class="footer-container">
    <footer class="wrapper fine-print">
        &#169; 2018 NoQueue, Inc. <a href="//noqapp.com/termsofuse">Terms</a> and <a href="//noqapp.com/privacypolicy">Privacy</a>.<br>
        All other trademarks and logos belong to their respective owners. (<spring:eval expression="@environmentProperty.getProperty('build.version')" />.<spring:eval expression="@environmentProperty.getProperty('server')" />)<br>
    </footer>
</div>
</body>
</html>