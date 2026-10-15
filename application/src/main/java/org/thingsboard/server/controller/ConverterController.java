/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.integration.ConverterExecutorService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * CE port of PE's converter API. Dropped vs PE: debugIn events endpoint, converter library,
 * edge templates. testUpLink/testDownLink request/response contracts match PE.
 */
@RestController
@TbCoreComponent
@RequestMapping(value = {"/api"})
@RequiredArgsConstructor
public class ConverterController extends BaseController {

    public static final String CONVERTER_ID = "converterId";

    private final ConverterService converterService;
    private final ConverterExecutorService converterExecutorService;
    private final EventService eventService;

    @ApiOperation(value = "Get Converter (getConverterById)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/converter/{converterId}"})
    public Converter getConverterById(
            @Parameter(description = "A string value representing the converter id", required = true)
            @PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ);
        return checkAccess(checkNotNull(converterService.findConverterById(
                getTenantId(), new ConverterId(toUUID(strConverterId)))));
    }

    @ApiOperation(value = "Save Converter (saveConverter)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/converter"})
    public Converter saveConverter(
            @Parameter(description = "A JSON value representing the converter", required = true)
            @RequestBody Converter converter) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, Operation.WRITE);
        converter.setTenantId(getTenantId());
        ActionType actionType = converter.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        if (converter.getId() != null) {
            checkAccess(checkNotNull(converterService.findConverterById(getTenantId(), converter.getId())));
        }
        try {
            Converter saved = converterService.saveConverter(converter);
            // PE records an UPDATED lifecycle event whenever a converter's script changes
            // (DefaultDataConverterService); without it the converter's "Lifecycle events" tab
            // would never have a single row.
            eventService.saveAsync(LifecycleEvent.builder()
                    .tenantId(getTenantId())
                    .entityId(saved.getId().getId())
                    .serviceId(serviceInfoProvider.getServiceId())
                    .lcEventType(ComponentLifecycleEvent.UPDATED.name())
                    .success(true)
                    .build());
            logEntityActionService.logEntityAction(getTenantId(), saved.getId(), saved, actionType, getCurrentUser());
            return saved;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.CONVERTER), converter,
                    actionType, getCurrentUser(), e);
            throw e;
        }
    }

    @ApiOperation(value = "Get Converters (getConverters)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = {"/converters"}, params = {"pageSize", "page"})
    public PageData<Converter> getConverters(
            @Parameter(description = "只列 edge 模板。模板与普通转换器分居两个命名空间，互不可见。")
            @RequestParam(value = "isEdgeTemplate", required = false, defaultValue = "false") boolean isEdgeTemplate,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Case-insensitive 'substring' filter based on converter name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Integration type filter")
            @RequestParam(required = false) String integrationType) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (isEdgeTemplate) {
            return checkNotNull(converterService.findTenantEdgeTemplateConverters(getTenantId(), pageLink));
        }
        IntegrationType typeFilter = StringUtils.isNotEmpty(integrationType) ? IntegrationType.valueOf(integrationType) : null;
        return converterService.findTenantConverters(getTenantId(), typeFilter, pageLink);
    }

    @ApiOperation(value = "Delete Converter (deleteConverter)",
            notes = "Deletion fails if the converter is still referenced by an integration.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = {"/converter/{converterId}"})
    public void deleteConverter(
            @Parameter(description = "A string value representing the converter id", required = true)
            @PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, Operation.DELETE);
        ConverterId converterId = new ConverterId(toUUID(strConverterId));
        Converter converter = checkAccess(checkNotNull(converterService.findConverterById(getTenantId(), converterId)));
        try {
            converterService.deleteConverter(getTenantId(), converterId);
            logEntityActionService.logEntityAction(getTenantId(), converterId, converter, ActionType.DELETED,
                    getCurrentUser(), strConverterId);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.CONVERTER), ActionType.DELETED,
                    getCurrentUser(), e, strConverterId);
            throw e;
        }
    }

    @ApiOperation(value = "Test uplink converter (testUpLinkConverter)",
            notes = "Body: {metadata: {..}, payload: '<base64>', decoder: '<script>'}. Returns {output, error}.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/converter/testUpLink"})
    public JsonNode testUpLinkConverter(
            @Parameter(description = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @RequestBody JsonNode inputParams) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        ObjectNode result = JacksonUtil.newObjectNode();
        try {
            byte[] payload = Base64.getDecoder().decode(inputParams.get("payload").asText());
            Map<String, String> metadata = toStringMap(inputParams.get("metadata"));
            String decoder = inputParams.get("decoder").asText();
            JsonNode output = converterExecutorService.executeUplink(tenantId,
                    scriptLang != null ? scriptLang : ScriptLanguage.TBEL, decoder, payload, metadata);
            result.put("output", JacksonUtil.toString(output));
            result.put("error", "");
        } catch (Exception e) {
            result.put("output", "");
            result.put("error", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
        return result;
    }

    @ApiOperation(value = "Test downlink converter (testDownLinkConverter)",
            notes = "Body: {msg: '<json string>', msgType, metadata: {..}, integrationMetadata: {..}, encoder: '<script>'}. Returns {output, error}.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = {"/converter/testDownLink"})
    public JsonNode testDownLinkConverter(
            @Parameter(description = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @RequestBody JsonNode inputParams) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        ObjectNode result = JacksonUtil.newObjectNode();
        try {
            JsonNode msg = JacksonUtil.toJsonNode(inputParams.get("msg").asText());
            String msgType = inputParams.get("msgType").asText();
            Map<String, String> metadata = toStringMap(inputParams.get("metadata"));
            Map<String, String> integrationMetadata = toStringMap(inputParams.get("integrationMetadata"));
            String encoder = inputParams.get("encoder").asText();
            JsonNode output = converterExecutorService.executeDownlink(tenantId,
                    scriptLang != null ? scriptLang : ScriptLanguage.TBEL, encoder, msg, msgType, metadata, integrationMetadata);
            result.put("output", JacksonUtil.toString(output));
            result.put("error", "");
        } catch (Exception e) {
            result.put("output", "");
            result.put("error", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
        return result;
    }

    private Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> map = new HashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText()));
        }
        return map;
    }

    private Converter checkAccess(Converter converter) throws ThingsboardException {
        if (!getCurrentUser().getTenantId().equals(converter.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!",
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return converter;
    }

}
