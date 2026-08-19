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
import org.thingsboard.server.common.data.report.configuration.components.AbstractLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;

@Schema
public class SplitViewComponent
extends AbstractLayoutReportComponent {
    private ReportComponent leftView;
    private ReportComponent rightView;
    private Float splitPosition;
    private Integer splitGap;
    private VerticalAlignment leftVerticalAlignment;
    private VerticalAlignment rightVerticalAlignment;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.SPLIT_VIEW;
    }

    @Generated
    public ReportComponent getLeftView() {
        return this.leftView;
    }

    @Generated
    public ReportComponent getRightView() {
        return this.rightView;
    }

    @Generated
    public Float getSplitPosition() {
        return this.splitPosition;
    }

    @Generated
    public Integer getSplitGap() {
        return this.splitGap;
    }

    @Generated
    public VerticalAlignment getLeftVerticalAlignment() {
        return this.leftVerticalAlignment;
    }

    @Generated
    public VerticalAlignment getRightVerticalAlignment() {
        return this.rightVerticalAlignment;
    }

    @Generated
    public void setLeftView(ReportComponent leftView) {
        this.leftView = leftView;
    }

    @Generated
    public void setRightView(ReportComponent rightView) {
        this.rightView = rightView;
    }

    @Generated
    public void setSplitPosition(Float splitPosition) {
        this.splitPosition = splitPosition;
    }

    @Generated
    public void setSplitGap(Integer splitGap) {
        this.splitGap = splitGap;
    }

    @Generated
    public void setLeftVerticalAlignment(VerticalAlignment leftVerticalAlignment) {
        this.leftVerticalAlignment = leftVerticalAlignment;
    }

    @Generated
    public void setRightVerticalAlignment(VerticalAlignment rightVerticalAlignment) {
        this.rightVerticalAlignment = rightVerticalAlignment;
    }

    @Override
    @Generated
    public String toString() {
        return "SplitViewComponent(leftView=" + String.valueOf(this.getLeftView()) + ", rightView=" + String.valueOf(this.getRightView()) + ", splitPosition=" + this.getSplitPosition() + ", splitGap=" + this.getSplitGap() + ", leftVerticalAlignment=" + String.valueOf((Object)this.getLeftVerticalAlignment()) + ", rightVerticalAlignment=" + String.valueOf((Object)this.getRightVerticalAlignment()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SplitViewComponent)) {
            return false;
        }
        SplitViewComponent other = (SplitViewComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Float this$splitPosition = this.getSplitPosition();
        Float other$splitPosition = other.getSplitPosition();
        if (this$splitPosition == null ? other$splitPosition != null : !((Object)this$splitPosition).equals(other$splitPosition)) {
            return false;
        }
        Integer this$splitGap = this.getSplitGap();
        Integer other$splitGap = other.getSplitGap();
        if (this$splitGap == null ? other$splitGap != null : !((Object)this$splitGap).equals(other$splitGap)) {
            return false;
        }
        ReportComponent this$leftView = this.getLeftView();
        ReportComponent other$leftView = other.getLeftView();
        if (this$leftView == null ? other$leftView != null : !this$leftView.equals(other$leftView)) {
            return false;
        }
        ReportComponent this$rightView = this.getRightView();
        ReportComponent other$rightView = other.getRightView();
        if (this$rightView == null ? other$rightView != null : !this$rightView.equals(other$rightView)) {
            return false;
        }
        VerticalAlignment this$leftVerticalAlignment = this.getLeftVerticalAlignment();
        VerticalAlignment other$leftVerticalAlignment = other.getLeftVerticalAlignment();
        if (this$leftVerticalAlignment == null ? other$leftVerticalAlignment != null : !((Object)((Object)this$leftVerticalAlignment)).equals((Object)other$leftVerticalAlignment)) {
            return false;
        }
        VerticalAlignment this$rightVerticalAlignment = this.getRightVerticalAlignment();
        VerticalAlignment other$rightVerticalAlignment = other.getRightVerticalAlignment();
        return !(this$rightVerticalAlignment == null ? other$rightVerticalAlignment != null : !((Object)((Object)this$rightVerticalAlignment)).equals((Object)other$rightVerticalAlignment));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SplitViewComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Float $splitPosition = this.getSplitPosition();
        result = result * 59 + ($splitPosition == null ? 43 : ((Object)$splitPosition).hashCode());
        Integer $splitGap = this.getSplitGap();
        result = result * 59 + ($splitGap == null ? 43 : ((Object)$splitGap).hashCode());
        ReportComponent $leftView = this.getLeftView();
        result = result * 59 + ($leftView == null ? 43 : $leftView.hashCode());
        ReportComponent $rightView = this.getRightView();
        result = result * 59 + ($rightView == null ? 43 : $rightView.hashCode());
        VerticalAlignment $leftVerticalAlignment = this.getLeftVerticalAlignment();
        result = result * 59 + ($leftVerticalAlignment == null ? 43 : ((Object)((Object)$leftVerticalAlignment)).hashCode());
        VerticalAlignment $rightVerticalAlignment = this.getRightVerticalAlignment();
        result = result * 59 + ($rightVerticalAlignment == null ? 43 : ((Object)((Object)$rightVerticalAlignment)).hashCode());
        return result;
    }

    @Generated
    public SplitViewComponent() {
    }
}

