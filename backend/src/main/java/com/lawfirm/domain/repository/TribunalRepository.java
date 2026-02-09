package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Tribunal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TribunalRepository extends JpaRepository<Tribunal, Long> {

    Optional<Tribunal> findByCode(String code);

    Optional<Tribunal> findByCodeAndActiveTrue(String code);

    List<Tribunal> findAllByActiveTrue();
}
