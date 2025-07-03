package com.referralapp.JobReferralApp.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import com.referralapp.JobReferralApp.appuser.AppUserRepository;
import com.referralapp.JobReferralApp.appuser.AppUser;
import com.referralapp.JobReferralApp.email.EmailService;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private EmailService emailService;

    // TODO: Restrict to recruiter role
    @PostMapping("/create")
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        job.setId(null); // Ensure new job
        Job savedJob = jobRepository.save(job);
        return ResponseEntity.ok(savedJob);
    }

    // Get jobs for a specific recruiter
    @GetMapping("/my")
    public ResponseEntity<List<Job>> getMyJobs(@RequestParam Long recruiterId) {
        List<Job> jobs = jobRepository.findByRecruiterId(recruiterId);
        return ResponseEntity.ok(jobs);
    }

    // Edit job
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job jobDetails) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (!jobOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Job job = jobOpt.get();
        job.setTitle(jobDetails.getTitle());
        job.setDescription(jobDetails.getDescription());
        job.setRequiredSkills(jobDetails.getRequiredSkills());
        job.setYearsOfExperience(jobDetails.getYearsOfExperience());
        Job updatedJob = jobRepository.save(job);
        return ResponseEntity.ok(updatedJob);
    }

    // Delete job
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        if (!jobRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        jobRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Public endpoint to get all jobs
    @GetMapping("/all")
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getJobRecommendations(@RequestParam("userId") Long userId) {
        try {
            Optional<Resume> resumeOpt = resumeRepository.findByUserId(userId);
            if (resumeOpt.isEmpty()) {
                return ResponseEntity.ok(java.util.Collections.emptyList()); // No resume, return empty list
            }
            Resume resume = resumeOpt.get();
            java.util.List<String> userSkills = resume.getSkills() != null ? resume.getSkills() : java.util.Collections.emptyList();
            int userYears = 0;
            // Optionally extract years of experience from parsedData if needed
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(resume.getParsedData());
                JsonNode data = root.path("data");
                userYears = data.path("totalYearsExperience").asInt(0);
            } catch (Exception e) {
                userYears = 0;
            }
            if (userSkills.isEmpty()) {
                return ResponseEntity.ok(java.util.Collections.emptyList()); // No skills, return empty list
            }
            List<Job> jobs = jobRepository.findAll();
            List<Object> recommendations = new java.util.ArrayList<>();
            for (Job job : jobs) {
                List<String> requiredSkills = job.getRequiredSkills() != null ? job.getRequiredSkills() : java.util.Collections.emptyList();
                int requiredYears = job.getYearsOfExperience();
                // Skill match
                List<String> matchedSkills = new java.util.ArrayList<>();
                List<String> missingSkills = new java.util.ArrayList<>();
                for (String skill : requiredSkills) {
                    if (userSkills.stream().anyMatch(s -> s.equalsIgnoreCase(skill))) {
                        matchedSkills.add(skill);
                    } else {
                        missingSkills.add(skill);
                    }
                }
                // Only recommend if at least one skill matches
                if (matchedSkills.isEmpty()) {
                    continue;
                }
                double skillMatch = requiredSkills.isEmpty() ? 1.0 : ((double) matchedSkills.size() / requiredSkills.size());
                // Experience match
                boolean experienceMatch = userYears >= requiredYears;
                double matchPercent = skillMatch * 0.8 + (experienceMatch ? 0.2 : 0.0);
                int matchPercentage = (int) Math.round(matchPercent * 100);
                recommendations.add(java.util.Map.of(
                    "job", job,
                    "matchPercentage", matchPercentage,
                    "matchedSkills", matchedSkills,
                    "missingSkills", missingSkills,
                    "experienceMatch", experienceMatch
                ));
            }
            // Sort by match percentage descending
            recommendations.sort((a, b) -> Integer.compare((Integer) ((java.util.Map)a).get("matchPercentage"), (Integer) ((java.util.Map)b).get("matchPercentage")) * -1);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate recommendations: " + e.getMessage());
        }
    }

    // Job seeker applies to a job
    @PostMapping("/applications/apply")
    public ResponseEntity<?> applyToJob(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long jobId = payload.get("jobId");
        if (userId == null || jobId == null) {
            return ResponseEntity.badRequest().body("userId and jobId are required");
        }
        // Optionally, check if already applied
        if (applicationRepository.existsByUserIdAndJobId(userId, jobId)) {
            return ResponseEntity.status(409).body("You have already applied to this job.");
        }
        Application application = new Application();
        application.setUserId(userId);
        application.setJobId(jobId);
        // Find recruiterId from job
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            application.setRecruiterId(jobOpt.get().getRecruiterId());
        }
        application.setStatus("APPLIED");
        applicationRepository.save(application);
        return ResponseEntity.ok(application);
    }

    // Get all applications for a user
    @GetMapping("/applications/byUser")
    public ResponseEntity<?> getApplicationsByUser(@RequestParam("userId") Long userId) {
        List<Application> applications = applicationRepository.findAllByUserId(userId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Application app : applications) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", app.getId());
            map.put("jobId", app.getJobId());
            map.put("status", app.getStatus());
            Optional<Job> jobOpt = jobRepository.findById(app.getJobId());
            if (jobOpt.isPresent()) {
                map.put("jobTitle", jobOpt.get().getTitle());
                map.put("jobDescription", jobOpt.get().getDescription());
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // Get all applications for a recruiter
    @GetMapping("/applications/byRecruiter")
    public ResponseEntity<?> getApplicationsByRecruiter(@RequestParam("recruiterId") Long recruiterId) {
        List<Application> applications = applicationRepository.findAllByRecruiterId(recruiterId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Application app : applications) {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", app.getId());
            map.put("jobId", app.getJobId());
            map.put("status", app.getStatus());
            Optional<Job> jobOpt = jobRepository.findById(app.getJobId());
            if (jobOpt.isPresent()) {
                map.put("jobTitle", jobOpt.get().getTitle());
            }
            Optional<AppUser> userOpt = appUserRepository.findById(app.getUserId());
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                map.put("userId", user.getId());
                map.put("userEmail", user.getEmail());
                map.put("userFirstName", user.getFirstName());
                map.put("userLastName", user.getLastName());
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // Shortlist an application and send email
    @PostMapping("/applications/shortlist")
    public ResponseEntity<?> shortlistApplication(@RequestBody Map<String, Object> payload) {
        Long applicationId = ((Number)payload.get("applicationId")).longValue();
        String message = (String) payload.get("message");
        Optional<Application> appOpt = applicationRepository.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Application not found");
        }
        Application app = appOpt.get();
        app.setStatus("SHORTLISTED");
        applicationRepository.save(app);
        // Fetch applicant and job details
        Optional<AppUser> userOpt = appUserRepository.findById(app.getUserId());
        Optional<Job> jobOpt = jobRepository.findById(app.getJobId());
        if (userOpt.isPresent() && jobOpt.isPresent()) {
            AppUser user = userOpt.get();
            Job job = jobOpt.get();
            String emailBody = "<p>Dear " + user.getFirstName() + ",</p>"
                + "<p>Congratulations! You have been shortlisted for the position of <b>" + job.getTitle() + "</b>.</p>"
                + "<p>Message from recruiter:</p>"
                + "<blockquote>" + message + "</blockquote>"
                + "<p>Best of luck!<br/>HireVision Team</p>";
            emailService.send(user.getEmail(), "You have been shortlisted!", emailBody);
        }
        return ResponseEntity.ok("Application shortlisted and email sent.");
    }
} 