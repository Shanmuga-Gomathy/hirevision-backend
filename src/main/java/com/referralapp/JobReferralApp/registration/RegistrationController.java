package com.referralapp.JobReferralApp.registration;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path="api/v1/registration")
@AllArgsConstructor
@Validated
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegistrationRequest request) {
        try {
            String result = registrationService.register(request);
            Map<String, String> response = new HashMap<>();
            
            if (result.contains("successful")) {
                response.put("status", "success");
                response.put("message", result);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", result);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping(path = "confirm")
    public ResponseEntity<Map<String, String>> confirm(@RequestParam("token") String token) {
        try {
            String result = registrationService.confirmToken(token);
            Map<String, String> response = new HashMap<>();
            
            if ("confirmed".equals(result)) {
                response.put("status", "success");
                response.put("message", "Email confirmed successfully!");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", result);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Email confirmation failed. Please try again.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping(path = "approve")
    public ResponseEntity<Map<String, String>> approveRecruiter(@RequestParam("token") String token) {
        try {
            String result = registrationService.approveRecruiter(token);
            Map<String, String> response = new HashMap<>();
            
            if (result.contains("successfully")) {
                response.put("status", "success");
                response.put("message", result);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", result);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Approval failed. Please try again.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping(path = "reject")
    public ResponseEntity<Map<String, String>> rejectRecruiter(@RequestParam("token") String token) {
        try {
            String result = registrationService.rejectRecruiter(token);
            Map<String, String> response = new HashMap<>();
            
            if (result.contains("successfully")) {
                response.put("status", "success");
                response.put("message", result);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", result);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Rejection failed. Please try again.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping(path = "check-email")
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(
            @RequestParam("email") String email,
            @RequestParam("userType") String userType) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isAvailable = !registrationService.existsByEmailAndUserType(email, userType);
            response.put("available", isAvailable);
            response.put("message", isAvailable ? "Email available" : "Email already registered for this role");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("available", false);
            response.put("message", "Error checking email availability");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping(path = "health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Registration service is running");
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "fix-enabled-values")
    public ResponseEntity<Map<String, Object>> fixEnabledValues() {
        Map<String, Object> response = new HashMap<>();
        try {
            int fixedCount = registrationService.fixNullEnabledValues();
            response.put("status", "success");
            response.put("message", "Fixed " + fixedCount + " records with null enabled values");
            response.put("fixedCount", fixedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to fix enabled values: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping(path = "enable-user")
    public ResponseEntity<Map<String, Object>> enableUser(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = registrationService.enableUserManually(email);
            if (success) {
                response.put("status", "success");
                response.put("message", "User " + email + " enabled successfully");
            } else {
                response.put("status", "error");
                response.put("message", "Failed to enable user " + email);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error enabling user: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
