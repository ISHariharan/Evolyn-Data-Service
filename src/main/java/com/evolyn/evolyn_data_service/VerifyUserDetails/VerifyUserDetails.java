package com.evolyn.evolyn_data_service.VerifyUserDetails;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.AuthStoreService.AuthStoreDAO;

@RestController
@RequestMapping("/evolyn/api/internal")
public class VerifyUserDetails {
    public final VerifyUserRepository verifyUserRepository;
    public VerifyUserDetails(VerifyUserRepository verifyUserRepositor){
        this.verifyUserRepository = verifyUserRepositor;
    }

    @PostMapping("/verify/userdetails")
    public ResponseEntity<Object> verifyUserDetails (@RequestBody VerifyUserDetailsDTO Request) {
        System.out.println("Request Email : " + Request.getEmail());
        System.out.println("Request Password : " + Request.getPassword());
        try {
            if(!verifyUserRepository.existsByEmail(Request.getEmail())){
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User Not Found"));
            }
            Optional<AuthStoreDAO> userOpt = verifyUserRepository.findByEmail(Request.getEmail());
            if(userOpt.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User Not Found"));
            }

            AuthStoreDAO user = userOpt.get();
            if(!user.getPassword().equals(Request.getPassword())) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Wrong Password"));
            }
            user.setStatus("loggedin");
            verifyUserRepository.save(user);
            return ResponseEntity.ok(Map.of(
                "message", "User verified successfully"
            ));
        } catch (Exception err) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User Not Found"));
        }
    }

    @PostMapping("/loggedin/user")
    public ResponseEntity<Object> verifyLoggedInUser(@RequestBody VerifyLoggedInUserDTO Request) {
        System.out.println("Request Email : " + Request.getEmail());
        try {
            String userFirtName = "";
            String userLastName = "";
            UUID userId = null;
            boolean exists = verifyUserRepository.existsByEmail(Request.getEmail());
            if(exists) {
                Optional<AuthStoreDAO> userOpt = verifyUserRepository.findByEmail(Request.getEmail());
                AuthStoreDAO user = userOpt.get();
                userFirtName = user.getFirstName();
                userLastName = user.getLastName();
                userId = user.getUuid();
            }
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of(
                        "firstName", userFirtName,
                        "lastName", userLastName,
                        "Id", userId
                    ));
        } catch (Exception err) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "User Not Found"));
        }
    }

}
