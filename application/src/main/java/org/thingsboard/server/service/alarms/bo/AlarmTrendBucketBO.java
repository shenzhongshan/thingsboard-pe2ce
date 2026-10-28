/** 文件职责：告警趋势分桶的内部业务对象。 */
package org.thingsboard.server.service.alarms.bo;

/** 告警趋势一个桶的下标和数量。 */
public final class AlarmTrendBucketBO {

    private final int bucketIndex;
    private final long alarmCount;

    public AlarmTrendBucketBO(int bucketIndex, long alarmCount) {
        this.bucketIndex = bucketIndex;
        this.alarmCount = alarmCount;
    }

    public int getBucketIndex() {
        return bucketIndex;
    }

    public long getAlarmCount() {
        return alarmCount;
    }
}