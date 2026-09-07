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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { merge, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, map, mergeMap, share, tap } from 'rxjs/operators';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { isEqual } from '@core/utils';

/**
 * Autocomplete over the entity groups of one {@link groupType}, optionally scoped to one owner.
 * PE parity: `tb-entity-group-autocomplete`.
 *
 * The model is the group's UUID string, or the full `{entityType: ENTITY_GROUP, id}` when
 * `useFullEntityId` is set — the two shapes PE supports.
 */
@Component({
  selector: 'tb-entity-group-autocomplete',
  templateUrl: './entity-group-autocomplete.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityGroupAutocompleteComponent),
      multi: true
    }
  ],
  standalone: false
})
export class EntityGroupAutocompleteComponent implements ControlValueAccessor, OnInit, OnDestroy {

  selectEntityGroupFormGroup: UntypedFormGroup;

  modelValue: string | EntityId | null = null;

  private groupTypeValue: EntityType;

  get groupType(): EntityType {
    return this.groupTypeValue;
  }

  @Input()
  set groupType(value: EntityType) {
    if (this.groupTypeValue !== value) {
      // Switching the type invalidates whatever group is currently held.
      if (this.groupTypeValue) {
        this.reset();
      }
      this.groupTypeValue = value;
    }
  }

  private ownerIdValue: EntityId = null;

  get ownerId(): EntityId {
    return this.ownerIdValue;
  }

  @Input()
  set ownerId(value: EntityId) {
    if (!isEqual(this.ownerIdValue, value)) {
      this.ownerIdValue = value;
      // Keep an already resolved group (writeValue path) but drop a stale search list.
      const keepValue = this.getCurrentEntityGroup() === null;
      this.reset(keepValue);
    }
  }

  @Input()
  excludeGroupIds: string[];

  @Input()
  @coerceBoolean()
  excludeGroupAll: boolean;

  @Input()
  placeholderText = 'entity-group.entity-group';

  @Input()
  notFoundText = 'entity-group.no-entity-groups-matching';

  @Input()
  requiredText = 'entity-group.entity-group-required';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  useFullEntityId: boolean;

  @Output()
  entityGroupLoaded = new EventEmitter<EntityInfoData>();

  @ViewChild('entityGroupInput', {static: true}) entityGroupInput: ElementRef<HTMLInputElement>;

  filteredEntityGroups: Observable<Array<EntityInfoData>>;

  searchText = '';

  private pristine = true;

  private cleanFilteredEntityGroups = new Subject<Array<EntityInfoData>>();

  private propagateChange = (_v: any) => { };

  constructor(public translate: TranslateService,
              private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder) {
    this.selectEntityGroupFormGroup = this.fb.group({
      entityGroup: [null]
    });
  }

  ngOnInit() {
    const fetched = this.selectEntityGroupFormGroup.get('entityGroup').valueChanges.pipe(
      debounceTime(150),
      tap((value: EntityInfoData | string) => {
        const modelValue = (typeof value === 'string' || !value) ? null :
          (this.useFullEntityId ? value.id : value.id.id);
        this.updateView(modelValue, typeof value === 'string' ? null : value);
        if (value === null) {
          this.clear();
        }
      }),
      map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
      mergeMap(name => this.fetchEntityGroups(name)),
      share()
    );
    this.filteredEntityGroups = merge(this.cleanFilteredEntityGroups, fetched);
  }

  ngOnDestroy() {
    this.cleanFilteredEntityGroups.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.selectEntityGroupFormGroup.disable({emitEvent: false});
    } else {
      this.selectEntityGroupFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityInfoData | EntityId | string | null): void {
    this.searchText = '';
    if (value === null || value === undefined || value === '') {
      this.modelValue = null;
      this.selectEntityGroupFormGroup.get('entityGroup').patchValue(null, {emitEvent: false});
      this.entityGroupLoaded.next(null);
    } else if ((value as EntityInfoData)?.id?.id && (value as EntityInfoData).name) {
      const info = value as EntityInfoData;
      this.modelValue = this.useFullEntityId ? info.id : info.id.id;
      this.selectEntityGroupFormGroup.get('entityGroup').patchValue(info, {emitEvent: false});
      this.entityGroupLoaded.next(info);
    } else {
      // Only an id — look the group up so the field can show its name (and so the parent learns its type/owner).
      const groupId = typeof value === 'string' ? value : (value as EntityId).id;
      this.entityGroupService.getEntityGroup(groupId, {ignoreLoading: true, ignoreErrors: true}).subscribe({
        next: (group) => {
          const info: EntityInfoData = {id: group.id, name: group.name};
          this.modelValue = this.useFullEntityId ? group.id : group.id.id;
          this.ownerIdValue = group.ownerId;
          this.groupTypeValue = group.type;
          this.selectEntityGroupFormGroup.get('entityGroup').patchValue(info, {emitEvent: false});
          this.entityGroupLoaded.next(info);
        },
        error: () => {
          this.modelValue = null;
          this.selectEntityGroupFormGroup.get('entityGroup').patchValue(null, {emitEvent: false});
          this.entityGroupLoaded.next(null);
        }
      });
    }
    this.pristine = true;
  }

  onFocus() {
    if (this.pristine) {
      this.selectEntityGroupFormGroup.get('entityGroup').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.pristine = false;
    }
  }

  clear() {
    // Must be '' not null: the ngOnInit pipe above calls clear() again whenever the emitted value is
    // strictly null, so patching null here would re-trigger itself on every debounce tick forever.
    this.selectEntityGroupFormGroup.get('entityGroup').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.entityGroupInput.nativeElement.blur();
      this.entityGroupInput.nativeElement.focus();
    }, 0);
  }

  displayEntityGroupFn(group?: EntityInfoData): string | undefined {
    return group ? group.name : undefined;
  }

  textIsNotEmpty(text: string): boolean {
    return !!(text && text.length > 0);
  }

  private getCurrentEntityGroup(): EntityInfoData | null {
    const value = this.selectEntityGroupFormGroup.get('entityGroup').value;
    return value && typeof value !== 'string' ? value : null;
  }

  private reset(keepValue = false) {
    this.cleanFilteredEntityGroups.next([]);
    this.pristine = true;
    if (!keepValue) {
      this.selectEntityGroupFormGroup.get('entityGroup').patchValue(null, {emitEvent: false});
      setTimeout(() => this.updateView(null, null));
    }
  }

  private updateView(value: string | EntityId | null, group: EntityInfoData | null) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
      if (group) {
        this.entityGroupLoaded.next(group);
      }
    }
  }

  private fetchEntityGroups(searchText?: string): Observable<Array<EntityInfoData>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {property: 'name', direction: Direction.ASC});
    return this.getEntityGroups(pageLink).pipe(
      catchError(() => of(emptyPageData<EntityInfoData>())),
      map(pageData => {
        let groups = pageData.data;
        if (this.excludeGroupAll) {
          groups = groups.filter(group => group.name !== 'All');
        }
        if (this.excludeGroupIds && this.excludeGroupIds.length) {
          groups = groups.filter(group => this.excludeGroupIds.indexOf(group.id.id) === -1);
        }
        return groups;
      })
    );
  }

  private getEntityGroups(pageLink: PageLink): Observable<PageData<EntityInfoData>> {
    if (!this.groupType) {
      return of(emptyPageData<EntityInfoData>());
    }
    if (this.ownerId) {
      return this.entityGroupService.getEntityGroupEntityInfosByOwnerId(pageLink, this.ownerId, this.groupType,
        {ignoreLoading: true, ignoreErrors: true});
    }
    return this.entityGroupService.getEntityGroupsByType(this.groupType, pageLink, null,
      {ignoreLoading: true, ignoreErrors: true}).pipe(
      map(pageData => ({
        ...pageData,
        data: pageData.data.map(group => ({id: group.id, name: group.name} as EntityInfoData))
      }))
    );
  }
}
