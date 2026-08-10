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
package org.thingsboard.server.dao.group;

import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

/**
 * Exposes EntityGroup as a plain Dao/ExportableEntityDao so the version-control export/import
 * framework (DefaultExportableEntitiesService, which scans all Dao<?> beans by getEntityType())
 * can resolve external ids and look up existing entities on import. The actual
 * create/update/membership logic stays in EntityGroupService/BaseEntityGroupService — this DAO
 * exists only to satisfy that framework's generic entity lookup contract.
 */
public interface EntityGroupDao extends Dao<EntityGroup>, ExportableEntityDao<EntityGroupId, EntityGroup> {
}
