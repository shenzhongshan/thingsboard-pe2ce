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

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/**
 * The "daily activity" sparkline on the integration list: per-hour message counts over the last
 * 24 hours drawn as a bare polyline.
 *
 * Inline SVG rather than a charting library — PE draws this with jquery-sparkline, which this build
 * does not bundle, and the mark is a single unlabelled, unaxed, non-interactive line at 72x36. The
 * geometry (size, 2px stroke, rgba(0,0,0,.54)) matches PE's sparkline options.
 */
@Component({
  selector: 'tb-integration-activity-sparkline',
  template: `
    @if (points) {
      <svg class="tb-int-sparkline" [attr.width]="width" [attr.height]="height"
           [attr.viewBox]="'0 0 ' + width + ' ' + height" preserveAspectRatio="none"
           [attr.aria-label]="ariaLabel">
        <polyline [attr.points]="points" fill="none" stroke="rgba(0, 0, 0, 0.54)"
                  stroke-width="2" stroke-linejoin="round" stroke-linecap="round"></polyline>
      </svg>
    }
  `,
  styles: [`
    :host { display: inline-flex; align-items: center; }
    .tb-int-sparkline { overflow: visible; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true
})
export class IntegrationActivitySparklineComponent {

  readonly width = 72;
  readonly height = 36;

  points: string = null;
  ariaLabel = '';

  @Input()
  set data(values: number[]) {
    this.points = this.buildPoints(values);
    this.ariaLabel = values?.length ? `${values.reduce((a, b) => a + b, 0)}` : '';
  }

  private buildPoints(values: number[]): string {
    if (!values?.length) {
      return null;
    }
    // A single bucket has no line to draw, so duplicate it into a flat two-point segment.
    const series = values.length === 1 ? [values[0], values[0]] : values;
    const max = Math.max(...series);
    const min = Math.min(...series);
    // A flat series (including all-zero) would divide by zero; draw it along the baseline.
    const range = max - min;
    const padding = 2;
    const usableHeight = this.height - padding * 2;
    const step = this.width / (series.length - 1);
    return series
      .map((value, i) => {
        const y = range === 0
          ? this.height - padding
          : this.height - padding - ((value - min) / range) * usableHeight;
        return `${(i * step).toFixed(2)},${y.toFixed(2)}`;
      })
      .join(' ');
  }

}
