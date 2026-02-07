package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(String name);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id IN :ids")
    Set<Role> findAllByIdWithPermissions(Set<Long> ids);
}
