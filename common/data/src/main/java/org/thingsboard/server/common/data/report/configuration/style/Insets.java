/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.style;

import java.beans.ConstructorProperties;
import lombok.Generated;

public class Insets {
    private int left;
    private int right;
    private int top;
    private int bottom;

    public Insets(int margin) {
        this.left = margin;
        this.top = margin;
        this.right = margin;
        this.bottom = margin;
    }

    @Generated
    public int getLeft() {
        return this.left;
    }

    @Generated
    public int getRight() {
        return this.right;
    }

    @Generated
    public int getTop() {
        return this.top;
    }

    @Generated
    public int getBottom() {
        return this.bottom;
    }

    @Generated
    public void setLeft(int left) {
        this.left = left;
    }

    @Generated
    public void setRight(int right) {
        this.right = right;
    }

    @Generated
    public void setTop(int top) {
        this.top = top;
    }

    @Generated
    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Insets)) {
            return false;
        }
        Insets other = (Insets)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getLeft() != other.getLeft()) {
            return false;
        }
        if (this.getRight() != other.getRight()) {
            return false;
        }
        if (this.getTop() != other.getTop()) {
            return false;
        }
        return this.getBottom() == other.getBottom();
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Insets;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getLeft();
        result = result * 59 + this.getRight();
        result = result * 59 + this.getTop();
        result = result * 59 + this.getBottom();
        return result;
    }

    @Generated
    public String toString() {
        return "Insets(left=" + this.getLeft() + ", right=" + this.getRight() + ", top=" + this.getTop() + ", bottom=" + this.getBottom() + ")";
    }

    @ConstructorProperties(value={"left", "right", "top", "bottom"})
    @Generated
    public Insets(int left, int right, int top, int bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }
}

