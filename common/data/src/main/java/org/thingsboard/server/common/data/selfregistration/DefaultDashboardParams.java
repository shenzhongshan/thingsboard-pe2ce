package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;

public class DefaultDashboardParams {
    @Schema(description="Default dashboard Id to assign for the new user.", example="784f394c-42b6-435a-983c-b7beff2784f9")
    private String id;
    @Schema(description="Set default dashboard to full screen mode.")
    private boolean fullscreen;

    @Generated
    public DefaultDashboardParams() {
    }

    @Generated
    public String getId() {
        return this.id;
    }

    @Generated
    public boolean isFullscreen() {
        return this.fullscreen;
    }

    @Generated
    public void setId(String id) {
        this.id = id;
    }

    @Generated
    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    @Generated
    public String toString() {
        return "DefaultDashboardParams(id=" + this.getId() + ", fullscreen=" + this.isFullscreen() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DefaultDashboardParams)) {
            return false;
        }
        DefaultDashboardParams other = (DefaultDashboardParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isFullscreen() != other.isFullscreen()) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        return !(this$id == null ? other$id != null : !this$id.equals(other$id));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DefaultDashboardParams;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isFullscreen() ? 79 : 97);
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        return result;
    }
}

