package com.coaching_app.repositories;

import com.coaching_app.enums.AgeGroup;
import com.coaching_app.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByUserId(Long userId);

    List<Player> findByAgeGroup(AgeGroup ageGroup);

    List<Player> findByUserIdAndAgeGroup(Long userId, AgeGroup ageGroup);

    Optional<Player> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String s, String s1);

    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.user u LEFT JOIN FETCH u.team WHERE p.playerID = :id")
    Optional<Player> findByIdWithTeam(@Param("id") Long id);
}