__Can break any time Google decides to change the HTML structure of their Maps page.__

To build and run

* Download and install Github tools from https://desktop.github.com/
* Download and install Spring Tool Suite from https://spring.io/tools/sts/all (or editor/IDE of your choice)
* Download and install Java Development Kit : http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
* Download and install maven from https://maven.apache.org/download.cgi and https://maven.apache.org/install.html
  * Extract into C:\Users\Chris\mvn\ to avoid 'path to long' error
  * Add to path : C:\Users\Chris\mvn\apache-maven-3.5.0\bin
* Firefox browser
  * Install Firefox browser 
  * Download geckodriver from https://github.com/mozilla/geckodriver/releases
  * Extract from zip
  * Add to path : C:\Users\Chris\Downloads\geckodriver-v0.19.0-win64
* _if you want to try Chrome_ ----- Google Chrome browser (doesn't work yet)
  * Download ChromeDriver from https://sites.google.com/a/chromium.org/chromedriver/downloads (click on "Latest Release" link)
  * Extract from zip
  * Add to path : C:\Users\Chris\Downloads\ ... appropriate directory ...
* Run git bash 
* In git bash 
  * cd *yourDirectoryContainingRepositories*
  * git clone https://github.com/chrisxkeith/commute-time-aggregator.git
* In STS
  * File > Import > the maven pom.xml file/project
  * Set parameters in DurationCollector.java (__TODO__ : see comments at top of file)
* In git bash
  * cd commute-time-aggregator/DurationCollector
  * mvn clean install exec:java -Dexec.mainClass="com.ckkeith.duration_collector.DurationCollector"
  * ...It may take a minute or two for the browser to come up

Notes

* Scrapes google maps for estimated commute duration
* Writes duration and time stamp to tab-separated file
* To get good data while running:
  * Don't manually close browser window
  * Don't switch networks (e.g., log into a VPN)
  * Make sure that your browser is up-to-date
* Does __not__ record which route is the fastest (currently).
* Waze has similar functionality, but does not show a range of times.

