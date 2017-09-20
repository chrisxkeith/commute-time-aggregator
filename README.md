commute-time-aggregator
=======================

Aggregate commute durations over time to see pattern(s)

To build:

* Download and install Github tools from https://www.github.com
* Download and install Spring Tool Suite from https://spring.io/tools/sts/all (or editor/IDE of your choice) 
* Download and install maven from https://maven.apache.org/download.cgi and https://maven.apache.org/install.html
  * Extract into C:\Users\Chris\mvn\ to avoid 'path to long' error
  * Add to path : C:\Users\Chris\mvn\apache-maven-3.5.0\bin
* Run git bash 
* In git bash 
  * cd *yourDirectoryContainingRepositories*
  * git clone https://github.com/chrisxkeith/commute-time-aggregator.git
* In STS, set parameters in DurationCollector.java (TODO : see comments at top of file)
* In git bash
  * cd commute-time-aggregator/DurationCollector
  * mvn clean install exec:java -Dexec.mainClass="com.ckkeith.duration_collector.DurationCollector"
  * ...It may take a minute or two for Firefox to come up
