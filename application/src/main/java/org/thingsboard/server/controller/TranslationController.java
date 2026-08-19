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
import com.google.common.hash.Hashing;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.translation.TranslationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class TranslationController extends BaseController {

    /**
     * Locales the JDK exposes but that are not useful as UI locales: either deprecated aliases or
     * scripts with no locale file. Kept in sync with PE.
     */
    public static final List<String> LOCALE_CODES_TO_EXCLUDE = List.of("ru_UA", "ccp_IN", "ccp_BD", "ii_CN", "sat_IN");

    private static final ObjectNode AVAILABLE_JAVA_LOCALE_CODES = JacksonUtil.newObjectNode();

    static {
        Arrays.stream(DateFormat.getAvailableLocales())
                .filter(locale -> StringUtils.countMatches(locale.toString(), "_") == 1
                        && !LOCALE_CODES_TO_EXCLUDE.contains(locale.toString())
                        && !locale.toString().matches(".*\\d+.*"))
                .forEach(locale -> AVAILABLE_JAVA_LOCALE_CODES.put(locale.toString(), displayName(locale)));
    }

    private final TranslationService translationService;

    @ApiOperation(value = "Get translation info (getTranslationInfos)",
            notes = "Fetch the list of available locales and corresponding details such as language display name, " +
                    "country display name and translation progress percentage.\n\nResponse example:\n\n" +
                    "```json\n[\n  {\n    \"localeCode\": \"uk_UA\",\n    \"language\": \"Ukrainian (українська)\",\n" +
                    "    \"country\": \"Україна\",\n    \"progress\": 32\n  }]\n```\n\n" +
                    "Security check is performed to verify that the user has 'READ' permission for the white labeling resource.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/translation/info")
    public List<TranslationInfo> getTranslationInfos() throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.READ);
        return translationService.getTranslationInfos(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId());
    }

    @ApiOperation(value = "Get list of available locales (getAvailableLocales)",
            notes = "Fetch the map of locale code to display name, covering bundled system locales and locales " +
                    "customized at any owner level.\n\n" +
                    "Security check is performed to verify that the user has 'READ' permission for the white labeling resource.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/translation/availableLocales")
    public JsonNode getAvailableLocales() throws ThingsboardException {
        ObjectNode result = JacksonUtil.newObjectNode();
        Set<String> availableLocaleCodes = translationService.getAvailableLocaleCodes(
                getCurrentUser().getTenantId(), getCurrentUser().getCustomerId());
        for (String localeCode : availableLocaleCodes) {
            result.put(localeCode, displayName(toLocale(localeCode)));
        }
        return result;
    }

    @ApiOperation(value = "Get list of available java locales (getAvailableJavaLocales)",
            notes = "The result is a map where the key is the locale code and the value is the locale language and country.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/translation/availableJavaLocales")
    public JsonNode getAvailableJavaLocales() {
        return AVAILABLE_JAVA_LOCALE_CODES;
    }

    @ApiOperation(value = "Get system translation for login page (getLoginPageTranslation)",
            notes = "Fetch the end-user translation of the login page for the specified locale. " +
                    "The owner is resolved from the login white labeling of the requested domain.")
    @GetMapping("/noauth/translation/login/{localeCode}")
    public void getLoginPageTranslation(@Parameter(description = "Locale code (e.g. 'en_US').") @PathVariable String localeCode,
                                        @RequestHeader(name = "If-None-Match", required = false) String etag,
                                        @RequestHeader(name = "Accept-Encoding", required = false) String acceptEncodingHeader,
                                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonNode loginTranslation = translationService.getLoginTranslation(localeCode, request.getServerName());
        writeTranslation(loginTranslation, etag, acceptEncodingHeader, response);
    }

    @ApiOperation(value = "Get end-user all-to-one translation (getFullTranslation)",
            notes = "Fetch the end-user translation for the specified locale. The result is the merge of the user " +
                    "custom translation, the system language translation and the default locale translation.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/translation/full/{localeCode}")
    public void getFullTranslation(@Parameter(description = "Locale code (e.g. 'en_US').") @PathVariable String localeCode,
                                   @RequestHeader(name = "If-None-Match", required = false) String etag,
                                   @RequestHeader(name = "Accept-Encoding", required = false) String acceptEncodingHeader,
                                   HttpServletResponse response) throws Exception {
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = getCurrentUser().getCustomerId();
        JsonNode fullTranslation = translationService.getFullTranslation(tenantId, customerId, localeCode);
        writeTranslation(fullTranslation, etag, acceptEncodingHeader, response);
    }

    @ApiOperation(value = "Get end-user multi-translation for basic edit (getTranslationForBasicEdit)",
            notes = "Fetch the translation info map where each value is an info object containing the key translation, " +
                    "origin translation, translation of the parent level and translation status.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/translation/edit/basic/{localeCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode getTranslationForBasicEdit(@Parameter(description = "Locale code (e.g. 'en_US').") @PathVariable String localeCode) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.READ);
        return translationService.getTranslationForBasicEdit(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), localeCode);
    }

    @ApiOperation(value = "Download end-user all-to-one translation (downloadFullTranslation)",
            notes = "Fetch the end-user translation for the specified locale as a downloadable json file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping("/translation/full/{localeCode}/download")
    public ResponseEntity<ByteArrayResource> downloadFullTranslation(@Parameter(description = "Locale code (e.g. 'en_US').") @PathVariable String localeCode) throws ThingsboardException {
        checkWhiteLabelingPermissions(Operation.READ);
        JsonNode fullTranslation = checkNotNull(translationService.getFullTranslation(
                getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), localeCode));
        String fileName = localeCode + "_custom_translation.json";
        ByteArrayResource translation = new ByteArrayResource(
                JacksonUtil.writeValueAsStringWithDefaultPrettyPrinter(fullTranslation).getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;filename=" + fileName)
                .header("x-filename", fileName)
                .contentLength(translation.contentLength())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(translation);
    }

    /**
     * Translation payloads are large (hundreds of KB) and change rarely, so they are served with a
     * content-derived ETag. Unlike PE there is no server-side ETag cache: the hash is always recomputed
     * from the freshly resolved translation, which cannot go stale after a custom translation is edited.
     */
    private void writeTranslation(JsonNode translation, String requestEtag, String acceptEncodingHeader,
                                  HttpServletResponse response) throws IOException {
        byte[] content = JacksonUtil.writeValueAsBytes(translation);
        String etag = Hashing.sha256().hashBytes(content).toString();
        if (StringUtils.isNotEmpty(requestEtag) && StringUtils.remove(requestEtag, '"').equals(etag)) {
            response.setStatus(HttpStatus.NOT_MODIFIED.value());
            return;
        }
        response.setHeader("ETag", etag);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        compressResponseWithGzipIFAccepted(acceptEncodingHeader, response, content);
    }

    private void checkWhiteLabelingPermissions(Operation operation) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.WHITE_LABELING, operation);
    }

    /** Parses "xx" or "xx_XX" into a Locale without going through the deprecated Locale constructors. */
    private static Locale toLocale(String localeCode) {
        return Locale.forLanguageTag(localeCode.replace('_', '-'));
    }

    private static String displayName(Locale locale) {
        String displayCountry = locale.getDisplayCountry(locale);
        return locale.getDisplayLanguage(locale) + (displayCountry.isBlank() ? "" : " (" + displayCountry + ")");
    }

}
