package com.coaching_app.repositories;

import com.coaching_app.models.IndividualPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PlayerStatsRepository extends JpaRepository<IndividualPerformance, Long> {

    List<IndividualPerformance> findByGame_Id(Long gameId);

    List<IndividualPerformance> findByFirstNameIgnoreCaseAndFamilyNameIgnoreCase(
            String firstName, String familyName
    );
    List<IndividualPerformance> findByPlayer_PlayerID(Long playerID);
    @Modifying
    @Transactional
    @Query("UPDATE IndividualPerformance ip SET ip.player = :#{#player} " +
            "WHERE LOWER(ip.firstName) = LOWER(:firstName) " +
            "AND LOWER(ip.familyName) = LOWER(:familyName) " +
            "AND ip.player IS NULL")
    int linkPlayerByName(
            @Param("firstName") String firstName,
            @Param("familyName") String familyName,
            @Param("player") com.coaching_app.models.Player player
    );
}