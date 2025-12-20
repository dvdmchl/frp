package org.dreamabout.sw.frp.be.config.db;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl")
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<Long> {

    private final SecurityContextService securityContextService;
    private static final Long SYSTEM_USER_ID = 0L;

    @Override
    public Optional<Long> getCurrentAuditor() {
        var authentication = securityContextService.getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(SYSTEM_USER_ID);
        }

        var principal = authentication.getPrincipal();

        if (principal instanceof UserEntity user) {
            return Optional.of(user.getId());
        }

        return Optional.of(SYSTEM_USER_ID);
    }
}
