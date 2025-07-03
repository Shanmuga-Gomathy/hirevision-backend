package com.referralapp.JobReferralApp.registration.token;

import com.referralapp.JobReferralApp.appuser.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ConfirmationToken {

    
    @SequenceGenerator(
            name = "confirmation_token_sequence",
            sequenceName = "confirmation_token_sequence",
            allocationSize = 1
    )
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "confirmation_token_sequence"
    )

    private Long id;
    @Column(nullable = false)
    private String token;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;

    // New fields for approval system
    private String approvalToken; // For admin approval links
    
    @Enumerated(EnumType.STRING)
    private TokenType tokenType = TokenType.EMAIL_CONFIRMATION;

    @ManyToOne
    @JoinColumn(
            nullable = false,
            name = "app_user_id"
    )
    private AppUser appUser;

    public ConfirmationToken(String token, LocalDateTime createdAt, LocalDateTime expiresAt, AppUser appUser) {
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.appUser = appUser;
        this.tokenType = TokenType.EMAIL_CONFIRMATION;
    }
    
    public ConfirmationToken(String token, LocalDateTime createdAt, LocalDateTime expiresAt, AppUser appUser, TokenType tokenType) {
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.appUser = appUser;
        this.tokenType = tokenType;
    }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    public boolean isConfirmed() {
        return this.confirmedAt != null;
    }
    
    public boolean isEmailConfirmation() {
        return TokenType.EMAIL_CONFIRMATION.equals(this.tokenType);
    }
    
    public boolean isAdminApproval() {
        return TokenType.ADMIN_APPROVAL.equals(this.tokenType);
    }
    
    public void confirm() {
        this.confirmedAt = LocalDateTime.now();
    }
}
