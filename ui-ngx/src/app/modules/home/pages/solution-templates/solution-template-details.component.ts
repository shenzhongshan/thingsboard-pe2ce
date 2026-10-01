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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { DialogService } from '@core/services/dialog.service';
import { SolutionService } from '@core/http/solution.service';
import {
  SolutionTemplateLevel,
  SolutionTemplateLevelTranslationMap,
  TenantSolutionTemplateDetails
} from '@shared/models/solution-template.models';
import {
  SolutionInstallDialogComponent,
  SolutionInstallDialogData
} from '@home/pages/solution-templates/solution-install-dialog.component';

const AUTOPLAY_INTERVAL_MS = 3000;

@Component({
    selector: 'tb-solution-template-details',
    templateUrl: './solution-template-details.component.html',
    styleUrls: ['./solution-template-details.component.scss'],
    standalone: false
})
export class SolutionTemplateDetailsComponent extends PageComponent implements OnInit, OnDestroy {

  solutionTemplateDetails: TenantSolutionTemplateDetails;
  images: string[];
  carouselIndex = 0;

  SolutionTemplateLevel = SolutionTemplateLevel;
  SolutionTemplateLevelTranslationMap = SolutionTemplateLevelTranslationMap;

  private autoplayHandle: ReturnType<typeof setInterval>;

  constructor(protected store: Store<AppState>,
              private solutionService: SolutionService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private translate: TranslateService,
              private route: ActivatedRoute,
              private router: Router) {
    super(store);
    this.solutionTemplateDetails = this.route.snapshot.data.solutionTemplateDetails;
    this.images = this.solutionTemplateDetails.imageUrls || [];
  }

  ngOnInit(): void {
    this.startAutoplay();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.stopAutoplay();
  }

  prevImage(): void {
    this.carouselIndex = (this.carouselIndex - 1 + this.images.length) % this.images.length;
  }

  nextImage(): void {
    this.carouselIndex = (this.carouselIndex + 1) % this.images.length;
  }

  goToImage(index: number): void {
    this.carouselIndex = index;
  }

  private startAutoplay(): void {
    if (this.images.length > 1) {
      this.autoplayHandle = setInterval(() => this.nextImage(), AUTOPLAY_INTERVAL_MS);
    }
  }

  private stopAutoplay(): void {
    if (this.autoplayHandle) {
      clearInterval(this.autoplayHandle);
      this.autoplayHandle = null;
    }
  }

  installSolutionTemplate(): void {
    const waitSubject = new Subject<void>();
    this.dialogService.progress(waitSubject.asObservable(), this.translate.instant('solution-template.installing'));
    this.solutionService.installSolutionTemplate(this.solutionTemplateDetails.id).subscribe(response => {
      if (response.success) {
        setTimeout(() => {
          waitSubject.next();
          waitSubject.complete();
          this.router.navigateByUrl(`dashboards/${response.dashboardId.id}`);
        }, this.solutionTemplateDetails.installTimeoutMs);
      } else {
        waitSubject.next();
        waitSubject.complete();
        this.dialog.open<SolutionInstallDialogComponent, SolutionInstallDialogData>(SolutionInstallDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            solutionInstallResponse: response,
            instructions: false,
            showMainDashboardButton: false
          }
        }).afterClosed().subscribe(() => {
          this.reload();
        });
      }
    }, () => {
      waitSubject.next();
      waitSubject.complete();
    });
  }

  openInstructions(): void {
    this.solutionService.getSolutionTemplateInstructions(this.solutionTemplateDetails.id).subscribe(instructions => {
      this.dialog.open<SolutionInstallDialogComponent, SolutionInstallDialogData>(SolutionInstallDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          solutionInstallResponse: instructions,
          instructions: true,
          showMainDashboardButton: true
        }
      });
    });
  }

  deleteSolutionTemplate(): void {
    this.dialogService.confirm(
      this.translate.instant('solution-template.delete-solution-title', {solutionTitle: this.solutionTemplateDetails.title}),
      this.translate.instant('solution-template.delete-solution-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(result => {
      if (result) {
        this.solutionService.uninstallSolutionTemplate(this.solutionTemplateDetails.id).subscribe(() => {
          this.reload();
        });
      }
    });
  }

  private reload(): void {
    this.solutionService.getSolutionTemplateDetails(this.solutionTemplateDetails.id).subscribe(details => {
      this.solutionTemplateDetails = details;
    });
  }

}
