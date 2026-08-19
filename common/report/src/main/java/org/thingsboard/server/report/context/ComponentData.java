/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 *  org.thingsboard.server.common.data.report.configuration.DataSource
 */
package org.thingsboard.server.report.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.report.context.chart.LatestChartData;
import org.thingsboard.server.report.context.chart.TsChartData;
import org.thingsboard.server.report.util.ThymeleafUtil;

public class ComponentData {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ComponentData.class);
    private final int usablePageWidthPx;
    private List<Map<String, String>> entityDatas;
    private LatestChartData latestChartData;
    private TsChartData tsChartData;
    private Map<String, Object> variables;
    private byte[] image;
    private String error;

    public ComponentData(int usablePageWidthPx) {
        this(usablePageWidthPx, null, new ArrayList<Map<String, String>>(), new HashMap<String, Object>());
    }

    public ComponentData(int usablePageWidthPx, String error) {
        this(usablePageWidthPx);
        this.error = error;
    }

    public ComponentData(int usablePageWidthPx, byte[] image) {
        this(usablePageWidthPx);
        this.image = image;
    }

    public ComponentData(int usablePageWidthPx, LatestChartData latestChartData, Map<String, Object> variables) {
        this(usablePageWidthPx);
        this.latestChartData = latestChartData;
        this.variables = variables;
    }

    public ComponentData(int usablePageWidthPx, TsChartData tsChartData, Map<String, Object> variables) {
        this(usablePageWidthPx);
        this.tsChartData = tsChartData;
        this.variables = variables;
    }

    public ComponentData(int usablePageWidthPx, List<Map<String, String>> entityDatas) {
        this(usablePageWidthPx, null, entityDatas, new HashMap<String, Object>());
    }

    public ComponentData(int usablePageWidthPx, DataSource dataSource, List<Map<String, String>> entityDatas) {
        this(usablePageWidthPx, dataSource, entityDatas, new HashMap<String, Object>());
    }

    public ComponentData(int usablePageWidthPx, Map<String, Object> variables) {
        this(usablePageWidthPx, null, new ArrayList<Map<String, String>>(), variables);
    }

    public ComponentData(int usablePageWidthPx, DataSource dataSource, List<Map<String, String>> entityDatas, Map<String, Object> variables) {
        this.usablePageWidthPx = usablePageWidthPx;
        this.entityDatas = entityDatas;
        this.variables = variables;
        if (dataSource != null) {
            this.injectVariablesFromEntityDatas(dataSource);
        }
        variables.put("rowCount", this.entityDatas.size());
    }

    private void injectVariablesFromEntityDatas(DataSource dataSource) {
        Map<String, DataKey> labelToDataKey = Stream.concat(Optional.ofNullable(dataSource.getDataKeys()).orElse(Collections.emptyList()).stream(), Optional.ofNullable(dataSource.getLatestDataKeys()).orElse(Collections.emptyList()).stream()).collect(Collectors.toMap(dataKey -> ThymeleafUtil.normalizeVariableName(dataKey.getLabel()), Function.identity(), (existing, replacement) -> existing, LinkedHashMap::new));
        for (Map.Entry<String, DataKey> entry : labelToDataKey.entrySet()) {
            for (Map<String, String> entityData : this.entityDatas) {
                String label = (String)entry.getKey();
                DataKey dataKey2 = (DataKey)entry.getValue();
                this.variables.put(label, entityData.getOrDefault(dataKey2.getLabel(), ""));
            }
        }
    }

    public ComponentData merge(ComponentData other) {
        this.entityDatas = this.mergeEntityDatas(this.entityDatas, other.getEntityDatas());
        this.variables.putAll(other.getVariables());
        return this;
    }

    private List<Map<String, String>> mergeEntityDatas(List<Map<String, String>> dataList, List<Map<String, String>> otherDataList) {
        LinkedHashMap<String, Map<String, String>> merged = new LinkedHashMap<String, Map<String, String>>();
        Stream.of(dataList, otherDataList).filter(Objects::nonNull).flatMap(Collection::stream).forEach(map -> {
            String id = (String)map.get("id");
            if (id != null) {
                merged.merge(id, new HashMap<String, String>(map), (existing, incoming) -> {
                    existing.putAll(incoming);
                    return existing;
                });
            }
        });
        return new ArrayList<Map<String, String>>(merged.values());
    }

    @Generated
    public int getUsablePageWidthPx() {
        return this.usablePageWidthPx;
    }

    @Generated
    public List<Map<String, String>> getEntityDatas() {
        return this.entityDatas;
    }

    @Generated
    public LatestChartData getLatestChartData() {
        return this.latestChartData;
    }

    @Generated
    public TsChartData getTsChartData() {
        return this.tsChartData;
    }

    @Generated
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    @Generated
    public byte[] getImage() {
        return this.image;
    }

    @Generated
    public String getError() {
        return this.error;
    }

    @Generated
    public void setEntityDatas(List<Map<String, String>> entityDatas) {
        this.entityDatas = entityDatas;
    }

    @Generated
    public void setLatestChartData(LatestChartData latestChartData) {
        this.latestChartData = latestChartData;
    }

    @Generated
    public void setTsChartData(TsChartData tsChartData) {
        this.tsChartData = tsChartData;
    }

    @Generated
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    @Generated
    public void setImage(byte[] image) {
        this.image = image;
    }

    @Generated
    public void setError(String error) {
        this.error = error;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ComponentData)) {
            return false;
        }
        ComponentData other = (ComponentData)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getUsablePageWidthPx() != other.getUsablePageWidthPx()) {
            return false;
        }
        List<Map<String, String>> this$entityDatas = this.getEntityDatas();
        List<Map<String, String>> other$entityDatas = other.getEntityDatas();
        if (this$entityDatas == null ? other$entityDatas != null : !((Object)this$entityDatas).equals(other$entityDatas)) {
            return false;
        }
        LatestChartData this$latestChartData = this.getLatestChartData();
        LatestChartData other$latestChartData = other.getLatestChartData();
        if (this$latestChartData == null ? other$latestChartData != null : !((Object)this$latestChartData).equals(other$latestChartData)) {
            return false;
        }
        TsChartData this$tsChartData = this.getTsChartData();
        TsChartData other$tsChartData = other.getTsChartData();
        if (this$tsChartData == null ? other$tsChartData != null : !((Object)this$tsChartData).equals(other$tsChartData)) {
            return false;
        }
        Map<String, Object> this$variables = this.getVariables();
        Map<String, Object> other$variables = other.getVariables();
        if (this$variables == null ? other$variables != null : !((Object)this$variables).equals(other$variables)) {
            return false;
        }
        if (!Arrays.equals(this.getImage(), other.getImage())) {
            return false;
        }
        String this$error = this.getError();
        String other$error = other.getError();
        return !(this$error == null ? other$error != null : !this$error.equals(other$error));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ComponentData;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getUsablePageWidthPx();
        List<Map<String, String>> $entityDatas = this.getEntityDatas();
        result = result * 59 + ($entityDatas == null ? 43 : ((Object)$entityDatas).hashCode());
        LatestChartData $latestChartData = this.getLatestChartData();
        result = result * 59 + ($latestChartData == null ? 43 : ((Object)$latestChartData).hashCode());
        TsChartData $tsChartData = this.getTsChartData();
        result = result * 59 + ($tsChartData == null ? 43 : ((Object)$tsChartData).hashCode());
        Map<String, Object> $variables = this.getVariables();
        result = result * 59 + ($variables == null ? 43 : ((Object)$variables).hashCode());
        result = result * 59 + Arrays.hashCode(this.getImage());
        String $error = this.getError();
        result = result * 59 + ($error == null ? 43 : $error.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ComponentData(usablePageWidthPx=" + this.getUsablePageWidthPx() + ", entityDatas=" + String.valueOf(this.getEntityDatas()) + ", latestChartData=" + String.valueOf(this.getLatestChartData()) + ", tsChartData=" + String.valueOf(this.getTsChartData()) + ", variables=" + String.valueOf(this.getVariables()) + ", image=" + Arrays.toString(this.getImage()) + ", error=" + this.getError() + ")";
    }
}

