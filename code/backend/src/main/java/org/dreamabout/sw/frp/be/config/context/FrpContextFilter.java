package org.dreamabout.sw.frp.be.config.context;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
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
                FrpThreadContext.set("frpHeader", ctxHeader);
            }
            chain.doFilter(req, res);
        } finally {
            FrpThreadContext.clear();
        }
    }
}
