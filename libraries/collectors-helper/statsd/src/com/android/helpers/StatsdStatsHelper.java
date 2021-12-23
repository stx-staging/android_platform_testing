/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.helpers;

import androidx.annotation.VisibleForTesting;

import com.android.os.nano.StatsLog;

import java.util.HashMap;
import java.util.Map;

/**
 * StatsdStatsHelper consist of helper methods to set the Statsd metadata collection and retrieve
 * the necessary information from statsd.
 */
public class StatsdStatsHelper implements ICollectorHelper<Long> {

    static final String STATSDSTATS_PREFIX = "statsdstats";
    static final String ATOM_STATS_PREFIX = "atom_stats";
    static final String MATCHER_STATS_PREFIX = "matcher_stats";
    static final String CONDITION_STATS_PREFIX = "condition_stats";
    static final String METRIC_STATS_PREFIX = "metric_stats";
    static final String ALERT_STATS_PREFIX = "alert_stats";
    static final String CONFIG_STATS_PREFIX = "config_stats";
    static final String ANOMALY_ALARM_STATS_PREFIX = "anomaly_alarm_stats";

    interface IStatsdHelper {
        StatsLog.StatsdStatsReport getStatsdStatsReport();
    }

    private static class DefaultStatsdHelper implements IStatsdHelper {

        private StatsdHelper mStatsdHelper = new StatsdHelper();

        @Override
        public StatsLog.StatsdStatsReport getStatsdStatsReport() {
            return mStatsdHelper.getStatsdStatsReport();
        }
    }

    private IStatsdHelper mStatsdHelper = new DefaultStatsdHelper();

    public StatsdStatsHelper() {}

    /**
     * Constructor to simulate an externally provided statsd helper instance. Should not be used
     * except for testing.
     */
    @VisibleForTesting
    StatsdStatsHelper(IStatsdHelper helper) {
        mStatsdHelper = helper;
    }

    /** Resets statsd metadata */
    @Override
    public boolean startCollecting() {
        // TODO: http://b/204890512 implement metadata reset
        return true;
    }

    /** Collect the statsd metadata accumulated during the test run. */
    @Override
    public Map<String, Long> getMetrics() {
        Map<String, Long> resultMap = new HashMap<>();

        final StatsLog.StatsdStatsReport report = mStatsdHelper.getStatsdStatsReport();
        populateAtomStats(report.atomStats, resultMap);
        populateConfigStats(report.configStats, resultMap);
        populateAnomalyAlarmStats(report.anomalyAlarmStats, resultMap);

        return resultMap;
    }

    private static void populateAtomStats(
            StatsLog.StatsdStatsReport.AtomStats[] atomStats, Map<String, Long> resultMap) {
        final String metricKeyPrefix =
                MetricUtility.constructKey(STATSDSTATS_PREFIX, ATOM_STATS_PREFIX);

        for (final StatsLog.StatsdStatsReport.AtomStats dataItem : atomStats) {
            final String metricKeyPrefixWithTag =
                    MetricUtility.constructKey(metricKeyPrefix, String.valueOf(dataItem.tag));
            resultMap.put(
                    MetricUtility.constructKey(metricKeyPrefixWithTag, "count"),
                    Long.valueOf(dataItem.count));
            resultMap.put(
                    MetricUtility.constructKey(metricKeyPrefixWithTag, "error_count"),
                    Long.valueOf(dataItem.errorCount));
        }
    }

    private static void populateConfigStats(
            StatsLog.StatsdStatsReport.ConfigStats[] configStats, Map<String, Long> resultMap) {
        final String metricKeyPrefix =
                MetricUtility.constructKey(STATSDSTATS_PREFIX, CONFIG_STATS_PREFIX);

        for (final StatsLog.StatsdStatsReport.ConfigStats dataItem : configStats) {
            final String metricKeyPrefixWithTag =
                    MetricUtility.constructKey(metricKeyPrefix, String.valueOf(dataItem.id));

            resultMap.put(
                    MetricUtility.constructKey(metricKeyPrefixWithTag, "metric_count"),
                    Long.valueOf(dataItem.metricCount));
            resultMap.put(
                    MetricUtility.constructKey(metricKeyPrefixWithTag, "condition_count"),
                    Long.valueOf(dataItem.conditionCount));
            resultMap.put(
                    MetricUtility.constructKey(metricKeyPrefixWithTag, "matcher_count"),
                    Long.valueOf(dataItem.matcherCount));
            resultMap.put(
                    MetricUtility.constructKey(metricKeyPrefixWithTag, "alert_count"),
                    Long.valueOf(dataItem.alertCount));

            populateMatcherStats(dataItem.matcherStats, resultMap, metricKeyPrefixWithTag);
            populateConditionStats(dataItem.conditionStats, resultMap, metricKeyPrefixWithTag);
            populateMetricStats(dataItem.metricStats, resultMap, metricKeyPrefixWithTag);
            populateAlertStats(dataItem.alertStats, resultMap, metricKeyPrefixWithTag);
        }
    }

    private static void populateMetricStats(
            StatsLog.StatsdStatsReport.MetricStats[] stats,
            Map<String, Long> resultMap,
            String metricKeyPrefix) {
        for (final StatsLog.StatsdStatsReport.MetricStats dataItem : stats) {
            final String metricKey =
                    MetricUtility.constructKey(
                            metricKeyPrefix,
                            METRIC_STATS_PREFIX,
                            String.valueOf(dataItem.id),
                            "max_tuple_counts");
            resultMap.put(metricKey, Long.valueOf(dataItem.maxTupleCounts));
        }
    }

    private static void populateConditionStats(
            StatsLog.StatsdStatsReport.ConditionStats[] stats,
            Map<String, Long> resultMap,
            String metricKeyPrefix) {
        for (final StatsLog.StatsdStatsReport.ConditionStats dataItem : stats) {
            final String metricKey =
                    MetricUtility.constructKey(
                            metricKeyPrefix,
                            CONDITION_STATS_PREFIX,
                            String.valueOf(dataItem.id),
                            "max_tuple_counts");
            resultMap.put(metricKey, Long.valueOf(dataItem.maxTupleCounts));
        }
    }

    private static void populateMatcherStats(
            StatsLog.StatsdStatsReport.MatcherStats[] stats,
            Map<String, Long> resultMap,
            String metricKeyPrefix) {
        for (final StatsLog.StatsdStatsReport.MatcherStats dataItem : stats) {
            final String metricKey =
                    MetricUtility.constructKey(
                            metricKeyPrefix,
                            MATCHER_STATS_PREFIX,
                            String.valueOf(dataItem.id),
                            "matched_times");
            resultMap.put(metricKey, Long.valueOf(dataItem.matchedTimes));
        }
    }

    private static void populateAlertStats(
            StatsLog.StatsdStatsReport.AlertStats[] stats,
            Map<String, Long> resultMap,
            String metricKeyPrefix) {
        for (final StatsLog.StatsdStatsReport.AlertStats dataItem : stats) {
            final String metricKey =
                    MetricUtility.constructKey(
                            metricKeyPrefix,
                            ALERT_STATS_PREFIX,
                            String.valueOf(dataItem.id),
                            "alerted_times");
            resultMap.put(metricKey, Long.valueOf(dataItem.alertedTimes));
        }
    }

    private static void populateAnomalyAlarmStats(
            StatsLog.StatsdStatsReport.AnomalyAlarmStats anomalyAlarmStats,
            Map<String, Long> resultMap) {
        if (anomalyAlarmStats == null) {
            return;
        }
        final String metricKey =
                MetricUtility.constructKey(
                        STATSDSTATS_PREFIX, ANOMALY_ALARM_STATS_PREFIX, "alarms_registered");
        resultMap.put(metricKey, Long.valueOf(anomalyAlarmStats.alarmsRegistered));
    }

    /** No op. */
    @Override
    public boolean stopCollecting() {
        return true;
    }
}
