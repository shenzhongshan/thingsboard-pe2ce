/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jfree.chart.axis.NumberTickUnit
 *  org.jfree.chart.axis.TickUnit
 *  org.jfree.chart.axis.TickUnitSource
 */
package org.thingsboard.server.report.renderer.chart;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnitSource;

public class TbNumberTickUnitSource
implements TickUnitSource,
Serializable {
    private int power;
    private int factor;
    private final NumberFormat formatter;
    private final DecimalFormat dfNeg4 = new DecimalFormat("0.0000");
    private final DecimalFormat dfNeg3 = new DecimalFormat("0.000");
    private final DecimalFormat dfNeg2 = new DecimalFormat("0.00");
    private final DecimalFormat dfNeg1 = new DecimalFormat("0.0");
    private final DecimalFormat df0 = new DecimalFormat("#,##0");
    private final DecimalFormat df = new DecimalFormat("#.######E0");

    public TbNumberTickUnitSource() {
        this(null);
    }

    public TbNumberTickUnitSource(NumberFormat formatter) {
        this.formatter = formatter;
        this.power = 0;
        this.factor = 1;
    }

    public TickUnit getLargerTickUnit(TickUnit unit) {
        TickUnit t = this.getCeilingTickUnit(unit);
        if (t.equals((Object)unit)) {
            this.next();
            t = new NumberTickUnit(this.getTickSize(), this.getTickLabelFormat(), this.getMinorTickCount());
        }
        return t;
    }

    public TickUnit getCeilingTickUnit(TickUnit unit) {
        return this.getCeilingTickUnit(unit.getSize());
    }

    public TickUnit getCeilingTickUnit(double size) {
        return this.getCeilingTickUnit(size, true);
    }

    public TickUnit getCeilingTickUnit(double size, boolean roundToNearest) {
        if (Double.isInfinite(size)) {
            throw new IllegalArgumentException("Must be finite.");
        }
        if (roundToNearest) {
            size = this.nearestRoundedNumber(size);
        }
        this.power = (int)Math.ceil(Math.log10(size));
        this.factor = 1;
        boolean done = false;
        while (!done) {
            boolean bl = done = !this.previous();
            if (!(this.getTickSize() < size)) continue;
            this.next();
            done = true;
        }
        return new NumberTickUnit(this.getTickSize(), this.getTickLabelFormat(), this.getMinorTickCount());
    }

    private double nearestRoundedNumber(double number) {
        double magnitude = Math.floor(Math.log10(number));
        double roundBase = Math.pow(10.0, magnitude);
        BigDecimal bd = new BigDecimal(Double.toString(number));
        BigDecimal rounded = bd.divide(new BigDecimal(roundBase), 0, RoundingMode.HALF_UP).multiply(new BigDecimal(roundBase));
        return rounded.doubleValue();
    }

    private boolean next() {
        if (this.factor == 1) {
            this.factor = 2;
            return true;
        }
        if (this.factor == 2) {
            this.factor = 3;
            return true;
        }
        if (this.factor == 3) {
            this.factor = 5;
            return true;
        }
        if (this.factor == 5) {
            if (this.power == 300) {
                return false;
            }
            ++this.power;
            this.factor = 1;
            return true;
        }
        throw new IllegalStateException("We should never get here.");
    }

    private boolean previous() {
        if (this.factor == 1) {
            this.factor = 5;
            --this.power;
            return true;
        }
        if (this.factor == 2) {
            this.factor = 1;
            return true;
        }
        if (this.factor == 3) {
            this.factor = 2;
            return true;
        }
        if (this.factor == 5) {
            this.factor = 3;
            return true;
        }
        throw new IllegalStateException("We should never get here.");
    }

    private double getTickSize() {
        return (double)this.factor * Math.pow(10.0, this.power);
    }

    private NumberFormat getTickLabelFormat() {
        if (this.formatter != null) {
            return this.formatter;
        }
        if (this.power == -4) {
            return this.dfNeg4;
        }
        if (this.power == -3) {
            return this.dfNeg3;
        }
        if (this.power == -2) {
            return this.dfNeg2;
        }
        if (this.power == -1) {
            return this.dfNeg1;
        }
        if (this.power >= 0 && this.power <= 6) {
            return this.df0;
        }
        return this.df;
    }

    private int getMinorTickCount() {
        if (this.factor == 1) {
            return 10;
        }
        if (this.factor == 5) {
            return 5;
        }
        return 0;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TbNumberTickUnitSource)) {
            return false;
        }
        TbNumberTickUnitSource that = (TbNumberTickUnitSource)obj;
        if (!Objects.equals(this.formatter, that.formatter)) {
            return false;
        }
        if (this.power != that.power) {
            return false;
        }
        return this.factor == that.factor;
    }
}

