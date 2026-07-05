package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.Views;
import org.thingsboard.server.common.data.menu.MenuItem;

@Schema
public class CustomMenuConfig {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(CustomMenuConfig.class);
    @Schema(description="List of custom menu items", requiredMode=Schema.RequiredMode.REQUIRED)
    @JsonView(value={Views.Public.class})
    @Valid
    private List<MenuItem> items = new ArrayList<MenuItem>();

    @Generated
    public List<MenuItem> getItems() {
        return this.items;
    }

    @Generated
    public void setItems(List<MenuItem> items) {
        this.items = items;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CustomMenuConfig)) {
            return false;
        }
        CustomMenuConfig other = (CustomMenuConfig)o;
        if (!other.canEqual(this)) {
            return false;
        }
        List<MenuItem> this$items = this.getItems();
        List<MenuItem> other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof CustomMenuConfig;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<MenuItem> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "CustomMenuConfig(items=" + String.valueOf(this.getItems()) + ")";
    }

    @Generated
    public CustomMenuConfig() {
    }

    @ConstructorProperties(value={"items"})
    @Generated
    public CustomMenuConfig(List<MenuItem> items) {
        this.items = items;
    }
}

