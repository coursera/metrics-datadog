package io.dropwizard.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import org.fest.assertions.api.Assertions;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatadogReporterFactoryTest {
  @Test
  public void isDiscoverable() throws Exception {
    Assertions
        .assertThat(new DiscoverableSubtypeResolver().getDiscoveredSubtypes())
        .contains(DatadogReporterFactory.class);
  }

  @Test
  public void testExactFilterMatch() {
    MetricFilter metricFilter = createMetricFilter(ImmutableSet.of("foobar"), ImmutableSet.of());
    // our includes filter should always accept an exact match
    Assertions.assertThat(metricFilter.matches("foobar", mock(Metric.class))).isTrue();
  }

  @Test
  public void testPartialFilterMatch() {
    MetricFilter metricFilter = createMetricFilter(ImmutableSet.of("foo"), ImmutableSet.of());
    // a provided includes filter of "foo" should not pass the partial match "foobar"
    Assertions.assertThat(metricFilter.matches("foobar", mock(Metric.class))).isFalse();
  }

  @Test
  public void testPartialRegexFilterMatch() {
    MetricFilter metricFilter = createMetricFilter(ImmutableSet.of("foo.*"), ImmutableSet.of());
    // a provided regex expression "foo.*" should pass the partial match "foobar"
    Assertions.assertThat(metricFilter.matches("foobar", mock(Metric.class))).isTrue();
  }

  private static MetricFilter createMetricFilter(ImmutableSet includesSet, ImmutableSet excludesSet) {
    DatadogReporterFactory reporterFactory = mock(DatadogReporterFactory.class);
    when(reporterFactory.getIncludes()).thenReturn(includesSet);
    when(reporterFactory.getExcludes()).thenReturn(excludesSet);
    when(reporterFactory.getFilter()).thenCallRealMethod();
    return reporterFactory.getFilter();
  }
}