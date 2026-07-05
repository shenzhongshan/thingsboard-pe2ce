/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.thingsboard.server.common.data.id.CustomMenuId;

public class CustomMenu extends CustomMenuInfo {

    private static final long serialVersionUID = 1L;

    @Valid
    @Schema(description = "The custom menu item tree")
    private CustomMenuConfig config;

    public CustomMenu() {
        super();
    }

    public CustomMenu(CustomMenuId id) {
        super(id);
    }

    public CustomMenu(CustomMenuInfo info) {
        super(info);
    }

    public CustomMenu(CustomMenu other) {
        super(other);
        this.config = other.getConfig();
    }

    public CustomMenu(CustomMenuInfo info, CustomMenuConfig config) {
        super(info);
        this.config = config;
    }

    public CustomMenuConfig getConfig() {
        return config;
    }

    public void setConfig(CustomMenuConfig config) {
        this.config = config;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CustomMenu other = (CustomMenu) obj;
        if (config == null) {
            return other.config == null;
        } else {
            return config.equals(other.config);
        }
    }

    @Override
    public String toString() {
        return "CustomMenu [" + super.toString() + ", config=" + config + "]";
    }
}
