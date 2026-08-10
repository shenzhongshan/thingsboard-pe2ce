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
package org.thingsboard.server.service.converter;

import org.thingsboard.server.common.data.LibraryConvertersInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.List;
import java.util.Map;

/**
 * Read-only access to ThingsBoard's public data-converters library, a git repository of ready-made
 * uplink/downlink converters laid out as {@code VENDORS/<vendor>/<model>/<INTEGRATION>/<direction>}.
 * The repository is cloned and refreshed by the shared git sync service.
 */
public interface ConverterLibraryService {

    List<Vendor> getVendors(IntegrationType integrationType, String converterType, int page, int pageSize, boolean loadImages);

    List<Model> getVendorModels(IntegrationType integrationType, String converterType, String vendorName, int page, int pageSize, boolean loadImages);

    /** Converter body (the JS/TBEL configuration), or "{}" if the library has none. */
    String getConverter(IntegrationType integrationType, String converterType, String vendorName, String model);

    String getConverterMetadata(IntegrationType integrationType, String converterType, String vendorName, String model);

    /** Example payload, for pre-filling the converter test dialog. */
    String getPayload(IntegrationType integrationType, String converterType, String vendorName, String model);

    /** Per integration-type directory: whether the library has uplink and/or downlink converters. */
    Map<String, LibraryConvertersInfo> getConvertersInfo();

}
