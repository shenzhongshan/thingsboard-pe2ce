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

import {
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipGrid } from '@angular/material/chips';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { GROUP_ALL_NAME } from '@shared/models/entity-group.models';
import { PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { isEqual } from 'lodash';

export type CreateEntityGroupFunction = (groupType: EntityType, groupName: string,
                                         ownerId: EntityId) => Observable<EntityInfoData | null>;

/**
 * Chip list of entity groups of a single type, optionally scoped to one owner. PE parity:
 * `tb-entity-group-list`. Scoping matters — a group belongs to exactly one owner, so the picker
 * must re-list (and drop any existing selection) whenever the owner changes.
 */
@Component({
  selector: 'tb-entity-group-list',
  templateUrl: './entity-group-list.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityGroupListComponent),
      multi: true
    }
  ],
  standalone: false
})
export class EntityGroupListComponent implements ControlValueAccessor, OnInit, OnChanges {

  entityGroupListFormGroup: UntypedFormGroup;

  private modelValue: Array<string> | null;

  @Input()
  groupType: EntityType;

  private ownerIdValue: EntityId;
  get ownerId(): EntityId {
    return this.ownerIdValue;
  }
  @Input()
  set ownerId(value: EntityId) {
    if (!isEqual(this.ownerIdValue, value)) {
      this.ownerIdValue = value;
      this.reset();
    }
  }

  @Input()
  labelText: string;

  @Input()
  placeholderText = '';

  @Input()
  requiredText = 'entity-group.entity-group-list-empty';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  /** Hides the automatic 'All' group — membership there is implied by ownership, not assignable. */
  @Input()
  @coerceBoolean()
  excludeGroupAll = false;

  /** Emits the selected groups as id/name pairs rather than bare ids. */
  @Input()
  @coerceBoolean()
  useGroupInfoValue = false;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  /** When set, the autocomplete offers a "create a new one" action for the typed name. */
  @Input()
  createGroupFunction: CreateEntityGroupFunction;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @ViewChild('entityGroupInput') entityGroupInput: ElementRef<HTMLInputElement>;
  @ViewChild('entityGroupAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('chipList', {static: true}) chipList: MatChipGrid;

  entityGroups: Array<EntityInfoData> = [];
  filteredEntityGroups: Observable<Array<EntityInfoData>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_v: any) => { };

  constructor(private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder) {
    this.entityGroupListFormGroup = this.fb.group({
      entityGroups: [this.entityGroups],
      entityGroup: [null]
    });
  }

  ngOnInit() {
    this.filteredEntityGroups = this.entityGroupListFormGroup.get('entityGroup').valueChanges
      .pipe(
        tap((value) => {
          if (value && typeof value !== 'string') {
            this.add(value);
          } else if (value === null) {
            this.clear(this.entityGroupInput.nativeElement.value);
          }
        }),
        filter((value) => typeof value === 'string'),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchEntityGroups(name)),
        share()
      );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue && propName === 'groupType') {
        this.reset();
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityGroupListFormGroup.disable({emitEvent: false});
    } else {
      this.entityGroupListFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<EntityInfoData> | Array<string> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      if ((value[0] as EntityInfoData).id) {
        const groups = value as Array<EntityInfoData>;
        this.modelValue = groups.map(group => group.id.id);
        this.entityGroups = [...groups];
        this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      } else {
        const ids = value as Array<string>;
        this.modelValue = [...ids];
        this.entityGroupService.getEntityGroupEntityInfosByIds(ids, {ignoreLoading: true}).subscribe(groups => {
          this.entityGroups = groups;
          this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
        });
      }
    } else {
      this.entityGroups = [];
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  remove(entityGroup: EntityInfoData) {
    const index = this.entityGroups.indexOf(entityGroup);
    if (index >= 0) {
      this.entityGroups.splice(index, 1);
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
      this.modelValue.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.updateView();
      this.clear();
    }
  }

  onFocus() {
    if (this.dirty) {
      this.entityGroupListFormGroup.get('entityGroup').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayEntityGroupFn(entityGroup?: EntityInfoData): string | undefined {
    return entityGroup ? entityGroup.name : undefined;
  }

  textIsNotEmpty(text: string): boolean {
    return !!(text && text.length > 0);
  }

  createEntityGroup($event: Event, groupName: string) {
    $event.preventDefault();
    if (!this.createGroupFunction) {
      return;
    }
    this.createGroupFunction(this.groupType, groupName, this.ownerId).subscribe(created => {
      if (created) {
        this.add(created);
      } else {
        setTimeout(() => {
          this.entityGroupInput.nativeElement.blur();
          this.entityGroupInput.nativeElement.focus();
        }, 0);
      }
    });
  }

  private updateValidators() {
    this.entityGroupListFormGroup.get('entityGroups').setValidators(this.required ? [Validators.required] : []);
    this.entityGroupListFormGroup.get('entityGroups').updateValueAndValidity({emitEvent: false});
  }

  private reset() {
    this.entityGroups = [];
    this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    this.modelValue = null;
    if (this.entityGroupInput) {
      this.entityGroupInput.nativeElement.value = '';
    }
    this.entityGroupListFormGroup.get('entityGroup').patchValue('', {emitEvent: false});
    this.updateView();
    this.dirty = true;
  }

  private add(entityGroup: EntityInfoData) {
    if (!this.modelValue || this.modelValue.indexOf(entityGroup.id.id) === -1) {
      if (!this.modelValue) {
        this.modelValue = [];
      }
      this.modelValue.push(entityGroup.id.id);
      this.entityGroups.push(entityGroup);
      this.entityGroupListFormGroup.get('entityGroups').setValue(this.entityGroups);
    }
    this.updateView();
    this.clear();
  }

  private updateView() {
    this.propagateChange(this.useGroupInfoValue ? this.entityGroups : this.modelValue);
  }

  /** Resets the typed text and re-triggers the autocomplete so the panel reflects the new selection. */
  private clear(value: string = '') {
    if (!this.entityGroupInput) {
      return;
    }
    this.entityGroupInput.nativeElement.value = value;
    this.entityGroupListFormGroup.get('entityGroup').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.entityGroupInput.nativeElement.blur();
      this.entityGroupInput.nativeElement.focus();
    }, 0);
  }

  private fetchEntityGroups(searchText?: string): Observable<Array<EntityInfoData>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {property: 'name', direction: Direction.ASC});
    return this.getEntityGroups(pageLink).pipe(
      catchError(() => of(null)),
      map(pageData => {
        let groups = pageData ? pageData.data : [];
        if (this.excludeGroupAll) {
          groups = groups.filter(group => group.name !== GROUP_ALL_NAME);
        }
        if (this.modelValue) {
          groups = groups.filter(group => !this.modelValue.includes(group.id.id));
        }
        return groups;
      })
    );
  }

  private getEntityGroups(pageLink: PageLink): Observable<PageData<EntityInfoData>> {
    if (this.ownerId) {
      return this.entityGroupService.getEntityGroupEntityInfosByOwnerId(pageLink, this.ownerId, this.groupType,
        {ignoreLoading: true});
    }
    return this.entityGroupService.getEntityGroupsByType(this.groupType, pageLink, null, {ignoreLoading: true}).pipe(
      map(pageData => ({...pageData, data: pageData.data.map(group => ({id: group.id, name: group.name} as EntityInfoData))}))
    );
  }
}
