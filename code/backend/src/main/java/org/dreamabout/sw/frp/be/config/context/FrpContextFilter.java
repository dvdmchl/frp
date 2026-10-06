package org.dreamabout.sw.frp.be.config.context;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.dreamabout.sw.multitenancy.core.MultitenancyThreadContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FrpContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        try {
            String ctxHeader = ((HttpServletRequest) req).getHeader("X-Frp-Context");
            if (ctxHeader != null) {
                MultitenancyThreadContext.set("frpHeader", ctxHeader);
            }
            chain.doFilter(req, res);
        } finally {
            MultitenancyThreadContext.clear();
        }
    }
}
