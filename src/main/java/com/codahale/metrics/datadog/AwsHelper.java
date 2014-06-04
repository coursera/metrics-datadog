package com.codahale.metrics.datadog;

import java.io.IOException;
import java.util.concurrent.Future;

import com.ning.http.client.*;

public class AwsHelper {

  public static final String url = "http://169.254.169.254/latest/meta-data/instance-id";

  public static String getEc2InstanceId() throws IOException {
    AsyncHttpClient client = new AsyncHttpClient();
    try {
      Future<Response> f = client.prepareGet(url).execute();
      Response resp = f.get();

      return resp.getResponseBody();
    } catch (Throwable t) {
      throw new IOException(t);
    }
  }
}