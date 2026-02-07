package com.boilerplate.infrastructure.security;

import com.boilerplate.domain.model.Group;
import com.boilerplate.domain.model.Permission;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public class UserPrincipal implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Flatten roles from all groups
        for (Group group : user.getGroups()) {
            for (Role role : group.getRoles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

                // Add permission-based authorities
                for (Permission permission : role.getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(permission.getName()));
                }
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return user.getAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.getCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }
}
