package com.kodegladiators.kaasutracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResource(
        Long id,
        BigDecimal amount,
        String category,
        String merchant,
        String description,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate expenseDate,
        String receiptUrl,
        boolean isConfirmed
) {
}
