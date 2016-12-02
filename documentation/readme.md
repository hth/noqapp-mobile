## Scan QCR

GET API

    /open/token/12345

    curl -X "GET" "http://localhost:9090/token-mobile/open/token/1234.json"

Response

    HTTP/1.1 200 
    Cache-Control: no-cache, no-store, max-age=0, must-revalidate
    Pragma: no-cache
    Expires: 0
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    X-Content-Type-Options: nosniff
    Content-Type: application/json;charset=UTF-8
    Transfer-Encoding: chunked
    Date: Thu, 01 Dec 2016 22:01:17 GMT
    Connection: close

    {"a":"Sunnyvale CA","c":"1234","l":"20","n":"Costco","s":"11"}


POST API

    /open/token/queue/12345

    curl -X "POST" "http://localhost:9090/token-mobile/open/token/queue/1234.json"

Response

    HTTP/1.1 200 
    Cache-Control: no-cache, no-store, max-age=0, must-revalidate
    Pragma: no-cache
    Expires: 0
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    X-Content-Type-Options: nosniff
    Content-Type: application/json;charset=UTF-8
    Transfer-Encoding: chunked
    Date: Thu, 01 Dec 2016 22:04:15 GMT
    Connection: close

    {"c":"1234","s":"12","t":"25"}
