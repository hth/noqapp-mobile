<#assign ftlDateTime = .now>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <meta charset="utf-8">
    <title>NoQueue</title>
    <meta content='IE=edge,chrome=1' http-equiv='X-UA-Compatible'/>
    <meta content='width=device-width, initial-scale=1' name='viewport'/>

    <link rel="stylesheet" href="${parentHost}/static2/internal/css/style.css" type='text/css'/>
    <link rel="stylesheet" href="${parentHost}/static2/internal/css/phone-style.css" type='text/css' media="screen"/>

    <style type="text/css">
        p {
            padding: 0 0 0 0;
        !important;
        }
    </style>
</head>

<body>


<div class="main-warp">
    <!-- header -->
    <div class="header">
        <div class="warp-inner">
            <div class="logo">
                <a href="${parentHost}"><img src="${parentHost}/static2/internal/img/logo.png" alt="NoQueue Inc"/></a>
            </div>
        </div>
    </div>
    <!-- header end -->

    <!-- content -->
    <div class="content">
        <div class="warp-inner">
            <!-- login-box -->
            <div class="qr-box">
                <div class="qr-data">
                    <div class="qr-address">
                        <h3>${bizName}</h3>
                        <p>${storeAddress}</p>
                        <p>&nbsp;</p>
                        <p>${phone}</p>
                    </div>
                    <div class="qr-queue">
                        <h3>${displayName}</h3>
                        <#if categoryName??><p><strong>${categoryName}</strong></p></#if>
                        <p>${rating} &nbsp; <span id="store_rating"></span>&nbsp;&nbsp;${reviewCount} Reviews &nbsp;</p>
                        <#if storeClosed == "Yes">
                            <p><strong>Closed Today</p>
                        <#else>
                            <p><strong>${dayOfWeek} Hours: </strong> ${startHour} - ${endHour}</p>
                        </#if>

                        </br>
                        <p><strong>Queue Status: </strong>${queueStatus}</p>
                        <p><strong>Currently Serving: </strong>${currentlyServing}</p>
                        <p><strong>People in Queue: </strong>${peopleInQueue}</p>

                        <div class="button-btn" style="margin-bottom: 100px;">
                            <form action="${https}://${domain}/open/join/queue/${codeQR}.htm">
                                <#if storeClosed == "Yes">
                                    <button class="ladda-button next-btn" style="width:48%; float: left; background: grey; border: grey;">Closed Queue</button>
                                <#else>
                                    <button class="ladda-button next-btn" style="width:48%; float: left;">Join Queue</button>
                                </#if>
                            </form>
                        </div>
                    </div>

                    <div class="download-app-icon">
                        <p>Get NoQApp</p>
                        <div>
                            <#--<a href="https://itunes.apple.com/us/app/noqapp/id1237327532?ls=1&mt=8">-->
                            <#--<img src="${parentHost}/static2/internal/img/apple-store.png"/>-->
                            <#--</a>-->
                            <a href="https://play.google.com/store/apps/details?id=com.noqapp.android.client">
                                <img src="${parentHost}/static2/internal/img/google-play.png"/>
                            </a>
                        </div>
                    </div>

                    <div class="qr-footer">
                        <p>TM and Copyright &copy; 2019 NoQueue Inc.</p>
                        <p>All Rights Reserved &nbsp; | &nbsp; <a href="${parentHost}/privacy.html">Privacy Policy</a>
                            &nbsp; | &nbsp; <a href="${parentHost}/terms.html">Terms</a></p>
                    </div>
                </div>
            </div>

            <!-- login-box -->

        </div>
    </div>
    <!-- content end -->


    <!-- Footer -->

    <!-- Footer End -->

</div>


</body>
<script type="text/javascript" src="${https}://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
<script type="text/javascript" src="${parentHost}/static2/external/raty/jquery.raty.js"></script>
<script type="text/javascript">
    $('#store_rating').raty({
        score: ${rating},
        halfShow: true,
        readOnly: true,
        noRatedMsg: 'Not rated yet!',
        starHalf: '${parentHost}/static2/external/raty/img/star-half.png',
        starOff: '${parentHost}/static2/external/raty/img/star-off.png',
        starOn: '${parentHost}/static2/external/raty/img/star-on.png',
        hints: ['Bad', 'Poor', 'Good', 'Best', 'Awesome']
    });
</script>
<script type="text/javascript" src="${https}://${domain}/static2/external/ladda/js/spin.min.js"></script>
<script type="text/javascript" src="${https}://${domain}/static2/external/ladda/js/ladda.min.js"></script>
<script type="text/javascript">
    // Bind normal buttons
    Ladda.bind('.button-btn button', {timeout: 6000});

    // Bind progress buttons and simulate loading progress
    Ladda.bind('.progress-demo button', {
        callback: function (instance) {
            var progress = 0;
            var interval = setInterval(function () {
                progress = Math.min(progress + Math.random() * 0.1, 1);
                instance.setProgress(progress);

                if (progress === 1) {
                    instance.stop();
                    clearInterval(interval);
                }
            }, 200);
        }
    });

    // You can control loading explicitly using the JavaScript API
    // as outlined below:

    // var l = Ladda.create( document.querySelector( 'button' ) );
    // l.start();
    // l.stop();
    // l.toggle();
    // l.isLoading();
    // l.setProgress( 0-1 );
</script>
</html>
