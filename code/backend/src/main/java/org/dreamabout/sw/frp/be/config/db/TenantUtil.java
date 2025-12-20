package org.dreamabout.sw.frp.be.config.db;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantUtil {

    private final JdbcTemplate jdbcTemplate;
    private final SecurityContextService securityContextService;

    private static final String SELECT_SCHEMA_NAME_BY_USERNAME = """
                SELECT s.name FROM frp_user u
                JOIN frp_schema s ON s.id = u.schema_id 
                WHERE email = ?
            """;

    public TenantIdentifier getCurrentTenantIdentifier() {

        var authentication = securityContextService.getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails userDetails) {
            var username = userDetails.getUsername();
            var schemaName = getCurrentTenantIdentifierName(username);
            return TenantIdentifier.of(schemaName);
        }
        return TenantIdentifier.of(Constant.TEMPLATE_SCHEMA);
    }

    private String getCurrentTenantIdentifierName(String userName) {
        var schemas = jdbcTemplate.queryForList(
                SELECT_SCHEMA_NAME_BY_USERNAME,
                String.class,
                userName
        );
        return schemas.isEmpty()
                ? Constant.TEMPLATE_SCHEMA
                : schemas.get(0);
    }
}
