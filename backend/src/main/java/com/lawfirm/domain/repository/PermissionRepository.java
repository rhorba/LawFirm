package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    Set<Permission> findAllByResourceAndAction(
        Permission.PermissionResource resource,
        Permission.PermissionAction action
    );
}
