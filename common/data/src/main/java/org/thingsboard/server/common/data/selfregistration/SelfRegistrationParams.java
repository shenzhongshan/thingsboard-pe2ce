package org.thingsboard.server.common.data.selfregistration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.List;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.selfregistration.CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.DefaultDashboardParams;
import org.thingsboard.server.common.data.selfregistration.HomeDashboardParams;
import org.thingsboard.server.common.data.selfregistration.MobileSelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationType;
import org.thingsboard.server.common.data.selfregistration.SignUpField;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property="type")
@JsonSubTypes(value={@JsonSubTypes.Type(value=WebSelfRegistrationParams.class, name="WEB"), @JsonSubTypes.Type(value=MobileSelfRegistrationParams.class, name="MOBILE")})
public interface SelfRegistrationParams
extends Serializable {
    public SelfRegistrationType getType();

    public Boolean getEnabled();

    public String getTitle();

    public CaptchaParams getCaptcha();

    public NotificationTargetId getNotificationRecipient();

    public List<SignUpField> getSignUpFields();

    public String getCustomerTitlePrefix();

    public Boolean getShowPrivacyPolicy();

    public Boolean getShowTermsOfUse();

    public DefaultDashboardParams getDefaultDashboard();

    public HomeDashboardParams getHomeDashboard();

    public List<GroupPermission> getPermissions();

    public EntityGroupId getCustomerGroupId();

    public CustomMenuId getCustomMenuId();

    public SignUpSelfRegistrationParams toSignUpSelfRegistrationParams(PlatformType var1);
}
