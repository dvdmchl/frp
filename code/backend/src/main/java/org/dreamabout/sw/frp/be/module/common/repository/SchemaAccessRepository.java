package org.dreamabout.sw.frp.be.module.common.repository;

import org.dreamabout.sw.frp.be.module.common.model.*;
import org.dreamabout.sw.multitenancy.core.Multitenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Multitenant
public interface SchemaAccessRepository extends JpaRepository<SchemaAccessEntity, Long> {

    List<SchemaAccessEntity> findAllBySchema(SchemaEntity schema);

    @Query("SELECT sa FROM SchemaAccessEntity sa WHERE sa.schema.name = :schemaName AND (sa.user.id = :userId OR sa.group.id IN :groupIds)")
    List<SchemaAccessEntity> findAccess(@Param("schemaName") String schemaName, @Param("userId") Long userId, @Param("groupIds") Collection<Long> groupIds);

    @Query("SELECT sa.schema FROM SchemaAccessEntity sa WHERE sa.user.id = :userId OR sa.group.id IN :groupIds")
    List<SchemaEntity> findAvailableSchemas(@Param("userId") Long userId, @Param("groupIds") Collection<Long> groupIds);

    @Query("SELECT sa FROM SchemaAccessEntity sa WHERE sa.schema.name = :schemaName AND sa.user.id = :userId")
    List<SchemaAccessEntity> findDirectAccess(@Param("schemaName") String schemaName, @Param("userId") Long userId);

    @Query("SELECT sa.schema FROM SchemaAccessEntity sa WHERE sa.user.id = :userId")
    List<SchemaEntity> findDirectAvailableSchemas(@Param("userId") Long userId);

    Optional<SchemaAccessEntity> findBySchemaAndUser(SchemaEntity schema, UserEntity user);

    Optional<SchemaAccessEntity> findBySchemaAndGroup(SchemaEntity schema, GroupEntity group);
}
