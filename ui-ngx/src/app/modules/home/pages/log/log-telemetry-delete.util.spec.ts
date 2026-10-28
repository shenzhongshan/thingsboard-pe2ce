import assert from 'node:assert/strict';
import { buildLogTelemetryDeleteOptions } from './log-telemetry-delete.util';

const deletionResult = (strategy: string, startDateTime?: Date, endDateTime?: Date, rewriteLatest = false) => ({
  strategy,
  startDateTime,
  endDateTime,
  rewriteLatest
} as any);

assert.deepEqual(
  buildLogTelemetryDeleteOptions(deletionResult('DELETE_ALL_DATA')),
  {deleteAllDataForKeys: true, rewriteLatestIfDeleted: false, deleteLatest: true}
);

assert.deepEqual(
  buildLogTelemetryDeleteOptions(deletionResult('DELETE_ALL_DATA_EXCEPT_LATEST_VALUE')),
  {deleteAllDataForKeys: true, rewriteLatestIfDeleted: false, deleteLatest: false}
);

assert.deepEqual(
  buildLogTelemetryDeleteOptions(deletionResult('DELETE_LATEST_VALUE', undefined, undefined, true), 1700000000123),
  {
    deleteAllDataForKeys: false,
    startTs: 1700000000123,
    endTs: 1700000000124,
    rewriteLatestIfDeleted: true,
    deleteLatest: true
  }
);

assert.deepEqual(
  buildLogTelemetryDeleteOptions(deletionResult(
    'DELETE_ALL_DATA_FOR_TIME_PERIOD', new Date(1700000000000), new Date(1700003600000), true
  )),
  {
    deleteAllDataForKeys: false,
    startTs: 1700000000000,
    endTs: 1700003600000,
    rewriteLatestIfDeleted: true,
    deleteLatest: true
  }
);

assert.equal(buildLogTelemetryDeleteOptions(deletionResult('DELETE_LATEST_VALUE'), undefined), null);
assert.equal(buildLogTelemetryDeleteOptions(deletionResult(
  'DELETE_ALL_DATA_FOR_TIME_PERIOD', new Date(1700003600000), new Date(1700000000000)
)), null);
