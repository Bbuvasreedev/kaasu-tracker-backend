package com.kodegladiators.kaasutracker.repository;

import com.kodegladiators.kaasutracker.model.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByUserIdOrderByExpenseDateDesc(String userId);

    Optional<Expense> findByIdAndUserId(Long id, String userId);

    @Query("""
            select expense
            from Expense expense
            where expense.user.id = :userId
              and expense.expenseDate between :startDate and :endDate
            order by expense.expenseDate desc
            """)
    List<Expense> findByUserIdAndExpenseDateBetween(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select coalesce(sum(expense.amount), 0)
            from Expense expense
            where expense.user.id = :userId
              and expense.expenseDate between :startDate and :endDate
            """)
    BigDecimal sumAmountByUserIdAndExpenseDateBetween(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}