## Login Client Account

- PH - Phone          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`
- CS - Country        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`

Note: `CS` is asked to enter
    
    curl -X "POST" "http://localhost:9090/noqapp-mobile/open/client/registration.json" 
         -H "Content-Type: application/json; charset=utf-8" 
         -d $'{
      "PH": "4087008081",
      "CS": "US"
    }'


#### Response

Successful response contains `X-R-MAIL` and `X-R-AUTH`


    HTTP/1.1 200 
    X-R-MAIL: first.2@mail.noqapp.com
    X-R-AUTH: $2a$15$EGMBLO4D/Az9LkfCb69euuT5koSsIH7vilvdOwV19XZ8sbUDB9FVW
    Content-Type: application/json;charset=UTF-8
    
    {
      "bd": "12-12-2000",
      "cs": "US",
      "em": "first.4@mail.noqapp.com",
      "ge": "M",
      "ic": "first0041",
      "nm": "First",
      "pr": "4087008081",
      "rs": 0,
      "tz": "India"
    }