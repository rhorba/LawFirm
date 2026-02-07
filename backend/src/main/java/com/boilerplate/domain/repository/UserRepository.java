package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.groups g LEFT JOIN FETCH g.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsernameWithRolesAndPermissions(String username);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.groups g LEFT JOIN FETCH g.roles WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdWithGroupsAndRoles(Long id);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :id AND u.deletedAt IS NULL")
    int softDeleteById(Long id, LocalDateTime deletedAt);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = null WHERE u.id = :id AND u.deletedAt IS NOT NULL")
    int restoreById(Long id);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id IN :ids AND u.deletedAt IS NULL")
    int softDeleteByIds(List<Long> ids, LocalDateTime deletedAt);

    long countByDeletedAtIsNullAndGroupsRolesName(String roleName);
}
