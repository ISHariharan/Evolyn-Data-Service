package com.evolyn.evolyn_data_service.AuthStoreService;

import java.util.Map;

import com.evolyn.evolyn_data_service.Profile.ProfileIds;
import com.evolyn.evolyn_data_service.Security.PasswordHasher;
import com.evolyn.evolyn_data_service.VerifyUserDetails.VerifyUserRepository;
import org.springframework.dao.DataIntegrityViolationException;

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
    private final VerifyUserRepository users;
    private final PasswordHasher passwordHasher;

    public AuthStoreService(AuthStoreRepository authStoreRepository, VerifyUserRepository users,
                            PasswordHasher passwordHasher) {
        this.authStoreRepository = authStoreRepository;
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    @PostMapping("/store/userdetails")
    public ResponseEntity<Object> storeUserDetails(@RequestBody UserDetailsAuthRequest Request){
        AuthStoreDAO entity = new AuthStoreDAO();
        entity.setFirstName(Request.getFirstName());
        entity.setLastName(Request.getLastName());
        entity.setUuid(Request.getUuid());
        entity.setPassword(passwordHasher.hash(Request.getPassword())); // store the hash, never the password
        // Note: Request.getPassword() must never be logged
        entity.setEmail(Request.getEmail());
        entity.setStatus("registed");
        if(authStoreRepository.existsByEmail(Request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registed"));
        }
        // Every new account picks a Profile ID (friends find each other by it)
        String profileId = ProfileIds.normalize(Request.getProfileId());
        String problem = ProfileIds.problem(profileId);
        if (problem != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", problem));
        }
        if (users.existsByProfileId(profileId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "@" + profileId + " is already taken", "code", "PROFILE_ID_TAKEN"));
        }
        entity.setProfileId(profileId);
        try {
            authStoreRepository.saveAndFlush(entity);
            return ResponseEntity.ok().build();
        } catch (DataIntegrityViolationException raced) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "That email or Profile ID was just taken. Try again."));
        } catch (Exception err) {
            System.out.println("Error while saving user data : " + err);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something Went Wrong"));
        }
    }
}