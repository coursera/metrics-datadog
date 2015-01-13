package io.dropwizard.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableSet;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.transport.AbstractTransportFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.regex.Pattern;

@JsonTypeName("datadog")
public class DatadogReporterFactory extends BaseReporterFactory {

  @JsonProperty
  private String host = null;

  @JsonProperty
  private List<String> tags = null;

  @Valid
  @NotNull
  @JsonProperty
  private AbstractTransportFactory transport = null;

  public ScheduledReporter build(MetricRegistry registry) {
    return DatadogReporter.forRegistry(registry)
        .withTransport(transport.build())
        .withHost(host)
        .withTags(tags)
        .filter(getFilter())
        .convertDurationsTo(getDurationUnit())
        .convertRatesTo(getRateUnit())
        .build();
  }

  @Override
  public MetricFilter getFilter() {
    return new MetricFilter() {
      public boolean matches(final String name, final Metric metric) {
        // based off of the io.dropwizard.metrics.BaseReporterFactory implementation, but perform regex checks instead
        // of relying on a strict Set.contains()
        boolean useIncl = !getIncludes().isEmpty();
        boolean useExcl = !getExcludes().isEmpty();

        if (useIncl && useExcl) {
          return containsMatch(getIncludes(), name) || !containsMatch(getExcludes(), name);
        } else if (useIncl && !useExcl) {
          return containsMatch(getIncludes(), name);
        } else if (!useIncl && useExcl) {
          return !containsMatch(getExcludes(), name);
        } else {
          return true;
        }
      }
    };
  }

  private static boolean containsMatch(ImmutableSet<String> regexExpressions, String metricName) {
    for (String regexExpression : regexExpressions) {
      // Note that since Pattern.matches() requires the entire string to match the specified regex pattern we do not
      // have to worry about unintentional partial matches, i.e. a metricName of "foobar" matching against a provided
      // includes value "foo". The user would have to explicitly specify "foo.*" for this match to occur.
      if (Pattern.matches(regexExpression, metricName)) {
        // just need to match on a single value - return as soon as we do
        return true;
      }
    }

    return false;
  }
}