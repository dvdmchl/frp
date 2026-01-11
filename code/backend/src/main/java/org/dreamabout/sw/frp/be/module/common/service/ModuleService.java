package org.dreamabout.sw.frp.be.module.common.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.common.model.dto.ModuleDefinitionDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ModuleService {

    private List<ModuleDefinitionDto> modules = Collections.emptyList();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ClassPathResource resource = new ClassPathResource("modules.yaml");
            if (resource.exists()) {
                Map<String, List<ModuleDefinitionDto>> map = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});
                modules = map.getOrDefault("modules", Collections.emptyList());
                log.info("Loaded {} modules from definition file.", modules.size());
            } else {
                log.warn("modules.yaml not found.");
            }
        } catch (IOException e) {
            log.error("Failed to load modules configuration", e);
        }
    }

    public List<ModuleDefinitionDto> getAllModules() {
        return modules;
    }

    public Optional<ModuleDefinitionDto> getModule(String code) {
        return modules.stream()
                .filter(m -> m.code().equalsIgnoreCase(code))
                .findFirst();
    }
}
