package com.referralapp.JobReferralApp.registration.token;

import com.referralapp.JobReferralApp.appuser.AppUserRole;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ConfirmationTokenService {

    private final ConfirmationTokenRepository confirmationTokenRepository;

    // Save token to DB
    public void saveConfirmationToken(ConfirmationToken token) {
        confirmationTokenRepository.save(token);
    }

    // Get token from DB
    public Optional<ConfirmationToken> getToken(String token) {
        return confirmationTokenRepository.findByToken(token);
    }

    // Update confirmedAt to now
    public int setConfirmedAt(String token) {
        return confirmationTokenRepository.updateConfirmedAt(token, LocalDateTime.now());
    }

    // New methods for approval system
    public void saveToken(ConfirmationToken token) {
        confirmationTokenRepository.save(token);
    }

    public Optional<ConfirmationToken> getTokenByApprovalToken(String approvalToken) {
        return confirmationTokenRepository.findByApprovalToken(approvalToken);
    }

    public boolean hasActiveToken(String email, AppUserRole userType) {
        return confirmationTokenRepository.findByAppUserEmailAndAppUserAppUserRoleAndExpiresAtAfter(
                email, userType, LocalDateTime.now()).isPresent();
    }
}
