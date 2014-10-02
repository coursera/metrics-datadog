package com.yammer.metrics.reporting;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.Transport.Request;
import com.yammer.metrics.reporting.model.DatadogCounter;
import com.yammer.metrics.reporting.model.DatadogGauge;
import com.yammer.metrics.stats.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class DatadogReporter extends AbstractPollingReporter implements
                                                             MetricProcessor<Long> {

  public boolean printVmMetrics = true;
  protected final Locale locale = Locale.US;
  protected final Clock clock;
  private final String host;
  protected final MetricPredicate predicate;
  protected final Transport transport;
  protected final EnumSet<Expansions> expansions;
  private static final Logger LOG = LoggerFactory
      .getLogger(DatadogReporter.class);
  private final VirtualMachineMetrics vm;
  private final MetricNameFormatter metricNameFormatter;
  private final List<String> tags;
  private Request request;

  public DatadogReporter(MetricsRegistry metricsRegistry,
                         MetricPredicate predicate, VirtualMachineMetrics vm, Transport transport,
                         Clock clock, String host, EnumSet<Expansions> expansions,
                         Boolean printVmMetrics,
                         MetricNameFormatter metricNameFormatter,
                         List<String> tags) {
    super(metricsRegistry, "datadog-reporter");
    this.vm = vm;
    this.transport = transport;
    this.predicate = predicate;
    this.clock = clock;
    this.host = host;
    this.expansions = expansions;
    this.printVmMetrics = printVmMetrics;
    this.metricNameFormatter = metricNameFormatter;
    this.tags = tags;
  }

  @Override
  public void run() {
    try {
      try {
        request = transport.prepare();
      } catch (IOException ioe) {
        LOG.error("Could not prepare request", ioe);
        return;
      }

      final long epoch = clock.time() / 1000;
      if (this.printVmMetrics) {
        pushVmMetrics(epoch);
      }
      pushRegularMetrics(epoch);
      request.send();
    } catch (Throwable t) {
      LOG.error("Error processing metrics", t);
    }
  }

  public void processCounter(MetricName name, Counter counter, Long epoch)
      throws Exception {
    pushCounter(name, counter.count(), epoch);
  }

  public void processGauge(MetricName name, Gauge<?> gauge, Long epoch)
      throws Exception {
    Object value = gauge.value();
    if (value instanceof Number) {
      pushGauge(name, (Number) value, epoch);
    } else {
      LOG.debug("Gauge " + name + " had non Number value, skipped");
    }
  }

  public void processHistogram(MetricName name, Histogram histogram, Long epoch)
      throws Exception {
    pushSummarizable(name, histogram, epoch);
    pushSampling(name, histogram, epoch);
  }

  public void processMeter(MetricName name, Metered meter, Long epoch)
      throws Exception {
    if (expansions.contains(Expansions.COUNT)) {
      pushCounter(name, meter.count(), epoch, Expansions.COUNT.toString());
    }

    maybeExpand(Expansions.RATE_MEAN, name, meter.meanRate(), epoch);
    maybeExpand(Expansions.RATE_1_MINUTE, name, meter.oneMinuteRate(), epoch);
    maybeExpand(Expansions.RATE_5_MINUTE, name, meter.fiveMinuteRate(), epoch);
    maybeExpand(Expansions.RATE_15_MINUTE, name, meter.fifteenMinuteRate(), epoch);
  }

  public void processTimer(MetricName name, Timer timer, Long epoch)
      throws Exception {
    processMeter(name, timer, epoch);
    pushSummarizable(name, timer, epoch);
    pushSampling(name, timer, epoch);
  }

  private void pushSummarizable(MetricName name, Summarizable summarizable,
                                Long epoch) {
    maybeExpand(Expansions.MIN, name, summarizable.min(), epoch);
    maybeExpand(Expansions.MAX, name, summarizable.max(), epoch);
    maybeExpand(Expansions.MEAN, name, summarizable.mean(), epoch);
    maybeExpand(Expansions.STD_DEV, name, summarizable.stdDev(), epoch);
  }

  private void pushSampling(MetricName name, Sampling sampling, Long epoch) {
    final Snapshot snapshot = sampling.getSnapshot();
    maybeExpand(Expansions.MEDIAN, name, snapshot.getMedian(), epoch);
    maybeExpand(Expansions.P75, name, snapshot.get75thPercentile(), epoch);
    maybeExpand(Expansions.P95, name, snapshot.get95thPercentile(), epoch);
    maybeExpand(Expansions.P98, name, snapshot.get98thPercentile(), epoch);
    maybeExpand(Expansions.P99, name, snapshot.get99thPercentile(), epoch);
    maybeExpand(Expansions.P999, name, snapshot.get999thPercentile(), epoch);
  }

  private void maybeExpand(Expansions expansion, MetricName name, Number count, Long epoch) {
    if (expansions.contains(expansion)) {
      pushGauge(name, count, epoch, expansion.toString());
    }
  }

  protected void pushRegularMetrics(long epoch) {
    for (Entry<String, SortedMap<MetricName, Metric>> entry : getMetricsRegistry()
        .groupedMetrics(predicate).entrySet()) {
      for (Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
        final Metric metric = subEntry.getValue();
        if (metric != null) {
          try {
            metric.processWith(this, subEntry.getKey(), epoch);
          } catch (Exception e) {
            LOG.error("Error pushing metric", e);
          }
        }
      }
    }
  }

  protected void pushVmMetrics(long epoch) {
    sendGauge("jvm.memory.heap.committed", vm.heapCommitted(), epoch);
    sendGauge("jvm.memory.heap.used", vm.heapUsed(), epoch);

    pushGauge("jvm.daemon_thread_count", vm.daemonThreadCount(), epoch);
    pushGauge("jvm.thread_count", vm.threadCount(), epoch);

    for (Entry<String, VirtualMachineMetrics.GarbageCollectorStats> entry : vm
        .garbageCollectors().entrySet()) {
      final String tag = "[type:" + entry.getKey() + "]";
      pushGauge("jvm.gc.time" + tag, entry.getValue().getTime(TimeUnit.MILLISECONDS), epoch);
      pushCounter("jvm.gc.runs" + tag, entry.getValue().getRuns(), epoch);
    }
  }

  private void pushCounter(MetricName metricName, Long count, Long epoch,
                           String... path) {
    pushCounter(metricNameFormatter.format(metricName, path), count, epoch);

  }

  private void pushCounter(String name, Long count, Long epoch) {
    DatadogCounter counter = new DatadogCounter(name, count, epoch, host, this.tags);
    try {
      request.addCounter(counter);
    } catch (Exception e) {
      LOG.error("Error writing counter", e);
    }
  }

  private void pushGauge(MetricName metricName, Number count, Long epoch,
                         String... path) {
    sendGauge(metricNameFormatter.format(metricName, path), count, epoch);
  }

  private void pushGauge(String name, long count, long epoch) {
    sendGauge(name, new Long(count), epoch);
  }

  private void sendGauge(String name, Number count, Long epoch) {
    DatadogGauge gauge = new DatadogGauge(name, count, epoch, host, this.tags);
    try {
      request.addGauge(gauge);
    } catch (Exception e) {
      LOG.error("Error writing gauge", e);
    }
  }

  @Override
  public void shutdown() {
    super.shutdown();
    try {
      transport.close();
    } catch (IOException e) {
      LOG.error("Error closing the datadog transport, ignored.", e);
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

  public static class Builder {

    private String host = null;
    private EnumSet<Expansions> expansions = Expansions.ALL;
    private Boolean vmMetrics = true;
    private String apiKey = null;
    private Clock clock = Clock.defaultClock();
    private MetricPredicate predicate = MetricPredicate.ALL;
    private MetricNameFormatter metricNameFormatter = new DefaultMetricNameFormatter();
    private List<String> tags = new ArrayList<String>();
    private MetricsRegistry metricsRegistry = Metrics.defaultRegistry();
    private Transport transport = null;

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

    public Builder withVmMetricsEnabled(Boolean enabled) {
      this.vmMetrics = enabled;
      return this;
    }

    public Builder withApiKey(String key) {
      this.apiKey = key;
      return this;
    }

    /**
     * Tags that would be sent to datadog with each and every metrics. This could be used to set global metrics
     * like version of the app, environment etc.
     * @param tags List of tags eg: [env:prod, version:1.0.1, name:kafka_client] etc
     */
    public Builder withTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public Builder withClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    public Builder withPredicate(MetricPredicate predicate) {
      this.predicate = predicate;
      return this;
    }

    public Builder withMetricNameFormatter(MetricNameFormatter formatter) {
      this.metricNameFormatter = formatter;
      return this;
    }

    public Builder withMetricsRegistry(MetricsRegistry metricsRegistry) {
      this.metricsRegistry = metricsRegistry;
      return this;
    }

    /**
     * The transport mechanism to push metrics to datadog. Supports http webservice and UDP dogstatsd protocol
     * as of now.
     *
     * @see HttpTransport
     * @see com.yammer.metrics.reporting.transport.UdpTransport
     */
    public Builder withTransport(Transport transport) {
      this.transport = transport;
      return this;
    }

    public DatadogReporter build() {
      if (transport == null) {
        this.transport = new HttpTransport(apiKey);
      }
      return new DatadogReporter(
        metricsRegistry,
        this.predicate,
        VirtualMachineMetrics.getInstance(),
        transport,
        this.clock,
        this.host,
        this.expansions,
        this.vmMetrics,
        metricNameFormatter,
        this.tags);
    }
  }
}
