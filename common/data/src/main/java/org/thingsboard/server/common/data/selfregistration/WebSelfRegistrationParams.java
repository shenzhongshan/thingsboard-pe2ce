package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Generated;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.selfregistration.AbstractSelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationType;

public class WebSelfRegistrationParams
extends AbstractSelfRegistrationParams {
    @Schema(description="Domain name for self registration URL. Typically this matches the domain name from the Login White Labeling page.")
    @NotNull
    private DomainId domainId;

    @Override
    public SelfRegistrationType getType() {
        return SelfRegistrationType.WEB;
    }

    @Generated
    public WebSelfRegistrationParams() {
    }

    @Generated
    public DomainId getDomainId() {
        return this.domainId;
    }

    @Generated
    public void setDomainId(DomainId domainId) {
        this.domainId = domainId;
    }

    @Override
    @Generated
    public String toString() {
        return "WebSelfRegistrationParams(domainId=" + String.valueOf(this.getDomainId()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WebSelfRegistrationParams)) {
            return false;
        }
        WebSelfRegistrationParams other = (WebSelfRegistrationParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DomainId this$domainId = this.getDomainId();
        DomainId other$domainId = other.getDomainId();
        return !(this$domainId == null ? other$domainId != null : !((Object)this$domainId).equals(other$domainId));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof WebSelfRegistrationParams;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        DomainId $domainId = this.getDomainId();
        result = result * 59 + ($domainId == null ? 43 : ((Object)$domainId).hashCode());
        return result;
    }
}

