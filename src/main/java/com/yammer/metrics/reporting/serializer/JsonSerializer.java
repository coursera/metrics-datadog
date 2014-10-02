package com.yammer.metrics.reporting.serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yammer.metrics.reporting.model.DatadogCounter;
import com.yammer.metrics.reporting.model.DatadogGauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Serialize datadog time series object into json
 *
 * @see <a href="http://docs.datadoghq.com/api/">API docs</a>
 */
public class JsonSerializer implements Serializer {
  private static final JsonFactory jsonFactory = new JsonFactory();
  private static final ObjectMapper mapper = new ObjectMapper(jsonFactory);
  private static final Logger LOG = LoggerFactory
      .getLogger(JsonSerializer.class);

  private JsonGenerator jsonOut;
  private ByteArrayOutputStream outputStream;

  public void startObject() throws IOException {
    outputStream = new ByteArrayOutputStream(2048);
    jsonOut = jsonFactory.createGenerator(outputStream);
    jsonOut.writeStartObject();
    jsonOut.writeFieldName("series");
    jsonOut.writeStartArray();
  }

  public void appendGauge(DatadogGauge gauge) throws IOException {
    mapper.writeValue(jsonOut, gauge);
  }

  public void appendCounter(DatadogCounter counter) throws IOException {
    mapper.writeValue(jsonOut, counter);
  }

  public void endObject() throws IOException {
    jsonOut.writeEndArray();
    jsonOut.writeEndObject();
    jsonOut.flush();
    outputStream.close();
  }

  public String getAsString() throws UnsupportedEncodingException {
    return outputStream.toString("UTF-8");
  }
}
