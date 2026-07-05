package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;

public class HomeDashboardParams {
    @Schema(description="Home dashboard Id to assign for the new user.", example="784f394c-42b6-435a-983c-b7beff2784f9")
    private String id;
    @Schema(description="Indicates if hide toolbar should be hidden.")
    private boolean hideToolbar;

    @Generated
    public HomeDashboardParams() {
    }

    @Generated
    public String getId() {
        return this.id;
    }

    @Generated
    public boolean isHideToolbar() {
        return this.hideToolbar;
    }

    @Generated
    public void setId(String id) {
        this.id = id;
    }

    @Generated
    public void setHideToolbar(boolean hideToolbar) {
        this.hideToolbar = hideToolbar;
    }

    @Generated
    public String toString() {
        return "HomeDashboardParams(id=" + this.getId() + ", hideToolbar=" + this.isHideToolbar() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HomeDashboardParams)) {
            return false;
        }
        HomeDashboardParams other = (HomeDashboardParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isHideToolbar() != other.isHideToolbar()) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        return !(this$id == null ? other$id != null : !this$id.equals(other$id));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HomeDashboardParams;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isHideToolbar() ? 79 : 97);
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        return result;
    }
}

