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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

/**
 * CE port of PE's blob entity API — listing, download and deletion of stored binary artifacts
 * (generated report files and similar). Blobs are produced by the platform, so there is no
 * create endpoint, matching PE.
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class BlobEntityController extends BaseController {

    public static final String BLOB_ENTITY_ID = "blobEntityId";

    private final BlobEntityService blobEntities;

    @ApiOperation(value = "Get Blob Entity Info (getBlobEntityInfoById)",
            notes = "Fetch blob metadata (without the payload) by id.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntity/info/{blobEntityId}")
    public BlobEntityWithCustomerInfo getBlobEntityInfoById(
            @Parameter(description = "A string value representing the blob entity id", required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        return checkBlobEntityInfoId(new BlobEntityId(toUUID(strBlobEntityId)), Operation.READ);
    }

    @ApiOperation(value = "Download Blob Entity (downloadBlobEntity)",
            notes = "Downloads the stored file. The blob's name is used as the attachment file name.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntity/{blobEntityId}/download")
    public ResponseEntity<ByteArrayResource> downloadBlobEntity(
            @Parameter(description = "A string value representing the blob entity id", required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        BlobEntity blobEntity = checkBlobEntityId(new BlobEntityId(toUUID(strBlobEntityId)), Operation.READ);
        if (blobEntity.getData() == null) {
            throw new ThingsboardException("Blob entity has no data!", org.thingsboard.server.common.data.exception.ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        ByteArrayResource resource = new ByteArrayResource(blobEntity.getData().array());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(blobEntity.getName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header("Content-Disposition", contentDisposition.toString())
                .header("x-filename", blobEntity.getName())
                .contentLength(resource.contentLength())
                .contentType(parseMediaType(blobEntity.getContentType()))
                .body(resource);
    }

    @ApiOperation(value = "Delete Blob Entity (deleteBlobEntity)", notes = "Deletes the blob entity by id.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/blobEntity/{blobEntityId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteBlobEntity(
            @Parameter(description = "A string value representing the blob entity id", required = true)
            @PathVariable(BLOB_ENTITY_ID) String strBlobEntityId) throws ThingsboardException {
        checkParameter(BLOB_ENTITY_ID, strBlobEntityId);
        BlobEntityId blobEntityId = new BlobEntityId(toUUID(strBlobEntityId));
        checkBlobEntityInfoId(blobEntityId, Operation.DELETE);
        blobEntities.deleteBlobEntity(getTenantId(), blobEntityId);
    }

    @ApiOperation(value = "Get Blob Entities (getBlobEntities)",
            notes = "Returns a page of blob metadata, newest first. Customer users only see their own blobs.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntities", params = {"pageSize", "page"})
    public PageData<BlobEntityWithCustomerInfo> getBlobEntities(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Optional blob entity type filter") @RequestParam(required = false) String type,
            @Parameter(description = "Case-insensitive 'substring' filter based on blob name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = "Start of the time window (createdTime)") @RequestParam(required = false) Long startTime,
            @Parameter(description = "End of the time window (createdTime)") @RequestParam(required = false) Long endTime) throws ThingsboardException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.BLOB_ENTITY, Operation.READ);
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        // A customer user is confined to its own blobs; a tenant admin sees the whole tenant.
        CustomerId customerId = currentUser.isCustomerUser() ? currentUser.getCustomerId() : null;
        return blobEntities.findBlobEntitiesByTenantId(getTenantId(), customerId, type, pageLink);
    }

    @ApiOperation(value = "Get Blob Entities By Ids (getBlobEntitiesByIds)",
            notes = "Returns blob metadata for the requested ids that the user is allowed to read.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/blobEntities", params = {"blobEntityIds"})
    public List<BlobEntityInfo> getBlobEntitiesByIds(
            @Parameter(description = "A list of blob entity ids, separated by comma", required = true)
            @RequestParam("blobEntityIds") String[] strBlobEntityIds) throws ThingsboardException {
        checkArrayParameter("blobEntityIds", strBlobEntityIds);
        accessControlService.checkPermission(getCurrentUser(), Resource.BLOB_ENTITY, Operation.READ);
        List<BlobEntityId> blobEntityIds = new ArrayList<>(strBlobEntityIds.length);
        for (String strBlobEntityId : strBlobEntityIds) {
            blobEntityIds.add(new BlobEntityId(toUUID(strBlobEntityId)));
        }
        return blobEntities.findBlobEntityInfoByIds(getTenantId(), blobEntityIds);
    }

}
