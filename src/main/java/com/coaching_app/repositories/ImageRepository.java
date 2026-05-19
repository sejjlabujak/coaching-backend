package com.coaching_app.repositories;

import com.coaching_app.models.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByPlayerPlayerID(Long playerID);

    void deleteByPlayerPlayerID(Long playerID);
}