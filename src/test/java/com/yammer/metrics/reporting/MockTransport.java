package com.yammer.metrics.reporting;

import java.io.IOException;

public class MockTransport implements Transport {

  public MockRequest lastRequest;
  public int numRequests = 0;

  public MockTransport() {
  }

  public void close() throws IOException {
  }

  public static class MockRequest extends HttpTransport.HttpRequest {

    public MockRequest(HttpTransport transport) throws IOException {
      super(transport);
    }

    public String getPostBody() throws IOException {
      return super.serializer.getAsString();
    }
  }

  public Request prepare() throws IOException {
    MockRequest request = new MockRequest(new HttpTransport("http://example.com"));
    lastRequest = request;
    numRequests++;

    return lastRequest;
  }

}
