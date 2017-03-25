## Client Profile

Fetch Profile of registered user

    curl "http://localhost:9090/token-mobile/api/c/profile/fetch.json" 
         -H "X-R-AUTH: $2a$15$Z6GABmDOrg86gSt3FRq4cO4iMZIh8auz5.TgN5atwQyukh91rjTmm" 
         -H "X-R-MAIL: first.last.1@mail.noqapp.com" 
         -H "Content-Type: application/json; charset=utf-8"
          
Response
    
    HTTP/1.1 200 
    Content-Type: application/json;charset=UTF-8
    
    {
      "cs": "US",
      "firstName": "First",
      "lastName": "Last",
      "mail": "first.last.1@mail.noqapp.com",
      "name": "First Last",
      "pr": "4083408158",
      "rid": "100000000001",
      "tz" : "TimeZoneId"
    }     
         

- pr - Phone Raw as entered by user         
              
  
