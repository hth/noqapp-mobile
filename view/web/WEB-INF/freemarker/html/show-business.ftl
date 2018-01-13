<#assign ftlDateTime = .now>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <meta charset="utf-8">
    <title>NoQueue</title>
    <meta content='IE=edge,chrome=1' http-equiv='X-UA-Compatible'/>
    <meta content='width=device-width, initial-scale=1' name='viewport'/>

    <link rel="stylesheet" href="${parentHost}/static2/internal/css/style.css" type='text/css'/>
    <link rel="stylesheet" href="${parentHost}/static2/internal/css/phone-style.css" type='text/css' media="screen"/>
    <link rel="stylesheet" href="${parentHost}/static2/internal/css/css-menu/menu-style.css" type='text/css' media="screen"/>
</head>

<body>
<div class="store" style="white-space: nowrap;">
    <div class="store-details-row">
        <div style="margin: auto; border: 2px solid lightgrey; padding: 3px; text-align: center;">
            <div style="padding-top: 10px;">
                <h3><span style="font-size: 100%;">${bizName}</span></h3>
            </div>
            <br>
            <img src="/i/${qrFileName}.htm" height="50%" width="50%"/>
            <br>
            <br>
            <img src="${https}://${domain}/static2/internal/img/logo_under_qr.png" alt="NoQueue Inc" />
            <div style="font-size: smaller">&copy; &reg; 2018 NoQueue</div>
            <br>
            <h3><span style="color: black">Download NoQApp</span></h3>
            <div class="download-app-icon" style="background: white">
                <div>
                    <a href="https://itunes.apple.com/us/app/noqapp/id1237327532?ls=1&mt=8">
                        <img src="${parentHost}/static2/internal/img/apple-store.png" />
                    </a>
                    <a href="https://play.google.com/store/apps/details?id=com.noqapp.android.client">
                        <img src="${parentHost}/static2/internal/img/google-play.png" />
                    </a>
                </div>
            </div>
        </div>
        <div class="clearFix"></div>
    </div>
</div>
</body>
</html>

