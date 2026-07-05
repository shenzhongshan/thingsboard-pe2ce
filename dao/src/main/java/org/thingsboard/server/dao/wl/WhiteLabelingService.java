package org.thingsboard.server.dao.wl;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.selfregistration.WebSelfRegistrationParams;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.resource.ImageCacheKey;

public interface WhiteLabelingService {
    public WhiteLabelingParams getSystemWhiteLabelingParams();

    public LoginWhiteLabelingParams getSystemLoginWhiteLabelingParams();

    public WhiteLabelingParams getTenantWhiteLabelingParams(TenantId var1);

    public WhiteLabelingParams getCustomerWhiteLabelingParams(TenantId var1, CustomerId var2);

    public WhiteLabelingParams getMergedTenantWhiteLabelingParams(TenantId var1) throws Exception;

    public WhiteLabelingParams getMergedCustomerWhiteLabelingParams(TenantId var1, CustomerId var2) throws Exception;

    public LoginWhiteLabelingParams getTenantLoginWhiteLabelingParams(TenantId var1) throws Exception;

    public LoginWhiteLabelingParams getCustomerLoginWhiteLabelingParams(TenantId var1, CustomerId var2) throws Exception;

    public LoginWhiteLabelingParams getMergedLoginWhiteLabelingParams(String var1) throws Exception;

    public ImageCacheKey getLoginImageKey(String var1, boolean var2) throws Exception;

    public WhiteLabelingParams saveSystemWhiteLabelingParams(WhiteLabelingParams var1);

    public WhiteLabelingParams saveTenantWhiteLabelingParams(TenantId var1, WhiteLabelingParams var2);

    public WhiteLabelingParams saveCustomerWhiteLabelingParams(TenantId var1, CustomerId var2, WhiteLabelingParams var3);

    public LoginWhiteLabelingParams saveSystemLoginWhiteLabelingParams(LoginWhiteLabelingParams var1);

    public LoginWhiteLabelingParams saveTenantLoginWhiteLabelingParams(TenantId var1, LoginWhiteLabelingParams var2) throws Exception;

    public LoginWhiteLabelingParams saveCustomerLoginWhiteLabelingParams(TenantId var1, CustomerId var2, LoginWhiteLabelingParams var3) throws Exception;

    public WhiteLabelingParams mergeSystemWhiteLabelingParams(WhiteLabelingParams var1);

    public WhiteLabelingParams mergeTenantWhiteLabelingParams(WhiteLabelingParams var1);

    public WhiteLabelingParams mergeCustomerWhiteLabelingParams(TenantId var1, CustomerId var2, WhiteLabelingParams var3);

    public void deleteAllTenantWhiteLabeling(TenantId var1);

    public boolean isWhiteLabelingAllowed(TenantId var1, CustomerId var2);

    public boolean isCustomerWhiteLabelingAllowed(TenantId var1);

    public boolean isWhiteLabelingConfigured(TenantId var1);

    public JsonNode saveMailTemplates(TenantId var1, JsonNode var2);

    public JsonNode getCurrentTenantMailTemplates(TenantId var1, boolean var2);

    public JsonNode findMailTemplatesByTenantId(TenantId var1, TenantId var2);

    public JsonNode getMergedTenantMailTemplates(TenantId var1) throws ThingsboardException;

    public WhiteLabeling findByEntityId(TenantId var1, CustomerId var2, WhiteLabelingType var3);

    public WhiteLabeling findWhiteLabelingByDomainAndType(String var1, WhiteLabelingType var2);

    public WebSelfRegistrationParams saveTenantSelfRegistrationParams(TenantId var1, WebSelfRegistrationParams var2);

    public WebSelfRegistrationParams getTenantSelfRegistrationParams(TenantId var1);

    public WebSelfRegistrationParams getWebSelfRegistrationParams(String var1);

    public JsonNode getWebPrivacyPolicy(String var1);

    public JsonNode getTenantPrivacyPolicy(TenantId var1);

    public JsonNode getWebTermsOfUse(String var1);

    public JsonNode getTenantTermsOfUse(TenantId var1);

    public void deleteWhiteLabeling(TenantId var1, CustomerId var2, WhiteLabelingType var3);
}

