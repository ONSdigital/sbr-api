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
 
### Troubleshooting
See [FAQ](FAQ.md) for possible and common solutions.

### Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.

### License

Copyright ©‎ 2017, Office for National Statistics (https://www.ons.gov.uk)

Released under MIT license, see [LICENSE](LICENSE.md) for details.
