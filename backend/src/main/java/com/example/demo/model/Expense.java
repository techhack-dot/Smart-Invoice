package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Double amount;
    private String category;
    private LocalDate expenseDate;
    private String username;
    private String receiptFileName;

    public Expense() {}

    public Expense(String title, Double amount, String category, LocalDate expenseDate, String username, String receiptFileName) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.username = username;
        this.receiptFileName = receiptFileName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getReceiptFileName() { return receiptFileName; }
    public void setReceiptFileName(String receiptFileName) { this.receiptFileName = receiptFileName; }

    // Enterprise Audit Fields
    private String auditStatus; // e.g., "COMPLIANT", "FLAGGED"
    private String auditMessage; // e.g., "Meal expense exceeds $75 enterprise threshold"

    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }

    public String getAuditMessage() { return auditMessage; }
    public void setAuditMessage(String auditMessage) { this.auditMessage = auditMessage; }
}