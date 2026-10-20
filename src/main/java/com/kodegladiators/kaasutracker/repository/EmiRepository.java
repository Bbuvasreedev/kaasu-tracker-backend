package com.kodegladiators.kaasutracker.repository;

import com.kodegladiators.kaasutracker.model.Emi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmiRepository extends JpaRepository<Emi, Long> {

    List<Emi> findAllByUserId(String userId);

    @Query("select emi from Emi emi where emi.isActive = true and emi.dueDateDay = :dueDateDay")
    List<Emi> findAllActiveByDueDateDay(@Param("dueDateDay") Integer dueDateDay);
}
