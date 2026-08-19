/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.thingsboard.server.common.data.exception.ThingsboardException
 *  org.thingsboard.server.common.data.id.EntityId
 *  org.thingsboard.server.common.data.id.ReportTemplateId
 *  org.thingsboard.server.common.data.kv.Aggregation
 *  org.thingsboard.server.common.data.kv.BaseReadTsKvQuery
 *  org.thingsboard.server.common.data.kv.ReadTsKvQueryResult
 *  org.thingsboard.server.common.data.kv.TsKvEntry
 *  org.thingsboard.server.common.data.page.PageData
 *  org.thingsboard.server.common.data.page.SortOrder$Direction
 *  org.thingsboard.server.common.data.query.AlarmCountQuery
 *  org.thingsboard.server.common.data.query.AlarmData
 *  org.thingsboard.server.common.data.query.AlarmDataQuery
 *  org.thingsboard.server.common.data.query.EntityCountQuery
 *  org.thingsboard.server.common.data.query.EntityData
 *  org.thingsboard.server.common.data.query.EntityDataQuery
 *  org.thingsboard.server.common.data.report.Report
 *  org.thingsboard.server.common.data.report.ReportTemplate
 *  org.thingsboard.server.common.data.report.configuration.timewindow.Interval
 */
package org.thingsboard.server.report.datasource;

import java.util.Collection;
import java.util.List;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.report.context.TbReportCtx;

public interface ReportDataService {
    public ReportTemplate findReportTemplate(ReportTemplateId var1, TbReportCtx var2) throws ThingsboardException;

    public byte[] downloadImage(String var1, String var2, TbReportCtx var3) throws ThingsboardException;

    public byte[] downloadPublicImage(String var1, TbReportCtx var2) throws ThingsboardException;

    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery var1, TbReportCtx var2);

    public Long countEntitiesByQuery(EntityCountQuery var1, TbReportCtx var2);

    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery var1, TbReportCtx var2);

    public PageData<AlarmData> findAlarmDataByQueryForEntities(AlarmDataQuery var1, Collection<EntityId> var2, TbReportCtx var3);

    public Long countAlarmsByQuery(AlarmCountQuery var1, TbReportCtx var2);

    public List<TsKvEntry> getTimeseries(EntityId var1, List<String> var2, Long var3, Long var4, Interval var5, String var6, Aggregation var7, SortOrder.Direction var8, Integer var9, boolean var10, TbReportCtx var11);

    public List<ReadTsKvQueryResult> findTimeseriesByQueries(EntityId var1, List<BaseReadTsKvQuery> var2, TbReportCtx var3);

    public Report createReport(Report var1, byte[] var2, TbReportCtx var3) throws ThingsboardException;
}

