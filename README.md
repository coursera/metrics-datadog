# Metrics Datadog Reporter

`metrics-datadog` is a simple reporting bridge between [Dropwizard Metrics](http://metrics.dropwizard.io/) and the [Datadog](https://www.datadoghq.com/) service. It includes support for:

* Datadog's tagging feature
* Metric reporting via the Datadog HTTP API, UDP or UDS. 
* Tight integration with the Dropwizard framework via the `dropwizard-metrics-datadog` sub-project.

## HTTP vs UDP vs UDS

Datadog supports two main metric ingestion methods:

- POSTing metrics via their [HTTP API](http://docs.datadoghq.com/api/#metrics-post)
- Sending metrics via UDP or UDS (using a statsd-like protocol) to the local [dogstatsd](http://docs.datadoghq.com/guides/dogstatsd/) agent

Datadog recommends the `dogstatsd` UDS-based approach, but some may prefer the HTTP-based approach
for various reasons e.g. a general adversity to running agents, the additional memory required by the agent and
forwarder (though this is configurable), stability, security or other environment/platform-level
conflicts.

Note that, in the event of a delivery failure, the HTTP-based transport does not buffer metrics in
memory. It will attempt a handful of retries and then give up. Hence, when faced with an extended network
partition window or a Datadog ingestion outage, some metrics will certainly be lost using this transport.
That said, note that the UDP/UDS-based reporter also cannot buffer metrics forever due
to memory constraints.

## Usage

~~~scala
import org.coursera.metrics.datadog.DatadogReporter
import org.coursera.metrics.datadog.DatadogReporter.Expansion._
import org.coursera.metrics.datadog.transport.Transport
import org.coursera.metrics.datadog.transport.HttpTransport
import org.coursera.metrics.datadog.transport.UdpTransport
import scala.concurrent.duration.SECONDS

...
val expansions = EnumSet.of(COUNT, RATE_1_MINUTE, RATE_15_MINUTE, MEDIAN, P95, P99)
val httpTransport = new HttpTransport.Builder().withApiKey(apiKey).build()
val reporter = DatadogReporter.forRegistry(registry)
  .withEC2Host()
  .withTransport(httpTransport)
  .withExpansions(expansions)
  .build()

reporter.start(10, SECONDS)
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

Example of using UDS transport:

~~~scala
...
val udpTransport = new UdpTransport.Builder().withStatsdHost("/var/run/datadog/dsd.socket").withPort(0).build()
val reporter = 
    ...
    .withTransport(udpTransport)
    ...
~~~

### Tag encoding and expansion

Datadog supports powerful [tagging](http://docs.datadoghq.com/faq/#tagging) 
functionality while the Metrics API does not. Thus, `metrics-datadog` utilizes 
a special, overloaded metric naming syntax that enables tags to piggyback on
metric names while passing through the Metrics library. The tags are unpacked 
by `metrics-datadog` at reporting time and are sent along to Datadog via the
configured transport layer. Here's the metric name syntax:

`metricName[tagName:tagValue,tagName:tagValue,...]`

`metrics-datadog` is mainly a reporting library and doesn't currently 
implement a tag-aware decorator on top of the core `Metrics` API. It
does, however, expose a `TaggedName` class that helps you encode/decode tags in 
metric names using the syntax above. You can utilize this helper class
methods when registering and recording metrics. Note that in order for tag
propagation to work, you'll need to use our `DefaultMetricNameFormatter` 
(or a formatter with compatible parsing logic).

We also support the notion of static, "additional tags". This feature allows 
you to define a set of tags that are appended to all metrics sent through 
the reporter. It's useful for setting static tags such as the 
environment, service name or version. Additional tags are configured via 
the `DatadogReporter` constructor. 

Finally, we support the notion of "dynamic tags". By implementing and 
registering a `DynamicTagsCallback` with `DatadogReporter`, you can control
the values of "additional tags" at runtime. Dynamic tags are merged with 
and override any additional tags set.

*Performance note*: Heavy use of tagging, especially tags values with high 
cardinality, can dramatically increase memory usage, as all tag permutations
are tracked and counted in-memory by the Metrics library. Also note that some
[MetricRegistry APIs](https://github.com/dropwizard/metrics/blob/master/metrics-core/src/main/java/io/dropwizard/metrics/MetricRegistry.java#L376)
do defensive copies on the entire metrics set, which can be prohibitively 
expensive CPU and memory-wise if you have a huge, heavily tagged metric set.

### Dropwizard Metrics Reporter

If you have a dropwizard project and have at least `dropwizard-core` 0.7.X, 
then you can perform the following steps to automatically report metrics to
datadog.

First, add the `dropwizard-metrics-datadog` dependency in your POM:

~~~xml    
    <dependency>
        <groupId>org.coursera</groupId>
        <artifactId>dropwizard-metrics-datadog</artifactId>
        <version>1.1.13</version>
    </dependency>
~~~

Then just add the following to your `dropwizard` YAML config file.

~~~yaml
metrics:
  frequency: 1 minute                       # Default is 1 second.
  reporters:
    - type: datadog
      host: <host>                          # Optional with UDP Transport
      tags:                                 # Optional. Defaults to (empty)
      includes:                             # Optional. Defaults to (all).
      excludes:                             # Optional. Defaults to (none).
      prefix:                               # Optional. Defaults to (none).
      expansions:                           # Optional. Defaults to (all).
      metricNameFormatter:                  # Optional. Default is "default".
      dynamicTagsCallback:                  # Optional. Defaults to (none).
      transport:
        type: http
        apiKey: <apiKey>
        connectTimeout: <duration>          # Optional. Default is 5 seconds
        socketTimeout: <duration>           # Optional. Default is 5 seconds
~~~

Once your `dropwizard` application starts, your metrics should start appearing
in Datadog.

#### Transport options

HTTP Transport:

~~~yaml
metrics:
  frequency: 1 minute                       # Default is 1 second.
  reporters:
    - type: datadog
      host: <host>
      transport:
        type: http
        apiKey: <apiKey>
        connectTimeout: <duration>          # Optional. Default is 5 seconds
        socketTimeout: <duration>           # Optional. Default is 5 seconds
~~~

UDP Transport:

~~~yaml
metrics:
  frequency: 1 minute                       # Default is 1 second.
  reporters:
    - type: datadog
      transport:
        type: udp
        prefix:                             # Optional. Default is (empty)
        statsdHost: "localhost"             # Optional. Default is "localhost"
        port: 8125                          # Optional. Default is 8125
~~~

UDS Transport:

~~~yaml
metrics:
  frequency: 1 minute                              # Default is 1 second.
  reporters:
    - type: datadog
      transport:
        type: udp
        prefix:                                    # Optional. Default is (empty)
        statsdHost: "/var/run/datadog/dsd.socket"
        port: 0
~~~

#### Filtering

If you want to filter only a few metrics, you can use the `includes` or 
`excludes` key to create a set of metrics to include or exclude respectively.

~~~yaml
metrics:
  frequency: 1 minute                       # Default is 1 second.
  reporters:
    - type: datadog
      host: <host>
      includes:
        - jvm.
        - ch.
~~~

The check is very simplistic so be as specific as possible. For example, if 
you have "jvm.", the filter will check if the includes has that value in any 
part of the metric name (not just the beginning).

#### Expansions

If you want to limit the set of expansions applied to each metric, you can specify
a custom set.

The full set of expansions can be found in the [Expansion enum](https://github.com/coursera/metrics-datadog/blob/master/metrics-datadog/src/main/java/org/coursera/metrics/datadog/DatadogReporter.java#L232).

~~~yaml
metrics:
  reporters:
    - type: datadog
      expansions:
        - COUNT
        - RATE_1_MINUTE
        - MAX
        - P95
~~~

#### Prefix

By default, the metric names are sent as-is (e.g. `io.dropwizard.jetty.MutableServletContextHandler.2xx-responses`)
The prefix option adds a custom prefix to each metric name:

~~~yaml
metrics:
  reporters:
    - type: datadog
      prefix: custom.prefix
~~~

would produce: `custom.prefix.io.dropwizard.jetty.MutableServletContextHandler.2xx-responses`

#### Metric Name Formatter

The metricNameFormatter option can be used to add custom logic when processing each
metric's name. By default it will use the DefaultMetricNameFormatter which handles
Datadog tags but does not modify the metric name.

~~~yaml
metrics:
  reporters:
    - type: datadog
      metricNameFormatter:
        type: custom
~~~

Adding a custom formatter requires a few things:

##### 1. Create a MetricNameFormatter

~~~java
public class CustomMetricNameFormatter extends DefaultMetricNameFormatter {
  @Override
  public String format(String name, String... path) {
    // Make response metrics names less verbose
    String newName = name.replace("io.dropwizard.jetty.MutableServletContextHandler", "");
    
    // Call DefaultMetricNameFormatter to handle tags
    return super.format(newName, path);
  }
}
~~~
##### 2. Create a MetricNameFormatterFactory with `@JsonTypeName` annotation

~~~java
@JsonTypeName("custom") // This must match the name specified in the configuration
public class CustomMetricNameFormatterFactory implements MetricNameFormatterFactory {
  @Override
  public MetricNameFormatter build() {
    return new CustomMetricNameFormatter();
  }
}
~~~

##### 3. Add the Factory to `org.coursera.metrics.datadog.MetricNameFormatterFactory` file

We need to make sure our `CustomMetricNameFormatterFactory` is added to the list of subTypes
for `MetricNameFormatterFactory`, otherwise the `"custom"` in our config won't be recognized.

Add a file called `org.coursera.metrics.datadog.MetricNameFormatterFactory` to
`src/main/resources/META-INF/services` and add the full path to your class to the file
(e.g. `com.company.CustomMetricNameFormatterFactory`)

See: http://www.dropwizard.io/1.0.0/docs/manual/configuration.html#polymorphic-configuration for details

#### Dynamic Tags Callback

Similar to the `MetricNameFormatter` steps, we need to:

1. Create a DynamicTagsCallback
2. Create a DynamicTagsCallbackFactory with `@JsonTypeName` annotation
3. Add the Factory to `org.coursera.metrics.datadog.DynamicTagsCallbackFactory` file

See above instructions for details.

## Maven Info

Metrics datadog reporter is available as an artifact on
[Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.coursera%22%20AND%20a%3A%22metrics-datadog%22)

* Group: org.coursera
* Artifact: metrics-datadog
* Version: 1.1.13

Dropwizard datadog reporter is available as an artifact on
[Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.coursera%22%20AND%20a%3A%22dropwizard-metrics-datadog%22)

* Group: org.coursera
* Artifact: dropwizard-metrics-datadog
* Version: 1.1.13

## Contributing

We follow Google's [Java Code
Style](https://google.github.io/styleguide/javaguide.html)
