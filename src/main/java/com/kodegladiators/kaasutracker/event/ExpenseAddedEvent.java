package com.kodegladiators.kaasutracker.event;

import java.math.BigDecimal;
import org.springframework.context.ApplicationEvent;

public class ExpenseAddedEvent extends ApplicationEvent {

    private final Long userId;
    private final BigDecimal expenseAmount;

    public ExpenseAddedEvent(Object source, Long userId, BigDecimal expenseAmount) {
        super(source);
        this.userId = userId;
        this.expenseAmount = expenseAmount;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getExpenseAmount() {
        return expenseAmount;
    }
}
