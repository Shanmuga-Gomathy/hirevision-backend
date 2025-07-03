package com.referralapp.JobReferralApp.appuser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    
    Optional<AppUser> findByEmailAndAppUserRole(String email, AppUserRole appUserRole);
    
    @Modifying
    @Query("UPDATE AppUser a SET a.enabled = TRUE WHERE a.email = ?1 AND (a.enabled = FALSE OR a.enabled IS NULL)")
    int enableAppUser(String email);
    
    @Modifying
    @Query("UPDATE AppUser a SET a.enabled = FALSE WHERE a.enabled IS NULL")
    int fixNullEnabledValues();
}
