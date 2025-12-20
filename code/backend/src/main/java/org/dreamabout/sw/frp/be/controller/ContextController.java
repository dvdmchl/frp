package org.dreamabout.sw.frp.be.controller;

import org.dreamabout.sw.frp.be.config.context.FrpThreadContext;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContextController {
    @GetMapping(ApiPath.CONTEXT_FULL)
    public String getContext() {
        String v = FrpThreadContext.get("frpHeader");
        return v != null ? v : "EMPTY";
    }
}
