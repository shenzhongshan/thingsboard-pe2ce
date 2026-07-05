package org.thingsboard.server.dao.sql.whitelabeling;

import java.util.List;
import java.util.Set;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.model.sql.WhiteLabelingEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.sql.whitelabeling.WhiteLabelingRepository;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.dao.wl.WhiteLabelingDao;

@Component
@SqlDao
public class JpaWhiteLabelingDao
extends JpaAbstractDaoListeningExecutorService
implements WhiteLabelingDao {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(JpaWhiteLabelingDao.class);
    @Autowired
    private WhiteLabelingRepository whiteLabelingRepository;

    @Override
    public WhiteLabeling save(TenantId tenantId, WhiteLabeling whiteLabeling) {
        return (WhiteLabeling)DaoUtil.getData((ToData)this.whiteLabelingRepository.save(new WhiteLabelingEntity(whiteLabeling)));
    }

    @Override
    public WhiteLabeling findById(TenantId tenantId, WhiteLabelingCompositeKey key) {
        return (WhiteLabeling)DaoUtil.getData(this.whiteLabelingRepository.findById(key));
    }

    @Override
    public WhiteLabeling findByDomainAndType(TenantId tenantId, String domain, WhiteLabelingType type) {
        return DaoUtil.getData(this.whiteLabelingRepository.findByDomainAndType(domain, type));
    }

    @Override
    public void removeById(TenantId tenantId, WhiteLabelingCompositeKey key) {
        this.whiteLabelingRepository.deleteById(key);
    }

    @Override
    public List<WhiteLabeling> findByTenantAndImageLink(TenantId tenantId, String imageLink, int limit) {
        return DaoUtil.convertDataList(this.whiteLabelingRepository.findByTenantAndImageLink(tenantId.getId(), imageLink, limit));
    }

    @Override
    public List<WhiteLabeling> findByImageLink(String imageLink, int limit) {
        return DaoUtil.convertDataList(this.whiteLabelingRepository.findByImageLink(imageLink, limit));
    }

    @Override
    public PageData<WhiteLabeling> findAllByType(PageLink pageLink, Set<WhiteLabelingType> types) {
        return DaoUtil.toPageData(this.whiteLabelingRepository.findAllByTypeIn(types, DaoUtil.toPageable(pageLink, List.of(new SortOrder("tenantId"), new SortOrder("customerId"), new SortOrder("type")))));
    }

    @Override
    public PageData<WhiteLabeling> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(this.whiteLabelingRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink, "tenantId", "customerId", "type")));
    }
}

