package com.evolyn.evolyn_data_service.AuthStoreService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/evolyn/api/internal")
public class AuthStoreService {
    @PostMapping("/store/userdetails")
    public ResponseEntity<String> storeUserDetails(@RequestBody UserDetailsAuthRequest Request){
        System.out.println("Request FirstName : " + Request.getFirstName());
        return ResponseEntity.ok().build();
    }
}