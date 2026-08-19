/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import java.beans.ConstructorProperties;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.AbstractReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;

@Schema
public abstract class AbstractDataReportComponent
extends AbstractReportComponent
implements DataReportComponent {
    private List<DataSource> dataSources;

    @Override
    @Generated
    public List<DataSource> getDataSources() {
        return this.dataSources;
    }

    @Generated
    public void setDataSources(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    @Override
    @Generated
    public String toString() {
        return "AbstractDataReportComponent(dataSources=" + String.valueOf(this.getDataSources()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractDataReportComponent)) {
            return false;
        }
        AbstractDataReportComponent other = (AbstractDataReportComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        List<DataSource> this$dataSources = this.getDataSources();
        List<DataSource> other$dataSources = other.getDataSources();
        return !(this$dataSources == null ? other$dataSources != null : !((Object)this$dataSources).equals(other$dataSources));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AbstractDataReportComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<DataSource> $dataSources = this.getDataSources();
        result = result * 59 + ($dataSources == null ? 43 : ((Object)$dataSources).hashCode());
        return result;
    }

    @Generated
    public AbstractDataReportComponent() {
    }

    @ConstructorProperties(value={"dataSources"})
    @Generated
    public AbstractDataReportComponent(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }
}

