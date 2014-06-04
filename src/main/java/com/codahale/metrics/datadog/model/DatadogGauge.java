package com.codahale.metrics.datadog.model;


public class DatadogGauge extends DatadogSeries<Number> {
  public DatadogGauge(String name, Number count, Long epoch, String host) {
    super(name, count, epoch, host);
  }
  
  public String getType() {
    return "gauge";
  }
}
