package io.dropwizard.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.RegexMetricFilter;
import org.coursera.metrics.datadog.transport.AbstractTransportFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

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

  @JsonProperty
  private boolean useRegexFilters = false;

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
    return (useRegexFilters() ?
        new RegexMetricFilter(getIncludes(), getExcludes()) : super.getFilter());
  }

  public boolean useRegexFilters() {
    return useRegexFilters;
  }
}