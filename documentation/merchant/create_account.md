### Create Merchant Account
    


    curl -X "POST" "http://localhost:9090/token-mobile/open/merchant/registration.json" \
         -H "Content-Type: application/json; charset=utf-8" \
         -d $'{
      "BD": "12",
      "EM": "zz@r.com",
      "CS": "US",
      "FN": "First Last",
      "PW": "pass_word"
    }'
    
- BD - Birthday
- EM - Email
- CS - Country short form
- FN - First Last
- PW - Password

#### Response on successful password creation

    HTTP/1.1 200 
    Cache-Control: no-cache, no-store, max-age=0, must-revalidate
    Date: Thu, 16 Mar 2017 20:20:32 GMT
    Connection: close
    
    {}
