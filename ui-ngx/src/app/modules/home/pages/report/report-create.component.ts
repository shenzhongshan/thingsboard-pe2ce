/**
 * 文件职责：独立报表生成页面的业务控制器。
 * 复用平台现有报表模板查询和报表生成接口，不修改原生 reporting 页面。
 */
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { ReportService } from '@core/http/report.service';
import { DialogService } from '@core/services/dialog.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { ReportInfo } from '@shared/models/report.models';
import { Authority } from '@shared/models/authority.enum';
import { AuthUser } from '@shared/models/user.model';
import {
  ReportGenerateDialogComponent,
  ReportGenerateDialogData
} from '@home/pages/report/report-generate-dialog.component';

@Component({
  selector: 'tb-report-create',
  templateUrl: './report-create.component.html',
  styleUrls: ['./report-create.component.scss'],
  standalone: false
})
export class ReportCreateComponent implements OnInit {

  reports: ReportInfo[] = [];
  reportPage = 0;
  reportPageSize = 10;
  reportTotalElements = 0;
  reportSearchText: string = null;
  searchMode = false;
  loadingReports = false;
  private readonly authUser: AuthUser;

  constructor(private reportService: ReportService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private store: Store<AppState>) {
    // 记录当前登录用户，用于前端隐藏无权删除的他人报表；后端仍会再次强制校验。
    this.authUser = getCurrentAuthUser(this.store);
  }

  ngOnInit(): void {
    this.loadReports();
  }

  /** 查询当前用户可访问的已生成报表，删除按钮仅对本人生成的报告显示。 */
  loadReports(): void {
    this.loadingReports = true;
    const pageLink = new PageLink(this.reportPageSize, this.reportPage, this.reportSearchText,
      { property: 'createdTime', direction: Direction.DESC });
    this.reportService.getReportInfos(pageLink).subscribe({
      next: (page) => {
        this.reports = page.data;
        this.reportTotalElements = page.totalElements;
        this.loadingReports = false;
      },
      error: () => {
        this.reports = [];
        this.reportTotalElements = 0;
        this.loadingReports = false;
      }
    });
  }

  /** 打开自定义生成弹窗，成功后只刷新当前页面的报表列表。 */
  generateReport(): void {
    this.dialog.open<ReportGenerateDialogComponent, ReportGenerateDialogData, boolean>(
      ReportGenerateDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog'],
        data: {}
      }
    ).afterClosed().subscribe((generated) => {
      if (generated) {
        this.reportPage = 0;
        this.loadReports();
      }
    });
  }

  /** 下载当前用户有权限访问的报表文件。 */
  downloadReport(event: Event, report: ReportInfo): void {
    event.stopPropagation();
    this.reportService.downloadReport(report.id.id, report.name).subscribe();
  }

  /** 删除当前用户有权限管理的报表，并复用原生二次确认文案。 */
  deleteReport(event: Event, report: ReportInfo): void {
    event.stopPropagation();
    this.dialogService.confirm(
      this.translate.instant('report.delete-report-title', { reportName: report.name }),
      this.translate.instant('report.delete-report-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes')
    ).subscribe((confirmed) => {
      if (confirmed) {
        this.reportService.deleteReport(report.id.id).subscribe(() => this.loadReports());
      }
    });
  }

  /** 客户用户只能删除本人生成的报告，租户管理员可删除租户内报告。 */
  canDeleteReport(report: ReportInfo): boolean {
    return this.authUser?.authority === Authority.TENANT_ADMIN
      || report.userId?.id === this.authUser?.userId;
  }

  /** 展开原生报表列表同款搜索框。 */
  enterSearchMode(): void {
    this.searchMode = true;
  }

  /** 关闭搜索并恢复完整列表。 */
  exitSearchMode(): void {
    this.searchMode = false;
    if (this.reportSearchText) {
      this.reportSearchText = null;
      this.reportPage = 0;
      this.loadReports();
    }
  }

  /** 搜索输入变化时从第一页重新查询。 */
  searchChanged(): void {
    this.reportPage = 0;
    this.loadReports();
  }

  /** 分页只影响下载列表，不影响报表生成表单。 */
  reportPageChanged(event: { pageIndex: number; pageSize: number }): void {
    this.reportPage = event.pageIndex;
    this.reportPageSize = event.pageSize;
    this.loadReports();
  }

}
