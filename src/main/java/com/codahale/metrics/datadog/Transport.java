package com.codahale.metrics.datadog;

import java.io.IOException;
import java.io.OutputStream;

public interface Transport {
  public Request prepare() throws IOException ;
  
  public interface Request {
    OutputStream getBodyWriter();
    void send() throws Exception;
  }
}
