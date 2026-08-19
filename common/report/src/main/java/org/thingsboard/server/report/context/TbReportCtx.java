/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.thingsboard.script.api.tbel.TbelInvokeService
 *  org.thingsboard.server.common.data.id.EntityId
 *  org.thingsboard.server.common.data.id.TenantId
 *  org.thingsboard.server.common.data.id.UserId
 *  org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig
 */
package org.thingsboard.server.report.context;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Generated;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

public abstract class TbReportCtx
implements Closeable {
    private final TenantId tenantId;
    private final ReportTemplateConfig configuration;
    private final String timeZone;
    private final UserId userId;
    private final EntityId userOwnerId;
    private final String accessToken;
    private final long accessTokenExpTs;
    private final String reportCreatedTime;
    private final Map<String, Object> params = new HashMap<String, Object>();
    private final Map<String, UUID> scripts = new HashMap<String, UUID>();
    private final TbelInvokeService tbelInvokeService;

    public abstract TbReportCtx createSubReportCxt(ReportTemplateConfig var1);

    @Override
    public void close() throws IOException {
        this.getScripts().values().forEach(scriptId -> {
            if (scriptId != null) {
                this.tbelInvokeService.release(scriptId);
            }
        });
    }

    @Generated
    protected TbReportCtx(TbReportCtxBuilder<?, ?> b) {
        this.tenantId = b.tenantId;
        this.configuration = b.configuration;
        this.timeZone = b.timeZone;
        this.userId = b.userId;
        this.userOwnerId = b.userOwnerId;
        this.accessToken = b.accessToken;
        this.accessTokenExpTs = b.accessTokenExpTs;
        this.reportCreatedTime = b.reportCreatedTime;
        this.tbelInvokeService = b.tbelInvokeService;
    }

    @Generated
    public TenantId getTenantId() {
        return this.tenantId;
    }

    @Generated
    public ReportTemplateConfig getConfiguration() {
        return this.configuration;
    }

    @Generated
    public String getTimeZone() {
        return this.timeZone;
    }

    @Generated
    public UserId getUserId() {
        return this.userId;
    }

    @Generated
    public EntityId getUserOwnerId() {
        return this.userOwnerId;
    }

    @Generated
    public String getAccessToken() {
        return this.accessToken;
    }

    @Generated
    public long getAccessTokenExpTs() {
        return this.accessTokenExpTs;
    }

    @Generated
    public String getReportCreatedTime() {
        return this.reportCreatedTime;
    }

    @Generated
    public Map<String, Object> getParams() {
        return this.params;
    }

    @Generated
    public Map<String, UUID> getScripts() {
        return this.scripts;
    }

    @Generated
    public TbelInvokeService getTbelInvokeService() {
        return this.tbelInvokeService;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TbReportCtx)) {
            return false;
        }
        TbReportCtx other = (TbReportCtx)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getAccessTokenExpTs() != other.getAccessTokenExpTs()) {
            return false;
        }
        TenantId this$tenantId = this.getTenantId();
        TenantId other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        ReportTemplateConfig this$configuration = this.getConfiguration();
        ReportTemplateConfig other$configuration = other.getConfiguration();
        if (this$configuration == null ? other$configuration != null : !this$configuration.equals(other$configuration)) {
            return false;
        }
        String this$timeZone = this.getTimeZone();
        String other$timeZone = other.getTimeZone();
        if (this$timeZone == null ? other$timeZone != null : !this$timeZone.equals(other$timeZone)) {
            return false;
        }
        UserId this$userId = this.getUserId();
        UserId other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) {
            return false;
        }
        EntityId this$userOwnerId = this.getUserOwnerId();
        EntityId other$userOwnerId = other.getUserOwnerId();
        if (this$userOwnerId == null ? other$userOwnerId != null : !this$userOwnerId.equals(other$userOwnerId)) {
            return false;
        }
        String this$accessToken = this.getAccessToken();
        String other$accessToken = other.getAccessToken();
        if (this$accessToken == null ? other$accessToken != null : !this$accessToken.equals(other$accessToken)) {
            return false;
        }
        String this$reportCreatedTime = this.getReportCreatedTime();
        String other$reportCreatedTime = other.getReportCreatedTime();
        if (this$reportCreatedTime == null ? other$reportCreatedTime != null : !this$reportCreatedTime.equals(other$reportCreatedTime)) {
            return false;
        }
        Map<String, Object> this$params = this.getParams();
        Map<String, Object> other$params = other.getParams();
        if (this$params == null ? other$params != null : !((Object)this$params).equals(other$params)) {
            return false;
        }
        Map<String, UUID> this$scripts = this.getScripts();
        Map<String, UUID> other$scripts = other.getScripts();
        if (this$scripts == null ? other$scripts != null : !((Object)this$scripts).equals(other$scripts)) {
            return false;
        }
        TbelInvokeService this$tbelInvokeService = this.getTbelInvokeService();
        TbelInvokeService other$tbelInvokeService = other.getTbelInvokeService();
        return !(this$tbelInvokeService == null ? other$tbelInvokeService != null : !this$tbelInvokeService.equals(other$tbelInvokeService));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TbReportCtx;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $accessTokenExpTs = this.getAccessTokenExpTs();
        result = result * 59 + (int)($accessTokenExpTs >>> 32 ^ $accessTokenExpTs);
        TenantId $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        ReportTemplateConfig $configuration = this.getConfiguration();
        result = result * 59 + ($configuration == null ? 43 : $configuration.hashCode());
        String $timeZone = this.getTimeZone();
        result = result * 59 + ($timeZone == null ? 43 : $timeZone.hashCode());
        UserId $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : $userId.hashCode());
        EntityId $userOwnerId = this.getUserOwnerId();
        result = result * 59 + ($userOwnerId == null ? 43 : $userOwnerId.hashCode());
        String $accessToken = this.getAccessToken();
        result = result * 59 + ($accessToken == null ? 43 : $accessToken.hashCode());
        String $reportCreatedTime = this.getReportCreatedTime();
        result = result * 59 + ($reportCreatedTime == null ? 43 : $reportCreatedTime.hashCode());
        Map<String, Object> $params = this.getParams();
        result = result * 59 + ($params == null ? 43 : ((Object)$params).hashCode());
        Map<String, UUID> $scripts = this.getScripts();
        result = result * 59 + ($scripts == null ? 43 : ((Object)$scripts).hashCode());
        TbelInvokeService $tbelInvokeService = this.getTbelInvokeService();
        result = result * 59 + ($tbelInvokeService == null ? 43 : $tbelInvokeService.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TbReportCtx(tenantId=" + String.valueOf(this.getTenantId()) + ", configuration=" + String.valueOf(this.getConfiguration()) + ", timeZone=" + this.getTimeZone() + ", userId=" + String.valueOf(this.getUserId()) + ", userOwnerId=" + String.valueOf(this.getUserOwnerId()) + ", accessToken=" + this.getAccessToken() + ", accessTokenExpTs=" + this.getAccessTokenExpTs() + ", reportCreatedTime=" + this.getReportCreatedTime() + ", params=" + String.valueOf(this.getParams()) + ", scripts=" + String.valueOf(this.getScripts()) + ", tbelInvokeService=" + String.valueOf(this.getTbelInvokeService()) + ")";
    }

    @Generated
    public static abstract class TbReportCtxBuilder<C extends TbReportCtx, B extends TbReportCtxBuilder<C, B>> {
        @Generated
        private TenantId tenantId;
        @Generated
        private ReportTemplateConfig configuration;
        @Generated
        private String timeZone;
        @Generated
        private UserId userId;
        @Generated
        private EntityId userOwnerId;
        @Generated
        private String accessToken;
        @Generated
        private long accessTokenExpTs;
        @Generated
        private String reportCreatedTime;
        @Generated
        private TbelInvokeService tbelInvokeService;

        @Generated
        public B tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this.self();
        }

        @Generated
        public B configuration(ReportTemplateConfig configuration) {
            this.configuration = configuration;
            return this.self();
        }

        @Generated
        public B timeZone(String timeZone) {
            this.timeZone = timeZone;
            return this.self();
        }

        @Generated
        public B userId(UserId userId) {
            this.userId = userId;
            return this.self();
        }

        @Generated
        public B userOwnerId(EntityId userOwnerId) {
            this.userOwnerId = userOwnerId;
            return this.self();
        }

        @Generated
        public B accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this.self();
        }

        @Generated
        public B accessTokenExpTs(long accessTokenExpTs) {
            this.accessTokenExpTs = accessTokenExpTs;
            return this.self();
        }

        @Generated
        public B reportCreatedTime(String reportCreatedTime) {
            this.reportCreatedTime = reportCreatedTime;
            return this.self();
        }

        @Generated
        public B tbelInvokeService(TbelInvokeService tbelInvokeService) {
            this.tbelInvokeService = tbelInvokeService;
            return this.self();
        }

        @Generated
        protected abstract B self();

        @Generated
        public abstract C build();

        @Generated
        public String toString() {
            return "TbReportCtx.TbReportCtxBuilder(tenantId=" + String.valueOf(this.tenantId) + ", configuration=" + String.valueOf(this.configuration) + ", timeZone=" + this.timeZone + ", userId=" + String.valueOf(this.userId) + ", userOwnerId=" + String.valueOf(this.userOwnerId) + ", accessToken=" + this.accessToken + ", accessTokenExpTs=" + this.accessTokenExpTs + ", reportCreatedTime=" + this.reportCreatedTime + ", tbelInvokeService=" + String.valueOf(this.tbelInvokeService) + ")";
        }
    }
}

