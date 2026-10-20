package com.kodegladiators.kaasutracker.service;

import com.kodegladiators.kaasutracker.dto.ExpenseDto;
import com.kodegladiators.kaasutracker.dto.ExpenseResource;
import com.kodegladiators.kaasutracker.event.ExpenseAddedEvent;
import com.kodegladiators.kaasutracker.model.Expense;
import com.kodegladiators.kaasutracker.model.User;
import com.kodegladiators.kaasutracker.repository.ExpenseRepository;
import com.kodegladiators.kaasutracker.repository.UserRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<ExpenseResource> findAllByUser(Long userId) {
        requireUser(userId);
        return expenseRepository.findAllByUserIdOrderByExpenseDateDesc(userId)
                .stream()
                .map(ExpenseService::toResource)
                .toList();
    }

    public ExpenseResource findById(Long userId, Long expenseId) {
        requireUser(userId);
        return toResource(findExpense(userId, expenseId));
    }

    @Transactional
    public ExpenseResource create(Long userId, ExpenseDto request) {
        User user = requireUser(userId);
        Expense expense = new Expense();
        apply(expense, request);
        expense.setUser(user);
        Expense savedExpense = expenseRepository.save(expense);
        publishExpenseAddedAfterCommit(userId, savedExpense);
        return toResource(savedExpense);
    }

    @Transactional
    public ExpenseResource update(Long userId, Long expenseId, ExpenseDto request) {
        requireUser(userId);
        Expense expense = findExpense(userId, expenseId);
        apply(expense, request);
        return toResource(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long userId, Long expenseId) {
        requireUser(userId);
        expenseRepository.delete(findExpense(userId, expenseId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Expense findExpense(Long userId, Long expenseId) {
        return expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private void publishExpenseAddedAfterCommit(Long userId, Expense expense) {
        ExpenseAddedEvent event = new ExpenseAddedEvent(this, userId, expense.getAmount());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(event);
                }
            });
        } else {
            eventPublisher.publishEvent(event);
        }
    }

    private static void apply(Expense expense, ExpenseDto request) {
        expense.setAmount(request.amount());
        expense.setCategory(request.category().trim());
        expense.setMerchant(request.merchant());
        expense.setDescription(request.description());
        expense.setExpenseDate(request.expenseDate());
        expense.setReceiptUrl(request.receiptUrl());
        expense.setConfirmed(request.isConfirmed());
    }

    private static ExpenseResource toResource(Expense expense) {
        return new ExpenseResource(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getMerchant(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getReceiptUrl(),
                expense.isConfirmed()
        );
    }
}
