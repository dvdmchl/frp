package org.dreamabout.sw.frp.be.config.db;

import lombok.experimental.UtilityClass;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@UtilityClass
public class TenantUtil {

    private static final String SELECT_SCHEMA_NAME_BY_USERNAME = """
                SELECT s.name FROM frp_user u
                JOIN frp_schema s ON s.id = u.schema_id 
                WHERE email = ?
            """;

    public static TenantIdentifier getCurrentTenantIdentifier(JdbcTemplate jdbcTemplate) {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails userDetails) {
            var username = userDetails.getUsername();
            var schemaName = getCurrentTenantIdentifierName(jdbcTemplate, username);
            return TenantIdentifier.of(schemaName);
        }
        return TenantIdentifier.of(Constant.TEMPLATE_SCHEMA);
    }

    private static String getCurrentTenantIdentifierName(JdbcTemplate jdbcTemplate, String userName) {
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
