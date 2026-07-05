package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

@RestController
@TbCoreComponent
@RequestMapping(value={"/api"})
public class WhiteLabelingController
extends BaseController {
    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @ApiOperation(value="Get White Labeling parameters", notes="Returns white-labeling parameters for the current user.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value={"/whiteLabel/whiteLabelParams"}, produces={"application/json"})
    public WhiteLabelingParams getWhiteLabelParams() throws Exception {
        Authority authority = this.getCurrentUser().getAuthority();
        WhiteLabelingParams whiteLabelingParams = null;
        boolean wlEnabled = true;
        if (!wlEnabled) {
            whiteLabelingParams = new WhiteLabelingParams();
            whiteLabelingParams.setHelpLinkBaseUrl(DataConstants.ALSUN_BASE_URL);
            whiteLabelingParams.setEnableHelpLinks(Boolean.valueOf(true));
        } else if (Authority.SYS_ADMIN.equals((Object)authority)) {
            whiteLabelingParams = this.whiteLabelingService.getSystemWhiteLabelingParams();
        } else if (Authority.TENANT_ADMIN.equals((Object)authority)) {
            whiteLabelingParams = this.whiteLabelingService.getMergedTenantWhiteLabelingParams(this.getTenantId());
        } else if (Authority.CUSTOMER_USER.equals((Object)authority)) {
            whiteLabelingParams = this.whiteLabelingService.getMergedCustomerWhiteLabelingParams(this.getTenantId(), this.getCurrentUser().getCustomerId());
        }
        whiteLabelingParams.setWhiteLabelingEnabled(wlEnabled);
        return whiteLabelingParams;
    }

    @ApiOperation(value="Get Login White Labeling parameters", notes="Returns login white-labeling parameters based on the hostname from request.")
    @GetMapping(value={"/noauth/whiteLabel/loginWhiteLabelParams"}, produces={"application/json"})
    public LoginWhiteLabelingParams getLoginWhiteLabelParams(HttpServletRequest request) throws Exception {
        boolean wlEnabled = true;
        if (!wlEnabled) {
            return new LoginWhiteLabelingParams();
        }
        return this.whiteLabelingService.getMergedLoginWhiteLabelingParams(request.getServerName());
    }

    @ApiOperation(value="Get White Labeling configuration (getCurrentWhiteLabelParams)", notes="Fetch the White Labeling configuration that corresponds to the authority of the user. The API call is designed to load the White Labeling configuration for edition. So, the result is NOT merged with the parent level White Labeling configuration. Let's assume there is a custom White Labeling  configured on a system level. And there is no custom White Labeling  items configured on a tenant level. In such a case, the API call will return default object for the tenant administrator. \n\nSecurity check is performed to verify that the user has 'READ' permission for the white labeling resource.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value={"/whiteLabel/currentWhiteLabelParams"}, produces={"application/json"})
    public WhiteLabelingParams getCurrentWhiteLabelParams(@Parameter(description="A string value representing the customer id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'") @RequestParam(value="customerId", required=false) String strCustomerId) throws ThingsboardException {
        boolean wlEnabled = true;
        if (!wlEnabled) {
            WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
            whiteLabelingParams.setHelpLinkBaseUrl(DataConstants.ALSUN_BASE_URL);
            whiteLabelingParams.setEnableHelpLinks(Boolean.valueOf(true));
            return whiteLabelingParams;
        }
        Authority authority = this.getCurrentUser().getAuthority();
        this.checkWhiteLabelingPermissions(Operation.READ);
        WhiteLabelingParams whiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals((Object)authority)) {
            whiteLabelingParams = this.whiteLabelingService.getSystemWhiteLabelingParams();
        } else if (Authority.TENANT_ADMIN.equals((Object)authority)) {
            if (StringUtils.isEmpty((String)strCustomerId)) {
                whiteLabelingParams = this.whiteLabelingService.getTenantWhiteLabelingParams(this.getTenantId());
            } else {
                CustomerId customerId = new CustomerId(this.toUUID(strCustomerId));
                this.checkCustomerId(customerId, Operation.READ);
                whiteLabelingParams = this.whiteLabelingService.getCustomerWhiteLabelingParams(this.getTenantId(), customerId);
            }
        } else if (Authority.CUSTOMER_USER.equals((Object)authority)) {
            whiteLabelingParams = this.whiteLabelingService.getCustomerWhiteLabelingParams(this.getTenantId(), this.getCurrentUser().getCustomerId());
        }
        return whiteLabelingParams;
    }

    @ApiOperation(value="Get Login White Labeling configuration (getCurrentWhiteLabelParams)", notes="Fetch the Login  White Labeling configuration that corresponds to the authority of the user. The API call is designed to load the Login White Labeling configuration for edition. So, the result is NOT merged with the parent level White Labeling configuration. Let's assume there is a custom White Labeling  configured on a system level. And there is no custom White Labeling  items configured on a tenant level. In such a case, the API call will return default object for the tenant administrator. \n\nSecurity check is performed to verify that the user has 'READ' permission for the white labeling resource.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value={"/whiteLabel/currentLoginWhiteLabelParams"}, produces={"application/json"})
    public LoginWhiteLabelingParams getCurrentLoginWhiteLabelParams(@Parameter(description="A string value representing the customer id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'") @RequestParam(value="customerId", required=false) String strCustomerId) throws Exception {
        boolean wlEnabled = true;
        if (!wlEnabled) {
            return new LoginWhiteLabelingParams();
        }
        Authority authority = this.getCurrentUser().getAuthority();
        this.checkWhiteLabelingPermissions(Operation.READ);
        LoginWhiteLabelingParams loginWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals((Object)authority)) {
            loginWhiteLabelingParams = this.whiteLabelingService.getSystemLoginWhiteLabelingParams();
        } else if (Authority.TENANT_ADMIN.equals((Object)authority)) {
            if (StringUtils.isEmpty((String)strCustomerId)) {
                loginWhiteLabelingParams = this.whiteLabelingService.getTenantLoginWhiteLabelingParams(this.getTenantId());
            } else {
                CustomerId customerId = new CustomerId(this.toUUID(strCustomerId));
                this.checkCustomerId(customerId, Operation.READ);
                loginWhiteLabelingParams = this.whiteLabelingService.getCustomerLoginWhiteLabelingParams(this.getTenantId(), customerId);
            }
        } else if (Authority.CUSTOMER_USER.equals((Object)authority)) {
            loginWhiteLabelingParams = this.whiteLabelingService.getCustomerLoginWhiteLabelingParams(this.getTenantId(), this.getCurrentUser().getCustomerId());
        }
        return loginWhiteLabelingParams;
    }

    @ApiOperation(value="Create Or Update White Labeling configuration (saveWhiteLabelParams)", notes="Creates or Updates the White Labeling configuration.\n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.", requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content={@Content(mediaType="application/json")}))
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value={"/whiteLabel/whiteLabelParams"})
    @ResponseStatus(value=HttpStatus.OK)
    public WhiteLabelingParams saveWhiteLabelParams(@Parameter(description="A JSON value representing the white labeling configuration") @RequestBody WhiteLabelingParams whiteLabelingParams, @Parameter(description="A string value representing the customer id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'") @RequestParam(value="customerId", required=false) String strCustomerId) throws ThingsboardException {
        Authority authority = this.getCurrentUser().getAuthority();
        this.checkWhiteLabelingPermissions(Operation.WRITE);
        WhiteLabelingParams savedWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals((Object)authority)) {
            savedWhiteLabelingParams = this.whiteLabelingService.saveSystemWhiteLabelingParams(whiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals((Object)authority)) {
            if (StringUtils.isEmpty((String)strCustomerId)) {
                savedWhiteLabelingParams = this.whiteLabelingService.saveTenantWhiteLabelingParams(this.getTenantId(), whiteLabelingParams);
            } else {
                CustomerId customerId = new CustomerId(this.toUUID(strCustomerId));
                this.checkCustomerId(customerId, Operation.READ);
                savedWhiteLabelingParams = this.whiteLabelingService.saveCustomerWhiteLabelingParams(this.getTenantId(), customerId, whiteLabelingParams);
            }
        } else if (Authority.CUSTOMER_USER.equals((Object)authority)) {
            savedWhiteLabelingParams = this.whiteLabelingService.saveCustomerWhiteLabelingParams(this.getTenantId(), this.getCurrentUser().getCustomerId(), whiteLabelingParams);
        }
        return savedWhiteLabelingParams;
    }

    @ApiOperation(value="Create Or Update Login White Labeling configuration (saveWhiteLabelParams)", notes="Creates or Updates the White Labeling configuration.\n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.", requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content={@Content(mediaType="application/json")}))
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value={"/whiteLabel/loginWhiteLabelParams"})
    @ResponseStatus(value=HttpStatus.OK)
    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(@Parameter(description="A JSON value representing the login white labeling configuration") @RequestBody LoginWhiteLabelingParams loginWhiteLabelingParams, @Parameter(description="A string value representing the customer id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'") @RequestParam(name="customerId", required=false) String strCustomerId) throws Exception {
        Authority authority = this.getCurrentUser().getAuthority();
        this.checkWhiteLabelingPermissions(Operation.WRITE);
        if (loginWhiteLabelingParams.getDomainId() != null) {
            this.checkEntityId(loginWhiteLabelingParams.getDomainId(), this.domainService::findDomainById, Operation.READ);
        }
        LoginWhiteLabelingParams savedLoginWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals((Object)authority)) {
            savedLoginWhiteLabelingParams = this.whiteLabelingService.saveSystemLoginWhiteLabelingParams(loginWhiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals((Object)authority)) {
            if (StringUtils.isEmpty((String)strCustomerId)) {
                savedLoginWhiteLabelingParams = this.whiteLabelingService.saveTenantLoginWhiteLabelingParams(this.getTenantId(), loginWhiteLabelingParams);
            } else {
                CustomerId customerId = new CustomerId(this.toUUID(strCustomerId));
                this.checkCustomerId(customerId, Operation.READ);
                savedLoginWhiteLabelingParams = this.whiteLabelingService.saveCustomerLoginWhiteLabelingParams(this.getTenantId(), customerId, loginWhiteLabelingParams);
            }
        } else if (Authority.CUSTOMER_USER.equals((Object)authority)) {
            savedLoginWhiteLabelingParams = this.whiteLabelingService.saveCustomerLoginWhiteLabelingParams(this.getTenantId(), this.getCurrentUser().getCustomerId(), loginWhiteLabelingParams);
        }
        return savedLoginWhiteLabelingParams;
    }

    @ApiOperation(value="Preview Login White Labeling configuration (saveWhiteLabelParams)", notes="Merge the White Labeling configuration with the parent configuration and return the result.\n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.", requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(content={@Content(mediaType="application/json")}))
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value={"/whiteLabel/previewWhiteLabelParams"})
    @ResponseStatus(value=HttpStatus.OK)
    public WhiteLabelingParams previewWhiteLabelParams(@Parameter(description="A JSON value representing the white labeling configuration") @RequestBody WhiteLabelingParams whiteLabelingParams) throws ThingsboardException {
        Authority authority = this.getCurrentUser().getAuthority();
        this.checkWhiteLabelingPermissions(Operation.WRITE);
        WhiteLabelingParams mergedWhiteLabelingParams = null;
        if (Authority.SYS_ADMIN.equals((Object)authority)) {
            mergedWhiteLabelingParams = this.whiteLabelingService.mergeSystemWhiteLabelingParams(whiteLabelingParams);
        } else if (Authority.TENANT_ADMIN.equals((Object)authority)) {
            mergedWhiteLabelingParams = this.whiteLabelingService.mergeTenantWhiteLabelingParams(whiteLabelingParams);
        } else if (Authority.CUSTOMER_USER.equals((Object)authority)) {
            mergedWhiteLabelingParams = this.whiteLabelingService.mergeCustomerWhiteLabelingParams(this.getTenantId(), this.getCurrentUser().getCustomerId(), whiteLabelingParams);
        }
        return mergedWhiteLabelingParams;
    }

    @ApiOperation(value="Check White Labeling Allowed", notes="Check if the White Labeling is enabled for the current user owner (tenant or customer)\n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.\n\nAvailable for users with 'TENANT_ADMIN' or 'CUSTOMER_USER' authority.")
    @PreAuthorize(value="hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value={"/whiteLabel/isWhiteLabelingAllowed"})
    public Boolean isWhiteLabelingAllowed() throws ThingsboardException {
        Authority authority = this.getCurrentUser().getAuthority();
        return this.whiteLabelingService.isWhiteLabelingAllowed(this.getTenantId(), this.getCurrentUser().getCustomerId());
    }

    @ApiOperation(value="Check Customer White Labeling Allowed", notes="Check if the White Labeling is enabled for the customers of the current tenant\n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.\n\nAvailable for users with 'TENANT_ADMIN' authority.")
    @PreAuthorize(value="hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value={"/whiteLabel/isCustomerWhiteLabelingAllowed"})
    public Boolean isCustomerWhiteLabelingAllowed() throws ThingsboardException {
        return this.whiteLabelingService.isCustomerWhiteLabelingAllowed(this.getTenantId());
    }

    @ApiOperation(value="Save the Mail templates settings (saveMailTemplates)", notes="Creates or Updates the Mail templates settings.\n\nAvailable for users with 'SYS_ADMIN' or 'TENANT_ADMIN' authority.\n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value={"/whiteLabel/mailTemplates"})
    @ResponseStatus(value=HttpStatus.OK)
    public JsonNode saveMailTemplates(@io.swagger.v3.oas.annotations.parameters.RequestBody(description="A JSON value representing the Administration Settings.") @RequestBody JsonNode mailTemplates) throws Exception {
        this.checkWhiteLabelingPermissions(Operation.WRITE);
        return this.whiteLabelingService.saveMailTemplates(this.getTenantId(), mailTemplates);
    }

    @ApiOperation(value="Get the Mail templates settings (getMailTemplates)", notes="Fetch Mail template settings. \n\nAvailable for users with 'SYS_ADMIN' or 'TENANT_ADMIN' authority.\n\nSecurity check is performed to verify that the user has 'READ' permission for the white labeling resource.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value={"/whiteLabel/mailTemplates"})
    @ResponseStatus(value=HttpStatus.OK)
    public JsonNode getMailTemplates(@Parameter(description="Use system settings if settings are not defined on tenant level.") @RequestParam(required=false, defaultValue="false") boolean systemByDefault) throws Exception {
        this.checkWhiteLabelingPermissions(Operation.READ);
        JsonNode mailTemplates = this.whiteLabelingService.getCurrentTenantMailTemplates(this.getTenantId(), systemByDefault);
        ((ObjectNode)mailTemplates).remove("apiUsageStateEnabled");
        ((ObjectNode)mailTemplates).remove("apiUsageStateWarning");
        ((ObjectNode)mailTemplates).remove("apiUsageStateDisabled");
        return mailTemplates;
    }

    @ApiOperation(value="Delete Login White Labeling configuration (deleteCurrentLoginWhiteLabelParams)", notes="Delete the Login White Labeling configuration that corresponds to the authority of the user. \n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value={"/whiteLabel/currentLoginWhiteLabelParams"})
    public void deleteCurrentLoginWhiteLabelParams(@Parameter(description="A string value representing the customer id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'") @RequestParam(value="customerId", required=false) String strCustomerId) throws Exception {
        CustomerId customerId;
        Authority authority = this.getCurrentUser().getAuthority();
        this.checkWhiteLabelingPermissions(Operation.WRITE);
        SecurityUser currentUser = this.getCurrentUser();
        if (Authority.TENANT_ADMIN.equals((Object)authority) && !StringUtils.isEmpty((String)strCustomerId)) {
            customerId = new CustomerId(this.toUUID(strCustomerId));
            this.checkCustomerId(customerId, Operation.WRITE);
        } else {
            customerId = currentUser.getCustomerId();
        }
        this.whiteLabelingService.deleteWhiteLabeling(currentUser.getTenantId(), customerId, WhiteLabelingType.LOGIN);
    }

    @ApiOperation(value="Delete General White Labeling configuration (deleteCurrentWhiteLabelParams)", notes="Delete the White Labeling configuration that corresponds to the authority of the user. \n\nSecurity check is performed to verify that the user has 'WRITE' permission for the white labeling resource.")
    @PreAuthorize(value="hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value={"/whiteLabel/currentWhiteLabelParams"})
    public void deleteCurrentWhiteLabelParams(@Parameter(description="A string value representing the customer id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'") @RequestParam(value="customerId", required=false) String strCustomerId) throws Exception {
        CustomerId customerId;
        this.checkWhiteLabelingPermissions(Operation.WRITE);
        SecurityUser currentUser = this.getCurrentUser();
        if (Authority.TENANT_ADMIN.equals((Object)currentUser.getAuthority()) && !StringUtils.isEmpty((String)strCustomerId)) {
            customerId = new CustomerId(this.toUUID(strCustomerId));
            this.checkCustomerId(customerId, Operation.WRITE);
        } else {
            customerId = currentUser.getCustomerId();
        }
        this.whiteLabelingService.deleteWhiteLabeling(currentUser.getTenantId(), customerId, WhiteLabelingType.GENERAL);
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        this.accessControlService.checkPermission(this.getCurrentUser(), Resource.WHITE_LABELING, operation);
    }
}

