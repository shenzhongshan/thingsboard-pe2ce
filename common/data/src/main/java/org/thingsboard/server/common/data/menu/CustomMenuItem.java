package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.Views;
import org.thingsboard.server.common.data.menu.CMItemLinkType;
import org.thingsboard.server.common.data.menu.CMItemType;
import org.thingsboard.server.common.data.menu.MenuItem;
import org.thingsboard.server.common.data.menu.MenuItemType;

@Schema
public class CustomMenuItem
implements MenuItem {
    @Schema(description="Name of the menu item", example="My Custom Menu", requiredMode=Schema.RequiredMode.REQUIRED)
    @JsonView(value={Views.Public.class})
    private String name;
    @Schema(description="URL of the menu item icon. Overrides 'materialIcon'", example="My Custom Menu")
    @JsonView(value={Views.Public.class})
    private String icon;
    @Schema(description="Type of menu item (LINK or SECTION). LINK type means item has no child items, SECTION type should have at least one child", example="LINK")
    @NotNull
    @JsonView(value={Views.Public.class})
    private CMItemType menuItemType;
    @Schema(description="Type of menu item (URL or DASHBOARD)", example="URL")
    @JsonView(value={Views.Public.class})
    private CMItemLinkType linkType;
    @Schema(description="Id of the Dashboard to open, when user clicks the menu item", example="https://mycompany.com")
    @JsonView(value={Views.Public.class})
    private String dashboardId;
    @Schema(description="Hide the dashboard toolbar")
    @JsonView(value={Views.Public.class})
    private Boolean hideDashboardToolbar;
    @Schema(description="URL to open in the iframe, when user clicks the menu item", example="https://myexternalurl.com")
    @JsonView(value={Views.Public.class})
    private String url;
    @Schema(description="Set the access token of the current user to a new dashboard")
    @JsonView(value={Views.Public.class})
    private boolean setAccessToken;
    @Schema(description="Mark if menu item is visible for user")
    @JsonView(value={Views.Private.class})
    private boolean visible;
    @Schema(description="List of child menu items")
    @JsonView(value={Views.Public.class})
    private List<CustomMenuItem> pages;

    @Override
    @JsonView(value={Views.Private.class})
    public MenuItemType getType() {
        return MenuItemType.CUSTOM;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getIcon() {
        return this.icon;
    }

    @Generated
    public CMItemType getMenuItemType() {
        return this.menuItemType;
    }

    @Generated
    public CMItemLinkType getLinkType() {
        return this.linkType;
    }

    @Generated
    public String getDashboardId() {
        return this.dashboardId;
    }

    @Generated
    public Boolean getHideDashboardToolbar() {
        return this.hideDashboardToolbar;
    }

    @Generated
    public String getUrl() {
        return this.url;
    }

    @Generated
    public boolean isSetAccessToken() {
        return this.setAccessToken;
    }

    @Override
    @Generated
    public boolean isVisible() {
        return this.visible;
    }

    @Generated
    public List<CustomMenuItem> getPages() {
        return this.pages;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Generated
    public void setMenuItemType(CMItemType menuItemType) {
        this.menuItemType = menuItemType;
    }

    @Generated
    public void setLinkType(CMItemLinkType linkType) {
        this.linkType = linkType;
    }

    @Generated
    public void setDashboardId(String dashboardId) {
        this.dashboardId = dashboardId;
    }

    @Generated
    public void setHideDashboardToolbar(Boolean hideDashboardToolbar) {
        this.hideDashboardToolbar = hideDashboardToolbar;
    }

    @Generated
    public void setUrl(String url) {
        this.url = url;
    }

    @Generated
    public void setSetAccessToken(boolean setAccessToken) {
        this.setAccessToken = setAccessToken;
    }

    @Generated
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Generated
    public void setPages(List<CustomMenuItem> pages) {
        this.pages = pages;
    }

    @Generated
    public String toString() {
        return "CustomMenuItem(name=" + this.getName() + ", icon=" + this.getIcon() + ", menuItemType=" + String.valueOf((Object)this.getMenuItemType()) + ", linkType=" + String.valueOf((Object)this.getLinkType()) + ", dashboardId=" + this.getDashboardId() + ", hideDashboardToolbar=" + this.getHideDashboardToolbar() + ", url=" + this.getUrl() + ", setAccessToken=" + this.isSetAccessToken() + ", visible=" + this.isVisible() + ", pages=" + String.valueOf(this.getPages()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CustomMenuItem)) {
            return false;
        }
        CustomMenuItem other = (CustomMenuItem)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isSetAccessToken() != other.isSetAccessToken()) {
            return false;
        }
        if (this.isVisible() != other.isVisible()) {
            return false;
        }
        Boolean this$hideDashboardToolbar = this.getHideDashboardToolbar();
        Boolean other$hideDashboardToolbar = other.getHideDashboardToolbar();
        if (this$hideDashboardToolbar == null ? other$hideDashboardToolbar != null : !((Object)this$hideDashboardToolbar).equals(other$hideDashboardToolbar)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$icon = this.getIcon();
        String other$icon = other.getIcon();
        if (this$icon == null ? other$icon != null : !this$icon.equals(other$icon)) {
            return false;
        }
        CMItemType this$menuItemType = this.getMenuItemType();
        CMItemType other$menuItemType = other.getMenuItemType();
        if (this$menuItemType == null ? other$menuItemType != null : !((Object)((Object)this$menuItemType)).equals((Object)other$menuItemType)) {
            return false;
        }
        CMItemLinkType this$linkType = this.getLinkType();
        CMItemLinkType other$linkType = other.getLinkType();
        if (this$linkType == null ? other$linkType != null : !((Object)((Object)this$linkType)).equals((Object)other$linkType)) {
            return false;
        }
        String this$dashboardId = this.getDashboardId();
        String other$dashboardId = other.getDashboardId();
        if (this$dashboardId == null ? other$dashboardId != null : !this$dashboardId.equals(other$dashboardId)) {
            return false;
        }
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) {
            return false;
        }
        List<CustomMenuItem> this$pages = this.getPages();
        List<CustomMenuItem> other$pages = other.getPages();
        return !(this$pages == null ? other$pages != null : !((Object)this$pages).equals(other$pages));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof CustomMenuItem;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isSetAccessToken() ? 79 : 97);
        result = result * 59 + (this.isVisible() ? 79 : 97);
        Boolean $hideDashboardToolbar = this.getHideDashboardToolbar();
        result = result * 59 + ($hideDashboardToolbar == null ? 43 : ((Object)$hideDashboardToolbar).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $icon = this.getIcon();
        result = result * 59 + ($icon == null ? 43 : $icon.hashCode());
        CMItemType $menuItemType = this.getMenuItemType();
        result = result * 59 + ($menuItemType == null ? 43 : ((Object)((Object)$menuItemType)).hashCode());
        CMItemLinkType $linkType = this.getLinkType();
        result = result * 59 + ($linkType == null ? 43 : ((Object)((Object)$linkType)).hashCode());
        String $dashboardId = this.getDashboardId();
        result = result * 59 + ($dashboardId == null ? 43 : $dashboardId.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        List<CustomMenuItem> $pages = this.getPages();
        result = result * 59 + ($pages == null ? 43 : ((Object)$pages).hashCode());
        return result;
    }

    @Generated
    public CustomMenuItem() {
    }
}

