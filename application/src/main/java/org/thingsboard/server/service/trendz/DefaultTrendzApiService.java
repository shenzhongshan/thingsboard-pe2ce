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
package org.thingsboard.server.service.trendz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzUsage;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTrendzApiService implements TrendzApiService {

    private final TrendzClient trendzClient;

    @Override
    public TrendzViewConfig getViewById(User user, UUID viewId) throws ThingsboardException {
        return trendzClient.getTrendzViewById(viewId, user);
    }

    @Override
    public PageData<TrendzViewConfigLite> getAllViews(User user, PageLink pageLink) throws ThingsboardException {
        return trendzClient.getAllTrendzViews(pageLink, user).toPageData();
    }

    @Override
    public TrendzSummary getTrendzSummary(User user) throws ThingsboardException {
        return trendzClient.getTrendzSummary(user);
    }

    @Override
    public TrendzUsage getTrendzUsage(User user) throws ThingsboardException {
        return trendzClient.getTrendzUsage(user);
    }

}
