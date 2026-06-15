// GameRepository.java
package com.coaching_app.repositories;

import com.coaching_app.models.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByFibaGameId(Integer fibaGameId);
    List<Game> findTop4ByOrderByDateDesc();

    @Query("SELECT g FROM Game g WHERE g.homeTeamRef.id = :teamId OR g.awayTeamRef.id = :teamId ORDER BY g.date DESC")
    List<Game> findByTeamIdOrderByDateDesc(@Param("teamId") Long teamId);

    @Query("SELECT g FROM Game g WHERE g.homeTeamRef.id = :teamId OR g.awayTeamRef.id = :teamId ORDER BY g.date DESC LIMIT 4")
    List<Game> findTop4ByTeamIdOrderByDateDesc(@Param("teamId") Long teamId);

    List<Game> findByHomeTeamRef_IdOrAwayTeamRef_IdOrderByDateDesc(Long homeId, Long awayId);

    List<Game> findTop4ByHomeTeamRef_IdOrAwayTeamRef_IdOrderByDateDesc(Long homeId, Long awayId);

    List<Game> findTop3ByHomeTeamRef_IdOrderByDateDesc(Long homeTeamRefId);
}