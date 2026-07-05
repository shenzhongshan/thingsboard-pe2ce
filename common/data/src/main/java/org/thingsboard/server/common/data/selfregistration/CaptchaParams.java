package org.thingsboard.server.common.data.selfregistration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.selfregistration.EnterpriseCaptchaParams;
import org.thingsboard.server.common.data.selfregistration.V2CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.V3CaptchaParams;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property="version")
@JsonSubTypes(value={@JsonSubTypes.Type(value=EnterpriseCaptchaParams.class, name="enterprise"), @JsonSubTypes.Type(value=V2CaptchaParams.class, name="v2"), @JsonSubTypes.Type(value=V3CaptchaParams.class, name="v3")})
public interface CaptchaParams
extends Serializable {
    public String getVersion();

    public CaptchaParams toInfo(PlatformType var1);
}

