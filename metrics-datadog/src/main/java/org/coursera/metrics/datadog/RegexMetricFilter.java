package org.coursera.metrics.datadog;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.google.common.collect.ImmutableSet;

import java.util.regex.Pattern;

public class RegexMetricFilter implements MetricFilter {

  ImmutableSet<String> includesExpressions;
  ImmutableSet<String> excludesExpressions;

  public RegexMetricFilter(ImmutableSet<String> includesExpressions,
                           ImmutableSet<String> excludesExpressions) {
    this.includesExpressions = includesExpressions;
    this.excludesExpressions = excludesExpressions;
  }

  public boolean matches(final String name, final Metric metric) {
    // based off of the io.dropwizard.metrics.BaseReporterFactory implementation, but perform regex
    // checks instead of relying on Set.contains()
    boolean useIncl = !includesExpressions.isEmpty();
    boolean useExcl = !excludesExpressions.isEmpty();

    if (useIncl && useExcl) {
      return containsMatch(includesExpressions, name) || !containsMatch(excludesExpressions, name);
    } else if (useIncl && !useExcl) {
      return containsMatch(includesExpressions, name);
    } else if (!useIncl && useExcl) {
      return !containsMatch(excludesExpressions, name);
    } else {
      return true;
    }
  }

  private static boolean containsMatch(ImmutableSet<String> regexExpressions, String metricName) {
    for (String regexExpression : regexExpressions) {
      if (Pattern.matches(regexExpression, metricName)) {
        // just need to match on a single value - return as soon as we do
        return true;
      }
    }

    return false;
  }
}
