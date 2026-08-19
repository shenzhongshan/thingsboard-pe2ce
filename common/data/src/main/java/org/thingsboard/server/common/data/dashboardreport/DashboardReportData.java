/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.dashboardreport;

import java.util.Arrays;
import lombok.Generated;

public class DashboardReportData {
    private byte[] data;
    private String name;
    private String contentType;

    @Generated
    public DashboardReportData() {
    }

    @Generated
    public byte[] getData() {
        return this.data;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getContentType() {
        return this.contentType;
    }

    @Generated
    public void setData(byte[] data) {
        this.data = data;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DashboardReportData)) {
            return false;
        }
        DashboardReportData other = (DashboardReportData)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!Arrays.equals(this.getData(), other.getData())) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$contentType = this.getContentType();
        String other$contentType = other.getContentType();
        return !(this$contentType == null ? other$contentType != null : !this$contentType.equals(other$contentType));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof DashboardReportData;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + Arrays.hashCode(this.getData());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $contentType = this.getContentType();
        result = result * 59 + ($contentType == null ? 43 : $contentType.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "DashboardReportData(data=" + Arrays.toString(this.getData()) + ", name=" + this.getName() + ", contentType=" + this.getContentType() + ")";
    }
}

