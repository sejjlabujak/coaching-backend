// TeamPerformanceRepository.java
package com.coaching_app.repositories;

import com.coaching_app.models.TeamPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamPerformanceRepository extends JpaRepository<TeamPerformance, Long> {}