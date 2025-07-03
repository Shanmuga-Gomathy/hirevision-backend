package com.referralapp.JobReferralApp.authentication.dto;

import com.referralapp.JobReferralApp.appuser.AppUserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String firstName;
    private String lastName;
    private AppUserRole role;
    private String message;
    private Long id;
} 