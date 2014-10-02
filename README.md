# Metrics Datadog Reporter
Simple Metrics reporter that sends reporting info to Datadog, supports both http and udp.

## Usage

~~~scala
import com.yammer.metrics.reporting.DatadogReporter
import com.yammer.metrics.reporting.DatadogReporter.Expansions._

...
val expansions = EnumSet.of(COUNT, RATE_1_MINUTE, RATE_15_MINUTE, MEDIAN, P95, P99)
val reporter = new DatadogReporter.Builder()
  .withEC2Host()
  .withApiKey(apiKey)
  .withExpansions(expansions)
  .withMetricNameFormatter(ShortenedNameFormatter)
  .build()

reporter.start(10, TimeUnit.SECONDS)
~~~

Example of using UDP transport:

~~~scala
...
val udpTransport = new UdpTransport.Builder().build()
val reporter = 
    ...
    .withTransport(udpTransport)
    ...
~~~


## Maven Info

Metrics datadog reporter is available as an artifact on
[Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.coursera%22%20AND%20a%3A%22metrics-datadog%22)

* Group: org.coursera
* Artifact: metrics-datadog
* Version: 0.1.7


## Contributing

We follow Google's [Java Code
Style](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html)
