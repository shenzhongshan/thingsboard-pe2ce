/*
 * Decompiled with CFR 0.152.
 */
package org.thingsboard.server.common.data.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;

public class DataSourceUtils {
    public static List<EntityKey> setEntityKeyIfNotExists(List<EntityKey> entityKeys, EntityKeyType type, String keyName) {
        EntityKey targetEntityKey = entityKeys.stream().filter(key -> key.getKey().equals(keyName) && key.getType().equals((Object)type)).findAny().orElse(null);
        if (targetEntityKey == null) {
            entityKeys = new ArrayList<EntityKey>(entityKeys);
            entityKeys.add(new EntityKey(type, keyName));
        }
        return entityKeys;
    }

    public static Optional<String> getEntityLatestValue(EntityData entity, EntityKeyType keyType, String key) {
        return DataSourceUtils.getLatestValue(entity.getLatest(), keyType, key);
    }

    public static Optional<String> getAlarmLatestValue(AlarmData alarm, EntityKeyType keyType, String key) {
        return DataSourceUtils.getLatestValue(alarm.getLatest(), keyType, key);
    }

    private static Optional<String> getLatestValue(Map<EntityKeyType, Map<String, TsValue>> latest, EntityKeyType keyType, String key) {
        Map<String, TsValue> values;
        TsValue value;
        if (latest != null && latest.containsKey((Object)keyType) && (value = (values = latest.get((Object)keyType)).get(key)) != null) {
            return Optional.ofNullable(value.getValue());
        }
        return Optional.empty();
    }

    public static EntityData entityDataFromEntityId(EntityId entityId) {
        // PE's EntityData additionally carries readAttrs/readTs flags; CE's does not, and neither
        // would matter for the empty placeholder this builds.
        return new EntityData(entityId, Collections.emptyMap(), Collections.emptyMap());
    }
}

