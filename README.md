# sbr-api
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)]()
[![Dependency Status](https://www.versioneye.com/user/projects/58e23bf2d6c98d00417476cc/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/58e23bf2d6c98d00417476cc)


### Prerequisites

* Java 8 or higher
* SBT ([Download](http://www.scala-sbt.org/))


### Development Setup (MacOS)

To install SBT quickly you can use Homebrew ([Brew](http://brew.sh)):
```shell
brew install sbt
```
Similarly we can get Scala (for development purposes) using brew:
```shell
brew install scala
```


### Running the App

To compile, build and run the application use the following command:

```shell
sbt api/run `-Dhttp.port=9002`
```
The default application port is 9000. To specify an alternative port use `-Dhttp.port=8080`.

#### Assembly

To package the project in a runnable fat-jar:

```shell
sbt assembly
```

#### Test

To test all test suites we can use:

```shell
sbt test
```

Testing an individual test suite can be specified by using the `testOnly`.

SBR Api uses its own test configuration settings for integration tests, the details of which can be found on the[ONS Confluence](https://collaborate2.ons.gov.uk/confluence/display/SBR/Scala+Testing​).

To run integration test run:
```shell
sbt it:test
```
See[CONTRIBUTING](CONTRIBUTING.md) for more details on creating tests. 

##### Approach


#### API Documentation
Swagger API is used to document and expose swagger definitions of the routes and capabilities for this project.

 To see the full definition set use path:
 `http://localhost:9000/swagger.json`
 
 For a graphical interface using Swagger Ui use path:
 `http://localhost:9000/docs`
 
#### Application Tracing
[zipkin](https://zipkin.io/) is used for application tracing.

Key implementation decisions:
* a Play filter is used to create a span (and if necessary a parent trace) for all requests.  The filter
automatically reports this span upon completion of the request.
* the TraceWSClient from play-zipkin-tracing is used as a replacement for Play's WSClient.  This
automatically creates and reports a child span for downstream API requests
* the particular reporter to use is injected by Guice when the application is configured.  When the Play
environment mode is "Dev" or "Test" a console reporter is used, which results in the trace simply being
written to standard out.  Only in "Prod" mode will an asynchronous HTTP reporter be injected to publish
traces to the Zipkin server configured in application.conf (and possibly overriden by the relevant
environment variables).

_Testing:_

By default, trace information will automatically be printed to standard out when the application is
run in Dev or Test modes (`sbt run` or `sbt test`).  No attempt will be made to publish trace information
to a Zipkin server.

If you want to publish traces to a Zipkin server:
* edit `providesZipkinReporter` in the TracingModule binding to return `zipkinHttpReporter` for the mode
you will be using
* run a Zipkin 1 server.  The simplest way to do this is via docker:

      docker run -d -p 9411:9411 openzipkin/zipkin:1.31.3

* run an acceptance test such as `sbt "testOnly EnterpriseAcceptanceSpec"` or run the application with `sbt run`
and exercise the relevant endpoint
* the trace information should then be available in the Zipkin UI at [http://localhost:9411/zipkin/](http://localhost:9411/zipkin/)

### Troubleshooting
See [FAQ](FAQ.md) for possible and common solutions.

### Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.

### License

Copyright ©‎ 2017, Office for National Statistics (https://www.ons.gov.uk)

Released under MIT license, see [LICENSE](LICENSE.md) for details.
