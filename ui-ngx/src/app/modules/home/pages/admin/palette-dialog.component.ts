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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import tinycolor from 'tinycolor2';
import { paletteHueLabels, paletteHues } from '@shared/models/white-labeling.models';

export interface PaletteDialogData {
  colors: {[hue: string]: string};
}

@Component({
  selector: 'tb-palette-dialog',
  templateUrl: './palette-dialog.component.html',
  styleUrls: ['./palette-dialog.component.scss'],
  standalone: false
})
export class PaletteDialogComponent {

  hues = paletteHues;
  hueLabels = paletteHueLabels;
  colors: {[hue: string]: string};

  constructor(@Inject(MAT_DIALOG_DATA) public data: PaletteDialogData,
              private dialogRef: MatDialogRef<PaletteDialogComponent, {[hue: string]: string}>) {
    this.colors = { ...data.colors };
  }

  isDark(hue: string): boolean {
    const color = this.colors[hue];
    return color ? tinycolor(color).isDark() : false;
  }

  colorChanged(hue: string, color: string) {
    if (color) {
      this.colors[hue] = color;
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.colors);
  }
}
