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
package org.thingsboard.server.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.translation.TranslationInfo;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTranslationService implements TranslationService {

    public static final String LOCALE_FILES_DIRECTORY_PATH = "public/assets/locale";
    public static final Pattern LOCALE_FILE_PATTERN = Pattern.compile("locale\\.constant-(.*?)\\.json");
    public static final String DEFAULT_LOCALE_CODE = "en_US";

    private static final List<String> LOGIN_PAGE_SECTIONS = List.of("login", "signup", "common", "action", "access");

    private final CustomTranslationService customTranslationService;
    private final WhiteLabelingService whiteLabelingService;
    private final CustomerService customerService;

    /**
     * Locale files ship with the Angular bundle (see ui-ngx/angular.json outputPath), so they are only on the
     * classpath when the UI module was built. A backend-only build must still start, hence these are loaded
     * lazily and fail soft - an empty map degrades every endpoint to "custom translation only" instead of
     * preventing the context from coming up.
     */
    private volatile Map<String, JsonNode> translationValueMap = Map.of();
    private volatile Map<String, TranslationInfo> translationInfoMap = Map.of();
    private volatile Set<String> defaultLocaleKeys = Set.of();

    @PostConstruct
    public void init() {
        Map<String, JsonNode> valueMap = new HashMap<>();
        Map<String, TranslationInfo> infoMap = new HashMap<>();

        JsonNode defaultTranslation = readResourceLocaleTranslation(DEFAULT_LOCALE_CODE);
        if (defaultTranslation == null) {
            log.warn("No bundled locale files found on the classpath under '{}'. " +
                    "Translation endpoints will only serve custom translations.", LOCALE_FILES_DIRECTORY_PATH);
            return;
        }
        defaultLocaleKeys = JacksonUtil.extractKeys(defaultTranslation);

        for (String localeCode : getAvailableResourceLocaleCodes()) {
            JsonNode resourceLocaleTranslation = readResourceLocaleTranslation(localeCode);
            if (resourceLocaleTranslation == null) {
                continue;
            }
            infoMap.put(localeCode, createTranslationInfo(defaultLocaleKeys, localeCode, resourceLocaleTranslation, false));
            valueMap.put(localeCode, resourceLocaleTranslation);
        }
        translationValueMap = Map.copyOf(valueMap);
        translationInfoMap = Map.copyOf(infoMap);
        log.info("Loaded {} bundled locale(s): {}", translationInfoMap.size(),
                translationInfoMap.keySet().stream().sorted().collect(Collectors.joining(", ")));
    }

    @Override
    public List<TranslationInfo> getTranslationInfos(TenantId tenantId, CustomerId customerId) {
        Map<String, TranslationInfo> translationInfos = new HashMap<>();
        translationInfoMap.forEach((localeCode, info) -> translationInfos.put(localeCode,
                new TranslationInfo(info.getLocaleCode(), info.getLanguage(), info.getCountry(), info.getProgress(), info.isCustomized())));

        Set<String> customizedLocales = customTranslationService.getMergedCustomizedLocales(tenantId, customerId);
        Set<String> currentCustomizedLocales = new HashSet<>(customTranslationService.getCustomizedLocales(tenantId, customerId));
        Set<String> engLocaleKeys = JacksonUtil.extractKeys(getFullTranslation(tenantId, customerId, DEFAULT_LOCALE_CODE));

        for (String customizedLocale : customizedLocales) {
            JsonNode customTranslation = customTranslationService.getMergedCustomTranslation(tenantId, customerId, customizedLocale);
            JsonNode resourceTranslation = readResourceLocaleTranslation(customizedLocale);
            if (resourceTranslation != null) {
                customTranslation = JacksonUtil.merge(resourceTranslation, customTranslation);
            }
            boolean customized = currentCustomizedLocales.contains(customizedLocale);
            translationInfos.put(customizedLocale, createTranslationInfo(engLocaleKeys, customizedLocale, customTranslation, customized));
        }
        return new ArrayList<>(translationInfos.values());
    }

    @Override
    public Set<String> getAvailableLocaleCodes(TenantId tenantId, CustomerId customerId) {
        Set<String> availableLocaleCodes = new HashSet<>(translationInfoMap.keySet());
        availableLocaleCodes.addAll(customTranslationService.getMergedCustomizedLocales(tenantId, customerId));
        return availableLocaleCodes;
    }

    @Override
    public JsonNode getLoginTranslation(String localeCode, String domainName) {
        JsonNode fullTranslation;
        WhiteLabeling whiteLabeling = whiteLabelingService.findWhiteLabelingByDomainAndType(domainName, WhiteLabelingType.LOGIN);
        if (whiteLabeling != null) {
            fullTranslation = getFullTranslation(whiteLabeling.getTenantId(), whiteLabeling.getCustomerId(), localeCode);
        } else {
            fullTranslation = getResourceTranslationOrDefault(localeCode);
        }

        ObjectNode loginPageTranslation = JacksonUtil.newObjectNode();
        for (String section : LOGIN_PAGE_SECTIONS) {
            copyIfPresent(fullTranslation, loginPageTranslation, section);
        }
        ObjectNode securityNode = JacksonUtil.newObjectNode();
        JsonNode security = fullTranslation.get("security");
        if (security != null) {
            copyIfPresent(security, securityNode, "2fa");
            copyIfPresent(security, securityNode, "password-requirement");
        }
        loginPageTranslation.set("security", securityNode);

        ObjectNode userNode = JacksonUtil.newObjectNode();
        JsonNode user = fullTranslation.get("user");
        if (user != null) {
            copyIfPresent(user, userNode, "invalid-email-format");
        }
        loginPageTranslation.set("user", userNode);
        return loginPageTranslation;
    }

    @Override
    public JsonNode getFullTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customTranslation = customTranslationService.getMergedCustomTranslation(tenantId, customerId, localeCode);
        JsonNode merged = JacksonUtil.merge(getResourceTranslationOrDefault(localeCode), customTranslation);
        // keys added by the customer only in the default locale must still surface in every other locale
        JsonNode defaultCustomTranslation = customTranslationService.getMergedCustomTranslation(tenantId, customerId, DEFAULT_LOCALE_CODE);
        addNonExisting(merged, defaultCustomTranslation);
        return merged;
    }

    @Override
    public JsonNode getTranslationForBasicEdit(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode fullTranslation = getFullTranslation(tenantId, customerId, localeCode).deepCopy();
        CustomTranslation current = customTranslationService.getCustomTranslation(tenantId, customerId, localeCode);
        JsonNode currentCustomTranslation = current != null ? current.getValue() : null;

        JsonNode originalTranslation = getTranslatedOnly(tenantId, customerId, DEFAULT_LOCALE_CODE, getResourceTranslation(DEFAULT_LOCALE_CODE));
        JsonNode parentOriginalTranslation = getParentTranslatedOnly(tenantId, customerId, DEFAULT_LOCALE_CODE, getResourceTranslation(DEFAULT_LOCALE_CODE));

        JsonNode translated = getTranslatedOnly(tenantId, customerId, localeCode, getResourceTranslation(localeCode));
        JsonNode parentTranslated = getParentTranslatedOnly(tenantId, customerId, localeCode, getResourceTranslation(localeCode));

        buildTranslationInfoForEdit(fullTranslation, translated, parentTranslated, originalTranslation, currentCustomTranslation, parentOriginalTranslation);
        return fullTranslation;
    }

    private JsonNode getTranslatedOnly(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode resourceTranslation) {
        JsonNode customTranslation = customTranslationService.getMergedCustomTranslation(tenantId, customerId, localeCode);
        return resourceTranslation != null ? JacksonUtil.merge(resourceTranslation, customTranslation) : customTranslation;
    }

    private JsonNode getParentTranslatedOnly(TenantId tenantId, CustomerId customerId, String localeCode, JsonNode resourceTranslation) {
        if (customerId != null && !customerId.isNullUid()) {
            Customer customer = customerService.findCustomerById(tenantId, customerId);
            if (customer != null && customer.isSubCustomer()) {
                return getTranslatedOnly(tenantId, customer.getParentCustomerId(), localeCode, resourceTranslation);
            }
            return getTranslatedOnly(tenantId, null, localeCode, resourceTranslation);
        }
        if (tenantId.isSysTenantId()) {
            return resourceTranslation;
        }
        return getTranslatedOnly(TenantId.SYS_TENANT_ID, null, localeCode, resourceTranslation);
    }

    /**
     * Rewrites every leaf of {@code fullTranslation} in place into an info object describing where its
     * current value came from. See {@link TranslationService#getTranslationForBasicEdit} for the key legend.
     */
    private void buildTranslationInfoForEdit(JsonNode fullTranslation, JsonNode translated, JsonNode parentTranslated,
                                             JsonNode original, JsonNode custom, JsonNode parentOrigin) {
        Iterator<String> fieldNames = fullTranslation.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fullNode = fullTranslation.get(fieldName);
            JsonNode customNode = custom == null ? null : custom.get(fieldName);
            JsonNode parentTranslatedNode = parentTranslated == null ? null : parentTranslated.get(fieldName);
            JsonNode originNode = original == null ? null : original.get(fieldName);
            JsonNode translatedNode = translated == null ? null : translated.get(fieldName);
            JsonNode parentOriginNode = parentOrigin == null ? null : parentOrigin.get(fieldName);

            if (fullNode.isObject()) {
                buildTranslationInfoForEdit(fullNode, translatedNode, parentTranslatedNode, originNode, customNode, parentOriginNode);
                continue;
            }

            ObjectNode info = JacksonUtil.newObjectNode();
            if (originNode != null) {
                info.put("o", originNode.asText());
            }
            if (parentTranslatedNode != null) {
                info.put("p", parentTranslatedNode.asText());
            }
            if (customNode != null) {
                info.put("s", "C");
                info.put("t", customNode.asText());
            } else if (translatedNode != null) {
                info.put("t", translatedNode.asText());
                info.put("s", "T");
            } else {
                info.put("s", "U");
            }
            if (parentTranslatedNode == null && parentOriginNode == null) {
                info.put("s", "A");
            }
            ((ObjectNode) fullTranslation).set(fieldName, info);
        }
    }

    /** Copies keys of {@code newNode} that {@code mainNode} does not have yet, leaving existing values untouched. */
    private void addNonExisting(JsonNode mainNode, JsonNode newNode) {
        if (mainNode == null || newNode == null) {
            return;
        }
        Iterator<String> fieldNames = newNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode mainValue = mainNode.get(fieldName);
            if (mainValue != null) {
                if (mainValue.isObject()) {
                    addNonExisting(mainValue, newNode.get(fieldName));
                } else if (mainValue.isArray()) {
                    JsonNode newValue = newNode.get(fieldName);
                    for (int i = 0; i < mainValue.size() && i < newValue.size(); i++) {
                        addNonExisting(mainValue.get(i), newValue.get(i));
                    }
                }
            } else if (mainNode instanceof ObjectNode) {
                JsonNode value = newNode.get(fieldName);
                if (!value.isNull()) {
                    ((ObjectNode) mainNode).set(fieldName, value);
                }
            }
        }
    }

    private static TranslationInfo createTranslationInfo(Set<String> engLocaleKeys, String localeCode, JsonNode translation, boolean customized) {
        Locale locale = Locale.forLanguageTag(localeCode.replace("_", "-"));
        return TranslationInfo.builder()
                .customized(customized)
                .localeCode(localeCode)
                .country(locale.getDisplayCountry())
                .language(StringUtils.capitalize(locale.getDisplayLanguage(locale) + " (" + locale.getDisplayLanguage() + ")"))
                .progress(calculateTranslationProgress(engLocaleKeys, translation))
                .build();
    }

    private static int calculateTranslationProgress(Set<String> defaultLocaleKeys, JsonNode translation) {
        if (defaultLocaleKeys.isEmpty()) {
            return 0;
        }
        Set<String> localeKeys = JacksonUtil.extractKeys(translation);
        long translated = defaultLocaleKeys.stream().filter(localeKeys::contains).count();
        return (int) (translated * 100L / defaultLocaleKeys.size());
    }

    /** Fresh mutable copy of the bundled translation for this locale, or null when the locale is custom-only. */
    private JsonNode getResourceTranslation(String localeCode) {
        JsonNode translation = translationValueMap.get(localeCode);
        return translation != null ? translation.deepCopy() : null;
    }

    /** Fresh mutable copy of the bundled translation, falling back to the default locale and then to an empty object. */
    private JsonNode getResourceTranslationOrDefault(String localeCode) {
        JsonNode translation = translationValueMap.getOrDefault(localeCode, translationValueMap.get(DEFAULT_LOCALE_CODE));
        return translation != null ? translation.deepCopy() : JacksonUtil.newObjectNode();
    }

    private static void copyIfPresent(JsonNode source, ObjectNode target, String field) {
        JsonNode value = source.get(field);
        if (value != null) {
            target.set(field, value);
        }
    }

    private static Set<String> getAvailableResourceLocaleCodes() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:" + LOCALE_FILES_DIRECTORY_PATH + "/*.json");
            return Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .map(DefaultTranslationService::getLocaleFromFileName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to list bundled locale files under '{}'", LOCALE_FILES_DIRECTORY_PATH, e);
            return Set.of();
        }
    }

    private static String getLocaleFromFileName(String filename) {
        if (filename == null) {
            return null;
        }
        Matcher matcher = LOCALE_FILE_PATTERN.matcher(filename);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static JsonNode readResourceLocaleTranslation(String localeCode) {
        String filePath = LOCALE_FILES_DIRECTORY_PATH + "/locale.constant-" + localeCode + ".json";
        try (InputStream in = DefaultTranslationService.class.getClassLoader().getResourceAsStream(filePath)) {
            if (in == null) {
                return null;
            }
            return JacksonUtil.OBJECT_MAPPER.readTree(in);
        } catch (Exception e) {
            log.warn("Failed to read locale translation for {}", localeCode, e);
            return null;
        }
    }

}
