package com.yammer.metrics.reporting;

import com.yammer.metrics.reporting.model.DatadogCounter;
import com.yammer.metrics.reporting.model.DatadogGauge;
import com.yammer.metrics.reporting.serializer.JsonSerializer;
import com.yammer.metrics.reporting.serializer.Serializer;
import org.apache.http.entity.ContentType;

import java.io.IOException;

import static org.apache.http.client.fluent.Request.*;

/**
 * Uses the datadog http webservie to push metrics.
 *
 * @see <a href="http://docs.datadoghq.com/api/">API docs</a>
 */
public class HttpTransport implements Transport {

  private final String seriesUrl;

  public HttpTransport(String apiKey) {
    this.seriesUrl = String.format("https://app.datadoghq.com/api/v1/series?api_key=%s", apiKey);
  }

  public Request prepare() throws IOException {
    return new HttpRequest(this);
  }

  public void close() throws IOException {
  }

  public static class HttpRequest implements Transport.Request {
    protected final Serializer serializer;

    protected final HttpTransport transport;

    public HttpRequest(HttpTransport transport) throws IOException {
      this.transport = transport;
      serializer = new JsonSerializer();
      serializer.startObject();
    }

    public void addGauge(DatadogGauge gauge) throws IOException {
      serializer.appendGauge(gauge);
    }

    public void addCounter(DatadogCounter counter) throws IOException {
      serializer.appendCounter(counter);
    }

    public void send() throws Exception {
      serializer.endObject();
      Post(this.transport.seriesUrl)
          .useExpectContinue()
          .bodyString(serializer.getAsString(), ContentType.APPLICATION_JSON).execute()
          .discardContent();
    }
  }
}
