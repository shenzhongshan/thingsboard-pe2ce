/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.report.configuration.DataKey
 */
package org.thingsboard.server.report.context.chart;

import org.thingsboard.server.common.data.report.configuration.DataKey;

@FunctionalInterface
public interface DataPostProcessFunction {
    public Object apply(DataKey var1, long var2, String var4);
}

