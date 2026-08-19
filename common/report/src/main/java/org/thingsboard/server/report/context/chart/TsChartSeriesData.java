/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 */
package org.thingsboard.server.report.context.chart;

import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.report.context.chart.TsChartDataSource;
import org.thingsboard.server.report.context.chart.TsChartSeriesEntry;

public class TsChartSeriesData {
    private TsChartDataSource dataSource;
    private DataKey dataKey;
    private List<TsChartSeriesEntry> data;
    private List<Double> numericData;
    private int index;
    private int keyIndex;
    private int datasetIndex;
    private int seriesIndex;

    public boolean isEmpty() {
        return this.data == null || this.data.isEmpty();
    }

    public Double calcMin() {
        if (!this.numericData.isEmpty()) {
            double result = this.numericData.get(0);
            for (int i = 1; i < this.numericData.size(); ++i) {
                double value = this.numericData.get(i);
                result = Math.min(result, value);
            }
            return result;
        }
        return null;
    }

    public Double calcMax() {
        if (!this.numericData.isEmpty()) {
            double result = this.numericData.get(0);
            for (int i = 1; i < this.numericData.size(); ++i) {
                double value = this.numericData.get(i);
                result = Math.max(result, value);
            }
            return result;
        }
        return null;
    }

    public Double calcTotal() {
        if (!this.numericData.isEmpty()) {
            double result = 0.0;
            for (int i = 0; i < this.numericData.size(); ++i) {
                double value = this.numericData.get(i);
                result += value;
            }
            return result;
        }
        return null;
    }

    public int calcCount() {
        return this.numericData.size();
    }

    public Double calcAvg() {
        Double total = this.calcTotal();
        if (total != null) {
            int count = this.calcCount();
            return total / (double)count;
        }
        return null;
    }

    public Double calcLatest() {
        if (!this.numericData.isEmpty()) {
            return this.numericData.get(this.numericData.size() - 1);
        }
        return null;
    }

    public String toString() {
        return "TsChartSeriesData{dataKey=" + String.valueOf(this.dataKey) + ", data=" + String.valueOf(this.data) + ", index=" + this.index + ", keyIndex=" + this.keyIndex + ", datasetIndex=" + this.datasetIndex + ", seriesIndex=" + this.seriesIndex + "}";
    }

    @Generated
    public TsChartSeriesData() {
    }

    @Generated
    public TsChartDataSource getDataSource() {
        return this.dataSource;
    }

    @Generated
    public DataKey getDataKey() {
        return this.dataKey;
    }

    @Generated
    public List<TsChartSeriesEntry> getData() {
        return this.data;
    }

    @Generated
    public List<Double> getNumericData() {
        return this.numericData;
    }

    @Generated
    public int getIndex() {
        return this.index;
    }

    @Generated
    public int getKeyIndex() {
        return this.keyIndex;
    }

    @Generated
    public int getDatasetIndex() {
        return this.datasetIndex;
    }

    @Generated
    public int getSeriesIndex() {
        return this.seriesIndex;
    }

    @Generated
    public void setDataSource(TsChartDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Generated
    public void setDataKey(DataKey dataKey) {
        this.dataKey = dataKey;
    }

    @Generated
    public void setData(List<TsChartSeriesEntry> data) {
        this.data = data;
    }

    @Generated
    public void setNumericData(List<Double> numericData) {
        this.numericData = numericData;
    }

    @Generated
    public void setIndex(int index) {
        this.index = index;
    }

    @Generated
    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
    }

    @Generated
    public void setDatasetIndex(int datasetIndex) {
        this.datasetIndex = datasetIndex;
    }

    @Generated
    public void setSeriesIndex(int seriesIndex) {
        this.seriesIndex = seriesIndex;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TsChartSeriesData)) {
            return false;
        }
        TsChartSeriesData other = (TsChartSeriesData)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getIndex() != other.getIndex()) {
            return false;
        }
        if (this.getKeyIndex() != other.getKeyIndex()) {
            return false;
        }
        if (this.getDatasetIndex() != other.getDatasetIndex()) {
            return false;
        }
        if (this.getSeriesIndex() != other.getSeriesIndex()) {
            return false;
        }
        TsChartDataSource this$dataSource = this.getDataSource();
        TsChartDataSource other$dataSource = other.getDataSource();
        if (this$dataSource == null ? other$dataSource != null : !((Object)this$dataSource).equals(other$dataSource)) {
            return false;
        }
        DataKey this$dataKey = this.getDataKey();
        DataKey other$dataKey = other.getDataKey();
        if (this$dataKey == null ? other$dataKey != null : !this$dataKey.equals(other$dataKey)) {
            return false;
        }
        List<TsChartSeriesEntry> this$data = this.getData();
        List<TsChartSeriesEntry> other$data = other.getData();
        if (this$data == null ? other$data != null : !((Object)this$data).equals(other$data)) {
            return false;
        }
        List<Double> this$numericData = this.getNumericData();
        List<Double> other$numericData = other.getNumericData();
        return !(this$numericData == null ? other$numericData != null : !((Object)this$numericData).equals(other$numericData));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TsChartSeriesData;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getIndex();
        result = result * 59 + this.getKeyIndex();
        result = result * 59 + this.getDatasetIndex();
        result = result * 59 + this.getSeriesIndex();
        TsChartDataSource $dataSource = this.getDataSource();
        result = result * 59 + ($dataSource == null ? 43 : ((Object)$dataSource).hashCode());
        DataKey $dataKey = this.getDataKey();
        result = result * 59 + ($dataKey == null ? 43 : $dataKey.hashCode());
        List<TsChartSeriesEntry> $data = this.getData();
        result = result * 59 + ($data == null ? 43 : ((Object)$data).hashCode());
        List<Double> $numericData = this.getNumericData();
        result = result * 59 + ($numericData == null ? 43 : ((Object)$numericData).hashCode());
        return result;
    }
}

