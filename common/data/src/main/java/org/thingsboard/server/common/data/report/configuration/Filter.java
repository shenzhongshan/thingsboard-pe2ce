/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Generated;
import org.thingsboard.server.common.data.query.KeyFilter;

@Schema
public class Filter {
    String id;
    String filter;
    List<KeyFilter> keyFilters;

    @Generated
    public String getId() {
        return this.id;
    }

    @Generated
    public String getFilter() {
        return this.filter;
    }

    @Generated
    public List<KeyFilter> getKeyFilters() {
        return this.keyFilters;
    }

    @Generated
    public void setId(String id) {
        this.id = id;
    }

    @Generated
    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Generated
    public void setKeyFilters(List<KeyFilter> keyFilters) {
        this.keyFilters = keyFilters;
    }

    @Generated
    public String toString() {
        return "Filter(id=" + this.getId() + ", filter=" + this.getFilter() + ", keyFilters=" + String.valueOf(this.getKeyFilters()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Filter)) {
            return false;
        }
        Filter other = (Filter)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$filter = this.getFilter();
        String other$filter = other.getFilter();
        if (this$filter == null ? other$filter != null : !this$filter.equals(other$filter)) {
            return false;
        }
        List<KeyFilter> this$keyFilters = this.getKeyFilters();
        List<KeyFilter> other$keyFilters = other.getKeyFilters();
        return !(this$keyFilters == null ? other$keyFilters != null : !((Object)this$keyFilters).equals(other$keyFilters));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Filter;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $filter = this.getFilter();
        result = result * 59 + ($filter == null ? 43 : $filter.hashCode());
        List<KeyFilter> $keyFilters = this.getKeyFilters();
        result = result * 59 + ($keyFilters == null ? 43 : ((Object)$keyFilters).hashCode());
        return result;
    }

    @Generated
    public Filter() {
    }
}

