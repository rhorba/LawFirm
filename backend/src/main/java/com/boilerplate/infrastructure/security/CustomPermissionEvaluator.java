package com.boilerplate.infrastructure.security;

import com.boilerplate.domain.model.Permission.PermissionAction;
import com.boilerplate.domain.model.Permission.PermissionResource;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        return hasPrivilege(authentication, permission.toString());
    }

    @Override
    public boolean hasPermission(
        Authentication authentication,
        Serializable targetId,
        String targetType,
        Object permission
    ) {
        if (authentication == null || targetType == null || permission == null) {
            return false;
        }

        // Build permission string: RESOURCE_ACTION
        String permissionName = targetType.toUpperCase() + "_" + permission.toString().toUpperCase();
        return hasPrivilege(authentication, permissionName);
    }

    private boolean hasPrivilege(Authentication authentication, String permissionName) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(authority -> authority.equals(permissionName));
    }

    public boolean hasResourcePermission(
        Authentication authentication,
        PermissionResource resource,
        PermissionAction action
    ) {
        if (authentication == null) {
            return false;
        }

        String permissionName = resource.name() + "_" + action.name();
        return hasPrivilege(authentication, permissionName);
    }
}
