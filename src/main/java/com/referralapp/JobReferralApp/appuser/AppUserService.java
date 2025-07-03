package com.referralapp.JobReferralApp.appuser;

import com.referralapp.JobReferralApp.registration.token.ConfirmationToken;
import com.referralapp.JobReferralApp.registration.token.ConfirmationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserService implements UserDetailsService {

    private final static String USER_NOT_FOUND_MSG = "User with email id %s not found";
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfirmationTokenService confirmationTokenService;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, email)));
    }

    public int enableAppUser(String email) {
        return appUserRepository.enableAppUser(email);
    }

    public String signUpUser(AppUser appUser, int expiry, boolean isHours) {
        boolean userExists = appUserRepository.findByEmailAndAppUserRole(
                appUser.getEmail(), appUser.getAppUserRole()).isPresent();

        if (userExists) {
            AppUser existingUser = appUserRepository.findByEmailAndAppUserRole(
                    appUser.getEmail(), appUser.getAppUserRole()).get();

            if (existingUser.isEnabled()) {
                throw new IllegalStateException("Email already confirmed and in use for this role");
            }

            String token = UUID.randomUUID().toString();
            ConfirmationToken confirmationToken = new ConfirmationToken(
                    token,
                    LocalDateTime.now(),
                    isHours ? LocalDateTime.now().plusHours(expiry) : LocalDateTime.now().plusMinutes(expiry),
                    existingUser
            );

            confirmationTokenService.saveConfirmationToken(confirmationToken);
            return token;
        }

        String encodedPassword = passwordEncoder.encode(appUser.getPassword());
        appUser.setPassword(encodedPassword);
        System.out.println("DEBUG: Creating user with role: " + appUser.getAppUserRole());
        appUserRepository.save(appUser);

        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                LocalDateTime.now(),
                isHours ? LocalDateTime.now().plusHours(expiry) : LocalDateTime.now().plusMinutes(expiry),
                appUser
        );
        confirmationTokenService.saveConfirmationToken(confirmationToken);

        return token;
    }

    // Default to 15 minutes for backward compatibility
    public String signUpUser(AppUser appUser) {
        return signUpUser(appUser, 15, false);
    }

    // New methods for approval system
    public boolean existsByEmailAndUserType(String email, AppUserRole userType) {
        return appUserRepository.findByEmailAndAppUserRole(email, userType).isPresent();
    }

    public AppUser updateUser(AppUser user) {
        return appUserRepository.save(user);
    }

    public AppUser findByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG, email)));
    }

    public int fixNullEnabledValues() {
        return appUserRepository.fixNullEnabledValues();
    }

    public AppUser findByEmailAndUserType(String email, String userType) {
        AppUserRole role = AppUserRole.valueOf(userType.toUpperCase());
        return appUserRepository.findByEmailAndAppUserRole(email, role)
                .orElseThrow(() -> new UsernameNotFoundException(String.format(USER_NOT_FOUND_MSG + " and role %s", email, userType)));
    }
}
