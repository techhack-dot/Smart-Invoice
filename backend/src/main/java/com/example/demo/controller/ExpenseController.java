package com.example.demo.controller;

import com.example.demo.model.Expense;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*") // Allows your Vercel frontend without CORS blocks
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // Use absolute path for reliable file storage in Docker / Render & Windows
    private final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();

    // 1. ADD EXPENSE WITH MULTIPART FILE & ENTERPRISE AUDIT ENGINE
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addExpense(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("title") String title,
            @RequestParam("amount") Double amount,
            @RequestParam("category") String category,
            @RequestParam("expenseDate") String expenseDate,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        Expense expense = new Expense();
        expense.setTitle(title);
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setExpenseDate(LocalDate.parse(expenseDate));
        expense.setUsername(username);

        // --- PART A: ROBUST FILE STORAGE (PDF / IMAGE) ---
        if (file != null && !file.isEmpty()) {
            try {
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFilename = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
                String fileName = System.currentTimeMillis() + "_" + originalFilename;
                Path targetLocation = uploadPath.resolve(fileName);

                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                expense.setReceiptFileName(fileName);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Error saving file: " + e.getMessage());
            }
        }

        // --- PART B: ENTERPRISE AUDIT & COMPLIANCE ENGINE ---
        String auditStatus = "COMPLIANT";
        String auditMessage = "Policy Verified";

        // Rule 1: Duplicate Invoice Fraud Check (checks if same amount and title exists for this user)
        List<Expense> existing = expenseRepository.findByUsername(username);
        boolean isDuplicate = existing.stream().anyMatch(e ->
                e.getAmount().equals(amount) && e.getTitle().equalsIgnoreCase(title)
        );

        if (isDuplicate) {
            auditStatus = "FLAGGED";
            auditMessage = "DUPLICATE ALERT: An identical invoice was already claimed!";
        }
        // Rule 2: Corporate Policy Cap (Meals > $75 or Office > $200)
        else if (category.equalsIgnoreCase("Meals") && amount > 75.0) {
            auditStatus = "FLAGGED";
            auditMessage = "POLICY VIOLATION: Meals exceed the $75.00 corporate cap without VP approval.";
        } else if (category.equalsIgnoreCase("Office") && amount > 200.0) {
            auditStatus = "FLAGGED";
            auditMessage = "POLICY VIOLATION: Office supplies over $200 require purchase order (PO).";
        }

        expense.setAuditStatus(auditStatus);
        expense.setAuditMessage(auditMessage);
        // ----------------------------------------------------

        Expense saved = expenseRepository.save(expense);
        return ResponseEntity.ok(saved);
    }

    // 2. GET USER EXPENSES
    @GetMapping("/user")
    public List<Expense> getExpenses(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        return expenseRepository.findByUsername(username);
    }

    // 3. VIEW / DOWNLOAD RECEIPT (PDF OR IMAGE INLINE)
    @GetMapping("/receipt/{fileName:.+}")
    public ResponseEntity<Resource> getReceipt(@PathVariable String fileName) {
        try {
            Path filePath = uploadPath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 4. DELETE EXPENSE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }
}