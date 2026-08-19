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
import org.thingsboard.server.common.data.report.configuration.components.AbstractImageComponent;

@Schema
public abstract class AbstractChartComponent
extends AbstractImageComponent {
    private int height;

    @Generated
    public int getHeight() {
        return this.height;
    }

    @Generated
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    @Generated
    public String toString() {
        return "AbstractChartComponent(height=" + this.getHeight() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractChartComponent)) {
            return false;
        }
        AbstractChartComponent other = (AbstractChartComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        return this.getHeight() == other.getHeight();
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AbstractChartComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getHeight();
        return result;
    }

    @Generated
    public AbstractChartComponent() {
    }
}

