package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.roles WHERE g.id = :id")
    Optional<Group> findByIdWithRoles(@Param("id") Long id);

    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.roles LEFT JOIN FETCH g.users WHERE g.id = :id")
    Optional<Group> findByIdWithRolesAndUsers(@Param("id") Long id);

    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.roles LEFT JOIN FETCH g.users")
    List<Group> findAllWithRolesAndUsers();

    boolean existsByName(String name);

    Optional<Group> findByName(String name);
}
