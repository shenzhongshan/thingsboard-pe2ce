/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.converter;

import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

/**
 * CE port of PE's converter persistence service. Dropped vs PE: edge templates, async lookups,
 * converter type maps, VC export.
 * <p>
 * Extends {@link EntityDaoService} (as in PE) so CONVERTER resolves through
 * EntityServiceRegistry — required by generic entity lookups such as the events API.
 */
public interface ConverterService extends EntityDaoService {

    Converter saveConverter(Converter converter);

    Converter findConverterById(TenantId tenantId, ConverterId converterId);

    Converter findConverterByName(TenantId tenantId, String name);

    PageData<Converter> findTenantConverters(TenantId tenantId, IntegrationType integrationType, PageLink pageLink);

    void deleteConverter(TenantId tenantId, ConverterId converterId);

    void deleteConvertersByTenantId(TenantId tenantId);

}
