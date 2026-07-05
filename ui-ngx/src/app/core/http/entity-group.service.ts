///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroup } from '@shared/models/entity-group.models';

@Injectable({
  providedIn: 'root'
})
export class EntityGroupService {

  constructor(private http: HttpClient) {
  }

  public getEntityGroupsByType(groupType: EntityType, pageLink: PageLink,
                               config?: RequestConfig): Observable<PageData<EntityGroup>> {
    return this.http.get<PageData<EntityGroup>>(`/api/entityGroups/${groupType}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroup(entityGroupId: string, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.get<EntityGroup>(`/api/entityGroup/${entityGroupId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveEntityGroup(entityGroup: EntityGroup, config?: RequestConfig): Observable<EntityGroup> {
    return this.http.post<EntityGroup>('/api/entityGroup', entityGroup, defaultHttpOptionsFromConfig(config));
  }

  public deleteEntityGroup(entityGroupId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/entityGroup/${entityGroupId}`, defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupEntities<T>(entityGroupId: string, pageLink: PageLink,
                                   config?: RequestConfig): Observable<PageData<T>> {
    return this.http.get<PageData<T>>(`/api/entityGroup/${entityGroupId}/entities${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public addEntitiesToEntityGroup(entityGroupId: string, entityIds: string[],
                                  config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/entityGroup/${entityGroupId}/addEntities`, entityIds,
      defaultHttpOptionsFromConfig(config));
  }

  public removeEntitiesFromEntityGroup(entityGroupId: string, entityIds: string[],
                                       config?: RequestConfig): Observable<void> {
    return this.http.post<void>(`/api/entityGroup/${entityGroupId}/deleteEntities`, entityIds,
      defaultHttpOptionsFromConfig(config));
  }

  public makeEntityGroupPublic(entityGroupId: string, isPublic: boolean,
                               config?: RequestConfig): Observable<EntityGroup> {
    return this.http.post<EntityGroup>(`/api/entityGroup/${entityGroupId}/makePublic?isPublic=${isPublic}`, null,
      defaultHttpOptionsFromConfig(config));
  }

}
