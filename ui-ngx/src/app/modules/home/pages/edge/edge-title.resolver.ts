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
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { EdgeService } from '@core/http/edge.service';

/**
 * 解析出 edge 的名字，供 edge 详情页各子页面的面包屑用（“<edge 名>: 设备组”）。
 * 对齐 PE 的同名 resolver —— 它的每个 edge 子路由都 resolve 一个 edgeTitle。
 */
@Injectable()
export class EdgeTitleResolver implements Resolve<string> {

  constructor(private edgeService: EdgeService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<string> {
    // edgeId 在路由段 ':edgeId/xxx' 上；resolver 跑在该路由上，params 直接可取。
    const edgeId = route.params.edgeId;
    return edgeId ? this.edgeService.getEdge(edgeId).pipe(map(edge => edge.name)) : of(null);
  }
}
