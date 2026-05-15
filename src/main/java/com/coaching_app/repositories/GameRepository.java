// GameRepository.java
package com.coaching_app.repositories;

import com.coaching_app.models.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByFibaGameId(Integer fibaGameId);
    List<Game> findTop4ByOrderByDateDesc();
}