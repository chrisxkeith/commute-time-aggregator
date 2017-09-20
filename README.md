commute-time-aggregator
=======================

Aggregate commute durations over time to see pattern(s)

To build:

* Download and install Github tools from https://www.github.com
* Download and install Spring Tool Suite from https://spring.io/tools/sts/all (or editor/IDE of your choice) 
* Download and install maven from https://maven.apache.org/download.cgi and https://maven.apache.org/install.html
* Run git bash 
* In git bash 
 * cd <yourDirectoryContainingRepositories>
 * git clone https://github.com/chrisxkeith/commute-time-aggregator.git
* In STS, set parameters in DurationCollector.java
 * (more directions to come)
* In git bash
 * cd commute-time-aggregator/DurationCollector
 * mvn clean exec:java -Dexec.mainClass="com.ckkeith.duration_collector.DurationCollector"
