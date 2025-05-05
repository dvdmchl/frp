package org.dreamabout.sw.frp.be.controller;

import org.dreamabout.sw.frp.be.config.context.FrpThreadContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContextController {
    @GetMapping("/api/context")
    public String getContext() {
        String v = FrpThreadContext.get("frpHeader");
        return v != null ? v : "EMPTY";
    }
}
