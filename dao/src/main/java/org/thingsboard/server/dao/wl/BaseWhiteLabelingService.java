package org.thingsboard.server.dao.wl;

import com.fasterxml.jackson.databind.JsonNode;
import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.net.URI;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.resource.ImageCacheKey;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.wl.WhiteLabelingCacheKey;
import org.thingsboard.server.dao.wl.WhiteLabelingDao;
import org.thingsboard.server.dao.wl.WhiteLabelingEvictEvent;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.exception.DataValidationException;

@Service
public class BaseWhiteLabelingService
extends AbstractCachedService<WhiteLabelingCacheKey, WhiteLabeling, WhiteLabelingEvictEvent>
implements WhiteLabelingService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(BaseWhiteLabelingService.class);
    private static final String ALLOW_WHITE_LABELING = "allowWhiteLabeling";
    private static final String ALLOW_CUSTOMER_WHITE_LABELING = "allowCustomerWhiteLabeling";
    private final AdminSettingsService adminSettingsService;
    private final DomainService domainService;
    private final WhiteLabelingDao whiteLabelingDao;
    private final TenantService tenantService;
    private final CustomerService customerService;
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public LoginWhiteLabelingParams getSystemLoginWhiteLabelingParams() {
        WhiteLabeling whiteLabeling = this.findByEntityId(TenantId.SYS_TENANT_ID, null, WhiteLabelingType.LOGIN);
        return this.constructLoginWlParams(whiteLabeling);
    }

    @Override
    public WhiteLabelingParams getSystemWhiteLabelingParams() {
        WhiteLabeling whiteLabeling = this.findByEntityId(TenantId.SYS_TENANT_ID, null, WhiteLabelingType.GENERAL);
        return this.constructWlParams(whiteLabeling != null ? whiteLabeling.getSettings() : null, true);
    }

    @Override
    public WhiteLabelingParams getTenantWhiteLabelingParams(TenantId tenantId) {
        return this.getEntityWhiteLabelParams(tenantId, null);
    }

    @Override
    public WhiteLabelingParams getCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId) {
        return this.getEntityWhiteLabelParams(tenantId, customerId);
    }

    @Override
    public LoginWhiteLabelingParams getTenantLoginWhiteLabelingParams(TenantId tenantId) {
        return this.getEntityLoginWhiteLabelParams(tenantId, null);
    }

    @Override
    public LoginWhiteLabelingParams getCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId) {
        return this.getEntityLoginWhiteLabelParams(tenantId, customerId);
    }

    @Override
    public LoginWhiteLabelingParams getMergedLoginWhiteLabelingParams(String domainName) throws Exception {
        LoginWhiteLabelingParams result;
        WhiteLabeling existingLoginWLSettings;
        if (this.isUsedOnSystemLevel(domainName) && (existingLoginWLSettings = this.whiteLabelingDao.findByDomainAndType(TenantId.SYS_TENANT_ID, domainName, WhiteLabelingType.LOGIN)) != null) {
            TenantId tenantId = existingLoginWLSettings.getTenantId();
            CustomerId customerId = existingLoginWLSettings.getCustomerId();
            result = this.getEntityLoginWhiteLabelParams(tenantId, customerId);
            if (customerId != null && !customerId.isNullUid()) {
                // CE adaptation: flat customers, no sub-customer hierarchy to walk.
                result.merge(this.getTenantLoginWhiteLabelingParams(tenantId));
            }
            result.merge(this.getSystemLoginWhiteLabelingParams());
        } else {
            result = this.getSystemLoginWhiteLabelingParams();
        }
        return result;
    }

    @Override
    public ImageCacheKey getLoginImageKey(String domainName, boolean faviconElseLogo) throws Exception {
        LoginWhiteLabelingParams result;
        WhiteLabeling existingLoginWLSettings;
        TenantId tenantId = null;
        if (this.isUsedOnSystemLevel(domainName) && (existingLoginWLSettings = this.whiteLabelingDao.findByDomainAndType(TenantId.SYS_TENANT_ID, domainName, WhiteLabelingType.LOGIN)) != null) {
            tenantId = existingLoginWLSettings.getTenantId();
            CustomerId customerId = existingLoginWLSettings.getCustomerId();
            result = this.getEntityLoginWhiteLabelParams(tenantId, customerId);
            if (customerId != null && !customerId.isNullUid()) {
                // CE adaptation: flat customers, no sub-customer hierarchy to walk.
                result.merge(this.getTenantLoginWhiteLabelingParams(tenantId));
            }
            result.merge(this.getSystemLoginWhiteLabelingParams());
        } else {
            result = this.getSystemLoginWhiteLabelingParams();
        }
        String url = faviconElseLogo ? (result.getFavicon() != null ? result.getFavicon().getUrl() : null) : result.getLogoImageUrl();
        if (StringUtils.isNotBlank((String)url)) {
            String[] parts;
            TenantId imageTenantId = null;
            if (url.startsWith("tb-image;/api/images/tenant/")) {
                imageTenantId = tenantId;
            } else if (url.startsWith("tb-image;/api/images/system/")) {
                imageTenantId = TenantId.SYS_TENANT_ID;
            }
            if (imageTenantId != null && (parts = url.split("/")).length == 5) {
                String key = parts[4];
                return ImageCacheKey.forImage(imageTenantId, key);
            }
        }
        return null;
    }

    @Override
    public WhiteLabelingParams getMergedTenantWhiteLabelingParams(TenantId tenantId) {
        WhiteLabelingParams result = this.getTenantWhiteLabelingParams(tenantId);
        return this.mergeTenantWhiteLabelingParams(result);
    }

    @Override
    public WhiteLabelingParams getMergedCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId) {
        WhiteLabelingParams result = this.getCustomerWhiteLabelingParams(tenantId, customerId);
        return this.mergeCustomerWhiteLabelingParams(tenantId, customerId, result);
    }

    @Override
    public WhiteLabelingParams saveSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        this.saveWhiteLabelParams(TenantId.SYS_TENANT_ID, null, whiteLabelingParams);
        return this.getSystemWhiteLabelingParams();
    }

    @Override
    public LoginWhiteLabelingParams saveSystemLoginWhiteLabelingParams(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        if (loginWhiteLabelingParams.getDomainId() != null) {
            throw new DataValidationException("Domain is prohibited for system level");
        }
        if (loginWhiteLabelingParams.getBaseUrl() != null && !isValidUrl(loginWhiteLabelingParams.getBaseUrl())) {
            throw new IncorrectParameterException("Base url " + loginWhiteLabelingParams.getBaseUrl() + " is invalid");
        }
        this.saveLoginWhiteLabelParams(TenantId.SYS_TENANT_ID, null, loginWhiteLabelingParams);
        return this.getSystemLoginWhiteLabelingParams();
    }

    @Override
    public LoginWhiteLabelingParams saveTenantLoginWhiteLabelingParams(TenantId tenantId, LoginWhiteLabelingParams loginWhiteLabelingParams) {
        this.saveEntityLoginWhiteLabelingParams(tenantId, null, loginWhiteLabelingParams);
        return this.getTenantLoginWhiteLabelingParams(tenantId);
    }

    @Override
    public LoginWhiteLabelingParams saveCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams loginWhiteLabelingParams) {
        this.saveEntityLoginWhiteLabelingParams(tenantId, customerId, loginWhiteLabelingParams);
        return this.getCustomerLoginWhiteLabelingParams(tenantId, customerId);
    }

    private boolean isUsedOnSystemLevel(String domainName) {
        try {
            LoginWhiteLabelingParams systemParams = this.getSystemLoginWhiteLabelingParams();
            if (systemParams != null && this.isBaseUrlMatchesDomain(systemParams.getBaseUrl(), domainName)) {
                return false;
            }
            AdminSettings generalSettings = this.adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "general");
            String baseUrl = generalSettings.getJsonValue().get("baseUrl").asText();
            return !this.isBaseUrlMatchesDomain(baseUrl, domainName);
        }
        catch (Exception e) {
            log.warn("Failed to validate domain.", (Throwable)e);
            return false;
        }
    }

    private boolean isBaseUrlMatchesDomain(String baseUrl, String domainName) {
        String baseUrlDomainName = this.domainNameFromBaseUrl(baseUrl);
        return baseUrlDomainName != null && baseUrlDomainName.equalsIgnoreCase(domainName);
    }

    private String domainNameFromBaseUrl(String baseUrl) {
        if (StringUtils.isNotBlank((String)baseUrl)) {
            try {
                return URI.create(baseUrl).getHost();
            }
            catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private void saveEntityLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams loginWhiteLabelParams) {
        if (loginWhiteLabelParams.getDomainId() == null) {
            throw new IncorrectParameterException("Domain id could not be empty");
        }
        Domain domain = this.domainService.findDomainById(tenantId, loginWhiteLabelParams.getDomainId());
        if (!this.isUsedOnSystemLevel(domain.getName())) {
            throw new IncorrectParameterException("Current domain name " + domain.getName() + " already used in the system level");
        }
        if (loginWhiteLabelParams.getBaseUrl() != null && !isValidUrl(loginWhiteLabelParams.getBaseUrl())) {
            throw new IncorrectParameterException("Base url " + loginWhiteLabelParams.getBaseUrl() + " is invalid");
        }
        this.saveLoginWhiteLabelParams(tenantId, customerId, loginWhiteLabelParams);
    }

    @Override
    public WhiteLabelingParams saveTenantWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams) {
        this.saveWhiteLabelParams(tenantId, null, whiteLabelingParams);
        return this.getTenantWhiteLabelingParams(tenantId);
    }

    @Override
    public WhiteLabelingParams saveCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams) {
        this.saveWhiteLabelParams(tenantId, customerId, whiteLabelingParams);
        return this.getCustomerWhiteLabelingParams(tenantId, customerId);
    }

    @Override
    public WhiteLabelingParams mergeSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams;
    }

    @Override
    public WhiteLabelingParams mergeTenantWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams) {
        return whiteLabelingParams.merge(this.getSystemWhiteLabelingParams());
    }

    @Override
    public WhiteLabelingParams mergeCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams) {
        try {
            // CE adaptation: flat customers, so merge directly customer -> tenant -> system.
            WhiteLabelingParams tenantWhiteLabelingParams = this.getTenantWhiteLabelingParams(tenantId);
            whiteLabelingParams.setHideConnectivityDialog(tenantWhiteLabelingParams.getHideConnectivityDialog());
            return whiteLabelingParams.merge(tenantWhiteLabelingParams).merge(this.getSystemWhiteLabelingParams());
        }
        catch (Exception e) {
            log.error("Unable to merge Customer White Labeling Params!", (Throwable)e);
            throw new RuntimeException("Unable to merge Customer White Labeling Params!", e);
        }
    }

    @Override
    public void deleteAllTenantWhiteLabeling(TenantId tenantId) {
        for (WhiteLabelingType type : WhiteLabelingType.values()) {
            this.deleteWhiteLabeling(tenantId, null, type);
        }
    }

    private WhiteLabelingParams constructWlParams(JsonNode json, boolean isSystem) {
        WhiteLabelingParams result = null;
        if (json != null) {
            try {
                result = (WhiteLabelingParams)JacksonUtil.treeToValue((JsonNode)json, WhiteLabelingParams.class);
                if (isSystem) {
                    if (!json.has("helpLinkBaseUrl")) {
                        result.setHelpLinkBaseUrl(DataConstants.ALSUN_BASE_URL);
                    }
                    if (!json.has("uiHelpBaseUrl")) {
                        result.setUiHelpBaseUrl(null);
                    }
                    if (!json.has("enableHelpLinks")) {
                        result.setEnableHelpLinks(Boolean.valueOf(true));
                    }
                }
            }
            catch (IllegalArgumentException e) {
                log.error("Unable to read White Labeling Params from JSON!", (Throwable)e);
                throw new IncorrectParameterException("Unable to read White Labeling Params from JSON!");
            }
        }
        if (result == null) {
            result = new WhiteLabelingParams();
            if (isSystem) {
                result.setHelpLinkBaseUrl(DataConstants.ALSUN_BASE_URL);
                result.setUiHelpBaseUrl(null);
                result.setEnableHelpLinks(Boolean.valueOf(true));
            }
        }
        return result;
    }

    private WhiteLabelingParams getEntityWhiteLabelParams(TenantId tenantId, CustomerId customerId) {
        WhiteLabeling whiteLabeling;
        JsonNode jsonNode = null;
        if (this.isWhiteLabelingAllowed(tenantId, customerId) && (whiteLabeling = this.findByEntityId(tenantId, customerId, WhiteLabelingType.GENERAL)) != null) {
            jsonNode = whiteLabeling.getSettings();
        }
        return this.constructWlParams(jsonNode, false);
    }

    private LoginWhiteLabelingParams constructLoginWlParams(WhiteLabeling whiteLabeling) {
        if (whiteLabeling == null || whiteLabeling.getSettings() == null) {
            return new LoginWhiteLabelingParams();
        }
        LoginWhiteLabelingParams loginWLParams = (LoginWhiteLabelingParams)JacksonUtil.treeToValue((JsonNode)whiteLabeling.getSettings(), LoginWhiteLabelingParams.class);
        loginWLParams.setDomainId(whiteLabeling.getDomainId());
        return loginWLParams;
    }

    private LoginWhiteLabelingParams getEntityLoginWhiteLabelParams(TenantId tenantId, CustomerId customerId) {
        WhiteLabeling whiteLabeling = null;
        if (this.isWhiteLabelingAllowed(tenantId, customerId)) {
            whiteLabeling = this.findByEntityId(tenantId, customerId, WhiteLabelingType.LOGIN);
        }
        return this.constructLoginWlParams(whiteLabeling);
    }

    @Override
    public boolean isWhiteLabelingAllowed(TenantId tenantId, CustomerId customerId) {
        if (customerId != null && !customerId.isNullUid()) {
            Customer customer = this.customerService.findCustomerById(tenantId, customerId);
            if (customer == null) {
                return false;
            }
            // CE adaptation: flat customers, no sub-customer parent chain to check.
            if (this.isCustomerWhiteLabelingAllowed(customer.getTenantId())) {
                JsonNode allowWhiteLabelJsonNode = customer.getAdditionalInfo().get(ALLOW_WHITE_LABELING);
                if (allowWhiteLabelJsonNode == null) {
                    return true;
                }
                return allowWhiteLabelJsonNode.asBoolean();
            }
            return false;
        }
        if (!tenantId.isSysTenantId()) {
            JsonNode allowWhiteLabelJsonNode;
            Tenant tenant = this.tenantService.findTenantById(tenantId);
            JsonNode jsonNode = allowWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_WHITE_LABELING) : null;
            if (allowWhiteLabelJsonNode == null) {
                return true;
            }
            return allowWhiteLabelJsonNode.asBoolean();
        }
        return true;
    }

    @Override
    public boolean isCustomerWhiteLabelingAllowed(TenantId tenantId) {
        JsonNode allowWhiteLabelJsonNode;
        Tenant tenant = this.tenantService.findTenantById(tenantId);
        JsonNode jsonNode = allowWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_WHITE_LABELING) : null;
        if (allowWhiteLabelJsonNode == null) {
            return true;
        }
        if (allowWhiteLabelJsonNode.asBoolean()) {
            JsonNode allowCustomerWhiteLabelJsonNode;
            JsonNode jsonNode2 = allowCustomerWhiteLabelJsonNode = tenant.getAdditionalInfo() != null ? tenant.getAdditionalInfo().get(ALLOW_CUSTOMER_WHITE_LABELING) : null;
            if (allowCustomerWhiteLabelJsonNode == null) {
                return true;
            }
            return allowCustomerWhiteLabelJsonNode.asBoolean();
        }
        return false;
    }

    @Override
    public boolean isWhiteLabelingConfigured(TenantId tenantId) {
        return this.findByEntityId(tenantId, null, WhiteLabelingType.GENERAL) != null;
    }

    @Override
    public JsonNode saveMailTemplates(TenantId tenantId, JsonNode mailTemplates) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setTenantId(tenantId);
        whiteLabeling.setType(WhiteLabelingType.MAIL_TEMPLATES);
        whiteLabeling.setSettings(mailTemplates);
        JsonNode result = this.whiteLabelingDao.save(tenantId, whiteLabeling).getSettings();
        return result;
    }

    @Override
    public JsonNode getCurrentTenantMailTemplates(TenantId tenantId, boolean systemByDefault) {
        JsonNode mailTemplatesSettings = this.findMailTemplatesByTenantId(tenantId, tenantId);
        if (mailTemplatesSettings.isEmpty() && systemByDefault) {
            mailTemplatesSettings = this.findMailTemplatesByTenantId(tenantId, TenantId.SYS_TENANT_ID);
        }
        return mailTemplatesSettings;
    }

    @Override
    public JsonNode findMailTemplatesByTenantId(TenantId tenantId, TenantId settingsTenantId) {
        if (settingsTenantId == null) {
            return JacksonUtil.newObjectNode();
        }
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(settingsTenantId, WhiteLabelingType.MAIL_TEMPLATES);
        WhiteLabeling mailTemplates = this.whiteLabelingDao.findById(tenantId, key);
        return mailTemplates == null ? JacksonUtil.newObjectNode() : mailTemplates.getSettings();
    }

    @Override
    public JsonNode getMergedTenantMailTemplates(TenantId tenantId) throws ThingsboardException {
        JsonNode mailTemplatesSettings = this.getCurrentTenantMailTemplates(tenantId, true);
        if (mailTemplatesSettings.isEmpty()) {
            throw new ThingsboardException("Failed to get tenant mail templates configuration. Settings not found!", ThingsboardErrorCode.GENERAL);
        }
        JsonNode useSystemMailSettingsNode = mailTemplatesSettings.get("useSystemMailSettings");
        if (useSystemMailSettingsNode == null || useSystemMailSettingsNode.asBoolean()) {
            JsonNode systemMailTemplates = this.findMailTemplatesByTenantId(tenantId, TenantId.SYS_TENANT_ID);
            if (systemMailTemplates.isEmpty()) {
                throw new ThingsboardException("Failed to get system mail templates configuration. Settings not found!", ThingsboardErrorCode.GENERAL);
            }
            mailTemplatesSettings = systemMailTemplates;
        }
        return mailTemplatesSettings;
    }

    private void saveLoginWhiteLabelParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams whiteLabelingParams) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setTenantId(tenantId);
        whiteLabeling.setCustomerId(customerId);
        whiteLabeling.setType(WhiteLabelingType.LOGIN);
        whiteLabelingParams.setBaseUrl(whiteLabelingParams.getBaseUrl() == null ? null : whiteLabelingParams.getBaseUrl().toLowerCase());
        whiteLabeling.setSettings(JacksonUtil.valueToTree((Object)whiteLabelingParams));
        whiteLabeling.setDomainId(whiteLabelingParams.getDomainId());
        this.doSaveWhiteLabelingSettings(tenantId, whiteLabeling);
    }

    private void saveWhiteLabelParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams) {
        WhiteLabeling whiteLabeling = new WhiteLabeling();
        whiteLabeling.setTenantId(tenantId);
        whiteLabeling.setCustomerId(customerId);
        whiteLabeling.setType(WhiteLabelingType.GENERAL);
        whiteLabeling.setSettings(JacksonUtil.valueToTree((Object)whiteLabelingParams));
        this.doSaveWhiteLabelingSettings(tenantId, whiteLabeling);
    }

    private static EntityId getEntityIdForEvent(TenantId tenantId, CustomerId customerId) {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    private static boolean isValidUrl(String url) {
        try {
            new java.net.URL(url).toURI();
            return true;
        } catch (java.net.MalformedURLException | java.net.URISyntaxException e) {
            return false;
        }
    }

    private WhiteLabeling doSaveWhiteLabelingSettings(TenantId tenantId, WhiteLabeling whiteLabeling) {
        try {
            // CE adaptation: PE extracted base64 images from the settings JSON into stored
            // image resources via ImageService. CE's ImageService#replaceBase64WithImageUrl
            // operates on a HasImage entity (single image field), which does not fit the
            // WhiteLabeling settings blob, so we persist images inline (base64 data URLs).
            WhiteLabeling saved = this.whiteLabelingDao.save(tenantId, whiteLabeling);
            WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(saved.getTenantId(), saved.getCustomerId(), saved.getType());
            this.publishEvictEvent(new WhiteLabelingEvictEvent(WhiteLabelingCacheKey.forKey(key)));
            if (whiteLabeling.getDomainId() != null) {
                Domain domain = this.domainService.findDomainById(tenantId, whiteLabeling.getDomainId());
                this.publishEvictEvent(new WhiteLabelingEvictEvent(WhiteLabelingCacheKey.forTypeAndDomain(whiteLabeling.getType(), domain.getName())));
            }
            return saved;
        }
        catch (Exception t) {
            AbstractEntityService.checkConstraintViolation(t, "white_labeling_domain_id_type_key", "Such domain name is already used for such type of white labeling settings!");
            throw t;
        }
    }

    @Override
    public WhiteLabeling findByEntityId(TenantId tenantId, CustomerId customerId, WhiteLabelingType type) {
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(tenantId, customerId, type);
        log.trace("Executing findById for key [{}] ", (Object)key);
        return (WhiteLabeling)this.cache.getAndPutInTransaction(WhiteLabelingCacheKey.forKey(key), () -> this.whiteLabelingDao.findById(tenantId, key), true);
    }

    @Override
    public WhiteLabeling findWhiteLabelingByDomainAndType(String domainName, WhiteLabelingType type) {
        log.trace("Executing getWhiteLabeling for domain name [{}] and type [{}]", (Object)domainName, (Object)type);
        return (WhiteLabeling)this.cache.getAndPutInTransaction(WhiteLabelingCacheKey.forTypeAndDomain(type, domainName), () -> this.whiteLabelingDao.findByDomainAndType(TenantId.SYS_TENANT_ID, domainName, type), true);
    }

    @Override
    public WebSelfRegistrationParams saveTenantSelfRegistrationParams(TenantId tenantId, WebSelfRegistrationParams selfRegistrationParams) {
        if (selfRegistrationParams.getPrivacyPolicy() != null) {
            WhiteLabeling privacyPolicy = WhiteLabeling.builder().tenantId(tenantId).type(WhiteLabelingType.PRIVACY_POLICY).domainId(selfRegistrationParams.getDomainId()).settings((JsonNode)JacksonUtil.newObjectNode().put("privacyPolicy", selfRegistrationParams.getPrivacyPolicy())).build();
            this.doSaveWhiteLabelingSettings(tenantId, privacyPolicy);
        }
        if (selfRegistrationParams.getTermsOfUse() != null) {
            WhiteLabeling termsOfUse = WhiteLabeling.builder().tenantId(tenantId).type(WhiteLabelingType.TERMS_OF_USE).domainId(selfRegistrationParams.getDomainId()).settings((JsonNode)JacksonUtil.newObjectNode().put("termsOfUse", selfRegistrationParams.getTermsOfUse())).build();
            this.doSaveWhiteLabelingSettings(tenantId, termsOfUse);
        }
        selfRegistrationParams.setPrivacyPolicy(null);
        selfRegistrationParams.setTermsOfUse(null);
        WhiteLabeling selfRegistration = WhiteLabeling.builder().tenantId(tenantId).type(WhiteLabelingType.SELF_REGISTRATION).domainId(selfRegistrationParams.getDomainId()).settings(JacksonUtil.valueToTree((Object)selfRegistrationParams)).build();
        this.doSaveWhiteLabelingSettings(tenantId, selfRegistration);
        return this.getTenantSelfRegistrationParams(tenantId);
    }

    @Override
    public WebSelfRegistrationParams getTenantSelfRegistrationParams(TenantId tenantId) {
        WhiteLabeling whiteLabeling = this.findByEntityId(tenantId, null, WhiteLabelingType.SELF_REGISTRATION);
        return this.constructSRParams(whiteLabeling);
    }

    private WebSelfRegistrationParams constructSRParams(WhiteLabeling whiteLabeling) {
        if (whiteLabeling == null || whiteLabeling.getSettings() == null) {
            return null;
        }
        WebSelfRegistrationParams selfRegistrationParams = (WebSelfRegistrationParams)JacksonUtil.treeToValue((JsonNode)whiteLabeling.getSettings(), WebSelfRegistrationParams.class);
        selfRegistrationParams.setDomainId(whiteLabeling.getDomainId());
        return selfRegistrationParams;
    }

    @Override
    public WebSelfRegistrationParams getWebSelfRegistrationParams(String domainName) {
        WhiteLabeling whiteLabeling = this.findWhiteLabelingByDomainAndType(domainName, WhiteLabelingType.SELF_REGISTRATION);
        return whiteLabeling != null ? (WebSelfRegistrationParams)JacksonUtil.treeToValue((JsonNode)whiteLabeling.getSettings(), WebSelfRegistrationParams.class) : null;
    }

    @Override
    public JsonNode getWebPrivacyPolicy(String domainName) {
        WhiteLabeling whiteLabeling = this.findWhiteLabelingByDomainAndType(domainName, WhiteLabelingType.PRIVACY_POLICY);
        return whiteLabeling != null ? whiteLabeling.getSettings() : null;
    }

    @Override
    public JsonNode getTenantPrivacyPolicy(TenantId tenantId) {
        WhiteLabeling whiteLabeling = this.findByEntityId(tenantId, null, WhiteLabelingType.PRIVACY_POLICY);
        return whiteLabeling != null ? whiteLabeling.getSettings() : null;
    }

    @Override
    public JsonNode getWebTermsOfUse(String domainName) {
        WhiteLabeling whiteLabeling = this.findWhiteLabelingByDomainAndType(domainName, WhiteLabelingType.TERMS_OF_USE);
        return whiteLabeling != null ? whiteLabeling.getSettings() : null;
    }

    @Override
    public JsonNode getTenantTermsOfUse(TenantId tenantId) {
        WhiteLabeling whiteLabeling = this.findByEntityId(tenantId, null, WhiteLabelingType.TERMS_OF_USE);
        return whiteLabeling != null ? whiteLabeling.getSettings() : null;
    }

    @Override
    public void deleteWhiteLabeling(TenantId tenantId, CustomerId customerId, WhiteLabelingType type) {
        WhiteLabelingCompositeKey key = new WhiteLabelingCompositeKey(tenantId, customerId, type);
        WhiteLabeling whiteLabeling = this.whiteLabelingDao.findById(tenantId, key);
        if (whiteLabeling != null) {
            this.whiteLabelingDao.removeById(tenantId, key);
            this.publishEvictEvent(new WhiteLabelingEvictEvent(WhiteLabelingCacheKey.forKey(key)));
            if (whiteLabeling.getDomainId() != null) {
                Domain domain = this.domainService.findDomainById(tenantId, whiteLabeling.getDomainId());
                this.publishEvictEvent(new WhiteLabelingEvictEvent(WhiteLabelingCacheKey.forTypeAndDomain(type, domain.getName())));
            }
        }
    }

    @Override
    @TransactionalEventListener(classes={WhiteLabelingEvictEvent.class})
    public void handleEvictEvent(WhiteLabelingEvictEvent event) {
        this.cache.evict(event.getKey());
    }

    @ConstructorProperties(value={"adminSettingsService", "domainService", "whiteLabelingDao", "tenantService", "customerService", "imageService", "eventPublisher"})
    @Generated
    public BaseWhiteLabelingService(AdminSettingsService adminSettingsService, DomainService domainService, WhiteLabelingDao whiteLabelingDao, TenantService tenantService, CustomerService customerService, ImageService imageService, ApplicationEventPublisher eventPublisher) {
        this.adminSettingsService = adminSettingsService;
        this.domainService = domainService;
        this.whiteLabelingDao = whiteLabelingDao;
        this.tenantService = tenantService;
        this.customerService = customerService;
        this.imageService = imageService;
        this.eventPublisher = eventPublisher;
    }
}

