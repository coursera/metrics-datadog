package com.codahale.metrics.datadog.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public abstract class DatadogSeries<T extends Number> {
  abstract protected String getType();

  private String name;
  private T count;
  private Long epoch;
  private String host;
  private List<String> tags;

  // Expect the tags in the pattern
  // namespace.metricName[tag1:value1,tag2:value2,etc....]
  private final Pattern tagPattern = Pattern
      .compile("([\\w\\.]+)\\[([\\w\\W]+)\\]");

  public DatadogSeries(String name, T count, Long epoch, String host) {
    Matcher matcher = tagPattern.matcher(name);
    this.tags = new ArrayList<String>();

    if (matcher.find() && matcher.groupCount() == 2) {
      this.name = matcher.group(1);
      for(String t : matcher.group(2).split("\\,")) {
        this.tags.add(t);
      }
    } else {
      this.name = name;
    }

    this.count = count;
    this.epoch = epoch;
    this.host = host;
  }

  @JsonInclude(Include.NON_NULL)
  public String getHost() {
    return host;
  }

  public String getMetric() {
    return name;
  }

  public List<String> getTags() {
    return tags;
  }

  public List<List<Number>> getPoints() {
    List<Number> point = new ArrayList<Number>();
    point.add(epoch);
    point.add(count);

    List<List<Number>> points = new ArrayList<List<Number>>();
    points.add(point);
    return points;
  }
}
