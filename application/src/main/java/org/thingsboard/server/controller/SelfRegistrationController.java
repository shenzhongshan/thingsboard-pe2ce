package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.selfregistration.AbstractSelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@TbCoreComponent
@RequestMapping(value={"/api"})
public class SelfRegistrationController
extends BaseController {
    @Autowired
    private WhiteLabelingService whiteLabelingService;

    private static final String PRIVACY_POLICY = "privacyPolicy";
    private static final String TERMS_OF_USE = "termsOfUse";
    private static final String SELF_REGISTRATION_DESC = "Self Registration allows users to signup for using the platform and automatically create a Customer account for them. You may configure default dashboard and user roles that will be assigned for this Customer. This allows you to build out-of-the-box solutions for customers. Ability to white-label the login and main pages helps to brand the platform.";

    @ApiOperation(value="Create Or Update Self Registration parameters (saveSelfRegistrationParams)", notes="Creates or Updates the Self Registration parameters. When creating, platform generates Admin Settings Id as [time-based UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_1_(date-time_and_MAC_address)). The newly created Admin Settings Id will be present in the response. Specify existing Admin Settings Id to update the Self Registration parameters. Referencing non-existing Admin Settings Id will cause 'Not Found' error.\n\nSelf Registration allows users to signup for using the platform and automatically create a Customer account for them. You may configure default dashboard and user roles that will be assigned for this Customer. This allows you to build out-of-the-box solutions for customers. Ability to white-label the login and main pages helps to brand the platform.\n\nAvailable for users with 'TENANT_ADMIN' authority.\n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.")
    @PreAuthorize(value="hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping(value={"/selfRegistration/selfRegistrationParams"})
    public WebSelfRegistrationParams saveWebSelfRegistrationParams(@Parameter(description="A JSON value representing the Self Registration Parameters.", required=true) @RequestBody WebSelfRegistrationParams selfRegistrationParams) throws ThingsboardException {
        SecurityUser securityUser = this.getCurrentUser();
        Authority authority = securityUser.getAuthority();
        this.checkSelfRegistrationPermissions(Operation.WRITE);
        WebSelfRegistrationParams savedSelfRegistrationParams = null;
        if (selfRegistrationParams.getNotificationRecipient() != null) {
            this.checkNotificationTargetId(selfRegistrationParams.getNotificationRecipient(), Operation.READ);
        }
        if (Authority.TENANT_ADMIN.equals((Object)authority)) {
            JsonNode termsOfUseNode;
            savedSelfRegistrationParams = this.whiteLabelingService.saveTenantSelfRegistrationParams(this.getTenantId(), selfRegistrationParams);
            JsonNode privacyPolicyNode = this.whiteLabelingService.getTenantPrivacyPolicy(securityUser.getTenantId());
            if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                savedSelfRegistrationParams.setPrivacyPolicy(privacyPolicyNode.get(PRIVACY_POLICY).asText());
            }
            if ((termsOfUseNode = this.whiteLabelingService.getTenantTermsOfUse(securityUser.getTenantId())) != null && termsOfUseNode.has(TERMS_OF_USE)) {
                savedSelfRegistrationParams.setTermsOfUse(termsOfUseNode.get(TERMS_OF_USE).asText());
            }
        }
        return savedSelfRegistrationParams;
    }

    @ApiOperation(value="Get Self Registration parameters (getSelfRegistrationParams)", notes="Fetch the Self Registration parameters object for the tenant of the current user. \n\nAvailable for users with 'TENANT_ADMIN' authority.\n\nSecurity check is performed to verify that the user has 'READ' permission for the white labeling resource.")
    @PreAuthorize(value="hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value={"/selfRegistration/selfRegistrationParams"})
    public SelfRegistrationParams getWebSelfRegistrationParams() throws ThingsboardException {
        SecurityUser securityUser = this.getCurrentUser();
        this.checkSelfRegistrationPermissions(Operation.READ);
        WebSelfRegistrationParams selfRegistrationParams = null;
        if (Authority.TENANT_ADMIN.equals((Object)securityUser.getAuthority()) && (selfRegistrationParams = this.whiteLabelingService.getTenantSelfRegistrationParams(securityUser.getTenantId())) != null) {
            JsonNode termsOfUseNode;
            JsonNode privacyPolicyNode = this.whiteLabelingService.getTenantPrivacyPolicy(securityUser.getTenantId());
            if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
                selfRegistrationParams.setPrivacyPolicy(privacyPolicyNode.get(PRIVACY_POLICY).asText());
            }
            if ((termsOfUseNode = this.whiteLabelingService.getTenantTermsOfUse(securityUser.getTenantId())) != null && termsOfUseNode.has(TERMS_OF_USE)) {
                selfRegistrationParams.setTermsOfUse(termsOfUseNode.get(TERMS_OF_USE).asText());
            }
        }
        return selfRegistrationParams;
    }

    @PreAuthorize(value="hasAnyAuthority('TENANT_ADMIN')")
    @DeleteMapping(value={"/selfRegistration/selfRegistrationParams"})
    public void deleteWebSelfRegistrationParams() throws ThingsboardException {
        SecurityUser currentUser = this.getCurrentUser();
        this.checkSelfRegistrationPermissions(Operation.WRITE);
        this.whiteLabelingService.deleteWhiteLabeling(currentUser.getTenantId(), currentUser.getCustomerId(), WhiteLabelingType.SELF_REGISTRATION);
        this.whiteLabelingService.deleteWhiteLabeling(currentUser.getTenantId(), currentUser.getCustomerId(), WhiteLabelingType.PRIVACY_POLICY);
        this.whiteLabelingService.deleteWhiteLabeling(currentUser.getTenantId(), currentUser.getCustomerId(), WhiteLabelingType.TERMS_OF_USE);
    }

    @ApiOperation(value="Get Self Registration form parameters without authentication (getSignUpSelfRegistrationParams)", notes="Fetch the Self Registration parameters based on the domain name from the request. Available for non-authorized users. Contains the information to customize the sign-up form.")
    @GetMapping(value={"/noauth/selfRegistration/signUpSelfRegistrationParams"})
    public SignUpSelfRegistrationParams getSignUpSelfRegistrationParams(@RequestParam(required=false) String pkgName, @Parameter(description="Platform type", schema=@Schema(allowableValues={"ANDROID", "IOS"})) @RequestParam(required=false) PlatformType platformType, HttpServletRequest request) {
        // CE: mobile self-registration is not supported, web domain based only.
        if (!StringUtils.isEmpty((String)pkgName)) {
            return null;
        }
        AbstractSelfRegistrationParams selfRegistrationParams = this.whiteLabelingService.getWebSelfRegistrationParams(request.getServerName());
        return selfRegistrationParams != null ? selfRegistrationParams.toSignUpSelfRegistrationParams(platformType) : null;
    }

    @ApiOperation(value="Get Privacy Policy for Self Registration form (getPrivacyPolicy)", notes="Fetch the Privacy Policy based on the domain name from the request. Available for non-authorized users. ")
    @GetMapping(value={"/noauth/selfRegistration/privacyPolicy"})
    public String getPrivacyPolicy(@RequestParam(required=false) String pkgName, @RequestParam(required=false) PlatformType platform, HttpServletRequest request) {
        if (!StringUtils.isEmpty((String)pkgName)) {
            return "";
        }
        JsonNode privacyPolicyNode = this.whiteLabelingService.getWebPrivacyPolicy(request.getServerName());
        if (privacyPolicyNode != null && privacyPolicyNode.has(PRIVACY_POLICY)) {
            return privacyPolicyNode.get(PRIVACY_POLICY).toString();
        }
        return "";
    }

    @ApiOperation(value="Get Terms of Use for Self Registration form (getTermsOfUse)", notes="Fetch the Terms of Use based on the domain name from the request. Available for non-authorized users. ")
    @GetMapping(value={"/noauth/selfRegistration/termsOfUse"})
    public String getTermsOfUse(@RequestParam(required=false) String pkgName, @RequestParam(required=false) PlatformType platform, HttpServletRequest request) {
        if (!StringUtils.isEmpty((String)pkgName)) {
            return "";
        }
        JsonNode termsOfUse = this.whiteLabelingService.getWebTermsOfUse(request.getServerName());
        if (termsOfUse != null && termsOfUse.has(TERMS_OF_USE)) {
            return termsOfUse.get(TERMS_OF_USE).toString();
        }
        return "";
    }

    private void checkSelfRegistrationPermissions(Operation operation) throws ThingsboardException {
        this.accessControlService.checkPermission(this.getCurrentUser(), Resource.WHITE_LABELING, operation);
    }
}

