package com.evolyn.evolyn_data_service.VerifyUserDetails;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.evolyn.evolyn_data_service.Security.PasswordHasher;

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
    private final PasswordHasher passwordHasher;

    public VerifyUserDetails(VerifyUserRepository verifyUserRepositor, PasswordHasher passwordHasher){
        this.verifyUserRepository = verifyUserRepositor;
        this.passwordHasher = passwordHasher;
    }

    @PostMapping("/verify/userdetails")
    public ResponseEntity<Object> verifyUserDetails (@RequestBody VerifyUserDetailsDTO Request) {
        // Never log Request.getPassword()
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
            String stored = user.getPassword();
            boolean correct;
            if (passwordHasher.isHashed(stored)) {
                correct = passwordHasher.matches(Request.getPassword(), stored);
            } else {
                // Account from before hashing: compare the plain value once,
                // and if it's right, replace it with a hash right away.
                correct = stored != null && stored.equals(Request.getPassword());
                if (correct) user.setPassword(passwordHasher.hash(Request.getPassword()));
            }
            if(!correct) {
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
