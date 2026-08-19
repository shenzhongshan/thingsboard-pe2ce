/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import lombok.Generated;

public class ColorRange {
    private Double from;
    private Double to;
    private String color;
    public static final Comparator<ColorRange> COLOR_RANGE_COMPARATOR = (a, b) -> {
        if (ColorRange.isNumber(a.from) && ColorRange.isNumber(a.to) && ColorRange.isNumber(b.from) && ColorRange.isNumber(b.to)) {
            if (b.from >= a.from && b.to < a.to) {
                return 1;
            }
            if (a.from >= b.from && a.to < b.to) {
                return -1;
            }
            return (int)(a.from - b.from);
        }
        if (ColorRange.isNumber(a.from) && ColorRange.isNumber(b.from)) {
            return (int)(a.from - b.from);
        }
        if (ColorRange.isNumber(a.to) && ColorRange.isNumber(b.to)) {
            return (int)(a.to - b.to);
        }
        if (ColorRange.isNumber(a.from) && !ColorRange.isNumber(b.from)) {
            return 1;
        }
        if (!ColorRange.isNumber(a.from) && ColorRange.isNumber(b.from)) {
            return -1;
        }
        if (ColorRange.isNumber(a.to) && !ColorRange.isNumber(b.to)) {
            return 1;
        }
        if (!ColorRange.isNumber(a.to) && ColorRange.isNumber(b.to)) {
            return -1;
        }
        return 0;
    };

    @JsonIgnore
    public boolean includes(ColorRange other) {
        if (ColorRange.isNumber(this.from) && ColorRange.isNumber(this.to)) {
            if (ColorRange.isNumber(other.from) && ColorRange.isNumber(other.to)) {
                return other.from >= this.from && other.to < this.to;
            }
            return false;
        }
        if (ColorRange.isNumber(this.from)) {
            if (ColorRange.isNumber(other.from)) {
                return other.from >= this.from;
            }
            return false;
        }
        if (ColorRange.isNumber(this.to)) {
            if (ColorRange.isNumber(other.to)) {
                return other.to < this.to;
            }
            return false;
        }
        return false;
    }

    public static List<ColorRange> filterIncludingColorRanges(List<ColorRange> colorRanges) {
        ArrayList<ColorRange> result = new ArrayList<ColorRange>(colorRanges);
        boolean includes = true;
        while (includes) {
            int index = -1;
            int i = 0;
            while (i < result.size()) {
                ColorRange range = (ColorRange)result.get(i);
                int currentIndex = i++;
                if (!IntStream.range(0, result.size()).anyMatch(i1 -> {
                    ColorRange value = (ColorRange)result.get(i1);
                    return i1 != currentIndex && value.includes(range);
                })) continue;
                index = i;
                break;
            }
            if (index > -1) {
                result.remove(index);
                continue;
            }
            includes = false;
        }
        return result;
    }

    private static boolean isNumber(Double number) {
        return number != null && Double.isFinite(number);
    }

    @Generated
    public ColorRange() {
    }

    @Generated
    public Double getFrom() {
        return this.from;
    }

    @Generated
    public Double getTo() {
        return this.to;
    }

    @Generated
    public String getColor() {
        return this.color;
    }

    @Generated
    public void setFrom(Double from) {
        this.from = from;
    }

    @Generated
    public void setTo(Double to) {
        this.to = to;
    }

    @Generated
    public void setColor(String color) {
        this.color = color;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ColorRange)) {
            return false;
        }
        ColorRange other = (ColorRange)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$from = this.getFrom();
        Double other$from = other.getFrom();
        if (this$from == null ? other$from != null : !((Object)this$from).equals(other$from)) {
            return false;
        }
        Double this$to = this.getTo();
        Double other$to = other.getTo();
        if (this$to == null ? other$to != null : !((Object)this$to).equals(other$to)) {
            return false;
        }
        String this$color = this.getColor();
        String other$color = other.getColor();
        return !(this$color == null ? other$color != null : !this$color.equals(other$color));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ColorRange;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $from = this.getFrom();
        result = result * 59 + ($from == null ? 43 : ((Object)$from).hashCode());
        Double $to = this.getTo();
        result = result * 59 + ($to == null ? 43 : ((Object)$to).hashCode());
        String $color = this.getColor();
        result = result * 59 + ($color == null ? 43 : $color.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ColorRange(from=" + this.getFrom() + ", to=" + this.getTo() + ", color=" + this.getColor() + ")";
    }
}

