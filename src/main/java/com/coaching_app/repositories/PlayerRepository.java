package com.coaching_app.repositories;

import com.coaching_app.enums.AgeGroup;
import com.coaching_app.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByUserId(Long userId);

    List<Player> findByAgeGroup(AgeGroup ageGroup);

    List<Player> findByUserIdAndAgeGroup(Long userId, AgeGroup ageGroup);
}