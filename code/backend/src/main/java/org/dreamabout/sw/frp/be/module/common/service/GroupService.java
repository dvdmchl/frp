package org.dreamabout.sw.frp.be.module.common.service;

import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.module.common.model.AccessLevel;
import org.dreamabout.sw.frp.be.module.common.model.GroupEntity;
import org.dreamabout.sw.frp.be.module.common.model.SchemaAccessEntity;
import org.dreamabout.sw.frp.be.module.common.model.SchemaEntity;
import org.dreamabout.sw.frp.be.module.common.repository.GroupRepository;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaAccessRepository;
import org.dreamabout.sw.frp.be.module.common.repository.SchemaRepository;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final SchemaRepository schemaRepository;
    private final SchemaAccessRepository schemaAccessRepository;

    @Transactional
    public GroupEntity createGroup(String name, String description) {
        var group = GroupEntity.builder()
                .name(name)
                .description(description)
                .build();
        return groupRepository.save(group);
    }

    @Transactional
    public void addUserToGroup(Long userId, Long groupId) {
        var user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        var group = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        user.getGroups().add(group);
        userRepository.save(user);
    }

    @Transactional
    public void grantSchemaAccessToGroup(String schemaName, Long groupId, AccessLevel level, Long grantorId) {
        var schema = validateOwnership(schemaName, grantorId);

        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));

        var access = schemaAccessRepository.findBySchemaAndGroup(schema, group)
                .orElse(SchemaAccessEntity.builder().schema(schema).group(group).build());
        access.setAccessLevel(level);
        schemaAccessRepository.save(access);
    }

    @Transactional
    public void grantSchemaAccessToUser(String schemaName, Long userId, AccessLevel level, Long grantorId) {
        var schema = validateOwnership(schemaName, grantorId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        var access = schemaAccessRepository.findBySchemaAndUser(schema, user)
                .orElse(SchemaAccessEntity.builder().schema(schema).user(user).build());
        access.setAccessLevel(level);
        schemaAccessRepository.save(access);
    }

    private SchemaEntity validateOwnership(String schemaName, Long grantorId) {
        var schema = schemaRepository.findByName(schemaName)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found: " + schemaName));

        if (!schema.getOwnerId().equals(grantorId)) {
            throw new IllegalArgumentException("Only the owner can grant access to the schema.");
        }
        return schema;
    }
}
