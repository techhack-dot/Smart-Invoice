package com.example.demo.controller;

import com.example.demo.model.Expense;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // 1. ADD EXPENSE WITH RECEIPT DATA & ENTERPRISE AUDIT
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

        // Store file directly into the cloud database
        if (file != null && !file.isEmpty()) {
            try {
                expense.setReceiptData(file.getBytes());
                expense.setReceiptFileType(file.getContentType());
                expense.setReceiptFileName(file.getOriginalFilename());
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Error processing file bytes: " + e.getMessage());
            }
        }

        // Enterprise Smart-Audit Rules
        String auditStatus = "COMPLIANT";
        String auditMessage = "Policy Verified";

        List<Expense> existing = expenseRepository.findByUsername(username);
        boolean isDuplicate = existing.stream().anyMatch(e ->
                e.getAmount().equals(amount) && e.getTitle().equalsIgnoreCase(title)
        );

        if (isDuplicate) {
            auditStatus = "FLAGGED";
            auditMessage = "DUPLICATE ALERT: Identical claim already exists!";
        } else if (category.equalsIgnoreCase("Meals") && amount > 75.0) {
            auditStatus = "FLAGGED";
            auditMessage = "POLICY VIOLATION: Meals exceed $75.00 limit.";
        } else if (category.equalsIgnoreCase("Office") && amount > 200.0) {
            auditStatus = "FLAGGED";
            auditMessage = "POLICY VIOLATION: Office supplies exceed $200.00 limit.";
        }

        expense.setAuditStatus(auditStatus);
        expense.setAuditMessage(auditMessage);

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

    // 3. VIEW RECEIPT BY ID (100% Cloud-Safe, No 404s)
    @GetMapping("/receipt/{id}")
    public ResponseEntity<byte[]> getReceipt(@PathVariable Long id) {
        Optional<Expense> expOpt = expenseRepository.findById(id);

        if (expOpt.isPresent() && expOpt.get().getReceiptData() != null) {
            Expense exp = expOpt.get();
            String contentType = exp.getReceiptFileType() != null ? exp.getReceiptFileType() : "application/pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + exp.getReceiptFileName() + "\"")
                    .body(exp.getReceiptData());
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