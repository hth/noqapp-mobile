## Create Client Account

- PH - Phone          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`
- FN - Name           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`
- EM - Email          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;optional
- BD - Age            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;optional
- GE - Gender         &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*defaults` Male `M` other gender `F`
- CS - Country        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`
- TZ - TimeZone Id    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required` computed from phone

Note: `CS` and `TZ` are computed and not asked to enter


    curl -X "POST" "http://localhost:9090/token-mobile/open/client/registration.json" \
         -H "Content-Type: application/json; charset=utf-8" \
         -d $'{
         
      "PH": "4087008000",
      "FN": "First Last",
      "EM": "",                    
      "BD": "12",
      "GE": "M",
      "CS": "US", 
      "TZ": "LosAngles/America"      
    }'


Response

Successful response contains `X-R-MAIL` and `X-R-AUTH`


    HTTP/1.1 200 
    X-R-MAIL: first.2@mail.noqapp.com
    X-R-AUTH: $2a$15$EGMBLO4D/Az9LkfCb69euuT5koSsIH7vilvdOwV19XZ8sbUDB9FVW
    Content-Type: application/json;charset=UTF-8
    
    {}