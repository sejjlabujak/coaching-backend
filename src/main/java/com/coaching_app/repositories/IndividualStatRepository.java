// IndividualStatRepository.java
package com.coaching_app.repositories;

import com.coaching_app.models.IndividualPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndividualStatRepository extends JpaRepository<IndividualPerformance, Long> {
    List<IndividualPerformance> findByGame_Id(Long gameId);
}