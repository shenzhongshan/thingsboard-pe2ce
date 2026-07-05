package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.Views;
import org.thingsboard.server.common.data.menu.DefaultMenuItem;
import org.thingsboard.server.common.data.menu.HomeMenuItemType;
import org.thingsboard.server.common.data.menu.MenuItemType;

@Schema
public class HomeMenuItem
extends DefaultMenuItem {
    @Schema(description="DEFAULT or DASHBOARD. DASHBOARD means default home page presentation changed to refer to dashboard")
    @JsonView(value={Views.Public.class})
    private HomeMenuItemType homeType;
    @Schema(description="Id of the Dashboard to open, when user clicks the menu item")
    @JsonView(value={Views.Public.class})
    private String dashboardId;
    @Schema(description="Hide the dashboard toolbar")
    @JsonView(value={Views.Public.class})
    private boolean hideDashboardToolbar;

    @Override
    @JsonView(value={Views.Private.class})
    public MenuItemType getType() {
        return MenuItemType.HOME;
    }

    @Override
    @JsonView(value={Views.Private.class})
    public boolean isVisible() {
        return true;
    }

    @Generated
    public HomeMenuItemType getHomeType() {
        return this.homeType;
    }

    @Generated
    public String getDashboardId() {
        return this.dashboardId;
    }

    @Generated
    public boolean isHideDashboardToolbar() {
        return this.hideDashboardToolbar;
    }

    @Generated
    public void setHomeType(HomeMenuItemType homeType) {
        this.homeType = homeType;
    }

    @Generated
    public void setDashboardId(String dashboardId) {
        this.dashboardId = dashboardId;
    }

    @Generated
    public void setHideDashboardToolbar(boolean hideDashboardToolbar) {
        this.hideDashboardToolbar = hideDashboardToolbar;
    }

    @Override
    @Generated
    public String toString() {
        return "HomeMenuItem(homeType=" + String.valueOf((Object)this.getHomeType()) + ", dashboardId=" + this.getDashboardId() + ", hideDashboardToolbar=" + this.isHideDashboardToolbar() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HomeMenuItem)) {
            return false;
        }
        HomeMenuItem other = (HomeMenuItem)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (this.isHideDashboardToolbar() != other.isHideDashboardToolbar()) {
            return false;
        }
        HomeMenuItemType this$homeType = this.getHomeType();
        HomeMenuItemType other$homeType = other.getHomeType();
        if (this$homeType == null ? other$homeType != null : !((Object)((Object)this$homeType)).equals((Object)other$homeType)) {
            return false;
        }
        String this$dashboardId = this.getDashboardId();
        String other$dashboardId = other.getDashboardId();
        return !(this$dashboardId == null ? other$dashboardId != null : !this$dashboardId.equals(other$dashboardId));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HomeMenuItem;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        result = result * 59 + (this.isHideDashboardToolbar() ? 79 : 97);
        HomeMenuItemType $homeType = this.getHomeType();
        result = result * 59 + ($homeType == null ? 43 : ((Object)((Object)$homeType)).hashCode());
        String $dashboardId = this.getDashboardId();
        result = result * 59 + ($dashboardId == null ? 43 : $dashboardId.hashCode());
        return result;
    }

    @Generated
    public HomeMenuItem() {
    }
}

