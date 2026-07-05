package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Generated;
import org.thingsboard.server.common.data.selfregistration.AbstractSelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.MobileRedirectParams;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationType;

public class MobileSelfRegistrationParams
extends AbstractSelfRegistrationParams {
    @Schema(description="Mobile redirect params.", requiredMode=Schema.RequiredMode.REQUIRED)
    @NotNull
    private MobileRedirectParams redirect;

    @Override
    public SelfRegistrationType getType() {
        return SelfRegistrationType.MOBILE;
    }

    @Generated
    public MobileSelfRegistrationParams() {
    }

    @Generated
    public MobileRedirectParams getRedirect() {
        return this.redirect;
    }

    @Generated
    public void setRedirect(MobileRedirectParams redirect) {
        this.redirect = redirect;
    }

    @Override
    @Generated
    public String toString() {
        return "MobileSelfRegistrationParams(redirect=" + String.valueOf(this.getRedirect()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MobileSelfRegistrationParams)) {
            return false;
        }
        MobileSelfRegistrationParams other = (MobileSelfRegistrationParams)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MobileRedirectParams this$redirect = this.getRedirect();
        MobileRedirectParams other$redirect = other.getRedirect();
        return !(this$redirect == null ? other$redirect != null : !((Object)this$redirect).equals(other$redirect));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof MobileSelfRegistrationParams;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        MobileRedirectParams $redirect = this.getRedirect();
        result = result * 59 + ($redirect == null ? 43 : ((Object)$redirect).hashCode());
        return result;
    }
}

