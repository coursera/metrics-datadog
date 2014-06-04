# Metrics Datadog Reporter
Simple Metrics reporter that sends reporting info to Datadog.

## Usage

~~~scala
import com.codahale.metrics.datadog.DatadogReporter
import com.codahale.metrics.datadog.DatadogReporter.Expansions._

...
val expansions = EnumSet.of(COUNT, RATE_1_MINUTE, RATE_15_MINUTE, MEDIAN, P95, P99)
val reporter = new DatadogReporter.forRegistry(metricsRegistry)
  .withEC2Host()
  .withApiKey(apiKey)
  .withExpansions(expansions)
  .withMetricNameFormatter(ShortenedNameFormatter)
  .build()

reporter.start(10, TimeUnit.SECONDS)
~~~


## Maven Info

Metrics datadog reporter is available as an artifact on
[Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.coursera%22%20AND%20a%3A%22metrics-datadog%22)

* Group: org.coursera
* Artifact: metrics-datadog
* Version: 0.1.6
