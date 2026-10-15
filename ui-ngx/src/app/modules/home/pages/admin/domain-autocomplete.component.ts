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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { DomainService } from '@core/http/domain.service';
import { DomainInfo } from '@shared/models/oauth2.models';
import { PageLink } from '@shared/models/page/page-link';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { DomainTableConfigResolver } from '@home/pages/admin/oauth2/domains/domain-table-config.resolver';

// Mirrors tb-entity-autocomplete's suffix/toggle behavior ("创建新的" when empty, "×" when a
// value is selected) for the Domain entity type, which the generic EntityService does not know
// about (no case for EntityType.DOMAIN in its lookup/search switches). Kept as its own small
// component, backed by DomainService directly, rather than extending the generic entity
// framework for a resource used on only two screens.
@Component({
  selector: 'tb-domain-autocomplete',
  templateUrl: './domain-autocomplete.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DomainAutocompleteComponent),
    multi: true
  }],
  standalone: false
})
export class DomainAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectDomainFormGroup: UntypedFormGroup;

  filteredDomains: Observable<Array<DomainInfo>>;

  private modelValue: string | null;
  private dirty = false;
  private propagateChange: (value: any) => void = () => { };

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  @Output()
  domainChanged = new EventEmitter<DomainInfo>();

  @ViewChild('domainInput', {static: true}) domainInput: ElementRef;

  constructor(private domainService: DomainService,
              private dialog: MatDialog,
              private domainTableConfigResolver: DomainTableConfigResolver,
              private fb: UntypedFormBuilder) {
    this.selectDomainFormGroup = this.fb.group({
      domain: [null, [Validators.required]]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredDomains = this.selectDomainFormGroup.get('domain').valueChanges.pipe(
      debounceTime(150),
      tap(value => {
        const domain: DomainInfo = value && typeof value !== 'string' ? value : null;
        const modelValue = domain ? domain.id.id : null;
        if (!isEqual(this.modelValue, modelValue)) {
          this.modelValue = modelValue;
          this.propagateChange(this.modelValue);
          this.domainChanged.emit(domain);
        }
      }),
      map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
      switchMap(name => this.fetchDomains(name)),
      share()
    );
  }

  private fetchDomains(textSearch: string): Observable<Array<DomainInfo>> {
    return this.domainService.getTenantDomainInfos(new PageLink(50, 0, textSearch || null), {ignoreLoading: true}).pipe(
      catchError(() => of({data: [], totalPages: 0, totalElements: 0, hasNext: false})),
      map(page => page.data)
    );
  }

  async writeValue(value: string | null): Promise<void> {
    if (isDefinedAndNotNull(value)) {
      let domain: DomainInfo = null;
      let lookupFailed = false;
      try {
        domain = await this.domainService.getDomainInfoById(value as string, {ignoreLoading: true, ignoreErrors: true}).toPromise();
      } catch (e) {
        // A lookup failure (e.g. 403 — this domain belongs to a different tenant/sysadmin, but
        // was legitimately assigned before ownership was tenant-scoped) must NOT clear the
        // stored reference via propagateChange(null): that would silently blank out a valid
        // domainId the next time the form is saved, just because this viewer can't read its name.
        lookupFailed = true;
      }
      this.modelValue = domain ? domain.id.id : (lookupFailed ? (value as string) : null);
      // Fall back to displaying the raw id when the name can't be resolved, rather than leaving
      // the field looking empty (which would misleadingly show "创建新的" as if nothing were set).
      const displayValue = domain ? domain : (lookupFailed ? ({id: {id: value}, name: value} as unknown as DomainInfo) : '');
      this.selectDomainFormGroup.get('domain').patchValue(displayValue, {emitEvent: false});
      this.domainChanged.emit(domain);
    } else {
      this.modelValue = null;
      this.selectDomainFormGroup.get('domain').patchValue('', {emitEvent: false});
      this.domainChanged.emit(null);
    }
    this.dirty = true;
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.selectDomainFormGroup.disable({emitEvent: false});
    } else {
      this.selectDomainFormGroup.enable({emitEvent: false});
    }
  }

  onFocus() {
    if (this.dirty) {
      this.selectDomainFormGroup.get('domain').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayDomainFn(domain?: DomainInfo): string | undefined {
    return domain ? domain.name : undefined;
  }

  clear() {
    this.selectDomainFormGroup.get('domain').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.domainInput.nativeElement.blur();
      this.domainInput.nativeElement.focus();
    }, 0);
  }

  createNewDomain($event: Event) {
    $event.stopPropagation();
    this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<DomainInfo>, DomainInfo>(AddEntityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: this.domainTableConfigResolver.resolve(null as any)
      }
    }).afterClosed().subscribe((domain) => {
      if (domain) {
        this.selectDomainFormGroup.get('domain').patchValue(domain, {emitEvent: true});
        this.selectDomainFormGroup.markAsDirty();
      }
    });
  }
}
