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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.LibraryConvertersInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Serves the data-converters library out of a locally synced clone.
 * <p>
 * The repository is a fixed directory tree, so everything here is a walk at a known depth:
 * {@code VENDORS/<vendor>(1)/<model>(2)/<INTEGRATION>(3)/<direction>(4)}, with {@code logo.svg} at
 * the vendor level and {@code info.json} plus {@code photo.png} at the model level.
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultConverterLibraryService implements ConverterLibraryService {

    private static final String REPO_KEY = "data-converters-library";
    private static final String VENDORS_DIR = "VENDORS";
    private static final int VENDOR_DEPTH = 1;
    private static final int MODEL_DEPTH = 2;
    private static final int INTEGRATION_DEPTH = 3;
    private static final int DIRECTION_DEPTH = 4;

    private final GitSyncService gitSyncService;

    @Value("${integrations.converters.library.enabled:true}")
    private boolean enabled;
    @Value("${integrations.converters.library.url:https://github.com/thingsboard/data-converters.git}")
    private String repoUrl;
    @Value("${integrations.converters.library.branch:release/4.0.0}")
    private String branch;
    @Value("${integrations.converters.library.fetch_frequency:24}")
    private int fetchFrequencyHours;

    private volatile Map<String, LibraryConvertersInfo> convertersInfo;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() {
        if (!enabled) {
            log.info("Data converters library is disabled");
            return;
        }
        try {
            gitSyncService.registerSync(REPO_KEY, repoUrl, branch, TimeUnit.HOURS.toMillis(fetchFrequencyHours),
                    this::updateConvertersInfo);
        } catch (Exception e) {
            // The library is a convenience: without it integrations still work, they just have no
            // ready-made converters to pick from. Never let it stop the platform from starting.
            log.warn("Failed to sync data converters library from {}", repoUrl, e);
        }
    }

    @Override
    public List<Vendor> getVendors(IntegrationType integrationType, String converterType, int page, int pageSize, boolean loadImages) {
        log.trace("Executing getVendors [{}]", integrationType);
        return listDirs(VENDORS_DIR, VENDOR_DEPTH).stream()
                .filter(vendorDir -> hasIntegration(vendorDir, integrationType, converterType))
                .sorted(Comparator.comparing(RepoFile::name))
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(vendorDir -> new Vendor(vendorDir.name(),
                        loadImages ? loadImage(vendorDir.path(), MODEL_DEPTH, "logo", "image/svg+xml") : null))
                .toList();
    }

    @Override
    public List<Model> getVendorModels(IntegrationType integrationType, String converterType, String vendorName,
                                       int page, int pageSize, boolean loadImages) {
        log.trace("Executing getVendorModels [{}][{}][{}]", integrationType, converterType, vendorName);
        return listDirs(join(VENDORS_DIR, vendorName), INTEGRATION_DEPTH).stream()
                .filter(integrationDir -> matchesIntegration(integrationDir, integrationType, converterType))
                // The match is on the integration directory; the model is its parent.
                .map(integrationDir -> parentOf(integrationDir.path()))
                .sorted()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(modelPath -> new Model(
                        nameOf(modelPath),
                        JacksonUtil.toJsonNode(getFileContent(join(modelPath, "info.json"))),
                        loadImages ? loadImage(modelPath, INTEGRATION_DEPTH, "photo", "image/png") : null))
                .toList();
    }

    @Override
    public String getConverter(IntegrationType integrationType, String converterType, String vendorName, String model) {
        log.trace("Executing getConverter [{}][{}][{}][{}]", integrationType, converterType, vendorName, model);
        return getFileContent(join(converterDir(integrationType, converterType, vendorName, model), "converter.json"));
    }

    @Override
    public String getConverterMetadata(IntegrationType integrationType, String converterType, String vendorName, String model) {
        log.trace("Executing getConverterMetadata [{}][{}][{}][{}]", integrationType, converterType, vendorName, model);
        return getFileContent(join(converterDir(integrationType, converterType, vendorName, model), "metadata.json"));
    }

    @Override
    public String getPayload(IntegrationType integrationType, String converterType, String vendorName, String model) {
        log.trace("Executing getPayload [{}][{}][{}][{}]", integrationType, converterType, vendorName, model);
        List<RepoFile> payloads = listFiles(converterDir(integrationType, converterType, vendorName, model), -1, FileType.FILE).stream()
                .filter(file -> file.name().startsWith("payload"))
                .sorted(Comparator.comparing(RepoFile::name))
                .toList();
        if (payloads.isEmpty()) {
            return "{}";
        }
        // Prefer a JSON payload; some models only ship a binary/hex example.
        RepoFile payload = payloads.stream()
                .filter(file -> file.name().endsWith("json"))
                .findFirst()
                .orElse(payloads.get(0));
        return getFileContent(payload.path());
    }

    @Override
    public Map<String, LibraryConvertersInfo> getConvertersInfo() {
        return convertersInfo != null ? convertersInfo : Collections.emptyMap();
    }

    /** Recomputed on every repo update: which directions exist per integration-type directory. */
    private void updateConvertersInfo() {
        log.debug("Updating converters info");
        convertersInfo = listDirs(VENDORS_DIR, DIRECTION_DEPTH).stream()
                .collect(Collectors.groupingBy(
                        direction -> nameOf(parentOf(direction.path())),
                        Collectors.collectingAndThen(Collectors.toList(), directions -> new LibraryConvertersInfo(
                                hasDirection(directions, ConverterType.UPLINK),
                                hasDirection(directions, ConverterType.DOWNLINK)))));
    }

    private static boolean hasDirection(List<RepoFile> directions, ConverterType converterType) {
        return directions.stream().anyMatch(dir -> converterType.getDirectory().equals(dir.name()));
    }

    /** True when the vendor has at least one model supporting the integration (and direction). */
    private boolean hasIntegration(RepoFile vendorDir, IntegrationType integrationType, String converterType) {
        Set<String> supported = listDirs(vendorDir.path(), INTEGRATION_DEPTH).stream()
                .filter(integrationDir -> matchesIntegration(integrationDir, integrationType, converterType))
                .map(RepoFile::name)
                .collect(Collectors.toSet());
        return supported.contains(integrationType.getDirectory());
    }

    private boolean matchesIntegration(RepoFile integrationDir, IntegrationType integrationType, String converterType) {
        if (!integrationDir.name().equals(integrationType.getDirectory())) {
            return false;
        }
        if (StringUtils.isEmpty(converterType)) {
            return true;
        }
        return listDirs(integrationDir.path(), DIRECTION_DEPTH).stream()
                .anyMatch(direction -> direction.name().equals(converterType));
    }

    private String loadImage(String dir, int depth, String prefix, String mediaType) {
        String file = listFiles(dir, depth, FileType.FILE).stream()
                .filter(candidate -> candidate.name().startsWith(prefix))
                .findFirst()
                .map(RepoFile::path)
                .orElse(null);
        if (file == null) {
            return null;
        }
        try {
            return "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(gitSyncService.getFileContent(REPO_KEY, file));
        } catch (Exception e) {
            log.warn("Failed to read image {}: {}", file, e.getMessage());
            return null;
        }
    }

    private static String converterDir(IntegrationType integrationType, String converterType, String vendorName, String model) {
        return join(VENDORS_DIR, vendorName, model, integrationType.getDirectory(), converterType);
    }

    /**
     * Repository paths always use forward slashes, so they are handled as plain strings — building
     * them with {@link java.nio.file.Path} yields backslashes on Windows and every lookup misses.
     */
    private static String join(String... parts) {
        return String.join("/", parts);
    }

    private static String parentOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash > 0 ? path.substring(0, slash) : "";
    }

    private static String nameOf(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private List<RepoFile> listDirs(String path, int depth) {
        return listFiles(path, depth, FileType.DIRECTORY);
    }

    private List<RepoFile> listFiles(String path, int depth, FileType type) {
        checkEnabled();
        return gitSyncService.listFiles(REPO_KEY, path, depth, type);
    }

    private String getFileContent(String path) {
        checkEnabled();
        try {
            return new String(gitSyncService.getFileContent(REPO_KEY, path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // A model may legitimately omit metadata or an example payload.
            log.warn("Failed to get file content for path {}: {}", path, e.getMessage());
            return "{}";
        }
    }

    private void checkEnabled() {
        if (!enabled) {
            throw new IllegalArgumentException("Data converters library is disabled");
        }
    }

}
