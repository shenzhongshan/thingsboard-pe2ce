/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  jakarta.validation.constraints.NotNull
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

@Schema
public class HeaderFooter {
    private boolean enabled;
    @NotNull
    private List<ReportComponent> components;
    private HeaderFooter firstPage;

    @Generated
    public boolean isEnabled() {
        return this.enabled;
    }

    @Generated
    public List<ReportComponent> getComponents() {
        return this.components;
    }

    @Generated
    public HeaderFooter getFirstPage() {
        return this.firstPage;
    }

    @Generated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setComponents(List<ReportComponent> components) {
        this.components = components;
    }

    @Generated
    public void setFirstPage(HeaderFooter firstPage) {
        this.firstPage = firstPage;
    }

    @Generated
    public String toString() {
        return "HeaderFooter(enabled=" + this.isEnabled() + ", components=" + String.valueOf(this.getComponents()) + ", firstPage=" + String.valueOf(this.getFirstPage()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HeaderFooter)) {
            return false;
        }
        HeaderFooter other = (HeaderFooter)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isEnabled() != other.isEnabled()) {
            return false;
        }
        List<ReportComponent> this$components = this.getComponents();
        List<ReportComponent> other$components = other.getComponents();
        if (this$components == null ? other$components != null : !((Object)this$components).equals(other$components)) {
            return false;
        }
        HeaderFooter this$firstPage = this.getFirstPage();
        HeaderFooter other$firstPage = other.getFirstPage();
        return !(this$firstPage == null ? other$firstPage != null : !((Object)this$firstPage).equals(other$firstPage));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HeaderFooter;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isEnabled() ? 79 : 97);
        List<ReportComponent> $components = this.getComponents();
        result = result * 59 + ($components == null ? 43 : ((Object)$components).hashCode());
        HeaderFooter $firstPage = this.getFirstPage();
        result = result * 59 + ($firstPage == null ? 43 : ((Object)$firstPage).hashCode());
        return result;
    }

    @Generated
    public HeaderFooter() {
    }
}

