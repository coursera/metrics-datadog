package com.codahale.metrics.datadog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

public class HttpTransport implements Transport {
  private final String apiKey;
  private final AsyncHttpClient client;
  private final String seriesUrl;
  private static final Logger LOG = LoggerFactory
      .getLogger(HttpTransport.class);

  public HttpTransport(String host, String apiKey) {
    this.apiKey = apiKey;
    this.client = new AsyncHttpClient();
    this.seriesUrl = String.format("https://%s/api/v1/series?api_key=%s", host,
        apiKey);
  }

  public static class HttpRequest implements Transport.Request {
    private final BoundRequestBuilder requestBuilder;
    private final ByteArrayOutputStream out;

    public HttpRequest(HttpTransport transport, String apiKey,
        BoundRequestBuilder requestBuilder) throws IOException {
      this.requestBuilder = requestBuilder;
      this.requestBuilder.addHeader("Content-Type", "application/json");
      this.out = new ByteArrayOutputStream();
    }

    public OutputStream getBodyWriter() {
      return out;
    }

    public void send() throws Exception {
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
    }
  }

  public HttpRequest prepare() throws IOException {
    BoundRequestBuilder builder = client.preparePost(seriesUrl);
    return new HttpRequest(this, apiKey, builder);
  }
}
