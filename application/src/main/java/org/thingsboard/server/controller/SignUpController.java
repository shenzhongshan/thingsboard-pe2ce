package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.selfregistration.AbstractCaptchaParams;
import org.thingsboard.server.common.data.selfregistration.CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.LocalCaptchaParams;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpField;
import org.thingsboard.server.common.data.selfregistration.SignUpFieldId;
import org.thingsboard.server.common.data.selfregistration.V2CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.V3CaptchaParams;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;
import org.thingsboard.server.common.data.signup.SignUpRequest;
import org.thingsboard.server.common.data.signup.SignUpResult;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.config.SignUpConfig;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.data.RecaptchaValidationResult;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.captcha.LocalCaptchaService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.utils.MiscUtils;

/**
 * CE port of the PE self-registration sign-up flow, at PE parity for the web path: entity group
 * provisioning, group permissions, custom menu assignment and admin notifications are all wired.
 * <p>
 * Deliberately not ported: the PE mobile sign-up path (mobile app bundle self-registration params,
 * app secret validation, deep-link redirects) and reCaptcha Enterprise - the latter only ever
 * applies to ANDROID/IOS platforms in PE, so it is bound to the mobile path.
 */
@RestController
@TbCoreComponent
@RequestMapping(value={"/api"})
@Slf4j
public class SignUpController
extends BaseController {
    private static final String SELF_REGISTRATION_SETTINGS_WAS_NOT_FOUND = "Self registration settings was not found";
    private static final String SELF_REGISTRATION_SETTINGS_IS_NOT_ALLOWED = "Self registration is not allowed";
    private static final String WL_SETTINGS_WAS_NOT_FOUND = "White labeling settings was not found";
    private static final String PRIVACY_POLICY_ACCEPTED = "privacyPolicyAccepted";
    private static final String TERMS_OF_USE_ACCEPTED = "termsOfUseAccepted";

    private RestTemplate restTemplate;
    @Autowired
    private SignUpConfig signUpConfig;
    @Autowired
    private MailService mailService;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenFactory tokenFactory;
    @Autowired
    private SystemSecurityService systemSecurityService;
    @Autowired
    private WhiteLabelingService whiteLabelingService;
    @Autowired
    private EntityGroupService entityGroupService;
    @Autowired
    private GroupPermissionService groupPermissionService;
    @Autowired
    private NotificationCenter notificationCenter;
    @Autowired
    private LocalCaptchaService localCaptchaService;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
    }

    @ApiOperation(value="User Sign Up (signUp)", notes="Process user sign up request. Creates the Customer and corresponding User based on self Registration parameters for the domain. The result is either 'SUCCESS' or 'INACTIVE_USER_EXISTS'. If Success, the user will receive an email with instruction to activate the account.")
    @PostMapping(value={"/noauth/signup"})
    public SignUpResult signUp(@Parameter(description="A JSON value representing the signup request.", required=true) @RequestBody SignUpRequest signUpRequest, HttpServletRequest request) throws ThingsboardException {
        WhiteLabeling whiteLabeling = this.checkWebSRSettings(request.getServerName());
        SelfRegistrationParams selfRegistrationParams = JacksonUtil.treeToValue(whiteLabeling.getSettings(), WebSelfRegistrationParams.class);
        TenantId tenantId = whiteLabeling.getTenantId();
        this.validateSelfRegistrationAllowed(selfRegistrationParams);
        this.validateRequiredFields(signUpRequest, selfRegistrationParams);
        String email = signUpRequest.getFields().get(SignUpFieldId.EMAIL);
        String password = signUpRequest.getFields().get(SignUpFieldId.PASSWORD);
        CaptchaParams captcha = selfRegistrationParams.getCaptcha();
        if (captcha instanceof V2CaptchaParams || captcha instanceof V3CaptchaParams) {
            this.validateReCaptcha(signUpRequest.getRecaptchaResponse(), request.getRemoteAddr(), ((AbstractCaptchaParams)captcha).getSecretKey());
        } else if (captcha instanceof LocalCaptchaParams) {
            this.validateLocalCaptcha(signUpRequest.getCaptchaId(), signUpRequest.getCaptchaAnswer());
        } else {
            throw new DataValidationException("Error validating captcha: wrong captcha version");
        }
        DataValidator.validateEmail(email);
        User existingUser = this.userService.findUserByEmail(tenantId, email);
        if (existingUser != null) {
            UserCredentials credentials = this.userService.findUserCredentialsByUserId(tenantId, existingUser.getId());
            if (credentials.isEnabled()) {
                throw new DataValidationException("User with email '" + existingUser.getEmail() + "'  is already registered!");
            }
            return SignUpResult.INACTIVE_USER_EXISTS;
        }
        this.systemSecurityService.validatePassword(password, null);
        String firstName = signUpRequest.getFields().get(SignUpFieldId.FIRST_NAME);
        String lastName = signUpRequest.getFields().get(SignUpFieldId.LAST_NAME);

        Customer savedCustomer = this.createCustomer(signUpRequest, selfRegistrationParams, tenantId);
        EntityGroup savedUsersGroup = this.createCustomerUserGroup(selfRegistrationParams, savedCustomer);
        User savedUser = this.createUser(signUpRequest, selfRegistrationParams, savedCustomer);
        this.entityGroupService.addEntityToEntityGroup(tenantId, savedUsersGroup.getId(), savedUser.getId());
        this.userPermissionsService.onUserGroupMembershipChanged(tenantId, savedUsersGroup.getId());
        UserCredentials savedUserCredentials = this.saveUserCredentials(savedUser, signUpRequest);
        try {
            this.sendEmailVerification(tenantId, request, savedUserCredentials, email);
        } catch (ThingsboardException e) {
            // deleteCustomer cascades to its users, entity groups and their group permissions
            this.customerService.deleteCustomer(tenantId, savedCustomer.getId());
            throw e;
        }
        this.sendUserActivityNotification(tenantId, NotificationType.USER_REGISTERED,
                Optional.ofNullable(firstName).orElse("") + " " + Optional.ofNullable(lastName).orElse(""),
                email, selfRegistrationParams.getNotificationRecipient());

        this.logEntityActionService.logEntityAction(tenantId, savedCustomer.getId(), savedCustomer, savedCustomer.getId(), ActionType.ADDED, null);
        this.logEntityActionService.logEntityAction(tenantId, savedUser.getId(), savedUser, savedUser.getCustomerId(), ActionType.ADDED, null);
        this.logEntityActionService.logEntityAction(tenantId, savedUser.getId(), savedUser, savedCustomer.getId(),
                ActionType.ADDED_TO_ENTITY_GROUP, (User) null, savedUsersGroup.getId().toString(), savedUsersGroup.getName());
        return SignUpResult.SUCCESS;
    }

    /**
     * PE parity: provisions the "Self Registration Users" group under the freshly created customer
     * and persists the configured {@link GroupPermission}s against it.
     * <p>
     * Deliberate CE divergence: {@code UserServiceImpl.saveUser} additionally places every new user
     * in the owner's "All" and "Customer Users" groups (bound to the default Customer User role),
     * which PE does not do. The memberships are independent relation rows and permissions merge as a
     * union, so the configured roles here add to that baseline rather than replacing it. The upside
     * is that an empty {@code permissions} list still yields a usable account, whereas PE-exact
     * behaviour would leave the user with no permissions at all.
     */
    private EntityGroup createCustomerUserGroup(SelfRegistrationParams selfRegistrationParams, Customer savedCustomer) {
        TenantId tenantId = savedCustomer.getTenantId();
        EntityGroup usersEntityGroup = this.entityGroupService.findOrCreateUserGroup(tenantId, savedCustomer.getId(),
                "Self Registration Users", "Autogenerated Self Registration group");
        List<GroupPermission> permissions = selfRegistrationParams.getPermissions();
        if (permissions == null) {
            return usersEntityGroup;
        }
        for (GroupPermission permission : permissions) {
            // the configured entries are templates stored inside the settings blob; never reuse an id
            permission.setId(null);
            permission.setTenantId(tenantId);
            permission.setUserGroupId(usersEntityGroup.getId());
            if (permission.getEntityGroupId() != null) {
                EntityGroup entityGroup = this.entityGroupService.findEntityGroupById(tenantId, permission.getEntityGroupId());
                if (entityGroup == null) {
                    continue; // PE: silently skip permissions pointing at a deleted group
                }
                permission.setEntityGroupType(entityGroup.getType());
            }
            this.groupPermissionService.saveGroupPermission(tenantId, permission);
        }
        return usersEntityGroup;
    }

    /**
     * Notifies the configured notification target that a user registered / activated.
     * Never fails the sign-up: a missing notification template must not roll back the account.
     */
    private void sendUserActivityNotification(TenantId tenantId, NotificationType notificationType,
                                              String userFullName, String userEmail, NotificationTargetId recipient) {
        if (recipient == null) {
            return;
        }
        try {
            NotificationInfo info;
            if (notificationType == NotificationType.USER_ACTIVATED) {
                info = NotificationInfo.userActivated(userFullName, userEmail);
            } else if (notificationType == NotificationType.USER_REGISTERED) {
                info = NotificationInfo.userRegistered(userFullName, userEmail);
            } else {
                return;
            }
            this.notificationCenter.sendSystemNotification(tenantId, recipient, notificationType, info);
        } catch (Exception e) {
            log.error("Failed to send {} notification about user {}", notificationType, userEmail, e);
        }
    }

    /**
     * Deliberately uses the raw request host rather than {@code systemSecurityService.getBaseUrl}:
     * the activation link must point at the self-registration domain the user actually visited,
     * which is exactly what login white labeling's {@code prohibitDifferentUrl} would override.
     * PE does the same. The tenant is only used to resolve the mail template and sender.
     */
    private void sendEmailVerification(TenantId tenantId, HttpServletRequest request, UserCredentials userCredentials, String targetEmail) throws ThingsboardException {
        String baseUrl = MiscUtils.constructBaseUrl(request);
        String activationLink = String.format("%s/api/noauth/activateEmail?emailCode=%s", baseUrl, userCredentials.getActivateToken());
        try {
            this.mailService.sendActivationEmail(tenantId, activationLink, userCredentials.getActivationTokenTtl(), targetEmail);
        } catch (Exception e) {
            throw new ThingsboardException("Temporarily unable to send activation email", ThingsboardErrorCode.GENERAL);
        }
    }

    @ApiOperation(value="Resend Activation Email (resendEmailActivation)", notes="Request to resend the activation email for the user. Checks that user was not activated yet.")
    @PostMapping(value={"/noauth/resendEmailActivation"})
    public void resendEmailActivation(@Parameter(description="Email of the user.", required=true, example="john.doe@company.com") @RequestParam(value="email") String email, HttpServletRequest request) throws ThingsboardException {
        WhiteLabeling whiteLabeling = this.checkWebSRSettings(request.getServerName());
        TenantId tenantId = whiteLabeling.getTenantId();
        User existingUser = this.userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);
        UserCredentials credentials;
        if (existingUser != null) {
            credentials = this.userService.findUserCredentialsByUserId(existingUser.getTenantId(), existingUser.getId());
            if (credentials.isEnabled()) {
                throw new DataValidationException("User with email '" + existingUser.getEmail() + "'  is already active!");
            }
        } else {
            throw new DataValidationException("User with email '" + email + "'  is not registered!");
        }
        credentials = this.userService.checkUserActivationToken(tenantId, credentials);
        this.sendEmailVerification(tenantId, request, credentials, email);
    }

    @ApiOperation(value="Activate User using code from Email (activateEmail)", notes="Activate the user using code(link) from the activation email. Validates the code and redirects according to the signup flow.")
    @GetMapping(value={"/noauth/activateEmail"}, params={"emailCode"})
    public ResponseEntity<String> activateEmail(@Parameter(description="Activation token.", required=true) @RequestParam(value="emailCode") String emailCode, HttpServletRequest request) {
        HttpStatus responseStatus;
        HttpHeaders headers = new HttpHeaders();
        UserCredentials userCredentials = this.userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, emailCode);
        if (userCredentials != null) {
            String emailVerifiedURI = "/signup/emailVerified";
            try {
                URI location = new URI(emailVerifiedURI + "?emailCode=" + emailCode);
                headers.setLocation(location);
                responseStatus = HttpStatus.PERMANENT_REDIRECT;
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", emailVerifiedURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, (HttpStatusCode) responseStatus);
    }

    @ApiOperation(value="Activate and login using code from Email (activateUserByEmailCode)", notes="Activate the user using code(link) from the activation email and return the JWT Token.")
    @PostMapping(value={"/noauth/activateByEmailCode"})
    public JwtPair activateUserByEmailCode(@Parameter(description="Activation token.", required=true) @RequestParam(value="emailCode") String emailCode, HttpServletRequest request) throws ThingsboardException {
        WhiteLabeling whiteLabeling = this.checkWebSRSettings(request.getServerName());
        TenantId tenantId = whiteLabeling.getTenantId();
        UserCredentials userCredentials = this.userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, emailCode);
        if (userCredentials == null) {
            throw new ThingsboardException("Invalid email code!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (userCredentials.getPassword() == null) {
            throw new ThingsboardException("Unable to activate user!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        String encodedPassword = userCredentials.getPassword();
        UserCredentials credentials = this.userService.activateUserCredentials(TenantId.SYS_TENANT_ID, emailCode, encodedPassword);
        User user = this.userService.findUserById(TenantId.SYS_TENANT_ID, credentials.getUserId());
        this.setPrivacyPolicyAccepted(user);
        this.setTermsOfUseAccepted(user);
        user = this.userService.saveUser(tenantId, user);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal, this.getMergedUserPermissions(user, false));
        String baseUrl = MiscUtils.constructBaseUrl(request);
        String loginUrl = String.format("%s/login", baseUrl);
        String email = user.getEmail();
        try {
            this.mailService.sendAccountActivatedEmail(tenantId, loginUrl, email);
        } catch (Exception e) {
            log.warn("Unable to send account activated email for {}: {}", email, e.getMessage());
        }
        SelfRegistrationParams selfRegistrationParams = JacksonUtil.treeToValue(whiteLabeling.getSettings(), WebSelfRegistrationParams.class);
        this.sendUserActivityNotification(tenantId, NotificationType.USER_ACTIVATED,
                Optional.ofNullable(user.getFirstName()).orElse("") + " " + Optional.ofNullable(user.getLastName()).orElse(""),
                email, selfRegistrationParams.getNotificationRecipient());
        this.systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(request), ActionType.LOGIN, null);
        return this.tokenFactory.createTokenPair(securityUser);
    }

    private void setPrivacyPolicyAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(PRIVACY_POLICY_ACCEPTED, true);
        user.setAdditionalInfo(additionalInfo);
    }

    private boolean isPrivacyPolicyAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo != null && additionalInfo.has(PRIVACY_POLICY_ACCEPTED)) {
            return additionalInfo.get(PRIVACY_POLICY_ACCEPTED).asBoolean();
        }
        return false;
    }

    @ApiOperation(value="Check privacy policy (privacyPolicyAccepted)", notes="Checks that current user accepted the privacy policy.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value={"/signup/privacyPolicyAccepted"})
    @ResponseBody
    public Boolean privacyPolicyAccepted() throws ThingsboardException {
        SecurityUser securityUser = this.getCurrentUser();
        User user = this.userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        return this.isPrivacyPolicyAccepted(user);
    }

    @ApiOperation(value="Accept privacy policy (acceptPrivacyPolicy)", notes="Accept privacy policy by the current user.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value={"/signup/acceptPrivacyPolicy"})
    public JsonNode acceptPrivacyPolicy() throws ThingsboardException {
        SecurityUser securityUser = this.getCurrentUser();
        User user = this.userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        this.setPrivacyPolicyAccepted(user);
        user = this.userService.saveUser(securityUser.getTenantId(), user);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        securityUser = new SecurityUser(user, true, principal, this.getMergedUserPermissions(user, false));
        return this.buildTokenObject(securityUser);
    }

    private void setTermsOfUseAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(TERMS_OF_USE_ACCEPTED, true);
        user.setAdditionalInfo(additionalInfo);
    }

    private boolean isTermsOfUseAccepted(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (additionalInfo != null && additionalInfo.has(TERMS_OF_USE_ACCEPTED)) {
            return additionalInfo.get(TERMS_OF_USE_ACCEPTED).asBoolean();
        }
        return false;
    }

    @ApiOperation(value="Check Terms Of Use (termsOfUseAccepted)", notes="Checks that current user accepted the terms of use.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value={"/signup/termsOfUseAccepted"})
    @ResponseBody
    public Boolean termsOfUseAccepted() throws ThingsboardException {
        SecurityUser securityUser = this.getCurrentUser();
        User user = this.userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        return this.isTermsOfUseAccepted(user);
    }

    @ApiOperation(value="Accept Terms of Use (acceptTermsOfUse)", notes="Accept Terms of Use by the current user.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value={"/signup/acceptTermsOfUse"})
    public JsonNode acceptTermsOfUse() throws ThingsboardException {
        SecurityUser securityUser = this.getCurrentUser();
        User user = this.userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        this.setTermsOfUseAccepted(user);
        user = this.userService.saveUser(securityUser.getTenantId(), user);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        securityUser = new SecurityUser(user, true, principal, this.getMergedUserPermissions(user, false));
        return this.buildTokenObject(securityUser);
    }

    private JsonNode buildTokenObject(SecurityUser securityUser) throws ThingsboardException {
        JwtPair tokenPair = this.tokenFactory.createTokenPair(securityUser);
        ObjectNode tokenObject = JacksonUtil.newObjectNode();
        tokenObject.put("token", tokenPair.getToken());
        tokenObject.put("refreshToken", tokenPair.getRefreshToken());
        return tokenObject;
    }

    private void validateReCaptcha(String userResponse, String ipAddress, String recaptchaSecretKey) throws ThingsboardException {
        checkParameter("Recaptcha response", userResponse);
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("secret", recaptchaSecretKey);
        parameters.add("response", userResponse);
        parameters.add("remoteip", ipAddress);
        try {
            RecaptchaValidationResult result = this.restTemplate.postForEntity(this.signUpConfig.getRecaptchaVerificationUrl(), parameters, RecaptchaValidationResult.class).getBody();
            if (result == null || result.isFailure()) {
                log.error("reCAPTCHA validation failed: {}", result);
                throw new DataValidationException("Invalid reCaptcha response!");
            }
        } catch (RestClientException ex) {
            log.error("Error validating reCAPTCHA. Verification URL: [{}]", this.signUpConfig.getRecaptchaVerificationUrl());
            throw new ThingsboardException("Error validating reCAPTCHA", ex, ThingsboardErrorCode.GENERAL);
        }
    }

    private void validateLocalCaptcha(String captchaId, String captchaAnswer) throws ThingsboardException {
        checkParameter("Captcha id", captchaId);
        checkParameter("Captcha answer", captchaAnswer);
        if (!this.localCaptchaService.verifyAndEvict(captchaId, captchaAnswer)) {
            throw new DataValidationException("Invalid captcha response!");
        }
    }

    private void validateRequiredFields(SignUpRequest signUpRequest, SelfRegistrationParams selfRegistrationParams) throws ThingsboardException {
        Map<SignUpFieldId, String> signUpRequestFields = signUpRequest.getFields();
        List<SignUpField> selfRegisterSignUpFields = selfRegistrationParams.getSignUpFields();
        if (selfRegisterSignUpFields != null) {
            for (SignUpField field : selfRegisterSignUpFields) {
                if (!field.isRequired() || !field.getId().isValidate()) continue;
                this.checkNotNull(signUpRequestFields.get(field.getId()));
            }
        } else {
            this.checkNotNull(signUpRequestFields.get(SignUpFieldId.EMAIL));
            this.checkNotNull(signUpRequestFields.get(SignUpFieldId.PASSWORD));
            this.checkNotNull(signUpRequestFields.get(SignUpFieldId.FIRST_NAME));
            this.checkNotNull(signUpRequestFields.get(SignUpFieldId.LAST_NAME));
        }
    }

    private User createUser(SignUpRequest signUpRequest, SelfRegistrationParams selfRegistrationParams, Customer savedCustomer) throws ThingsboardException {
        User user = new User();
        Map<SignUpFieldId, String> signUpRequestFields = signUpRequest.getFields();
        user.setFirstName(signUpRequestFields.get(SignUpFieldId.FIRST_NAME));
        user.setLastName(signUpRequestFields.get(SignUpFieldId.LAST_NAME));
        user.setEmail(signUpRequestFields.get(SignUpFieldId.EMAIL));
        user.setPhone(signUpRequestFields.get(SignUpFieldId.PHONE));
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(savedCustomer.getTenantId());
        user.setCustomerId(savedCustomer.getId());
        ObjectNode objectNode = JacksonUtil.newObjectNode();
        objectNode.put("lang", "en_US");
        Optional.ofNullable(selfRegistrationParams.getDefaultDashboard()).ifPresent(dashboardParams -> {
            objectNode.put("defaultDashboardId", dashboardParams.getId());
            objectNode.put("defaultDashboardFullscreen", dashboardParams.isFullscreen());
        });
        Optional.ofNullable(selfRegistrationParams.getHomeDashboard()).ifPresent(homeDashboardParams -> {
            objectNode.put("homeDashboardId", homeDashboardParams.getId());
            objectNode.put("homeDashboardHideToolbar", homeDashboardParams.isHideToolbar());
        });
        user.setAdditionalInfo(objectNode);
        return this.checkNotNull(this.userService.saveUser(savedCustomer.getTenantId(), user));
    }

    private Customer createCustomer(SignUpRequest signUpRequest, SelfRegistrationParams selfRegistrationParams, TenantId tenantId) throws ThingsboardException {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        String customerTitlePrefix = Optional.ofNullable(selfRegistrationParams.getCustomerTitlePrefix()).orElse("");
        Map<SignUpFieldId, String> signUpRequestFields = signUpRequest.getFields();
        customer.setTitle(customerTitlePrefix + signUpRequestFields.get(SignUpFieldId.EMAIL));
        customer.setEmail(signUpRequestFields.get(SignUpFieldId.EMAIL));
        customer.setCountry(signUpRequestFields.get(SignUpFieldId.COUNTRY));
        customer.setState(signUpRequestFields.get(SignUpFieldId.STATE));
        customer.setCity(signUpRequestFields.get(SignUpFieldId.CITY));
        customer.setAddress(signUpRequestFields.get(SignUpFieldId.ADDRESS));
        customer.setAddress2(signUpRequestFields.get(SignUpFieldId.ADDRESS2));
        customer.setZip(signUpRequestFields.get(SignUpFieldId.ZIP));
        customer.setPhone(signUpRequestFields.get(SignUpFieldId.PHONE));
        customer.setCustomMenuId(selfRegistrationParams.getCustomMenuId());
        Customer savedCustomer = this.checkNotNull(this.customerService.saveCustomer(customer));
        if (selfRegistrationParams.getCustomerGroupId() != null) {
            this.entityGroupService.addEntityToEntityGroup(tenantId, selfRegistrationParams.getCustomerGroupId(), savedCustomer.getId());
        }
        return savedCustomer;
    }

    private UserCredentials saveUserCredentials(User savedUser, SignUpRequest signUpRequest) {
        UserCredentials userCredentials = this.userService.findUserCredentialsByUserId(savedUser.getTenantId(), savedUser.getId());
        userCredentials.setPassword(this.passwordEncoder.encode(signUpRequest.getFields().get(SignUpFieldId.PASSWORD)));
        this.userService.saveUserCredentials(savedUser.getTenantId(), userCredentials);
        return userCredentials;
    }

    private WhiteLabeling checkWebSRSettings(String domain) throws ThingsboardException {
        WhiteLabeling whiteLabeling = this.whiteLabelingService.findWhiteLabelingByDomainAndType(domain, WhiteLabelingType.SELF_REGISTRATION);
        this.checkNotNull(whiteLabeling, WL_SETTINGS_WAS_NOT_FOUND);
        this.checkNotNull(whiteLabeling.getSettings(), SELF_REGISTRATION_SETTINGS_WAS_NOT_FOUND);
        return whiteLabeling;
    }

    private void validateSelfRegistrationAllowed(SelfRegistrationParams selfRegistrationParams) {
        if (selfRegistrationParams.getEnabled() != null && !selfRegistrationParams.getEnabled()) {
            throw new DataValidationException(SELF_REGISTRATION_SETTINGS_IS_NOT_ALLOWED);
        }
    }
}
