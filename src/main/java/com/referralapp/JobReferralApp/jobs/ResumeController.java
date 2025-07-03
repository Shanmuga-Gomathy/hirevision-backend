package com.referralapp.JobReferralApp.jobs;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {
    private static final String AFFINDA_API_KEY = "aff_950169bd0ff0e7fd6c4ac57263f38306e101b418";
    private static final String AFFINDA_URL = "https://api.affinda.com/v2/resumes";

    @Autowired
    private ResumeRepository resumeRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("resume") MultipartFile resume, @RequestParam("userId") Long userId) {
        try {
            // Prepare request to Affinda using a configured RestTemplate
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
            restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + AFFINDA_API_KEY);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(resume.getBytes()) {
                @Override
                public String getFilename() {
                    return resume.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response;
            try {
                response = restTemplate.postForEntity(AFFINDA_URL, requestEntity, String.class);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                System.err.println("Error from Affinda API: " + e.getStatusCode());
                System.err.println("Response body: " + e.getResponseBodyAsString());
                throw new RuntimeException("Affinda API request failed", e);
            }

            // Save to DB (replace if exists)
            Optional<Resume> existing = resumeRepository.findByUserId(userId);
            Resume resumeEntity = existing.orElse(new Resume());
            resumeEntity.setUserId(userId);
            resumeEntity.setOriginalFileName(resume.getOriginalFilename());
            resumeEntity.setParsedData(response.getBody());
            resumeEntity.setCreatedAt(java.time.LocalDateTime.now());
            resumeEntity.setFileData(resume.getBytes());
            // Extract all skills from Affinda JSON and store in skills field
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                java.util.List<String> skills = new java.util.ArrayList<>();
                if (data.path("skills").isArray()) {
                    for (JsonNode skill : data.path("skills")) {
                        String skillName = skill.path("name").asText("");
                        if (!skillName.isEmpty()) skills.add(skillName);
                    }
                }
                resumeEntity.setSkills(skills);
            } catch (Exception e) {
                resumeEntity.setSkills(null);
            }
            resumeRepository.save(resumeEntity);

            return ResponseEntity.ok(new ObjectMapper().readTree(response.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to parse resume: " + e.getMessage());
        }
    }

    @GetMapping("/file")
    public ResponseEntity<?> getResumeFile(@RequestParam("userId") Long userId) {
        Optional<Resume> resumeOpt = resumeRepository.findByUserId(userId);
        if (resumeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No resume found for user.");
        }
        Resume resume = resumeOpt.get();
        if (resume.getFileData() != null) {
            String fileName = resume.getOriginalFileName() != null ? resume.getOriginalFileName() : "resume.pdf";
            String lowerFileName = fileName.toLowerCase();
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            String contentDisposition = "attachment; filename=" + fileName;
            if (lowerFileName.endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
                contentDisposition = "inline; filename=" + fileName;
            } else if (lowerFileName.endsWith(".docx")) {
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                contentDisposition = "attachment; filename=" + fileName;
            }
            return ResponseEntity.ok()
                .header("Content-Disposition", contentDisposition)
                .contentType(mediaType)
                .body(resume.getFileData());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume file not stored.");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyResume(@RequestParam("userId") Long userId) {
        Optional<Resume> resume = resumeRepository.findByUserId(userId);
        if (resume.isPresent()) {
            return ResponseEntity.ok(resume.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No resume found for user.");
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyResume(@RequestParam("userId") Long userId) {
        Optional<Resume> resume = resumeRepository.findByUserId(userId);
        if (resume.isPresent()) {
            resumeRepository.delete(resume.get());
            return ResponseEntity.ok("Resume deleted.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No resume found for user.");
        }
    }

    @GetMapping("/me/parsed")
    public ResponseEntity<?> getParsedResume(@RequestParam("userId") Long userId) {
        Optional<Resume> resumeOpt = resumeRepository.findByUserId(userId);
        if (resumeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No resume found for user.");
        }
        Resume resume = resumeOpt.get();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resume.getParsedData());
            JsonNode data = root.path("data");
            String name = data.path("name").path("raw").asText("");
            String email = data.path("emails").isArray() && data.path("emails").size() > 0 ? data.path("emails").get(0).asText("") : "";
            int years = data.path("totalYearsExperience").asInt(0);
            java.util.List<String> skills = new java.util.ArrayList<>();
            if (data.path("skills").isArray()) {
                for (JsonNode skill : data.path("skills")) {
                    String skillName = skill.path("name").asText("");
                    if (!skillName.isEmpty()) skills.add(skillName);
                }
            }
            java.util.Map<String, Object> result = java.util.Map.of(
                "name", name,
                "email", email,
                "yearsOfExperience", years,
                "skills", skills,
                "raw", root
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to parse resume data: " + e.getMessage());
        }
    }
} 