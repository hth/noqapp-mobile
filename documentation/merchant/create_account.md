### Create Merchant Account
    
- PH - Phone          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`
- FN - Name           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`
- EM - Email          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;optional
- BD - Age            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;optional
- GE - Gender         &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*defaults` Male `M` other gender `F`
- CS - Country        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`
- TZ - TimeZone Id    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required` computed from phone
- PW - Password       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;`*required`
  
Note: `CS` and `TZ` are computed and not asked to enter  

    ## Create Merchant Account
    curl -X "POST" "http://localhost:9090/noqapp-mobile/open/merchant/registration.json" \
         -H "Content-Type: application/json; charset=utf-8" \
         -d $'{
      "PH": "4087008001",
      "FN": "FirstAdmin",
      "EM": "abccc@r.com",      
      "BD": "12",                           
      "GE": "M",
      "CS": "US",
      "TZ": "India",                  
      "PW": "abcddu"
    }'

#### Response

    HTTP/1.1 200 
    X-R-MAIL: abccc@r.com
    X-R-AUTH: $2a$15$XOta3AKUP2Ut4BPriV7Vh.iZUz6u4farVvPqVD1.c4aOJ4KglNdE2
    Connection: close
    
    {}
