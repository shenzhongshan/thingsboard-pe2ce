package org.thingsboard.server.common.data.menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import org.thingsboard.server.common.data.menu.CustomMenuItem;
import org.thingsboard.server.common.data.menu.DefaultMenuItem;
import org.thingsboard.server.common.data.menu.HomeMenuItem;
import org.thingsboard.server.common.data.menu.MenuItemType;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property="type")
@JsonSubTypes(value={@JsonSubTypes.Type(value=HomeMenuItem.class, name="HOME"), @JsonSubTypes.Type(value=DefaultMenuItem.class, name="DEFAULT"), @JsonSubTypes.Type(value=CustomMenuItem.class, name="CUSTOM")})
public interface MenuItem
extends Serializable {
    public MenuItemType getType();

    public boolean isVisible();
}

