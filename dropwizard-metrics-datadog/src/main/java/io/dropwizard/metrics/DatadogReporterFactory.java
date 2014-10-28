package io.dropwizard.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.transport.HttpTransport;
import org.coursera.metrics.datadog.transport.Transport;
import org.coursera.metrics.datadog.transport.UdpTransport;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

@JsonTypeName("datadog")
public class DatadogReporterFactory extends BaseReporterFactory {
  private final String TRANSPORT_UDP = "udp";
  private final String TRANSPORT_HTTP = "http";

  @NotNull
  @Pattern(regexp = "http|udp", flags = Pattern.Flag.CASE_INSENSITIVE)
  @JsonProperty
  private String transportType = null;

  @JsonProperty
  private String host = null;

  @JsonProperty
  private String apiKey = null;

  @JsonProperty
  private List<String> tags = null;

  @JsonProperty
  private int connectTimeout = 5000;  // in milliseconds

  @JsonProperty
  private int socketTimeout = 5000;   // in milliseconds

  @AssertTrue(message = "When using HTTP protocol you must provide host and apiKey")
  private boolean isValidConfig() {
      if (TRANSPORT_HTTP.equals(transportType)) {
          if (host == null || apiKey == null) {
              return false;
          }
      }

      return true;
  }

  public ScheduledReporter build(MetricRegistry registry) {
    Transport transport;

    if (TRANSPORT_UDP.equals(transportType)) {
        transport = new UdpTransport.Builder()
                .build();
    } else if (TRANSPORT_HTTP.equals(transportType)) {
        transport = new HttpTransport.Builder()
                .withApiKey(apiKey)
                .withConnectTimeout(connectTimeout)
                .withSocketTimeout(socketTimeout)
                .build();
    } else {
        throw new IllegalArgumentException("Invalid transport type provided");
    }

    return DatadogReporter.forRegistry(registry)
      .withTransport(transport)
      .withHost(host)
      .withTags(tags)
      .filter(getFilter())
      .convertDurationsTo(getDurationUnit())
      .convertRatesTo(getRateUnit())
      .build();
  }
}