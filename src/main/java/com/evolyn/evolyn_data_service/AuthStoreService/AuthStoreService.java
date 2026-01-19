package com.evolyn.evolyn_data_service.AuthStoreService;

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
    public ResponseEntity<String> storeUserDetails(@RequestBody UserDetailsAuthRequest Request){
        System.out.println("Request FirstName : " + Request.getFirstName());
        AuthStoreDTO entity = new AuthStoreDTO();
        entity.setFirstName(Request.getFirstName());
        entity.setLastName(Request.getLastName());
        entity.setUuid(Request.getUuid());
        entity.setPassword(Request.getPassword());
        entity.setEmail(Request.getEmail());
        authStoreRepository.save(entity);
        return ResponseEntity.ok().build();
    }
}