__Worked with version of GMaps that Google chose to serve up to me on 2021.Apr.18.__

__Scrape estimated route times from Google Maps. Write a tab-delimited file of the data.__

To build and run

* Download and install Github desktop
* Download and Git tools from https://git-scm.com/download (if you like working from the command line)
* Download and install Java Development Kit : https://developers.redhat.com/products/openjdk/download (You will need a Redhat account.)
  * Use latest instead? -Use Java 14 (jdk-14.0.1-x64 MSI)
  * Shorten path to avoid 'path too long' error
  * Set JAVA_HOME environment variable. e.g., JAVA_HOME=C:\Program Files\RedHat\java-14-openjdk-14.0.1-1
* Download and install maven from https://maven.apache.org/download.cgi and https://maven.apache.org/install.html
  * Extract into C:\Users\\[you]\mvn\ to avoid 'path too long' error
  * Add to path, e.g., C:\Users\[you]\mvn\apache-maven-3.6.2\bin
* Download and install Spring Tool Suite from https://spring.io/tools/sts/all (or editor/IDE of your choice)
  * Extract into C:\Users\\[you]\sts to avoid 'path to long' error.
* Install Firefox driver
  * Download geckodriver from https://github.com/mozilla/geckodriver/releases
  * Extract from zip
  * Add to path, e.g., C:\Users\\[you]\Downloads\geckodriver-v0.19.0-win64
* _Can also install Google Chrome Selenium driver, but is less reliable_
  * Download ChromeDriver from https://sites.google.com/a/chromium.org/chromedriver/downloads (click on "Latest Release" link)
  * Extract from zip
  * Add to path : C:\Users\\[you]\Downloads\ ... appropriate directory ...
* In git bash 
  * cd *yourDirectoryContainingRepositories* (e.g., C:\Users\<you>\Documents\Github\)
  * git clone https://github.com/chrisxkeith/commute-time-aggregator.git
* Create text input file with parameters
  * C:\Users\\[you]\Documents\routeinfo.txt
  * See loadCollectionParams() method for file format. It's a fixed order of input lines.
* In git bash
  * cd commute-time-aggregator/DurationCollector
  * mvn clean install exec:java -Dexec.mainClass="com.ckkeith.duration_collector.DurationCollector"
  * The first time you run, it will take a while to download all the required jar files.
  * It will take a minute or two for the browser to come up.
  * Some browsers may keep your previous Maps settings (e.g., public transit vs. driving). Watch the browser for a couple of minutes to make sure that you are getting the data you expect. You can, while the automation in running, click on the car icon if necessary.
* In STS
  * File > Import
  * maven > existing maven projects 
  * C:\Users\<you>\Documents\Github\commute-time-aggregator\DurationCollector
  * pom.xml

To increase the odds that you'll get data

  * Make sure that your browser is up-to-date.
  * Don't click in browser window.
  * Don't switch networks (e.g., log into a VPN).
  * Try not to hover the mouse pointer over the browser window. Use keyboard to change between windows.
  * Don't close browser window.
 
Notes

* __Can break any time Google decides to change the HTML structure of their Maps page.__
* Writes duration, time stamp and route to tab-separated file in Documents/route-data/
* Presentation at https://docs.google.com/presentation/d/1Ulf6LNdhSGW9QETfhoIF9arNxw70buukseLuLyKfpf0/edit#slide=id.p
