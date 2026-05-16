package com.coaching_app.repositories;

import com.coaching_app.models.Injury;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InjuryRepository extends JpaRepository<Injury, Long> {

    List<Injury> findByPlayerId(Long playerId);

    // Only currently active injuries for a player
    List<Injury> findByPlayerIdAndIsActiveTrue(Long playerId);

    // All active injuries across all players (e.g. for a coach dashboard)
    List<Injury> findByIsActiveTrue();
}