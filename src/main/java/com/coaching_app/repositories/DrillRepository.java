//package com.coaching_app.repositories;
//
//import com.coaching_app.enums.TrainingFocus;
//import com.coaching_app.models.Drill;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//// repositories/DrillRepository.java
//@Repository
//public interface DrillRepository extends JpaRepository<Drill, Long> {
//    List<Drill> findByDeletedFalse();
//
//    @Query("SELECT d FROM Drill d WHERE d.deleted = false AND " +
//           "(:focus IS NULL OR d.focus = :focus) AND " +
//           "(:search IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%')))")
//    List<Drill> findFiltered(
//        @Param("focus") TrainingFocus focus,
//        @Param("search") String search
//    );
//}

package com.coaching_app.repositories;

import com.coaching_app.models.Drill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrillRepository extends JpaRepository<Drill, Long> {

    List<Drill> findByDeletedFalse();

    boolean existsByTitleIgnoreCaseAndUserIdAndDeletedFalse(String title, Long userId);

    @Query(value = "SELECT * FROM drills d WHERE d.deleted = false AND " +
            "d.user_id = :userId AND " +
            "(:focus IS NULL OR d.focus = :focus) AND " +
            "(:search IS NULL OR d.title ILIKE CONCAT('%', :search, '%'))",
            nativeQuery = true)
    List<Drill> findFiltered(
            @Param("userId") Long userId,
            @Param("focus") String focus,
            @Param("search") String search
    );
}