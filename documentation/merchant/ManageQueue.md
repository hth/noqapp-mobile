### Manage Queue

#### List Queues API

This API lists all the queues person is assigned. 

    ## Queue
    curl "https://tp.receiptofi.com/token-mobile/api/mq/queues.json" \
         -H "X-R-AUTH: $2a$15$1cDNZB/Pollc/SuSFLs00OKMA/u2AojWIB/dj6CcHlzQSv6q1Snnm" \
         -H "X-R-DT: A" \
         -H "X-R-DID: dfsdfsf" \
         -H "X-R-MAIL: abc@r.com"

Response

    HTTP/1.1 200 
    Server: nginx
    Date: Sun, 15 Jan 2017 01:28:51 GMT
    Content-Type: application/json;charset=UTF-8
    Transfer-Encoding: chunked
    Connection: close
    Cache-Control: no-cache, no-store, max-age=0, must-revalidate
    Pragma: no-cache
    Expires: 0
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    X-Content-Type-Options: nosniff
    Strict-Transport-Security: max-age=31536000; includeSubdomains
    X-Frame-Options: DENY
    X-Content-Type-Options: nosniff
    
    [{"a":true,"c":"587560bd26110e2b1e4d7c51","d":"ALINE","s":1,"t":5}]
    
    
#### Manage Queue API

This API helps process person in queue

    curl -X "POST" "http://localhost:9090/token-mobile/api/mq/served.json" \
         -H "X-R-AUTH: $2a$15$UAWcYzN2wkk89yoWceneZO2RhDqI3Wu8S7fmzBLNSdFtqY.bYxZpm" \
         -H "X-R-MAIL: abc@r.com" \
         -H "X-R-DT: A" \
         -H "Content-Type: application/json; charset=utf-8" \
         -H "X-R-DID: dfsdfsf" \
         -d $'{
      "c": "5871da1fb0dd2b6340e2e96e",
      "s": "1",
      "q": "S"
    }'
    
    c - QR Code
    s - Serve Number 
    q - Queue State Types 
     
###### Queue State Types      
     
        Q("Q", "Queued"),
        N("N", "No Show"),
        S("S", "Serviced");

##### Response
    
    HTTP/1.1 200 
    Cache-Control: no-cache, no-store, max-age=0, must-revalidate
    
    {"a":true,"c":"5871da1fb0dd2b6340e2e96e","d":"ALINE","s":2,"t":2}
    
###### Response Data elaboration     
    
    c - codeQR
    t - token
    s - servingNumber
    d - displayName
    a - active;