package com.codahale.metrics.datadog;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class DatadogReporter extends ScheduledReporter {

  private final Datadog datadog;
  private final Clock clock;
  private final String host;
  private final EnumSet<Expansions> expansions;
  private final MetricNameFormatter metricNameFormatter;
  private static final Logger LOG = LoggerFactory
      .getLogger(DatadogReporter.class);

  private DatadogReporter(MetricRegistry metricRegistry,
                          Datadog datadog,
                          MetricFilter filter,
                          Clock clock,
                          String host,
                          EnumSet<Expansions> expansions,
                          TimeUnit rateUnit,
                          TimeUnit durationUnit,
                          MetricNameFormatter metricNameFormatter) {
    super(metricRegistry,
        "datadog-reporter",
        filter,
        rateUnit,
        durationUnit);
    this.clock = clock;
    this.host = host;
    this.expansions = expansions;
    this.metricNameFormatter = metricNameFormatter;
    this.datadog = datadog;
  }

  @Override
  public void report(SortedMap<String, Gauge> gauges,
                     SortedMap<String, Counter> counters,
                     SortedMap<String, Histogram> histograms,
                     SortedMap<String, Meter> meters,
                     SortedMap<String, Timer> timers) {
    final long timestamp = clock.getTime() / 1000;

    try {
      datadog.createSeries();

      for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
        reportGauge(entry.getKey(), entry.getValue(), timestamp);
      }

      for (Map.Entry<String, Counter> entry : counters.entrySet()) {
        reportCounter(entry.getKey(), entry.getValue(), timestamp);
      }

      for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
        reportHistogram(entry.getKey(), entry.getValue(), timestamp);
      }

      for (Map.Entry<String, Meter> entry : meters.entrySet()) {
        reportMetered(entry.getKey(), entry.getValue(), timestamp);
      }

      for (Map.Entry<String, Timer> entry : timers.entrySet()) {
        reportTimer(entry.getKey(), entry.getValue(), timestamp);
      }

      datadog.sendSeries();
    } catch (Throwable t) {
      LOG.error("Error processing metrics", t);
    }
  }

  private void reportTimer(String name, Timer timer, long timestamp) {
    final Snapshot snapshot = timer.getSnapshot();

    try {
      datadog.addGauge(maybeExpand(Expansions.MAX, name),
          format(convertDuration(snapshot.getMax())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.MEAN, name),
          format(convertDuration(snapshot.getMean())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.MIN, name),
          format(convertDuration(snapshot.getMin())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.STD_DEV, name),
          format(convertDuration(snapshot.getStdDev())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P50, name),
          format(convertDuration(snapshot.getMedian())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P75, name),
          format(convertDuration(snapshot.get75thPercentile())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P95, name),
          format(convertDuration(snapshot.get95thPercentile())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P98, name),
          format(convertDuration(snapshot.get98thPercentile())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P99, name),
          format(convertDuration(snapshot.get99thPercentile())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P999, name),
          format(convertDuration(snapshot.get999thPercentile())),
          timestamp,
          host);
    } catch (Exception e) {
      LOG.error("Error writing timer", e);
    }

    reportMetered(name, timer, timestamp);
  }

  private void reportMetered(String name, Metered meter, long timestamp) {
    try {
      datadog.addCounter(maybeExpand(Expansions.COUNT, name),
          meter.getCount(),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.RATE_1_MINUTE, name),
          format(convertRate(meter.getOneMinuteRate())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.RATE_5_MINUTE, name),
          format(convertRate(meter.getFiveMinuteRate())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.RATE_15_MINUTE, name),
          format(convertRate(meter.getFifteenMinuteRate())),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.RATE_MEAN, name),
          format(convertRate(meter.getMeanRate())),
          timestamp,
          host);
    } catch (Exception e) {
      LOG.error("Error writing meter", e);
    }
  }

  private void reportHistogram(String name, Histogram histogram, long timestamp) {
    final Snapshot snapshot = histogram.getSnapshot();

    try {
      datadog.addCounter(maybeExpand(Expansions.COUNT, name),
          histogram.getCount(),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.MAX, name),
          format(snapshot.getMax()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.MEAN, name),
          format(snapshot.getMean()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.MIN, name),
          format(snapshot.getMin()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.STD_DEV, name),
          format(snapshot.getStdDev()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P50, name),
          format(snapshot.getMedian()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P75, name),
          format(snapshot.get75thPercentile()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P95, name),
          format(snapshot.get95thPercentile()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P98, name),
          format(snapshot.get98thPercentile()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P99, name),
          format(snapshot.get99thPercentile()),
          timestamp,
          host);
      datadog.addGauge(maybeExpand(Expansions.P999, name),
          format(snapshot.get999thPercentile()),
          timestamp,
          host);
    } catch (Exception e) {
      LOG.error("Error writing histogram", e);
    }
  }

  private void reportCounter(String name, Counter counter, long timestamp) {
    try {
      datadog.addCounter(name, counter.getCount(), timestamp, host);
    } catch (Exception e) {
      LOG.error("Error writing counter", e);
    }
  }

  private void reportGauge(String name, Gauge gauge, long timestamp) {
    final Number value = format(gauge.getValue());
    try{
      if (value != null) {
        datadog.addGauge(name, value, timestamp, host);
      }
    } catch (Exception e) {
      LOG.error("Error writing gauge", e);
    }
  }

  private Number format(Object o) {
    if (o instanceof Number) {
      return (Number) o;
    }
    return null;
  }

  private String maybeExpand(Expansions expansion, String name) {
    if (expansions.contains(expansion)) {
      return metricNameFormatter.format(name, expansion.toString());
    } else {
      return metricNameFormatter.format(name);
    }
  }

  public static enum Expansions {
    COUNT("count"),
    RATE_MEAN("meanRate"),
    RATE_1_MINUTE("1MinuteRate"),
    RATE_5_MINUTE("5MinuteRate"),
    RATE_15_MINUTE("15MinuteRate"),
    MIN("min"),
    MEAN("mean"),
    MAX("max"),
    STD_DEV("stddev"),
    MEDIAN("median"),
    P50("p50"),
    P75("p75"),
    P95("p95"),
    P98("p98"),
    P99("p99"),
    P999("p999");

    public static EnumSet<Expansions> ALL = EnumSet.allOf(Expansions.class);

    private final String displayName;

    private Expansions(String displayName) {
      this.displayName = displayName;
    }

    @Override
    public String toString() {
      return displayName;
    }
  }

  public static Builder forRegistry(MetricRegistry registry) {
    return new Builder(registry);
  }

  public static class Builder {
    private final MetricRegistry registry;
    private String host;
    private EnumSet<Expansions> expansions;
    private Clock clock;
    private TimeUnit rateUnit;
    private TimeUnit durationUnit;
    private MetricFilter filter;
    private MetricNameFormatter metricNameFormatter;

    public Builder(MetricRegistry registry) {
      this.registry = registry;
      this.expansions = Expansions.ALL;
      this.clock = Clock.defaultClock();
      this.rateUnit = TimeUnit.SECONDS;
      this.durationUnit = TimeUnit.MILLISECONDS;
      this.filter = MetricFilter.ALL;
      this.metricNameFormatter = new DefaultMetricNameFormatter();
    }

    public Builder withHost(String host) {
      this.host = host;
      return this;
    }

    public Builder withEC2Host() throws IOException {
      this.host = AwsHelper.getEc2InstanceId();
      return this;
    }

    public Builder withExpansions(EnumSet<Expansions> expansions) {
      this.expansions = expansions;
      return this;
    }

    @SuppressWarnings("unused")
    public Builder withClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    @SuppressWarnings("unused")
    public Builder convertRatesTo(TimeUnit rateUnit) {
      this.rateUnit = rateUnit;
      return this;
    }

    @SuppressWarnings("unused")
    public Builder convertDurationsTo(TimeUnit durationUnit) {
      this.durationUnit = durationUnit;
      return this;
    }

    public Builder filter(MetricFilter filter) {
      this.filter = filter;
      return this;
    }

    public Builder withMetricNameFormatter(MetricNameFormatter formatter) {
      this.metricNameFormatter = formatter;
      return this;
    }

    public DatadogReporter build(Datadog datadog) {
      return new DatadogReporter(
          this.registry,
          datadog,
          this.filter,
          this.clock,
          this.host,
          this.expansions,
          this.rateUnit,
          this.durationUnit,
          this.metricNameFormatter);
    }
  }
}