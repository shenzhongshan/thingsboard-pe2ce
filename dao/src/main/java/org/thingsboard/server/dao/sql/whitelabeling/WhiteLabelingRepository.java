package org.thingsboard.server.dao.sql.whitelabeling;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.model.sql.WhiteLabelingEntity;

public interface WhiteLabelingRepository
extends JpaRepository<WhiteLabelingEntity, WhiteLabelingCompositeKey> {
    @Query(value="SELECT wl FROM WhiteLabelingEntity wl LEFT JOIN DomainEntity d ON wl.domainId = d.id WHERE d.name = :domain AND wl.type = :type")
    public WhiteLabelingEntity findByDomainAndType(@Param(value="domain") String var1, @Param(value="type") WhiteLabelingType var2);

    @Query(nativeQuery=true, value="SELECT * FROM white_labeling wl WHERE wl.tenant_id = :tenantId and wl.settings ILIKE CONCAT('%\"', :imageLink, '\"%') limit :lmt")
    public List<WhiteLabelingEntity> findByTenantAndImageLink(@Param(value="tenantId") UUID var1, @Param(value="imageLink") String var2, @Param(value="lmt") int var3);

    @Query(nativeQuery=true, value="SELECT * FROM white_labeling wl WHERE wl.settings ILIKE CONCAT('%\"', :imageLink, '\"%') limit :lmt")
    public List<WhiteLabelingEntity> findByImageLink(@Param(value="imageLink") String var1, @Param(value="lmt") int var2);

    @Query(value="SELECT w FROM WhiteLabelingEntity w WHERE w.type IN :types")
    public Page<WhiteLabelingEntity> findAllByTypeIn(@Param(value="types") Set<WhiteLabelingType> var1, Pageable var2);

    public Page<WhiteLabelingEntity> findByTenantId(UUID var1, Pageable var2);
}

