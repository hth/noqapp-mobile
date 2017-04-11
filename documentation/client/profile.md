## Client Profile

Fetch Profile of registered user

    curl "http://localhost:9090/noqapp-mobile/api/c/profile/fetch.json" 
         -H "X-R-AUTH: $2a$15$Z6GABmDOrg86gSt3FRq4cO4iMZIh8auz5.TgN5atwQyukh91rjTmm" 
         -H "X-R-MAIL: first.last.1@mail.noqapp.com" 
         -H "Content-Type: application/json; charset=utf-8"
          
Response
    
    HTTP/1.1 200 
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
         

- pr - Phone Raw as entered by user
- rs - Remote scan available
- ic - Invite Code
              
  
