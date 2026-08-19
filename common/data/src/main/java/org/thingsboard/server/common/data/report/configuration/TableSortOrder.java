/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration;

import java.beans.ConstructorProperties;
import lombok.Generated;

public class TableSortOrder {
    private String column;
    private Direction direction;

    @Generated
    public String getColumn() {
        return this.column;
    }

    @Generated
    public Direction getDirection() {
        return this.direction;
    }

    @Generated
    public void setColumn(String column) {
        this.column = column;
    }

    @Generated
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TableSortOrder)) {
            return false;
        }
        TableSortOrder other = (TableSortOrder)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$column = this.getColumn();
        String other$column = other.getColumn();
        if (this$column == null ? other$column != null : !this$column.equals(other$column)) {
            return false;
        }
        Direction this$direction = this.getDirection();
        Direction other$direction = other.getDirection();
        return !(this$direction == null ? other$direction != null : !((Object)((Object)this$direction)).equals((Object)other$direction));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TableSortOrder;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $column = this.getColumn();
        result = result * 59 + ($column == null ? 43 : $column.hashCode());
        Direction $direction = this.getDirection();
        result = result * 59 + ($direction == null ? 43 : ((Object)((Object)$direction)).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TableSortOrder(column=" + this.getColumn() + ", direction=" + String.valueOf((Object)this.getDirection()) + ")";
    }

    @ConstructorProperties(value={"column", "direction"})
    @Generated
    public TableSortOrder(String column, Direction direction) {
        this.column = column;
        this.direction = direction;
    }

    @Generated
    public TableSortOrder() {
    }

    public static enum Direction {
        ASC,
        DESC;

    }
}

