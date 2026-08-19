/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.components.AbstractTableWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

@Schema
public class EntityTableComponent
extends AbstractTableWithLayoutReportComponent {
    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ENTITY_TABLE;
    }

    @Override
    @Generated
    public String toString() {
        return "EntityTableComponent()";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EntityTableComponent)) {
            return false;
        }
        EntityTableComponent other = (EntityTableComponent)o;
        return other.canEqual(this);
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof EntityTableComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        boolean result = true;
        return 1;
    }

    @Generated
    public EntityTableComponent() {
    }
}

