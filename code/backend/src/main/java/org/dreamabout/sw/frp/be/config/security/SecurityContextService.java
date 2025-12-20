package org.dreamabout.sw.frp.be.config.security;

import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityContextService {

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public UserEntity getPrincipal() {
        Authentication authentication = getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null || !(authentication.getPrincipal() instanceof UserEntity)) {
            throw new IllegalStateException("User not authenticated or principal is not UserEntity");
        }
        return (UserEntity) authentication.getPrincipal();
    }

    public void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void clearContext() {
        SecurityContextHolder.clearContext();
    }
}
