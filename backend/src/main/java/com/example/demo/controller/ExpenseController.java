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
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
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