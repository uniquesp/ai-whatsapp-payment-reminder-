package com.example.yoga_reminder.repository;

import com.example.yoga_reminder.domain.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'ACTIVE'
          AND s.endDate BETWEEN :today AND :noticeDate
    """)
    List<Subscription> findExpiringSubscriptions(
            @Param("today") LocalDate today,
            @Param("noticeDate") LocalDate noticeDate
    );
}

