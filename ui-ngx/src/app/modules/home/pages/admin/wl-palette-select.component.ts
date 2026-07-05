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

import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { materialColorPalette } from '@shared/models/material.models';
import { materialColorPalettes, Palette } from '@shared/models/white-labeling.models';
import { PaletteDialogComponent, PaletteDialogData } from '@home/pages/admin/palette-dialog.component';

@Component({
  selector: 'tb-wl-palette-select',
  templateUrl: './wl-palette-select.component.html',
  styleUrls: ['./wl-palette-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => WlPaletteSelectComponent),
    multi: true
  }],
  standalone: false
})
export class WlPaletteSelectComponent implements ControlValueAccessor {

  @Input() label: string;

  palettes = materialColorPalettes;

  palette: Palette = null;
  disabled = false;

  private propagateChange = (_v: any) => {};

  constructor(private dialog: MatDialog) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: Palette): void {
    this.palette = value || null;
  }

  get selectedType(): string {
    return this.palette?.type || null;
  }

  paletteColor(paletteKey: string): string {
    return materialColorPalette[paletteKey]?.['500'] || 'transparent';
  }

  get displayColor(): string {
    if (!this.palette) {
      return null;
    }
    if (this.palette.type === 'custom') {
      return this.palette.colors?.['500'] ||
        materialColorPalette[this.palette.extends]?.['500'] || null;
    }
    return materialColorPalette[this.palette.type]?.['500'] || null;
  }

  typeChanged(type: string) {
    this.palette = type ? { type } : null;
    this.propagateChange(this.palette);
  }

  customize() {
    const base = this.palette?.type === 'custom' ? (this.palette.extends || 'indigo') : (this.palette?.type || 'indigo');
    const baseColors = { ...(materialColorPalette[base] || materialColorPalette.indigo) };
    const colors = { ...baseColors, ...(this.palette?.type === 'custom' ? this.palette.colors : {}) };
    this.dialog.open<PaletteDialogComponent, PaletteDialogData, {[hue: string]: string}>(PaletteDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: { colors }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.palette = { type: 'custom', extends: base, colors: result };
        this.propagateChange(this.palette);
      }
    });
  }
}
