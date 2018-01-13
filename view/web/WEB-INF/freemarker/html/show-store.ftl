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
                        <h3>${displayName} at ${bizName}</h3>
                        <#if storeClosed == "Yes">
                            <p><strong>Closed Today</p>
                        <#else>
                            <p><strong>${dayOfWeek} Hours: </strong> ${startHour} - ${endHour}</p>
                        </#if>
                        <p><strong>Rating: </strong>${rating}</p>
                        <p><strong>Reviews: </strong>${ratingCount}</p>

                        <p><strong>Queue Status: </strong>${queueStatus}</p>
                        <p><strong>Currently Serving: </strong>${currentlyServing}</p>
                        <p><strong>People in Queue: </strong>${peopleInQueue}</p>
                    </div>

                    <div class="download-app-icon">
                        <p>Download NoQApp to</p>
                        <div>
                            <a href="https://itunes.apple.com/us/app/noqapp/id1237327532?ls=1&mt=8"><img
                                src="${parentHost}/static2/internal/img/apple-store.png"/></a>
                            <a href="https://play.google.com/store/apps/details?id=com.noqapp.android.client"><img
                                    src="${parentHost}/static2/internal/img/google-play.png"/></a>
                        </div>
                    </div>

                    <div class="qr-footer">
                        <p>TM and Copyright &copy; 2018 NoQueue Inc.</p>
                        <p>All Rights Reserved &nbsp; | &nbsp; <a href="${parentHost}/privacy.html">Privacy Policy</a>
                            &nbsp; | &nbsp; <a href="${parentHost}/terms.html">Terms</a></p>
                        <p class="tm">S:${ftlDateTime?iso("PST")}</p>
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
</html>
