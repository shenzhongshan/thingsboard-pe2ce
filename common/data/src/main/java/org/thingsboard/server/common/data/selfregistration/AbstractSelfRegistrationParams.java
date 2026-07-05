package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.oauth2.PlatformType;
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
    // CE adaptation: PE used EntityGroupId customerGroupId (group membership). CE is flat,
    // so the new self-registered user is added directly to this customer.
    @Schema(description="Id of the customer the new user will be added to.")
    protected CustomerId customerId;
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
        return this.enabled != null && this.enabled ? new SignUpSelfRegistrationParams(this.title, this.captcha.toInfo(platformType), this.signUpFields, this.showPrivacyPolicy, this.showTermsOfUse) : null;
    }
}
