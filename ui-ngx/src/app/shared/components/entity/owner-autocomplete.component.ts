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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityId } from '@shared/models/id/entity-id';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';

/**
 * Single-field picker over the owners the current user may address — the tenant itself plus its
 * customers (`/api/entityGroup/owners`). PE parity: `tb-owner-autocomplete`; it deliberately shows
 * one field, not the entity-type + entity pair that `tb-entity-select` renders.
 *
 * The model is an {@link EntityInfoData} when `useOwnerInfoValue` is set, otherwise a bare
 * {@link EntityId} — same two shapes PE supports.
 */
@Component({
  selector: 'tb-owner-autocomplete',
  templateUrl: './owner-autocomplete.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OwnerAutocompleteComponent),
      multi: true
    }
  ],
  standalone: false
})
export class OwnerAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectOwnerFormGroup: UntypedFormGroup;

  modelValue: EntityInfoData | EntityId | null;

  @Input()
  placeholderText = 'entity-group.owner';

  @Input()
  notFoundText = 'entity-group.no-owners-text';

  @Input()
  noOwnersMatchingText = 'entity-group.no-owners-matching';

  @Input()
  requiredText = 'entity-group.owner-required';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  /** Pre-selects this owner when the control has no value yet. */
  @Input()
  defaultOwnerId: EntityId;

  /** Owner ids to drop from the suggestion list (PE hides the tenant itself in group-owner mode). */
  @Input()
  excludeOwnerIds: string[];

  @Input()
  @coerceBoolean()
  useOwnerInfoValue = false;

  @Input()
  @coerceBoolean()
  disabled: boolean;

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

  @ViewChild('ownerInput', {static: true}) ownerInput: ElementRef<HTMLInputElement>;
  @ViewChild('ownerAutocomplete') matAutocomplete: MatAutocomplete;

  filteredOwners: Observable<Array<EntityInfoData>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_v: any) => { };

  constructor(private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder) {
    this.selectOwnerFormGroup = this.fb.group({
      owner: [null]
    });
  }

  ngOnInit() {
    this.filteredOwners = this.selectOwnerFormGroup.get('owner').valueChanges
      .pipe(
        debounceTime(150),
        tap((value: EntityInfoData | string) => {
          let modelValue: EntityInfoData | null;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        filter(value => typeof value === 'string'),
        map(value => value ? value as string : ''),
        distinctUntilChanged(),
        mergeMap(name => this.fetchOwners(name)),
        share()
      );
    if (this.defaultOwnerId && !this.modelValue) {
      this.applyDefaultOwner();
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
      this.selectOwnerFormGroup.disable({emitEvent: false});
    } else {
      this.selectOwnerFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: EntityInfoData | EntityId | null): void {
    this.searchText = '';
    this.modelValue = value ?? null;
    if (!value) {
      this.selectOwnerFormGroup.get('owner').patchValue(null, {emitEvent: false});
      if (this.defaultOwnerId) {
        this.applyDefaultOwner();
      }
      this.dirty = true;
      return;
    }
    const ownerInfo = value as EntityInfoData;
    if (ownerInfo.name) {
      this.selectOwnerFormGroup.get('owner').patchValue(ownerInfo, {emitEvent: false});
      this.dirty = true;
    } else {
      this.resolveOwnerInfo(value as EntityId);
    }
  }

  clear() {
    this.selectOwnerFormGroup.get('owner').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.ownerInput.nativeElement.blur();
      this.ownerInput.nativeElement.focus();
    }, 0);
  }

  onFocus() {
    if (this.dirty) {
      this.selectOwnerFormGroup.get('owner').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayOwnerFn(owner?: EntityInfoData): string | undefined {
    return owner ? owner.name : undefined;
  }

  textIsNotEmpty(text: string): boolean {
    return !!(text && text.length > 0);
  }

  private applyDefaultOwner() {
    this.resolveOwnerInfo(this.defaultOwnerId, true);
  }

  /** An owner id alone has no display name — look it up so the field shows the owner's title. */
  private resolveOwnerInfo(ownerId: EntityId, propagate = false) {
    const current = this.selectOwnerFormGroup.get('owner').value as EntityInfoData;
    if (current?.id?.id === ownerId.id) {
      if (propagate) {
        this.updateView(current);
      }
      return;
    }
    this.entityGroupService.getOwnerInfo(ownerId, {ignoreLoading: true, ignoreErrors: true}).pipe(
      catchError(() => of(null as EntityInfoData))
    ).subscribe(ownerInfo => {
      const resolved = ownerInfo ?? {id: ownerId, name: ''};
      this.selectOwnerFormGroup.get('owner').patchValue(resolved, {emitEvent: false});
      this.dirty = true;
      if (propagate) {
        this.updateView(resolved);
      }
    });
  }

  private updateValidators() {
    this.selectOwnerFormGroup.get('owner').setValidators(this.required ? [Validators.required] : []);
    this.selectOwnerFormGroup.get('owner').updateValueAndValidity({emitEvent: false});
  }

  private updateView(ownerInfo: EntityInfoData | null) {
    const value = ownerInfo ? (this.useOwnerInfoValue ? ownerInfo : ownerInfo.id) : null;
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private fetchOwners(searchText?: string): Observable<Array<EntityInfoData>> {
    this.searchText = searchText;
    // Ask for the excluded ids on top of the page so filtering them out can't empty the page.
    const pageSize = 50 + (this.excludeOwnerIds?.length ?? 0);
    const pageLink = new PageLink(pageSize, 0, searchText, {property: 'name', direction: Direction.ASC});
    return this.entityGroupService.getOwners(pageLink, {ignoreLoading: true}).pipe(
      catchError(() => of(null)),
      map(pageData => {
        const owners = pageData ? pageData.data : [];
        return this.excludeOwnerIds?.length ?
          owners.filter(owner => this.excludeOwnerIds.indexOf(owner.id.id) === -1) : owners;
      })
    );
  }
}
