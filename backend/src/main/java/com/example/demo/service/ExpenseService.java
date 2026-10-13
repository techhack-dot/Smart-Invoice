package com.example.demo.service;

import com.example.demo.model.Expense;
import com.example.demo.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    // 1. Save a new expense
    public Expense addExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    // 2. Get all expenses for a specific user (useful for dashboard)
    public List<Expense> getExpensesByUser(String username) {
        return expenseRepository.findByUsername(username);
    }

    // 3. Delete an expense by its ID
    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }
}