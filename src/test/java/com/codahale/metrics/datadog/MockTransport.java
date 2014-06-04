package com.codahale.metrics.datadog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MockTransport implements Transport {
  public MockRequest lastRequest;
  public int numRequests = 0;
  public OutputStream out;

  public MockTransport(OutputStream out) {
    this.out = out;
  }

  public MockTransport() {

  }

  public static class MockRequest implements Request {
    private final OutputStream out;

    MockRequest(OutputStream out) {
      if (out != null) {
        this.out = out;
      } else {
        this.out = new ByteArrayOutputStream();
      }
    }

    public OutputStream getBodyWriter() {
      return out;
    }

    public void send() throws Exception {

    }

    public byte[] getPostBody() {
      if (out instanceof ByteArrayOutputStream) {
        return ((ByteArrayOutputStream) out).toByteArray();
      }
      return null;
    }
  }

  public Request prepare() throws IOException {
    MockRequest request = new MockRequest(out);
    lastRequest = request;
    numRequests++;

    return lastRequest;
  }

}
