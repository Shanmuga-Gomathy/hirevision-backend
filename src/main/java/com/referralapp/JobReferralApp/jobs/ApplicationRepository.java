package com.referralapp.JobReferralApp.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    int countByRecruiterId(Long recruiterId);
    int countByRecruiterIdAndStatus(Long recruiterId, String status);
    boolean existsByUserIdAndJobId(Long userId, Long jobId);
    java.util.List<Application> findAllByUserId(Long userId);
    java.util.List<Application> findAllByRecruiterId(Long recruiterId);
} 