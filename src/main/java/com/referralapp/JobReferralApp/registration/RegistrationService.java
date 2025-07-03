package com.referralapp.JobReferralApp.registration;

import com.referralapp.JobReferralApp.appuser.AppUser;
import com.referralapp.JobReferralApp.appuser.AppUserRole;
import com.referralapp.JobReferralApp.appuser.AppUserService;
import com.referralapp.JobReferralApp.appuser.ApprovalStatus;
import com.referralapp.JobReferralApp.email.EmailSender;
import com.referralapp.JobReferralApp.registration.token.ConfirmationToken;
import com.referralapp.JobReferralApp.registration.token.ConfirmationTokenService;
import com.referralapp.JobReferralApp.registration.token.TokenType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final AppUserService appUserService;
    private final EmailValidator emailValidator;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailSender emailSender;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.name}")
    private String adminName;

    @Value("${token.expiry.minutes}")
    private int tokenExpiryMinutes;

    @Value("${token.expiry.admin-approval.hours}")
    private int adminApprovalTokenExpiryHours;

    @Value("${app.backend-url}")
    private String backendUrl;

    public String register(RegistrationRequest request) {
        // 1. Validate email format
        boolean isValidEmail = emailValidator.test(request.getEmail());
        if (!isValidEmail) {
            return "Please enter a valid email address";
        }

        // 2. Validate and parse the user type
        AppUserRole userRole;
        try {
            userRole = AppUserRole.valueOf(request.getUserType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return "Invalid user type. Please select a valid user type: USER or RECRUITER";
        }

        // Debug logging
        System.out.println("DEBUG: Registration request - Email: " + request.getEmail() + ", Role: " + userRole);

        // 3. Block ADMIN registration
        if (userRole == AppUserRole.ADMIN) {
            return "Admin registration is not allowed. Admin accounts are pre-configured.";
        }

        // 4. Check if email + userType combination already exists
        if (appUserService.existsByEmailAndUserType(request.getEmail(), userRole)) {
            return "Email already registered for this role. Please use a different email or select a different role.";
        }

        // 5. Check for active tokens (prevent spam)
        if (confirmationTokenService.hasActiveToken(request.getEmail(), userRole)) {
            return "A confirmation email has already been sent. Please check your email or wait for the token to expire.";
        }

        // 6. Create user with appropriate approval status
        AppUser user = new AppUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword(),
                userRole
        );

        // Debug logging
        System.out.println("DEBUG: Created user object with role: " + user.getAppUserRole());

        // Set approval status based on user type
        if (userRole == AppUserRole.USER) {
            user.setApprovalStatus(ApprovalStatus.PENDING); // Will be approved after email confirmation
        } else if (userRole == AppUserRole.RECRUITER) {
            user.setApprovalStatus(ApprovalStatus.PENDING); // Needs admin approval
        }

        // 7. Sign up user and get token
        String token;
        if (userRole == AppUserRole.RECRUITER) {
            token = appUserService.signUpUser(user, adminApprovalTokenExpiryHours, true);
        } else {
            token = appUserService.signUpUser(user, tokenExpiryMinutes, false);
        }

        // 8. Send appropriate email based on user type
        if (userRole == AppUserRole.USER) {
            sendJobSeekerConfirmationEmail(request, token);
            return "Registration successful! Please check your email to confirm your account.";
        } else if (userRole == AppUserRole.RECRUITER) {
            sendRecruiterApprovalEmail(request, token);
            return "Registration successful! Your account is pending admin approval. You will receive an email once approved.";
        }

        return "Registration successful!";
    }

    private void sendJobSeekerConfirmationEmail(RegistrationRequest request, String token) {
        String link = backendUrl + "/api/v1/registration/confirm?token=" + token;
        String emailContent = buildJobSeekerConfirmationEmail(request.getFirstName(), link);
        emailSender.send(request.getEmail(), emailContent);
    }

    private void sendRecruiterApprovalEmail(RegistrationRequest request, String token) {
        // Send pending approval email to recruiter
        String recruiterEmailContent = buildRecruiterPendingEmail(request.getFirstName());
        emailSender.send(request.getEmail(), recruiterEmailContent);

        // Send admin notification email
        String approvalToken = UUID.randomUUID().toString();
        String approveLink = backendUrl + "/api/v1/registration/approve?token=" + approvalToken;
        String rejectLink = backendUrl + "/api/v1/registration/reject?token=" + approvalToken;
        
        String adminEmailContent = buildAdminNotificationEmail(request, approveLink, rejectLink);
        emailSender.send(adminEmail, adminEmailContent);

        // Save approval token
        ConfirmationToken confirmationToken = confirmationTokenService.getToken(token)
                .orElseThrow(() -> new IllegalStateException("Token not found"));
        confirmationToken.setApprovalToken(approvalToken);
        confirmationToken.setTokenType(TokenType.ADMIN_APPROVAL);
        confirmationTokenService.saveToken(confirmationToken);
    }

    @Transactional
    public String confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token)
                .orElseThrow(() -> new IllegalStateException("Token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            return "Email already confirmed";
        }

        if (confirmationToken.isExpired()) {
            return "Token expired. Please register again.";
        }

        // Check if it's an email confirmation token
        if (!confirmationToken.isEmailConfirmation()) {
            return "Invalid token type";
        }

        confirmationTokenService.setConfirmedAt(token);
        
        // Get the user and enable them
        AppUser user = confirmationToken.getAppUser();
        System.out.println("DEBUG: Confirming user: " + user.getEmail());
        System.out.println("DEBUG: User enabled before: " + user.isEnabled());
        System.out.println("DEBUG: User ID: " + user.getId());
        
        // Directly update the user object instead of using repository method
        user.setEnabled(true);
        AppUser savedUser = appUserService.updateUser(user);
        
        // Double-check by fetching from database
        AppUser fetchedUser = appUserService.findByEmail(user.getEmail());
        System.out.println("DEBUG: User enabled after fetch: " + fetchedUser.isEnabled());
        
        // Update approval status for job seekers
        if (fetchedUser.getAppUserRole() == AppUserRole.USER) {
            fetchedUser.setApprovalStatus(ApprovalStatus.APPROVED);
            appUserService.updateUser(fetchedUser);
        }

        return "confirmed";
    }

    @Transactional
    public String approveRecruiter(String approvalToken) {
        ConfirmationToken token = confirmationTokenService
                .getTokenByApprovalToken(approvalToken)
                .orElseThrow(() -> new IllegalStateException("Approval token not found"));

        if (token.isExpired()) {
            return "Approval token expired";
        }

        AppUser user = token.getAppUser();
        
        // Debug logging
        System.out.println("DEBUG: User found: " + user.getEmail());
        System.out.println("DEBUG: User role: " + user.getAppUserRole());
        System.out.println("DEBUG: Expected role: " + AppUserRole.RECRUITER);
        System.out.println("DEBUG: Roles match: " + (user.getAppUserRole() == AppUserRole.RECRUITER));
        
        if (user.getAppUserRole() != AppUserRole.RECRUITER) {
            return "Invalid user type for approval. User role: " + user.getAppUserRole() + ", Expected: " + AppUserRole.RECRUITER;
        }

        if (user.isApproved()) {
            return "User already approved";
        }

        // Approve the user
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setEnabled(true);
        appUserService.updateUser(user);

        // Send approval email to recruiter
        String approvalEmailContent = buildRecruiterApprovedEmail(user.getFirstName());
        emailSender.send(user.getEmail(), approvalEmailContent);

        return "Recruiter approved successfully";
    }

    @Transactional
    public String rejectRecruiter(String approvalToken) {
        ConfirmationToken token = confirmationTokenService
                .getTokenByApprovalToken(approvalToken)
                .orElseThrow(() -> new IllegalStateException("Approval token not found"));

        if (token.isExpired()) {
            return "Approval token expired";
        }

        AppUser user = token.getAppUser();
        if (user.getAppUserRole() != AppUserRole.RECRUITER) {
            return "Invalid user type for rejection";
        }

        if (user.isRejected()) {
            return "User already rejected";
        }

        // Reject the user
        user.setApprovalStatus(ApprovalStatus.REJECTED);
        appUserService.updateUser(user);

        // Send rejection email to recruiter
        String rejectionEmailContent = buildRecruiterRejectedEmail(user.getFirstName());
        emailSender.send(user.getEmail(), rejectionEmailContent);

        return "Recruiter rejected successfully";
    }

    // Email template methods
    private String buildJobSeekerConfirmationEmail(String name, String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>" +
                "<table width=\"100%\" style=\"border-collapse:collapse;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
                "<tr><td bgcolor=\"#0b0c0c\">" +
                "<table width=\"100%\" style=\"max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">" +
                "<tr><td style=\"font-size:28px;padding:10px;color:#ffffff;font-weight:700\">Confirm your HireVision AI account</td></tr>" +
                "</table></td></tr></table>" +
                "<table align=\"center\" style=\"max-width:580px;width:100%!important\" width=\"100%\">" +
                "<tr><td bgcolor=\"#1D70B8\" height=\"10\"></td></tr></table>" +
                "<table align=\"center\" style=\"max-width:580px;width:100%!important\" width=\"100%\">" +
                "<tr><td height=\"30\"></td></tr>" +
                "<tr><td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;color:#0b0c0c\">" +
                "<p>Hi " + name + ",</p>" +
                "<p>Thank you for registering with HireVision AI. Please click on the link below to activate your account:</p>" +
                "<blockquote style=\"border-left:10px solid #b1b4b6;padding:15px\">" +
                "<p><a href=\"" + link + "\">Activate Now</a></p></blockquote>" +
                "<p>This link will expire in " + tokenExpiryMinutes + " minutes.</p>" +
                "<p>Welcome to HireVision AI!</p>" +
                "</td></tr><tr><td height=\"30\"></td></tr></table></div>";
    }

    private String buildRecruiterPendingEmail(String name) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">" +
                "<table align=\"center\" style=\"max-width:580px;width:100%!important\" width=\"100%\">" +
                "<tr><td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;color:#0b0c0c\">" +
                "<p>Hi " + name + ",</p>" +
                "<p>Thank you for registering as a Recruiter with HireVision AI.</p>" +
                "<p>Your account is currently pending admin approval. You will receive an email notification once your account has been reviewed.</p>" +
                "<p>This process typically takes 24-48 hours.</p>" +
                "<p>Thank you for your patience!</p>" +
                "</td></tr></table></div>";
    }

    private String buildAdminNotificationEmail(RegistrationRequest request, String approveLink, String rejectLink) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">" +
                "<table align=\"center\" style=\"max-width:580px;width:100%!important\" width=\"100%\">" +
                "<tr><td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;color:#0b0c0c\">" +
                "<p>Hi " + adminName + ",</p>" +
                "<p>A new recruiter has registered on HireVision AI:</p>" +
                "<ul>" +
                "<li><strong>Name:</strong> " + request.getFirstName() + " " + request.getLastName() + "</li>" +
                "<li><strong>Email:</strong> " + request.getEmail() + "</li>" +
                "</ul>" +
                "<p>Please review and take action:</p>" +
                "<blockquote style=\"border-left:10px solid #b1b4b6;padding:15px\">" +
                "<p><a href=\"" + approveLink + "\" style=\"background-color:#28a745;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;margin-right:10px;\">Approve</a>" +
                "<a href=\"" + rejectLink + "\" style=\"background-color:#dc3545;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;\">Reject</a></p>" +
                "</blockquote>" +
                "<p>This approval link will expire in 48 hours.</p>" +
                "</td></tr></table></div>";
    }

    private String buildRecruiterApprovedEmail(String name) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">" +
                "<table align=\"center\" style=\"max-width:580px;width:100%!important\" width=\"100%\">" +
                "<tr><td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;color:#0b0c0c\">" +
                "<p>Hi " + name + ",</p>" +
                "<p>Great news! Your HireVision AI recruiter account has been approved.</p>" +
                "<p>You can now log in and start posting jobs and managing candidates.</p>" +
                "<p>Welcome to HireVision AI!</p>" +
                "</td></tr></table></div>";
    }

    private String buildRecruiterRejectedEmail(String name) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">" +
                "<table align=\"center\" style=\"max-width:580px;width:100%!important\" width=\"100%\">" +
                "<tr><td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;color:#0b0c0c\">" +
                "<p>Hi " + name + ",</p>" +
                "<p>We regret to inform you that your HireVision AI recruiter account application has been rejected.</p>" +
                "<p>If you believe this was an error, please contact our support team.</p>" +
                "<p>Thank you for your interest in HireVision AI.</p>" +
                "</td></tr></table></div>";
    }

    // Helper method for email availability check
    public boolean existsByEmailAndUserType(String email, String userType) {
        try {
            AppUserRole role = AppUserRole.valueOf(userType.toUpperCase());
            return appUserService.existsByEmailAndUserType(email, role);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public int fixNullEnabledValues() {
        return appUserService.fixNullEnabledValues();
    }
    
    public boolean enableUserManually(String email) {
        try {
            AppUser user = appUserService.findByEmail(email);
            System.out.println("DEBUG: Manually enabling user: " + email);
            System.out.println("DEBUG: User enabled before: " + user.isEnabled());
            
            user.setEnabled(true);
            AppUser savedUser = appUserService.updateUser(user);
            
            System.out.println("DEBUG: User enabled after: " + savedUser.isEnabled());
            return savedUser.isEnabled();
        } catch (Exception e) {
            System.err.println("Error enabling user: " + e.getMessage());
            return false;
        }
    }
}
