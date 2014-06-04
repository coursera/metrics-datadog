package com.codahale.metrics.datadog;

import com.yammer.metrics.core.MetricName;

public class DefaultMetricNameFormatter implements MetricNameFormatter {

  public String format(MetricName name, String... path) {
    final StringBuilder sb = new StringBuilder(name.getGroup());
    sb.append('.');
    sb.append(name.getType()).append('.');

    if (name.hasScope()) {
      sb.append(name.getScope()).append('.');
    }

    String[] metricParts = name.getName().split("\\[");
    sb.append(metricParts[0]);

    for (String part : path) {
      sb.append('.').append(part);
    }

    for (int i = 1; i < metricParts.length; i++) {
      sb.append('[').append(metricParts[i]);
    }
    return sb.toString();
  }
}
