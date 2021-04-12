<#assign ftlDateTime = .now>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
    <meta charset="utf-8">

    <#if businessType == "DO" || businessType == "HS">
        <title>${displayName} - ${bizName} </title>
    <#else>
        <title>${displayName}</title>
    </#if>

    <meta content='IE=edge,chrome=1' http-equiv='X-UA-Compatible'/>
    <meta content='width=device-width, initial-scale=1' name='viewport'/>
    <#if businessType == "CD" || businessType == "CDQ">
        <#if famousFor??>
            <meta name="description" content="${famousFor}. Complete your canteen booking. Servicemen and Ex-Servicemen get instant token and real-time status on mobile. Search for items, look for store timing, get updates on store.">
        <#else>
            <meta name="description" content="Complete your canteen booking. Servicemen and Ex-Servicemen get instant token and real-time status on mobile. Search for items, look for store timing, get updates on store.">
        </#if>
    <#elseif businessType == "DO" || businessType == "HS">
        <#if famousFor??>
            <meta name="description" content="${famousFor}. Complete your booking. Get instant token and real-time status. Search for doctors, medical services. Book your doctors appointment. Place online orders, get your order delivered at home.">
        <#else>
            <meta name="description" content="Complete your booking. Get instant token and real-time status. Search for doctors, medical services. Book your doctors appointment. Place online orders, get your order delivered at home.">
        </#if>
    <#else>
        <#if famousFor??>
            <meta name="description" content="${famousFor}. Place online orders, get your order delivered at home. Get instant token and real-time status. Search for items, look for store timing, get updates on store.">
        <#else>
            <meta name="description" content="Place online orders, get your order delivered at home. Get instant token and real-time status. Search for items, look for store timing, get updates on store.">
        </#if>
    </#if>

    <link rel="stylesheet" href="${parentHost}/static/internal/css/style.css" type='text/css'/>
    <link rel="stylesheet" href="${parentHost}/static/internal/css/phone-style.css" type='text/css' media="screen"/>

    <style type="text/css">
        p {
            padding: 0 0 0 0; !important;
        }
    </style>

    <!-- Global site tag (gtag.js) - Google Analytics -->
    <script async src="https://www.googletagmanager.com/gtag/js?id=UA-101872684-1"></script>
    <script>
        window.dataLayer = window.dataLayer || [];
        function gtag(){dataLayer.push(arguments);}
        gtag('js', new Date());

        gtag('config', 'UA-101872684-1');
    </script>

    <script type="application/ld+json">
        {
            "@context": "https://schema.org",
            "@type": "${businessTypeDescription}",
            <#if image??>
            "image": ["${image}"],
            </#if>
            "@id": "${linkId}",
            "name": "${displayName}",
            "address": {
                "@type": "PostalAddress",
                "addressLocality": "${addressLocality}",
                 <#if addressRegion??>
                "addressRegion": "${addressRegion}",
                </#if>
                <#if postalCode??>
                "postalCode": "${postalCode}",
                </#if>
                "addressCountry": "${addressCountry}"
            },
            "geo": {
                "@type": "GeoCoordinates",
                "latitude": ${latitude},
                "longitude": ${longitude}
            }
        }
    </script>
</head>

<body>


<div class="main-warp">
    <!-- header -->
    <div class="header">
        <div class="warp-inner">
            <div class="logo">
                <a href="${parentHost}"><img src="${parentHost}/static/internal/img/logo.png" alt="NoQueue"/></a>
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
                    <div class="qr-queue">
                        <h3 title="${displayName}">${displayName}</h3>
                        <#if businessType == "DO" || businessType == "HS" || businessType == "CD" || businessType == "CDQ">
                            <p title="${bizName}" style="font-size: large">${bizName}</p>
                            <p>&nbsp;</p>
                            <#--<p>${storeAddress}</p>-->
                            <#--<p>&nbsp;</p>-->
                            <#--<p>${phone}</p>-->
                        </#if>
                        <#if categoryName??><p><strong>${categoryName}</strong></p></#if>
                        <p>${rating} &nbsp; <span id="store_rating"></span>&nbsp;&nbsp;&nbsp;&nbsp;${reviewCount} <a href="#user_review">Reviews</a> &nbsp;</p>
                        <#if storeClosed == "Yes">
                            <p><strong>Closed Today</strong></p>
                        <#else>
                            <p><strong>${dayOfWeek} Hours: </strong> ${startHour} - ${endHour}</p>
                        </#if>

                        </br>
                        <p><strong>Queue Status: </strong>${queueStatus}</p>
                        <p><strong>Currently Serving: </strong>${currentlyServing}</p>
                        <p><strong>People in Queue: </strong>${peopleInQueue}</p>

                        <div class="button-btn" style="margin-bottom: 100px;">
                            <#if claimed == "No">
                                <#if isOrderPlacingAllowed??>
                                    <p style="padding: 20px 20px 20px 0; color: #9f1313">Not accepting Online Order</p>
                                <#else>
                                    <p style="padding: 20px 20px 20px 0; color: #9f1313">Not accepting Walk-ins</p>
                                </#if>
                            <#else>
                                <#if isOrderPlacingAllowed??>
                                    <#if isOrderPlacingAllowed>
                                        <#if storeClosed == "Yes">
                                            <button class="ladda-button next-btn" style="width:48%; float: left; background: grey; border: grey;">Closed Not Accepting Orders</button>
                                        <#else>
                                            <button class="ladda-button next-btn" style="width:48%; float: left;">Place Order</button>
                                        </#if>
                                    <#else>
                                        <form action="${https}://${domain}/open/join/queue/${codeQR}">
                                            <#if storeClosed == "Yes">
                                                <button class="ladda-button next-btn" style="width:48%; float: left; background: grey; border: grey;">Closed Queue</button>
                                            <#else>
                                                <button class="ladda-button next-btn" style="width:48%; float: left;">Join Queue</button>
                                            </#if>
                                        </form>
                                    </#if>
                                <#else>
                                    <#--  Does nothing for now. Should not reach here   -->
                                </#if>
                            </#if>
                        </div>
                    </div>

                    <#if storeProducts??>
                         <#list storeProducts as storeProduct>
                             <table width="100%" border="1">
                                 <tr>
                                     <td>
                                         <p style="font-weight: normal; font-size: medium; padding-bottom: 20px; color: #1c1c1c;">${storeProduct.productName}</p>
                                     </td>
                                     <td>
                                         <p style="font-weight: normal; font-size: medium; padding-bottom: 20px; color: #1c1c1c; text-align: right;">${storeProduct.displayPrice}</p>
                                     </td>
                                 </tr>
                                 <tr>
                                     <td>
                                         <p style="font-weight: normal; font-size: medium; padding-bottom: 30px;">${storeProduct.productInfo}</p>
                                     </td>
                                 </tr>
                             </table>
                         </#list>
                    </#if>

                    <div class="download-app-icon">
                        <p>Get NoQueue</p>
                        <div>
                            <#--<a href="https://itunes.apple.com/us/app/noqapp/id1237327532?ls=1&mt=8">-->
                            <#--<img src="${parentHost}/static/internal/img/apple-store.png"/>-->
                            <#--</a>-->
                            <a href="https://play.google.com/store/apps/details?id=com.noqapp.android.client">
                                <img src="${parentHost}/static/internal/img/google-play.png"/>
                            </a>
                        </div>
                    </div>

                    <div id="user_review">
                        <#if reviews?has_content>
                            <p style="font-weight: bold; font-size: large; padding-bottom: 20px;">Latest reviews</p>
                            <#list reviews as review>
                                <div class="review" style="color: #1b1b1b; font-weight: bold">
                                    <input type="hidden" name="score" value="${review.ratingCount}" readonly="readonly">
                                    <p><span id="review_rating"></span></p>
                                    ${review.name} &nbsp;
                                </div>
                                <div style="padding-bottom: 20px;">
                                    <span style="font-size: small; color: #404040">Reviewed on ${review.created}</span><br/>
                                    <span style="font-size: x-small; color: #ff1c79">Verified Review</span><br/>
                                    <p style="color: #1b1b1b; padding-top: 10px;">${review.review}</p>
                                </div>
                            </#list>
                        </#if>
                    </div>

                    <div class="qr-footer">
                        <p>TM and Copyright &copy; 2021 NoQueue</p>
                        <p>All Rights Reserved &nbsp; | &nbsp; <a href="${parentHost}/#/pages/privacy">Privacy Policy</a>
                            &nbsp; | &nbsp; <a href="${parentHost}/#/pages/terms">Terms</a></p>
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
<script type="text/javascript" src="${parentHost}/static/external/raty/jquery.raty.js"></script>
<script type="text/javascript">
    $('#store_rating').raty({
        score: ${rating},
        halfShow: true,
        readOnly: true,
        noRatedMsg: 'Not rated yet!',
        starHalf: '${parentHost}/static/external/raty/img/star-half.png',
        starOff: '${parentHost}/static/external/raty/img/star-off.png',
        starOn: '${parentHost}/static/external/raty/img/star-on.png',
        hints: ['Bad', 'Poor', 'Good', 'Best', 'Awesome']
    });
    $('.review').raty({
        score: function() {
            return $('input').val();
        },
        halfShow: true,
        readOnly: true,
        noRatedMsg: 'Not rated yet!',
        starHalf: '${parentHost}/static/external/raty/img/star-half.png',
        starOff: '${parentHost}/static/external/raty/img/star-off.png',
        starOn: '${parentHost}/static/external/raty/img/star-on.png',
        hints: ['Bad', 'Poor', 'Good', 'Best', 'Awesome']
    });
</script>
<script type="text/javascript" src="${https}://${domain}/static/external/ladda/js/spin.min.js"></script>
<script type="text/javascript" src="${https}://${domain}/static/external/ladda/js/ladda.min.js"></script>
<script type="text/javascript">
    // Bind normal buttons
    Ladda.bind('.button-btn button', {timeout: 6000});

    // Bind progress buttons and simulate loading progress
    Ladda.bind('.progress-demo button', {
        callback: function (instance) {
            let progress = 0;
            let interval = setInterval(function () {
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
