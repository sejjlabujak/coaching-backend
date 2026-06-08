package com.coaching_app.repositories;

import com.coaching_app.models.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// repositories/SessionRepository.java
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    @Query(value = "SELECT * FROM sessions WHERE " +
            "deleted_at IS NULL AND " +
            "(:month IS NULL OR EXTRACT(MONTH FROM date) = :month) AND " +
            "(:year IS NULL OR EXTRACT(YEAR FROM date) = :year)",
            nativeQuery = true)
    List<Session> findByMonthAndYear(
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    @Query("SELECT s FROM Session s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Session> findActiveById(@Param("id") Long id);
}