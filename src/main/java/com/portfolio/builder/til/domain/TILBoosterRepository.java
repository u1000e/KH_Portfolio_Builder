package com.portfolio.builder.til.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TILBoosterRepository extends JpaRepository<TILBooster, Long> {

    Optional<TILBooster> findByTilId(Long tilId);

    List<TILBooster> findByTilIdIn(List<Long> tilIds);

    void deleteByTilId(Long tilId);
}
