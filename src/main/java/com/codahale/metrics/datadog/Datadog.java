package com.codahale.metrics.datadog;

import com.codahale.metrics.datadog.model.DatadogCounter;
import com.codahale.metrics.datadog.model.DatadogGauge;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Datadog {
  private final ByteArrayOutputStream out;
  private AsyncHttpClient.BoundRequestBuilder requestBuilder;
  private static final Logger LOG = LoggerFactory.getLogger(Datadog.class);
  private static final JsonFactory jsonFactory = new JsonFactory();
  private static final ObjectMapper mapper = new ObjectMapper(jsonFactory);
  private JsonGenerator jsonOut;

  public Datadog(String host, String apiKey) {
    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
    AsyncHttpClient client =
        new AsyncHttpClient(builder.setConnectionTimeoutInMs(5000)
          .setWebSocketIdleTimeoutInMs(30000)
          .build());

    this.requestBuilder = client
        .preparePost(String.format("https://%s/api/v1/series?api_key=%s",
                                   host,
                                   apiKey))
        .addHeader("Content-Type", "application/json");
    this.out = new ByteArrayOutputStream();
  }

  public Datadog(AsyncHttpClient.BoundRequestBuilder requestBuilder) {
    this.requestBuilder = requestBuilder;
    this.out = new ByteArrayOutputStream();
  }

  public void createSeries() {
    try {
      jsonOut = jsonFactory.createGenerator(out);
      jsonOut.writeStartObject();
      jsonOut.writeArrayFieldStart("series");
    } catch (Exception e) {
      LOG.error("Error creating json", e);
    }
  }

  public void endSeries() {
    try {
      jsonOut.writeEndArray();
      jsonOut.writeEndObject();
      jsonOut.flush();
    } catch (Throwable e) {
      LOG.error("Error ending json", e);
    }
  }

  public void sendSeries() {
    try {
      endSeries();
      out.flush();
      out.close();
      requestBuilder.setBody(out.toByteArray())
          .execute(new AsyncHandler<Void>() {

            public STATE onBodyPartReceived(HttpResponseBodyPart bp)
                throws Exception {
              return STATE.CONTINUE;
            }

            public Void onCompleted() throws Exception {
              return null;
            }

            public STATE onHeadersReceived(HttpResponseHeaders headers)
                throws Exception {
              return STATE.CONTINUE;
            }

            public STATE onStatusReceived(HttpResponseStatus arg0)
                throws Exception {
              return STATE.CONTINUE;
            }

            public void onThrowable(Throwable t) {
              LOG.error("Error Writing Datadog metrics", t);
            }

          }).get();
    } catch (Throwable e) {
      LOG.error("Error sending metrics", e);
    }
  }

  public void add(Object value) throws IOException {
    mapper.writeValue(jsonOut, value);
  }

  public void addGauge(String name, Number value, long timestamp, String host)
      throws IOException {
    add(new DatadogGauge(name, value, timestamp, host));
  }

  public void addCounter(String name, Long value, long timestamp, String host)
      throws IOException {
    add(new DatadogCounter(name, value, timestamp, host));
  }

  public OutputStream getBodyWriter() {
    return out;
  }
}
