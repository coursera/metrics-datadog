package com.yammer.metrics.reporting.transport;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.yammer.metrics.reporting.Transport;
import com.yammer.metrics.reporting.model.DatadogCounter;
import com.yammer.metrics.reporting.model.DatadogGauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Uses dogstatsd UDP protocol to push metrics to datadog. Note that datadog doesn't support
 * time in the UDP protocol. So all metrics are against current time.
 * <p/>
 * Also dogstatsd UDP doesn't support batching of metrics, so it pushes metrics as it receives
 * rather than batching.
 *
 * @see <a href="http://docs.datadoghq.com/guides/dogstatsd">dogstatsd</a>
 */
public class UdpTransport implements Transport {

  private static final Logger LOG = LoggerFactory.getLogger(UdpTransport.class);
  private final StatsDClient statsd;

  private UdpTransport(String prefix, String statsdHost, int port, String[] globalTags) {
    statsd = new NonBlockingStatsDClient(
        prefix,
        statsdHost,
        port,
        globalTags
    );
  }

  public void close() throws IOException {
    statsd.stop();
  }

  public static class Builder {
    String prefix = null;
    String statsdHost = "localhost";
    int port = 8125;

    public Builder withPrefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    public Builder withStatsdHost(String statsdHost) {
      this.statsdHost = statsdHost;
      return this;
    }

    public Builder withPort(int port) {
      this.port = port;
      return this;
    }

    public UdpTransport build() {
      return new UdpTransport(prefix, statsdHost, port, new String[0]);
    }
  }

  public Request prepare() throws IOException {
    return new DogstatsdRequest(statsd);
  }

  public static class DogstatsdRequest implements Transport.Request {
    private final StatsDClient statsdClient;

    public DogstatsdRequest(StatsDClient statsdClient) {
      this.statsdClient = statsdClient;
    }

    /**
     * statsd has no notion of batch request, so gauges are pushed as they are received
     */
    public void addGauge(DatadogGauge gauge) {
      if (gauge.getPoints().size() > 1) {
        LOG.debug("Gauge " + gauge.getMetric() + " has more than one data point, " +
            "will pick the first point only");
      }
      double value = gauge.getPoints().get(0).get(1).doubleValue();
      String[] tags = gauge.getTags().toArray(new String[gauge.getTags().size()]);
      statsdClient.gauge(gauge.getMetric(), value, tags);
    }

    /**
     * statsd has no notion of batch request, so counters are pushed as they are received
     */
    public void addCounter(DatadogCounter counter) {
      if (counter.getPoints().size() > 1) {
        LOG.debug("Counter " + counter.getMetric() + " has more than one data point, " +
            "will pick the first point only");
      }
      int value = counter.getPoints().get(0).get(1).intValue();
      String[] tags = counter.getTags().toArray(new String[counter.getTags().size()]);
      statsdClient.count(counter.getMetric(), value, tags);
    }

    /**
     * For statsd the metrics are pushed as they are received. So there is nothing do in send.
     */
    public void send() {
    }
  }
}
