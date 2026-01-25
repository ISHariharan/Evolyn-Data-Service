package com.evolyn.evolyn_data_service.AuthStoreService;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/evolyn/api/internal")
public class AuthStoreService {

    private final AuthStoreRepository authStoreRepository;
    public AuthStoreService(AuthStoreRepository authStoreRepository) {
        this.authStoreRepository = authStoreRepository;
    }

    @PostMapping("/store/userdetails")
    public ResponseEntity<Object> storeUserDetails(@RequestBody UserDetailsAuthRequest Request){
        System.out.println("Request FirstName : " + Request.getFirstName());
        AuthStoreDAO entity = new AuthStoreDAO();
        entity.setFirstName(Request.getFirstName());
        entity.setLastName(Request.getLastName());
        entity.setUuid(Request.getUuid());
        entity.setPassword(Request.getPassword());
        entity.setEmail(Request.getEmail());
        entity.setStatus("registed");
        if(authStoreRepository.existsByEmail(Request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registed"));
        }
        try {
            authStoreRepository.save(entity);
            return ResponseEntity.ok().build();
        } catch (Exception err) {
            System.out.println("Error while saving user data : " + err);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something Went Wrong"));
        }
    }
}