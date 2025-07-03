package com.referralapp.JobReferralApp.registration.token;

import com.referralapp.JobReferralApp.appuser.AppUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {
    Optional<ConfirmationToken> findByToken(String token);

    @Modifying
    @Query("UPDATE ConfirmationToken c SET c.confirmedAt = ?2 WHERE c.token = ?1")
    int updateConfirmedAt(String token, LocalDateTime confirmedAt);

    // New methods for approval system
    Optional<ConfirmationToken> findByApprovalToken(String approvalToken);
    
    Optional<ConfirmationToken> findByAppUserEmailAndAppUserAppUserRoleAndExpiresAtAfter(
            String email, AppUserRole appUserRole, LocalDateTime now);
}
