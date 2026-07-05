package org.thingsboard.server.dao.wl;

import java.util.List;
import java.util.Set;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;

public interface WhiteLabelingDao
extends TenantEntityDao<WhiteLabeling> {
    public WhiteLabeling save(TenantId var1, WhiteLabeling var2);

    public WhiteLabeling findById(TenantId var1, WhiteLabelingCompositeKey var2);

    public WhiteLabeling findByDomainAndType(TenantId var1, String var2, WhiteLabelingType var3);

    public void removeById(TenantId var1, WhiteLabelingCompositeKey var2);

    public List<WhiteLabeling> findByTenantAndImageLink(TenantId var1, String var2, int var3);

    public List<WhiteLabeling> findByImageLink(String var1, int var2);

    public PageData<WhiteLabeling> findAllByType(PageLink var1, Set<WhiteLabelingType> var2);
}

