package com.yammer.metrics.reporting;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yammer.metrics.core.Clock;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.VirtualMachineMetrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.DatadogReporter.Expansions;

public class DatadogReporterTest {

  MetricsRegistry metricsRegistry;
  MockTransport transport;
  VirtualMachineMetrics vm;
  Clock clock;
  DatadogReporter ddNoHost;
  DatadogReporter dd;
  List<String> tags;
  static final MetricPredicate ALL = MetricPredicate.ALL;

  @Before
  public void setUp() {
    metricsRegistry = new MetricsRegistry();
    transport = new MockTransport();
    clock = Clock.defaultClock();
    vm = VirtualMachineMetrics.getInstance();
    ddNoHost = new DatadogReporter(metricsRegistry, MetricPredicate.ALL,
                                   VirtualMachineMetrics.getInstance(), transport,
                                   Clock.defaultClock(),
                                   null, DatadogReporter.Expansions.ALL, true,
                                   new DefaultMetricNameFormatter(), tags);

    dd = new DatadogReporter(metricsRegistry, MetricPredicate.ALL,
                             VirtualMachineMetrics.getInstance(), transport, Clock.defaultClock(),
                             "hostname", DatadogReporter.Expansions.ALL, true,
                             new DefaultMetricNameFormatter(), tags);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBasicSend() throws IOException {
    dd.printVmMetrics = false;

    Counter counter = metricsRegistry.newCounter(DatadogReporterTest.class,
                                                 "my.counter");
    counter.inc();

    metricsRegistry.newGauge(DatadogReporterTest.class, "my.invocations",
                             new Gauge<Long>() {
                               private long numInovcations = 123;

                               @Override
                               public Long value() {
                                 return numInovcations++;
                               }

                             });

    assertEquals(0, transport.numRequests);
    dd.run();
    assertEquals(1, transport.numRequests);

    String body = transport.lastRequest.getPostBody();
    Map<String, Object> request = new ObjectMapper().readValue(body,
                                                               HashMap.class);

    assertEquals(1, request.keySet().size());
    List<Object> series = (List<Object>) request.get("series");

    assertEquals(2, series.size());
    Map<String, Object> counterEntry = (Map<String, Object>) series.get(0);
    Map<String, Object> gaugeEntry = (Map<String, Object>) series.get(1);

    assertEquals("com.yammer.metrics.reporting.DatadogReporterTest.my.counter",
                 counterEntry.get("metric"));
    assertEquals("counter", counterEntry.get("type"));
    List<List<Number>> points = (List<List<Number>>) counterEntry.get("points");
    assertEquals(1, points.get(0).get(1));

    assertEquals(
        "com.yammer.metrics.reporting.DatadogReporterTest.my.invocations",
        gaugeEntry.get("metric"));
    assertEquals("gauge", gaugeEntry.get("type"));
    points = (List<List<Number>>) gaugeEntry.get("points");
    assertEquals(123, points.get(0).get(1));
  }

  @Test
  public void testTimerExpansion() throws JsonParseException, JsonMappingException,
                                          IOException {
    // TODO: punting on testing actual values, as Timer is a class not an interface,
    // and requires a threadPool and all that.
    Timer timer = metricsRegistry.newTimer(DatadogReporterTest.class, "my.timer");
    timer.update(1, TimeUnit.MILLISECONDS);

    for (Expansions expansion : Expansions.ALL) {
      expansionTestHelper(expansion);
    }
  }

  @SuppressWarnings("unchecked")
  private void expansionTestHelper(Expansions expansion) throws JsonParseException,
                                                                JsonMappingException, IOException {
    MockTransport transport = new MockTransport();
    DatadogReporter dd = new DatadogReporter(metricsRegistry, MetricPredicate.ALL,
                                             VirtualMachineMetrics.getInstance(), transport,
                                             Clock.defaultClock(),
                                             "hostname", EnumSet.of(expansion), false,
                                             new DefaultMetricNameFormatter(), tags);

    assertEquals(0, transport.numRequests);
    dd.run();
    assertEquals(1, transport.numRequests);

    String body = transport.lastRequest.getPostBody();
    Map<String, Object> request = new ObjectMapper().readValue(body,
                                                               HashMap.class);
    List<Object> series = (List<Object>) request.get("series");

    assertEquals(1, series.size());
    Map<String, Object> metric = (Map<String, Object>) series.get(0);

    assertEquals(
        "com.yammer.metrics.reporting.DatadogReporterTest.my.timer." + expansion.toString(),
        metric.get("metric"));
    assertEquals(expansion.equals(Expansions.COUNT) ? "counter" : "gauge", metric.get("type"));
  }

  @Test
  public void testSupplyHostname() throws IOException {
    Counter counter = metricsRegistry.newCounter(DatadogReporterTest.class,
                                                 "my.counter");
    counter.inc();

    assertEquals(0, transport.numRequests);
    ddNoHost.run();
    assertEquals(1, transport.numRequests);
    String noHostBody = transport.lastRequest.getPostBody();

    dd.run();
    assertEquals(2, transport.numRequests);
    String hostBody = transport.lastRequest.getPostBody();

    assertFalse(noHostBody.indexOf("\"host\":\"hostname\"") > -1);
    assertTrue(hostBody.indexOf("\"host\":\"hostname\"") > -1);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTaggedMeter() throws Throwable {
    Meter s = metricsRegistry.newMeter(String.class,
                                       "meter[with,tags]", "ticks", TimeUnit.SECONDS);
    s.mark();

    ddNoHost.printVmMetrics = false;
    ddNoHost.run();
    String body = transport.lastRequest.getPostBody();

    Map<String, Object> request = new ObjectMapper().readValue(body,
                                                               HashMap.class);
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
}

