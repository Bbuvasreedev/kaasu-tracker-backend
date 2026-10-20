package com.kodegladiators.kaasutracker.controller;

import com.kodegladiators.kaasutracker.dto.ExpenseAnalysisResponse;
import com.kodegladiators.kaasutracker.dto.ExpenseDto;
import com.kodegladiators.kaasutracker.dto.ExpenseResource;
import com.kodegladiators.kaasutracker.dto.ReceiptAnalysisRequest;
import com.kodegladiators.kaasutracker.service.ExpenseService;
import com.kodegladiators.kaasutracker.service.ReceiptAnalysisService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ReceiptAnalysisService receiptAnalysisService;

    public ExpenseController(
            ExpenseService expenseService,
            ReceiptAnalysisService receiptAnalysisService
    ) {
        this.expenseService = expenseService;
        this.receiptAnalysisService = receiptAnalysisService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResource> create(
            @RequestParam Long userId,
            @Valid @RequestBody ExpenseDto expenseDto
    ) {
        ExpenseResource createdExpense = expenseService.create(userId, expenseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdExpense);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResource>> findAll(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(expenseService.findAllByUser(userId));
    }

    @PostMapping("/analyze")
    public ResponseEntity<ExpenseAnalysisResponse> analyzeReceipt(
            @Valid @RequestBody ReceiptAnalysisRequest request
    ) {
        return ResponseEntity.ok(receiptAnalysisService.analyzeReceipt(request.base64Image()));
    }
}