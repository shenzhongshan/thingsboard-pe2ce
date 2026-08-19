/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.query.EntityFilter;

@Schema
public class EntityAlias {
    String id;
    String alias;
    EntityFilter filter;

    @Generated
    public String getId() {
        return this.id;
    }

    @Generated
    public String getAlias() {
        return this.alias;
    }

    @Generated
    public EntityFilter getFilter() {
        return this.filter;
    }

    @Generated
    public void setId(String id) {
        this.id = id;
    }

    @Generated
    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Generated
    public void setFilter(EntityFilter filter) {
        this.filter = filter;
    }

    @Generated
    public String toString() {
        return "EntityAlias(id=" + this.getId() + ", alias=" + this.getAlias() + ", filter=" + String.valueOf(this.getFilter()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EntityAlias)) {
            return false;
        }
        EntityAlias other = (EntityAlias)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$alias = this.getAlias();
        String other$alias = other.getAlias();
        if (this$alias == null ? other$alias != null : !this$alias.equals(other$alias)) {
            return false;
        }
        EntityFilter this$filter = this.getFilter();
        EntityFilter other$filter = other.getFilter();
        return !(this$filter == null ? other$filter != null : !this$filter.equals(other$filter));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof EntityAlias;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $alias = this.getAlias();
        result = result * 59 + ($alias == null ? 43 : $alias.hashCode());
        EntityFilter $filter = this.getFilter();
        result = result * 59 + ($filter == null ? 43 : $filter.hashCode());
        return result;
    }

    @Generated
    public EntityAlias() {
    }

    @ConstructorProperties(value={"id", "alias", "filter"})
    @Generated
    public EntityAlias(String id, String alias, EntityFilter filter) {
        this.id = id;
        this.alias = alias;
        this.filter = filter;
    }
}

