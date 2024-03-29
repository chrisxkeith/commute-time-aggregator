// Please credit chris.keith@gmail.com
package com.ckkeith.duration_collector;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
// import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DurationCollector {

	public class CollectionParams {
		String routeId;
		String homeLocation;
		String workLocation;
		int startDayOfWeek;
		int endDayOfWeek;

		CollectionParams(String routeId, String homeLocation, String workLocation, int startDayOfWeek,
				int endDayOfWeek) {
			this.routeId = routeId;
			this.homeLocation = homeLocation;
			this.workLocation = workLocation;
			this.startDayOfWeek = startDayOfWeek;
			this.endDayOfWeek = endDayOfWeek;
		}

		public String toString(int dayOfWeek) {
			return routeId + "_" + getDayOfWeek(dayOfWeek);
		}
	}

	class RouteEstimate {
		public RouteEstimate(Integer minEstimate, Integer maxEstimate, String rawData, String routeName) {
			super();
			this.minEstimate = minEstimate;
			this.maxEstimate = maxEstimate;
			this.rawData = rawData;
			this.routeName = routeName;
		}
		Integer	minEstimate = Integer.MAX_VALUE;
		Integer maxEstimate = Integer.MAX_VALUE;
		String rawData = "unknown";
		String routeName = "unknown";
		
		public String toString() {
			return "minEstimate=" + minEstimate + ", maxEstimate=" + maxEstimate + ", rawData" + rawData
					+ ", routeName" + routeName;
		}
	}

	final boolean isDebug;
	final boolean useBrowser = true;
	final int sleepFactor = 2; // increase for slower computers.
	final int sleepSeconds = 30 * sleepFactor;
	final private String dirForResults;

	final private Pattern digitPattern = Pattern.compile("[0-9]+");

	// A format that will convert to a date when pasted into a Google spreadsheet.
	final private SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	// A format that will convert into ISO 8601 (I think).
	final private SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

	// Format that will convert to string for the Google Maps web page.
	final private SimpleDateFormat gMapsTimeFormat = new SimpleDateFormat("HH:mm a");

	final private List<CollectionParams> collectionParams = new ArrayList<CollectionParams>();

	private WebDriver driver = null;
	private String otherCollectionParamsFileName = null;
	private String logFileName = null;
	private int minutesPerSample;

	public DurationCollector(String[] args) throws Exception {
		isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
				.indexOf("jdwp") >= 0;
		String d = System.getProperty("user.home");
		if (d == null || d.length() == 0) {
			throw new IllegalArgumentException(
					"Unable to determine user.home directory from System.getProperty(\"user.home\")");
		}
		String pathEnd = File.separator + "Documents" + File.separator + "route-data";
		File dir = new File(d + pathEnd);
		if (!dir.exists()) {
			try {
				dir.mkdir();
			} catch (Exception e) {
				System.out.println("Unable to create : " + d + pathEnd);
				throw e;
			}
		}
		dirForResults = d + pathEnd;
		System.out.println("Logging data to : " + dirForResults);
		otherCollectionParamsFileName = d + File.separator + "Documents" + File.separator + "routeInfo.txt";
		File f = new File(otherCollectionParamsFileName);
		if (!f.exists()) {
			throw new Exception("No file : " + otherCollectionParamsFileName);
		}
		String tmpDir = d + File.separator + "Documents" + File.separator + "tmp";
		try {
			new File(tmpDir).mkdir();
		} catch (Exception e) {
			System.out.println("Unable to create : " + tmpDir);
			throw e;
		}
		logFileName = dirForResults + File.separator + "routeInfo_" + UUID.randomUUID().toString() + ".log";
		if (isDebug) {
			log("Running in debug mode, only collecting a few durations.");
		}
	}

	String getDayOfWeek(int dayOfWeek) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
		return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
	}

	private void loadCollectionParams(String start, String end, String id, int startDOW, int endDOW) throws Exception {
		collectionParams.add(new CollectionParams("to_" + /* name of data set file */ id, /* start location */ start,
				/* destination location */ end, startDOW, endDOW));
		collectionParams.add(new CollectionParams("from_" + /* name of data set file */ id,
				/* destination location */ end, /* start location */ start, startDOW, endDOW));
	}

	private String getNextLine(BufferedReader br, String name) throws Exception {
		String s = br.readLine();
		if (s == null || s.isEmpty()) {
			throw new RuntimeException(name + " not specified");
		}
		return s;
	}

	private void loadCollectionParams() throws Exception {
		collectionParams.clear();
		if (otherCollectionParamsFileName == null || this.otherCollectionParamsFileName.isEmpty()) {
			throw new Exception("No otherCollectionParamsFileName specified");
		}
		System.out.println("Reading params from : " + otherCollectionParamsFileName);
		BufferedReader br = new BufferedReader(new FileReader(otherCollectionParamsFileName));
		try {
			String personId = br.readLine();
			while (personId != null && !personId.isEmpty()) {
				String home = getNextLine(br, "start");
				String work = getNextLine(br, "destination");
				String firstDayOfWeek = getNextLine(br, "firstDayOfWeek");
				String lastDayOfWeek = getNextLine(br, "lastDayOfWeek");
				loadCollectionParams(home, work, personId, Integer.parseInt(firstDayOfWeek),
						Integer.parseInt(lastDayOfWeek));
				personId = br.readLine();
			}
		} finally {
			br.close();
		}
	}

	private int minutesFromString(String s) {
		int minutes = 0;
		if (s.contains(" h")) {
			String[] c = s.split(" h");
			Matcher m = digitPattern.matcher(c[0]);
			if (m.find()) {
				minutes += (Integer.parseInt(m.group()) * 60);
			}
			if (c.length > 1) {
				s = c[1];
			} else {
				s = null;
			}
		}
		if (s != null) {
			Matcher m = digitPattern.matcher(s);
			if (m.find()) {
				minutes += Integer.parseInt(m.group());
			}
		}
		return minutes;
	}

	@SuppressWarnings("unused")
	// Keep this around if clicking on calendar is unreliable.
	private void manuallySetDayOfWeek(CollectionParams cp, int dayOfWeek) throws Exception {
		// Doesn't work second time around. Console buffer not flushed?
		log("Manually select day in browser, then click back into console and press <ENTER> for : "
				+ cp.toString(dayOfWeek));
		System.in.read();
		log("Continuing with : " + cp.toString(dayOfWeek));
	}

	// TO DO : Any way to replace all calls to Thread.sleep() ?
	private void clickOnDayOfWeek(int dayOfWeek) throws Exception {
		WebDriverWait wait = new WebDriverWait(driver, sleepSeconds, 1000);
		wait.until(ExpectedConditions.elementToBeClickable(By.className("date-input")));
		driver.findElement(By.className("date-input")).click();
		wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(
				"td[class=\"goog-date-picker-date goog-date-picker-other-month goog-date-picker-wkend-end\"]")));
		WebElement cell = driver.findElement(By.cssSelector(
				"td[class=\"goog-date-picker-date goog-date-picker-other-month goog-date-picker-wkend-end\"]"));
		WebElement parent = cell.findElement(By.xpath(".."));
		List<WebElement> weekCells = parent.findElements(By.tagName("td"));
		weekCells.get(dayOfWeek - 1).click();
		Thread.sleep(5000);
	}

	private void setUpPage(CollectionParams cp, int dayOfWeek) throws Exception {
		driver.get("https://maps.google.com/");
		WebDriverWait wait = new WebDriverWait(driver, sleepSeconds, 1000);
		wait.until(ExpectedConditions.elementToBeClickable(By.id("searchboxinput")));
		driver.findElement(By.id("searchboxinput")).sendKeys(cp.workLocation + "\n");

		Thread.sleep(10 * 1000);
		driver.findElement(By.cssSelector("button[aria-label='Directions']"));
// Other attributes that could be used:
//		<button aria-label="Directions" data-value="Directions" jsaction="pane.placeActions.directions"
		WebElement currentElement = driver.switchTo().activeElement();
		currentElement.click();
		currentElement = driver.switchTo().activeElement();
		currentElement.sendKeys(cp.homeLocation + "\n");

		// Make sure we're using drive times, not public transit or walking.
		Thread.sleep(5 * 1000);
		driver.findElement(By.xpath("//img[@data-tooltip='Driving']")).click();

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()[contains(.,\"Leave now\")]]")));
		driver.findElement(By.xpath("//*[text()[contains(.,\"Leave now\")]]")).click();

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()[contains(.,\"Depart at\")]]")));
		driver.findElement(By.xpath("//*[text()[contains(.,\"Depart at\")]]")).click();
		clickOnDayOfWeek(dayOfWeek);
		// manuallySetDayOfWeek(cp, dayOfWeek);
	}

	private RouteEstimate collectDurationUsingBrowser(Calendar ts) throws Exception {
		WebDriverWait wait = new WebDriverWait(driver, sleepSeconds, 1000);

		wait.until(ExpectedConditions.elementToBeClickable(By.name("transit-time")));
		WebElement timeEl = driver.findElement(By.name("transit-time"));
		timeEl.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		timeEl.sendKeys(Keys.chord(Keys.DELETE));
		timeEl.sendKeys(this.gMapsTimeFormat.format(ts.getTime()));
		timeEl.sendKeys(Keys.chord(Keys.ENTER));
		Thread.sleep(5000);

		int minEstimate = Integer.MAX_VALUE;
		int maxEstimate = Integer.MAX_VALUE;

		// Google Maps can show multiple alternative routes. Find the minimum.
		List<WebElement> wList = driver.findElements(By.xpath("//span[contains(text(), \"typically\")]"));
		String durations = "not found";
		String route = "not found";
		for (WebElement w : wList) {
			durations = w.findElement(By.xpath("//span[contains(text(), \" min\")]")).getText();
			route = w.findElement(By.xpath("//h1[contains(text(), \" via \")]")).getText();
			if (durations.contains("-")) {
				String[] rangeLimits = durations.split("-");
				if (minEstimate > minutesFromString(rangeLimits[0])) {
					minEstimate = minutesFromString(rangeLimits[0]);
					maxEstimate = Math.min(maxEstimate, minutesFromString(rangeLimits[1]));
				}
			} else {
				// No range, use single estimate for both min and max.
				if (minEstimate > minutesFromString(durations)) {
					minEstimate = minutesFromString(durations);
					maxEstimate = Math.min(maxEstimate, minutesFromString(durations));
				}
			}
		}
		return new RouteEstimate(minEstimate, maxEstimate, durations, route);
	}

	private void initBrowserDriver() {
		if (driver == null) {
			driver = new FirefoxDriver();
			driver.manage().timeouts().implicitlyWait(sleepSeconds, TimeUnit.SECONDS);
		}
	}

	private BufferedWriter getWriter(CollectionParams cp, boolean doAppend, int dayOfWeek) throws Exception {
		String filePathString = getPath(cp, dayOfWeek);
		File f = new File(filePathString);
		FileWriter fstream;
		if (f.exists()) {
			fstream = new FileWriter(filePathString, doAppend);
		} else {
			fstream = new FileWriter(filePathString);
		}
		return new BufferedWriter(fstream);
	}

	private void writeHeader(CollectionParams cp, int dayOfWeek) throws Exception {
		BufferedWriter out = this.getWriter(cp, true, dayOfWeek);
		String s = cp.routeId + "\tminimum\taverage\tmaximum\troute\traw" + System.getProperty("line.separator");
		out.write(s);
		out.close();
	}

	private String getPath(CollectionParams cp, int dayOfWeek) {
		return dirForResults + "/" + cp.toString(dayOfWeek) + ".txt";
	}

	private void setupFile(CollectionParams cp, int dayOfWeek) throws Throwable {
		File f = new File(getPath(cp, dayOfWeek));
		if (f.exists()) {
			f.delete();
		}
		writeHeader(cp, dayOfWeek);
	}

	private void setupSelenium(CollectionParams cp, int dayOfWeek) throws Exception {
		initBrowserDriver();
		setUpPage(cp, dayOfWeek);
	}

	private int collectData(CollectionParams cp, int dayOfWeek) throws Throwable {
		int totalCalls = 0;
		LocalDateTime start = LocalDateTime.now();
		log("Starting : " + cp.toString(dayOfWeek));
		Calendar ts = Calendar.getInstance();
		ts.set(start.getYear(), start.getMonthValue(), dayOfWeek, 4, 0);
        Instant instant = ts.toInstant();
        Instant now = Instant.now();
        while (instant.isBefore(now)) {
            instant.plus(7, ChronoUnit.DAYS);
        }

		int endHour = 20;
		if (isDebug) {
			minutesPerSample = 60;
		} else {
			minutesPerSample = 10;
		}
		try {
			if (useBrowser) {
				setupSelenium(cp, dayOfWeek);
			}
			while (ts.get(Calendar.HOUR_OF_DAY) < endHour) {
				try {
					RouteEstimate newDuration;
					if (useBrowser) {
						newDuration = this.collectDurationUsingBrowser(ts);
					} else {
						newDuration = new RouteEstimate(Integer.MAX_VALUE, Integer.MAX_VALUE,
							"no raw data", "route name to come");
						ApiCollector.collectDuration(newDuration, this, instant, cp);
						instant.plus(minutesPerSample, ChronoUnit.MINUTES);
					}
					totalCalls++;
					if ((newDuration.minEstimate != Integer.MAX_VALUE)
							&& (newDuration.maxEstimate != Integer.MAX_VALUE)) {
						writeDuration(cp, ts, newDuration, dayOfWeek);
					} else {
						log("collectData()\tInvalid data\t" + newDuration.toString());
						log("collectData()\t" + ts.toString());
						if (useBrowser) {
							log("collectData()\t" + driver.findElement(By.xpath("//body")).getText());
						}
					}
				} catch (Throwable e) {
					log("collectData()\t" + ts.toString());
					log("collectData()\t" + e);
					e.printStackTrace();
					driver = null;
					throw e;
				}
				ts.add(Calendar.MINUTE, minutesPerSample);
			}
		} finally {
			if (driver != null) {
				driver.quit();
				driver = null;
			}
		}
		Long minutes = ChronoUnit.MINUTES.between(start, LocalDateTime.now());
		log("Finished : " + cp.toString(dayOfWeek) + "\trun time (minutes)\t" + minutes + "\ttotalCalls\t" + totalCalls);
		return totalCalls;
	}

	private int collectDurations() throws Throwable {
		int totalcalls = 0;
		for (CollectionParams cp : collectionParams) {
			for (int dayOfWeek = cp.startDayOfWeek; dayOfWeek <= cp.endDayOfWeek; dayOfWeek++) {
				setupFile(cp, dayOfWeek);
				totalcalls += collectData(cp, dayOfWeek);
			}
		}
		return totalcalls;
	}

	private void writeDuration(CollectionParams cp, Calendar ts, RouteEstimate newDuration,
			int dayOfWeek) throws Exception {
		Integer average = (newDuration.minEstimate + newDuration.maxEstimate) / 2;
		String s = outputDateFormat.format(ts.getTime())
				+ "\t" + newDuration.minEstimate
				+ "\t" + average
				+ "\t" + newDuration.maxEstimate
				+ "\t" + newDuration.routeName
				+ "\t" + newDuration.rawData;
		if (isDebug) {
			log(s);
		}
		BufferedWriter out = this.getWriter(cp, true, dayOfWeek);
		out.write(s + System.getProperty("line.separator"));
		out.close();
	}

	public void run() {
		LocalDateTime start = LocalDateTime.now();
		int totalCalls = 0;
		try {
			log("Starting all");
			loadCollectionParams();
			totalCalls = collectDurations();
		} catch (Throwable e) {
			log("run() : " + e.toString());
			driver = null;
		} finally {
			if (driver != null) {
				driver.quit();
				driver = null;
			}
			Long minutes = ChronoUnit.MINUTES.between(start, LocalDateTime.now());
			log("Finished all\trun time (minutes)\t" + minutes + "\ttotalCalls\t" + totalCalls);
		}
	}

	private void log(String s) {
		String d = logDateFormat.format(new Date());
		try {
			String msg = d + "\t" + s;
			System.out.println(msg);
			FileWriter fstream = new FileWriter(logFileName, true);
			fstream.write(msg + System.getProperty("line.separator"));
			fstream.close();
		} catch (Exception e) {
			System.out
					.println(d + "\tError writing log file : " + logFileName + "\t" + e.toString());
		}
	}

	public static void main(String[] args) {
		try {
			new DurationCollector(args).run();
		} catch (Exception e) {
			System.out.println(new Date().toString() + "\t" + e.getMessage());
		}
	}
}
