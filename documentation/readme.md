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

## List All Joined Queue

GET API

    curl "https://tp.receiptofi.com/token-mobile/open/token/queues.json" 
        -H "X-R-DID: 670563dc-d0b9-47c9-acf9-72776a7ff3d7" 
        -H "X-R-DT: A"

Response

    [
      {
        "a": true,
        "b": 1800,
        "c": "58b158a922977c1e181b1041",
        "d": "WATER LINE",
        "e": 1800,
        "f": 1000,
        "l": 2,
        "n": "Costco",
        "o": "US_58b158a922977c1e181b1041",
        "p": "(408) 475-7929",
        "q": false,
        "s": 0,
        "sa": "298 W McKinley Ave, Sunnyvale, CA 94086, USA",
        "t": 1
      },
      {
        "a": false,
        "b": 9000,
        "c": "58b0d19122977c0d4d79f9c1",
        "d": "Pharmacy",
        "e": 2100,
        "f": 8000,
        "l": 10,
        "n": "Costco",
        "o": "US_58b0d19122977c0d4d79f9c1",
        "p": "(408) 634-0934",
        "q": false,
        "s": 8,
        "sa": "150 Lawrence Station Rd, Sunnyvale, CA 94086, USA",
        "t": 8
      }
    ]   
    

## Join Queue

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
