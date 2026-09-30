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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HELP_ARTICLES, HelpArticle } from './help-articles.data';

@Component({
    selector: 'tb-help-article',
    templateUrl: './help-article.component.html',
    styleUrls: ['./help-article.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class HelpArticleComponent implements OnInit {

  article: HelpArticle;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const articleId = params.get('articleId');
      this.article = HELP_ARTICLES[articleId];
      this.cd.markForCheck();
    });
  }

  goBack() {
    this.router.navigateByUrl('/home');
  }
}
