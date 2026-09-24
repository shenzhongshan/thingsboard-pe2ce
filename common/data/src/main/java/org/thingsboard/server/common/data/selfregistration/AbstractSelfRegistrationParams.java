package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.selfregistration.CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.DefaultDashboardParams;
import org.thingsboard.server.common.data.selfregistration.HomeDashboardParams;
import org.thingsboard.server.common.data.selfregistration.SignUpField;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;

@Data
public abstract class AbstractSelfRegistrationParams
implements SelfRegistrationParams {
    @Schema(description="Indicates if self-registration is allowed via sign-up form")
    protected Boolean enabled;
    @Schema(description="The text message to appear on login form")
    @NotNull
    protected String title;
    @Schema(description="Captcha site key for 'I'm not a robot' validation")
    @NotNull
    protected CaptchaParams captcha;
    @Schema(description="List of sign-up form fields")
    @NotNull
    protected List<SignUpField> signUpFields;
    @Schema(description="Show or hide 'Privacy Policy'")
    protected Boolean showPrivacyPolicy;
    @Schema(description="Show or hide 'Terms of Use'")
    protected Boolean showTermsOfUse;
    @Schema(description="Recipient to use for notifications when new user self-registered.")
    protected NotificationTargetId notificationRecipient;
    @Schema(description="Prefix to add to created customer")
    protected String customerTitlePrefix;
    @Schema(description="Id of the customer group the created customer will be added to.")
    protected EntityGroupId customerGroupId;
    @Schema(description="Group permissions to assign to the self-registered user's group.")
    @NotNull
    protected List<GroupPermission> permissions;
    @Schema(description="Custom menu id to assign to the created customer.")
    protected CustomMenuId customMenuId;
    @Schema(description="Default dashboard params")
    protected DefaultDashboardParams defaultDashboard;
    @Schema(description="Home dashboard params")
    protected HomeDashboardParams homeDashboard;
    @Schema(description="Privacy policy text. Supports HTML.")
    protected String privacyPolicy;
    @Schema(description="Terms of User text. Supports HTML.")
    protected String termsOfUse;

    @Override
    public SignUpSelfRegistrationParams toSignUpSelfRegistrationParams(PlatformType platformType) {
        // PE semantics: a null 'enabled' counts as enabled, matching validateSelfRegistrationAllowed.
        return this.enabled != Boolean.FALSE ? new SignUpSelfRegistrationParams(this.title, this.captcha.toInfo(platformType), this.signUpFields, this.showPrivacyPolicy, this.showTermsOfUse) : null;
    }
}
