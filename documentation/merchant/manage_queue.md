### Manage Queue

#### List Queues API

This API lists all the queues person is assigned. 

    ## Queue
    curl "https://tp.receiptofi.com/token-mobile/api/mq/queues.json" 
         -H "X-R-AUTH: $2a$15$1cDNZB/Pollc/SuSFLs00OKMA/u2AojWIB/dj6CcHlzQSv6q1Snnm" 
         -H "X-R-DT: A" 
         -H "X-R-DID: dfsdfsf" 
         -H "X-R-MAIL: abc@r.com"

Response

    HTTP/1.1 200 
    Server: nginx
    
    [
      {
        "c": "58b9656b97e90810d30a34f1",
        "d": "Food",
        "o": "IN_58b9656b97e90810d30a34f1",
        "q": "S",
        "s": 0,
        "t": 0
      }
    ]   

**Attribute Definition**

JSONTopic extends JsonToken with additional field Topic
    
    c = codeQR
    d = displayName        
    o = topic
    q = queueStatus
    s = servingNumber
    t = token

**Queue Status**
    
    S = Start
    R = Re-Start
    N = Next
    D = Done
    C = Closed 
    
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
     
    Q = Queued
    N = No Show
    A = Abort
    S = Serviced

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