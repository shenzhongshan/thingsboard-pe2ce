package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.Views;
import org.thingsboard.server.common.data.menu.MenuItem;
import org.thingsboard.server.common.data.menu.MenuItemType;

@Schema
public class DefaultMenuItem
implements MenuItem {
    @Schema(description="Unique identifier for predefined menu items", example="home", accessMode=Schema.AccessMode.READ_ONLY)
    @JsonView(value={Views.Public.class})
    private String id;
    @Schema(description="Name of the menu item", example="My Custom Menu")
    @JsonView(value={Views.Public.class})
    private String name;
    @Schema(description="URL of the menu item icon. Overrides 'materialIcon'", example="My Custom Menu")
    @JsonView(value={Views.Public.class})
    private String icon;
    @Schema(description="Mark if menu item is visible for user")
    @JsonView(value={Views.Private.class})
    private boolean visible;
    @Schema(description="List of child menu items")
    @JsonView(value={Views.Public.class})
    private List<DefaultMenuItem> pages;

    @Override
    @JsonView(value={Views.Private.class})
    public MenuItemType getType() {
        return MenuItemType.DEFAULT;
    }

    @Generated
    public String getId() {
        return this.id;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getIcon() {
        return this.icon;
    }

    @Override
    @Generated
    public boolean isVisible() {
        return this.visible;
    }

    @Generated
    public List<DefaultMenuItem> getPages() {
        return this.pages;
    }

    @Generated
    public void setId(String id) {
        this.id = id;
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
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Generated
    public void setPages(List<DefaultMenuItem> pages) {
        this.pages = pages;
    }

    @Generated
    public String toString() {
        return "DefaultMenuItem(id=" + this.getId() + ", name=" + this.getName() + ", icon=" + this.getIcon() + ", visible=" + this.isVisible() + ", pages=" + String.valueOf(this.getPages()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DefaultMenuItem)) {
            return false;
        }
        DefaultMenuItem other = (DefaultMenuItem)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isVisible() != other.isVisible()) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
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
        List<DefaultMenuItem> this$pages = this.getPages();
        List<DefaultMenuItem> other$pages = other.getPages();
        return !(this$pages == null ? other$pages != null : !((Object)this$pages).equals(other$pages));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DefaultMenuItem;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isVisible() ? 79 : 97);
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $icon = this.getIcon();
        result = result * 59 + ($icon == null ? 43 : $icon.hashCode());
        List<DefaultMenuItem> $pages = this.getPages();
        result = result * 59 + ($pages == null ? 43 : ((Object)$pages).hashCode());
        return result;
    }

    @Generated
    public DefaultMenuItem() {
    }
}

