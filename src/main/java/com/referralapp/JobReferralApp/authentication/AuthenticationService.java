package com.referralapp.JobReferralApp.authentication;

import com.referralapp.JobReferralApp.authentication.dto.LoginRequest;
import com.referralapp.JobReferralApp.authentication.dto.LoginResponse;
import com.referralapp.JobReferralApp.appuser.AppUser;
import com.referralapp.JobReferralApp.appuser.AppUserService;
import com.referralapp.JobReferralApp.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final AppUserService appUserService;
    private final JwtService jwtService;

    public LoginResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        AppUser user = appUserService.findByEmailAndUserType(request.getEmail(), request.getUserType());
        UserDetails userDetails = user;
        
        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        
        return LoginResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getAppUserRole())
                .id(user.getId())
                .message("Login successful")
                .build();
    }

    public LoginResponse refreshToken(String refreshToken) {
        final String userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            UserDetails userDetails = appUserService.loadUserByUsername(userEmail);
            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                AppUser user = appUserService.findByEmail(userEmail);
                String newAccessToken = jwtService.generateToken(userDetails);
                
                return LoginResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshToken)
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getAppUserRole())
                        .id(user.getId())
                        .message("Token refreshed successfully")
                        .build();
            }
        }
        throw new RuntimeException("Invalid refresh token");
    }
} 