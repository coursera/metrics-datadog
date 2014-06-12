package com.codahale.metrics.datadog;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DatadogReporterTest {
  MetricRegistry metricsRegistry;
  DatadogReporter reporter;
  private Datadog datadog = mock(Datadog.class);
  private final long timestamp = 1000198;
  private final Clock clock = mock(Clock.class);
  private static final String host = "hostname";

  @Before
  public void setUp() {
    when(clock.getTime()).thenReturn(timestamp * 1000);
    metricsRegistry = new MetricRegistry();
    reporter = DatadogReporter
        .forRegistry(metricsRegistry)
        .withHost(host)
        .withClock(clock)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build(datadog);
  }

  @Test
  public void reportsByteGaugeValues() throws Exception {
    Gauge gauge = gauge((byte) 1);

    reporter.report(map("gauge", gauge),
        this.<Counter>map(),
        this.<Histogram>map(),
        this.<Meter>map(),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addGauge("gauge", (byte) 1, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsShortGaugeValues() throws Exception {
    reporter.report(map("gauge", gauge((short) 1)),
        this.<Counter>map(),
        this.<Histogram>map(),
        this.<Meter>map(),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addGauge("gauge", (short) 1, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsIntegerGaugeValues() throws Exception {
    reporter.report(map("gauge", gauge(1)),
        this.<Counter>map(),
        this.<Histogram>map(),
        this.<Meter>map(),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addGauge("gauge", 1, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsLongGaugeValues() throws Exception {
    reporter.report(map("gauge", gauge(1L)),
        this.<Counter>map(),
        this.<Histogram>map(),
        this.<Meter>map(),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addGauge("gauge", 1L, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsFloatGaugeValues() throws Exception {
    reporter.report(map("gauge", gauge(1.1f)),
        this.<Counter>map(),
        this.<Histogram>map(),
        this.<Meter>map(),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addGauge("gauge", 1.1f, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsDoubleGaugeValues() throws Exception {
    reporter.report(map("gauge", gauge(1.1)),
        this.<Counter>map(),
        this.<Histogram>map(),
        this.<Meter>map(),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addGauge("gauge", 1.1, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsCounters() throws Exception {
    final Counter counter = mock(Counter.class);
    when(counter.getCount()).thenReturn(100L);

    reporter.report(this.<Gauge>map(),
        this.<Counter>map("counter", counter),
        this.<Histogram>map(),
        this.<Meter>map(),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addCounter("counter", 100L, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsHistograms() throws Exception {
    final Histogram histogram = mock(Histogram.class);
    when(histogram.getCount()).thenReturn(1L);

    final Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.getMax()).thenReturn(2L);
    when(snapshot.getMean()).thenReturn(3.0);
    when(snapshot.getMin()).thenReturn(4L);
    when(snapshot.getStdDev()).thenReturn(5.0);
    when(snapshot.getMedian()).thenReturn(6.0);
    when(snapshot.get75thPercentile()).thenReturn(7.0);
    when(snapshot.get95thPercentile()).thenReturn(8.0);
    when(snapshot.get98thPercentile()).thenReturn(9.0);
    when(snapshot.get99thPercentile()).thenReturn(10.0);
    when(snapshot.get999thPercentile()).thenReturn(11.0);

    when(histogram.getSnapshot()).thenReturn(snapshot);

    reporter.report(this.<Gauge>map(),
        this.<Counter>map(),
        this.<Histogram>map("histogram", histogram),
        this.<Meter>map(),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addCounter("histogram.count", 1L, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.max", 2L, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.mean", 3.0, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.min", 4L, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.stddev", 5.0, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.p50", 6.0, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.p75", 7.0, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.p95", 8.0, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.p98", 9.0, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.p99", 10.0, timestamp, host);
    inOrder.verify(datadog).addGauge("histogram.p999", 11.0, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsMeters() throws Exception {
    final Meter meter = mock(Meter.class);
    when(meter.getCount()).thenReturn(1L);
    when(meter.getOneMinuteRate()).thenReturn(2.0);
    when(meter.getFiveMinuteRate()).thenReturn(3.0);
    when(meter.getFifteenMinuteRate()).thenReturn(4.0);
    when(meter.getMeanRate()).thenReturn(5.0);

    reporter.report(this.<Gauge>map(),
        this.<Counter>map(),
        this.<Histogram>map(),
        this.<Meter>map("meter", meter),
        this.<Timer>map());

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addCounter("meter.count", 1L, timestamp, host);
    inOrder.verify(datadog).addGauge("meter.1MinuteRate", 2.0, timestamp, host);
    inOrder.verify(datadog).addGauge("meter.5MinuteRate", 3.0, timestamp, host);
    inOrder.verify(datadog).addGauge("meter.15MinuteRate", 4.0, timestamp, host);
    inOrder.verify(datadog).addGauge("meter.meanRate", 5.0, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsTimers() throws Exception {
    final Timer timer = mock(Timer.class);
    when(timer.getCount()).thenReturn(1L);
    when(timer.getMeanRate()).thenReturn(2.0);
    when(timer.getOneMinuteRate()).thenReturn(3.0);
    when(timer.getFiveMinuteRate()).thenReturn(4.0);
    when(timer.getFifteenMinuteRate()).thenReturn(5.0);

    final Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.getMax()).thenReturn(TimeUnit.MILLISECONDS.toNanos(100));
    when(snapshot.getMean())
        .thenReturn((double) TimeUnit.MILLISECONDS.toNanos(200));
    when(snapshot.getMin())
        .thenReturn(TimeUnit.MILLISECONDS.toNanos(300));
    when(snapshot.getStdDev())
        .thenReturn((double) TimeUnit.MILLISECONDS.toNanos(400));
    when(snapshot.getMedian())
        .thenReturn((double) TimeUnit.MILLISECONDS.toNanos(500));
    when(snapshot.get75thPercentile())
        .thenReturn((double) TimeUnit.MILLISECONDS.toNanos(600));
    when(snapshot.get95thPercentile())
        .thenReturn((double) TimeUnit.MILLISECONDS.toNanos(700));
    when(snapshot.get98thPercentile())
        .thenReturn((double) TimeUnit.MILLISECONDS.toNanos(800));
    when(snapshot.get99thPercentile())
        .thenReturn((double) TimeUnit.MILLISECONDS.toNanos(900));
    when(snapshot.get999thPercentile())
        .thenReturn((double) TimeUnit.MILLISECONDS
        .toNanos(1000));

    when(timer.getSnapshot()).thenReturn(snapshot);

    reporter.report(this.<Gauge>map(),
        this.<Counter>map(),
        this.<Histogram>map(),
        this.<Meter>map(),
        map("timer", timer));

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addGauge("timer.max", 100.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.mean", 200.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.min", 300.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.stddev", 400.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.p50", 500.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.p75", 600.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.p95", 700.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.p98", 800.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.p99", 900.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.p999", 1000.0, timestamp, host);
    inOrder.verify(datadog).addCounter("timer.count", 1L, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.1MinuteRate", 3.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.5MinuteRate", 4.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.15MinuteRate", 5.0, timestamp, host);
    inOrder.verify(datadog).addGauge("timer.meanRate", 2.0, timestamp, host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsWithFilter() throws Exception {
    Counter counter = metricsRegistry.counter("my.metric.counter");
    counter.inc(123);
    Counter unwantedCounter = metricsRegistry.counter("counter");
    unwantedCounter.inc(456);

    DatadogReporter reporterWithFilter = DatadogReporter
        .forRegistry(metricsRegistry)
        .withHost(host)
        .withClock(clock)
        .filter(new NameMetricFilter("my.metric"))
        .build(datadog);

    reporterWithFilter.report();

    final InOrder inOrder = inOrder(datadog);
    inOrder.verify(datadog).createSeries();
    inOrder.verify(datadog).addCounter("my.metric.counter",
                                       123L,
                                       timestamp,
                                       host);
    inOrder.verify(datadog, never()).addCounter("counter",
                                               123L,
                                               timestamp,
                                               host);
    inOrder.verify(datadog).sendSeries();

    verify(datadog, times(1)).sendSeries();
    verifyNoMoreInteractions(datadog);
  }

  @Test
  public void reportsWithTagged() throws Exception {
    AsyncHttpClient.BoundRequestBuilder requestBuilder =
        mock(AsyncHttpClient.BoundRequestBuilder.class);
    Datadog dd = new Datadog(requestBuilder);
    DatadogReporter reporterMockSend = DatadogReporter
        .forRegistry(metricsRegistry)
        .withClock(clock)
        .withHost(host)
        .build(dd);
    metricsRegistry.meter(MetricRegistry.name(String.class, "meter[with,tags]"));

    reporterMockSend.report();

    String body = dd.getBodyWriter().toString();
    HashMap request = new ObjectMapper().readValue(body, HashMap.class);
    List<Object> series = (List<Object>) request.get("series");

    for (Object o : series) {
      HashMap<String, Object> rec = (HashMap<String, Object>) o;
      List<String> tags = (List<String>) rec.get("tags");
      String name = rec.get("metric").toString();

      assertTrue(name.startsWith("java.lang.String.meter"));
      assertEquals("with", tags.get(0));
      assertEquals("tags", tags.get(1));
    }
  }

  @Test
  public void reportsWithHostname() throws Exception {
    AsyncHttpClient.BoundRequestBuilder requestBuilder =
        mock(AsyncHttpClient.BoundRequestBuilder.class);
    Datadog dd = new Datadog(requestBuilder);
    Datadog ddNoHost = new Datadog(requestBuilder);
    DatadogReporter reporterMockSend = DatadogReporter
        .forRegistry(metricsRegistry)
        .withClock(clock)
        .withHost(host)
        .build(dd);
    DatadogReporter reporterMockSendNoHost = DatadogReporter
        .forRegistry(metricsRegistry)
        .withClock(clock)
        .build(ddNoHost);

    Counter counter =
        metricsRegistry.counter(
            MetricRegistry.name(DatadogReporterTest.class, "my.counter"));
    counter.inc();

    reporterMockSend.report();
    reporterMockSendNoHost.report();

    String hostBody = dd.getBodyWriter().toString();
    String noHostBody = ddNoHost.getBodyWriter().toString();
    assertFalse(noHostBody.contains("\"host\":\"hostname\""));
    assertTrue(hostBody.contains("\"host\":\"hostname\""));
  }

  private class NameMetricFilter implements MetricFilter {
    private final String include;

    private NameMetricFilter(final String include) {
      this.include = include;
    }

    public boolean matches(final String name, final Metric metric) {
      return (name.contains(include));
    }
  }

  private <T> SortedMap<String, T> map() {
    return new TreeMap<String, T>();
  }

  private <T> SortedMap<String, T> map(String name, T metric) {
    final TreeMap<String, T> map = new TreeMap<String, T>();
    map.put(name, metric);
    return map;
  }

  private <T> Gauge gauge(T value) {
    final Gauge gauge = mock(Gauge.class);
    when(gauge.getValue()).thenReturn(value);
    return gauge;
  }
}
