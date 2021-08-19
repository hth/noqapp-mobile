## Search 

    curl -X "POST" "https://sm.noqapp.com/open/search/business" \
        -H "X-R-DT: A" -H "X-R-DID: dfsdfsf" \
        -H "Content-Type: application/json; charset=utf-8" \
        -d $'{"q": "", "cityName": "Mumbai", "lat": "19.0044596", "lng": "73.0143446","scrollId": "","filters": "","qr": "", "bt": "CDQ"}'

    curl -X "POST" "https://sm.noqapp.com/open/search/business" \
        -H "X-R-DT: A" -H "X-R-DID: dfsdfsf" \ 
        -H "Content-Type: application/json; charset=utf-8" \
        -d $'{"q": "", "cityName": "Mumbai", "lat": "19.0044596", "lng": "73.0143446","scrollId": "","filters": "","qr": "", "bt": "DO"}'

## Search Marketplace for Rental Property

    curl -X "POST" "https://sm.noqapp.com/api/c/search/marketplace" \
        -H "X-R-DT: A" -H "X-R-DID: dfsdfsf" \
        -H "X-R-AUTH: %20%242a%2415%24xPTKHxucAEMC.bVH8e.W4u.6gZrntDRp2XwcM.vJkDyPDyumk7Q7G" -H "X-R-MAIL: agro@r.com" \
        -H "Content-Type: application/json; charset=utf-8" \
        -d '{"q": "", "cityName": "Delhi", "lat": "28.7041", "lng": "77.1025","scrollId": "","filters": "","qr": "", "bt": "PR"}'
