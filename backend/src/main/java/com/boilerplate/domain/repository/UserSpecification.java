package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.User;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("username")), pattern),
            cb.like(cb.lower(root.get("email")), pattern)
        );
    }

    public static Specification<User> hasRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            var rolesJoin = root.join("roles", JoinType.INNER);
            return cb.equal(rolesJoin.get("name"), roleName);
        };
    }

    public static Specification<User> hasEnabled(Boolean enabled) {
        if (enabled == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<User> isDeleted(boolean includeDeleted) {
        if (includeDeleted) {
            return (root, query, cb) -> cb.isNotNull(root.get("deletedAt"));
        }
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<User> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
