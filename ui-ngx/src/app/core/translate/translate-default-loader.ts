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

import { TranslateLoader, TranslationObject } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TranslateDefaultLoader implements TranslateLoader {

  constructor(private http: HttpClient) {

  }

  getTranslation(lang: string): Observable<TranslationObject> {
    return this.http.get<TranslationObject>(`/assets/locale/locale.constant-${lang}.json`).pipe(
      mergeMap((base) => {
        // Custom translation overrides only apply once authenticated — an anonymous/expired
        // token still hits the endpoint but gracefully falls back to the base locale on 401/403.
        if (!localStorage.getItem('jwt_token')) {
          return of(base);
        }
        return this.http.get<TranslationObject>(`/api/translation/custom/merged/${lang}`).pipe(
          map((custom) => deepMergeTranslations(base, custom)),
          catchError(() => of(base))
        );
      })
    );
  }
}

function deepMergeTranslations(base: TranslationObject, override: TranslationObject): TranslationObject {
  if (!override || typeof override !== 'object') {
    return base;
  }
  const result: TranslationObject = {...base};
  for (const key of Object.keys(override)) {
    const overrideValue = override[key];
    const baseValue = result[key];
    if (overrideValue && typeof overrideValue === 'object' && !Array.isArray(overrideValue) &&
      baseValue && typeof baseValue === 'object' && !Array.isArray(baseValue)) {
      result[key] = deepMergeTranslations(baseValue as TranslationObject, overrideValue as TranslationObject);
    } else {
      result[key] = overrideValue;
    }
  }
  return result;
}
