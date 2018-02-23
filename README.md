__Scrape estimated route times from Google Maps. Write a tab-delimited file of the data.__

To build and run

* Download and install Github tools from https://desktop.github.com/
* Download and install Spring Tool Suite from https://spring.io/tools/sts/all (or editor/IDE of your choice)
  * Extract into C:\Users\<you>\sts to avoid 'path to long' error.
* Download and install Java Development Kit : http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
  * Set JAVA_HOME environment variable. e.g., JAVA_HOME=C:\Program Files\Java\jdk1.8.0_65
* Download and install maven from https://maven.apache.org/download.cgi and https://maven.apache.org/install.html
  * Extract into C:\Users\<you>\mvn\ to avoid 'path to long' error
  * Add to path, e.g., C:\Users\<you>\mvn\apache-maven-3.5.0\bin
* Firefox browser
  * Install Firefox browser 
  * Download geckodriver from https://github.com/mozilla/geckodriver/releases
  * Extract from zip
  * Add to path, e.g., C:\Users\<you>\Downloads\geckodriver-v0.19.0-win64
* In git bash 
  * cd *yourDirectoryContainingRepositories* (e.g., C:\Users\<you>\Documents\Github\)
  * git clone https://github.com/chrisxkeith/commute-time-aggregator.git
* In STS
  * File > Import
  * maven > existing maven projects 
  * C:\Users\<you>\Documents\Github\commute-time-aggregator\DurationCollector
  * pom.xml
* Create text input file with parameters
  * C:\Users\<you>\Documents\routeinfo.txt
  * See DurationCollector() method for file format (fixed order of input lines)
* In git bash (may need Windows CMD instead)
  * cd commute-time-aggregator/DurationCollector
  * mvn clean install exec:java -Dexec.mainClass="com.ckkeith.duration_collector.DurationCollector"
  * The first time you run, it will take a while to download all the required jar files
  * It will take a minute or two for the browser to come up

To increase the odds that you'll get data

  * Make sure that your browser is up-to-date.
  * Don't click in browser window.
  * Don't switch networks (e.g., log into a VPN).
  * Try not to hover the mouse pointer over the browser window. Use keyboard to change between windows.
  * Don't close browser window.
 
Notes

* __Can break any time Google decides to change the HTML structure of their Maps page.__
* Writes duration, time stamp and route to tab-separated file
* Waze has similar functionality, but does not show a range of times.
* _if you want to try Google Chrome browser (doesn't work yet)_
  * Download ChromeDriver from https://sites.google.com/a/chromium.org/chromedriver/downloads (click on "Latest Release" link)
  * Extract from zip
  * Add to path : C:\Users\<you>\Downloads\ ... appropriate directory ...
