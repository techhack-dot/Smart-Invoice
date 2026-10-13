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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final String UPLOAD_DIR = "uploads/";

    // 1. ADD EXPENSE WITH FILE UPLOAD
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

        // Save file if provided
        if (file != null && !file.isEmpty()) {
            try {
                File dir = new File(UPLOAD_DIR);
                if (!dir.exists()) dir.mkdirs();

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                expense.setReceiptFileName(fileName);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Error saving file: " + e.getMessage());
            }
        }
        String auditStatus = "COMPLIANT";
String auditMessage = "Policy Verified";

// 2. CHECK RULE 1: DUPLICATE FRAUD DETECTION
// Look into the database for all previous bills uploaded by this user
List<Expense> existing = expenseRepository.findByUsername(username);

// Check if any old bill has the exact same Title and exact same Amount
boolean isDuplicate = existing.stream().anyMatch(e -> 
    e.getAmount().equals(amount) && e.getTitle().equalsIgnoreCase(title)
);

if (isDuplicate) {
    // If an identical bill already exists, flag it!
    auditStatus = "FLAGGED";
    auditMessage = "DUPLICATE ALERT: An identical invoice was already claimed!";
} 

// 3. CHECK RULE 2: COMPANY SPENDING LIMIT
// If the category is "Meals" and the bill is greater than $75:
else if (category.equalsIgnoreCase("Meals") && amount > 75.0) {
    auditStatus = "FLAGGED";
    auditMessage = "POLICY VIOLATION: Meals exceed the Rs.75.00 corporate cap without VP approval.";
}
else if (category.equalsIgnoreCase("Office") && amount > 200.0) {
            auditStatus = "FLAGGED";
            auditMessage = "POLICY VIOLATION: Office supplies over Rs.200 require purchase order (PO).";
        }
// 4. Attach these audit results to our Expense object
expense.setAuditStatus(auditStatus);
expense.setAuditMessage(auditMessage);

        Expense saved = expenseRepository.save(expense);
        return ResponseEntity.ok(saved);
    }

    // 2. GET ALL EXPENSES FOR CURRENT LOGGED IN USER
    @GetMapping("/user")
    public List<Expense> getExpenses(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);
        return expenseRepository.findByUsername(username);
    }

    // 3. DOWNLOAD / VIEW RECEIPT FILE
    @GetMapping("/receipt/{fileName}")
    public ResponseEntity<Resource> getReceipt(@PathVariable String fileName) {
        try {
            Path path = Paths.get(UPLOAD_DIR).resolve(fileName);
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.notFound().build();
    }

    // 4. DELETE EXPENSE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }
}