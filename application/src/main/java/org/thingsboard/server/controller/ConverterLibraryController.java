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

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.LibraryConvertersInfo;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.converter.ConverterLibraryService;
import org.thingsboard.server.service.converter.Model;
import org.thingsboard.server.service.converter.Vendor;

import java.util.List;
import java.util.Map;

/**
 * Browses ThingsBoard's public data-converters library so the integration wizard can offer
 * ready-made converters instead of asking the user to write one from scratch.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api/converter/library")
@RequiredArgsConstructor
@Slf4j
public class ConverterLibraryController extends BaseController {

    private final ConverterLibraryService converterLibraryService;

    @ApiOperation(value = "Get vendors (getVendors)",
            notes = "Lists vendors that ship at least one converter for the integration type.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{integrationType}/vendors")
    public List<Vendor> getVendors(
            @Parameter(description = "Integration type", required = true) @PathVariable IntegrationType integrationType,
            @Parameter(description = "Restrict to vendors having this direction: uplink or downlink") @RequestParam(required = false) String converterType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2147483647") int pageSize,
            @Parameter(description = "Include the vendor logo as an inline data URI") @RequestParam(defaultValue = "true") boolean loadImages) {
        return converterLibraryService.getVendors(integrationType, converterType, page, pageSize, loadImages);
    }

    @ApiOperation(value = "Get vendor models (getVendorModels)",
            notes = "Lists the vendor's device models that have a converter for the integration type.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/{integrationType}/{vendorName}/models")
    public List<Model> getVendorModels(
            @PathVariable IntegrationType integrationType,
            @PathVariable String vendorName,
            @RequestParam(required = false) String converterType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2147483647") int pageSize,
            @RequestParam(defaultValue = "true") boolean loadImages) {
        return converterLibraryService.getVendorModels(integrationType, converterType, vendorName, page, pageSize, loadImages);
    }

    @ApiOperation(value = "Get uplink converter (getUplinkConverter)",
            notes = "Returns the uplink converter body for the vendor, integration type and model.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink", produces = "text/plain")
    public String getUplinkConverter(@PathVariable IntegrationType integrationType,
                                     @PathVariable String vendorName, @PathVariable String model) {
        return converterLibraryService.getConverter(integrationType, ConverterType.UPLINK.getDirectory(), vendorName, model);
    }

    @ApiOperation(value = "Get uplink converter metadata (getUplinkConverterMetadata)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/metadata", produces = "text/plain")
    public String getUplinkConverterMetadata(@PathVariable IntegrationType integrationType,
                                             @PathVariable String vendorName, @PathVariable String model) {
        return converterLibraryService.getConverterMetadata(integrationType, ConverterType.UPLINK.getDirectory(), vendorName, model);
    }

    @ApiOperation(value = "Get uplink payload (getUplinkPayload)",
            notes = "Returns an example payload, for pre-filling the converter test dialog.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/uplink/payload", produces = "text/plain")
    public String getUplinkPayload(@PathVariable IntegrationType integrationType,
                                   @PathVariable String vendorName, @PathVariable String model) {
        return converterLibraryService.getPayload(integrationType, ConverterType.UPLINK.getDirectory(), vendorName, model);
    }

    @ApiOperation(value = "Get downlink converter (getDownlinkConverter)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink", produces = "text/plain")
    public String getDownlinkConverter(@PathVariable IntegrationType integrationType,
                                       @PathVariable String vendorName, @PathVariable String model) {
        return converterLibraryService.getConverter(integrationType, ConverterType.DOWNLINK.getDirectory(), vendorName, model);
    }

    @ApiOperation(value = "Get downlink converter metadata (getDownlinkConverterMetadata)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/metadata", produces = "text/plain")
    public String getDownlinkConverterMetadata(@PathVariable IntegrationType integrationType,
                                               @PathVariable String vendorName, @PathVariable String model) {
        return converterLibraryService.getConverterMetadata(integrationType, ConverterType.DOWNLINK.getDirectory(), vendorName, model);
    }

    @ApiOperation(value = "Get downlink payload (getDownlinkPayload)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/{integrationType}/{vendorName}/{model}/downlink/payload", produces = "text/plain")
    public String getDownlinkPayload(@PathVariable IntegrationType integrationType,
                                     @PathVariable String vendorName, @PathVariable String model) {
        return converterLibraryService.getPayload(integrationType, ConverterType.DOWNLINK.getDirectory(), vendorName, model);
    }

    @ApiOperation(value = "Get converters info (getConvertersInfo)",
            notes = "Per integration type, whether the library ships uplink and/or downlink converters. " +
                    "Lets the UI show only the types that have something to offer.")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/info")
    public Map<String, LibraryConvertersInfo> getConvertersInfo() {
        return converterLibraryService.getConvertersInfo();
    }

}
