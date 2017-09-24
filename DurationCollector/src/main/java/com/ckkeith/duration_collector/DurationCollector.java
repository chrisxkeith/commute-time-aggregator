package com.ckkeith.duration_collector;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.SimpleDateFormat;

public class DurationCollector {

	class CollectionParams {
		String routeId;
		String homeLocation;
		String workLocation;
		int dayOfWeek;

		CollectionParams(String routeId, String homeLocation, String workLocation, int dayOfWeek) {
			this.routeId = routeId;
			this.homeLocation = homeLocation;
			this.workLocation = workLocation;
			this.dayOfWeek = dayOfWeek;
		}
	}

	final boolean isDebug;
	final int sleepSeconds = 30;
	final private String dirForResults;

	final private Pattern digitPattern = Pattern.compile("[0-9]+");

	// A format that will convert to a date when pasted into a Google spreadsheet.
	final private SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	// Formats that will convert to format for the Maps web page.
	final private SimpleDateFormat gMapsTimeFormat = new SimpleDateFormat("HH:mm a");

	final private List<CollectionParams> collectionParams = new ArrayList<CollectionParams>();

	private WebDriver driver = null;

	public DurationCollector(String[] args) {
		isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
				.indexOf("jdwp") >= 0;
		String d = System.getProperty("user.home");
		if (d == null || d.length() == 0) {
			throw new IllegalArgumentException(
					"Unable to determine user.home directory from System.getProperty(\"user.home\")");
		}
		String pathEnd = File.separator + "Documents" + File.separator + "Github" + File.separator
				+ "commute-time-aggregator" + File.separator + "DurationCollector" + File.separator + "data";
		File dir = new File(d + pathEnd);
		if (!dir.exists()) {
			throw new RuntimeException("Unable to find: " + dir.getAbsolutePath());
		}
		dirForResults = d + pathEnd;
	}

	private void loadCollectionParams() throws Exception {
		Integer[] daysOfWeek = { Calendar.SUNDAY }; // TODO : get dayofweek working ----- , Calendar.WEDNESDAY, Calendar.FRIDAY, Calendar.SATURDAY };
		for (Integer dayOfWeek : daysOfWeek) {
			collectionParams.add(new CollectionParams(/* name of data set file */ "to_Netsuite_" + dayOfWeek,
					/* start location */ "2408 E. 24th Street, Oakland CA",
					/* destination location */ "2955 Campus Dr, 100, San Mateo, CA 94403", dayOfWeek));
			collectionParams.add(new CollectionParams(/* name of data set file */ "from_Netsuite_" + dayOfWeek,
					/* destination location */ "2955 Campus Dr, 100, San Mateo, CA 94403",
					/* start location */ "2408 E. 24th Street, Oakland CA",
					dayOfWeek));
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

	private void setUpPage(String origin, String destination) throws Exception {
		WebDriverWait wait = new WebDriverWait(driver, sleepSeconds, 1000);
		wait.until(ExpectedConditions.elementToBeClickable(By.id("searchboxinput")));
		driver.findElement(By.id("searchboxinput")).sendKeys(destination + "\n");

		// wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("section-hero-header-directions")));
		Thread.sleep(10 * 1000); // TODO : any way to wait here instead of sleep?
		driver.findElement(By.className("section-hero-header-directions")).click();

		WebElement currentElement = driver.switchTo().activeElement();
		currentElement.sendKeys(origin + "\n");

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()[contains(.,\"Leave now\")]]")));
		driver.findElement(By.xpath("//*[text()[contains(.,\"Leave now\")]]")).click();

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()[contains(.,\"Depart at\")]]")));
		driver.findElement(By.xpath("//*[text()[contains(.,\"Depart at\")]]")).click();
	}

	private java.util.AbstractMap.SimpleEntry<Integer, Integer> collectDuration(Calendar ts) throws Exception {
		WebDriverWait wait = new WebDriverWait(driver, sleepSeconds, 1000);

		wait.until(ExpectedConditions.elementToBeClickable(By.name("transit-time")));
		WebElement timeEl = driver.findElement(By.name("transit-time"));
		timeEl.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		timeEl.sendKeys(Keys.chord(Keys.DELETE));
		timeEl.sendKeys(this.gMapsTimeFormat.format(ts.getTime()));
		timeEl.sendKeys(Keys.chord(Keys.ENTER));

		Thread.sleep(5000); // TODO : any way to wait here instead of sleep?

//		driver.findElement(By.className("date-input")).click();
// ... some more gibberish to click on appropriate day of month ...
//		Thread.sleep(5000); // TODO : any way to wait here instead of sleep?

		int minEstimate = Integer.MAX_VALUE;
		int maxEstimate = Integer.MAX_VALUE;

		// Google Maps can show multiple alternative routes. Find the minimum.
		List<WebElement> wList = driver.findElements(By.xpath("//span[contains(text(), \"typically\")]"));
		for (WebElement w : wList) {
			String durations = w.findElement(By.xpath("//span[contains(text(), \" min\")]")).getText();
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
		return new java.util.AbstractMap.SimpleEntry<Integer, Integer>(minEstimate, maxEstimate);
	}

	private void initBrowserDriver() {
		if (driver == null) {
			driver = new FirefoxDriver();
//			driver = new ChromeDriver(); // TODO : will this help with other TODO's?
			driver.manage().timeouts().implicitlyWait(sleepSeconds, TimeUnit.SECONDS);
		}
	}

	private BufferedWriter getWriter(String routeId, boolean doAppend) throws Exception {
		String filePathString = dirForResults + "/" + routeId + "_travelTimes.txt";
		File f = new File(filePathString);
		FileWriter fstream;
		if (f.exists()) {
			fstream = new FileWriter(filePathString, doAppend);
		} else {
			fstream = new FileWriter(filePathString);
		}
		return new BufferedWriter(fstream);
	}

	private void writeHeader(CollectionParams cp) throws Exception {
		BufferedWriter out = this.getWriter(cp.routeId, true);
		String s = cp.routeId + "\tminimum\taverage\tmaximum" + System.getProperty("line.separator");
		out.write(s);
		out.close();
	}
	
	private void setupFile(CollectionParams cp) throws Exception {
		String filePathString = dirForResults + "/" + cp.routeId + "_travelTimes.txt";
		File f = new File(filePathString);
		if (f.exists()) {
			f.delete();
		}
		writeHeader(cp);
	}
	
	private void collectDurations() throws Exception {
		for (CollectionParams cp : collectionParams) {
			initBrowserDriver();
			try {
				setupFile(cp);
				driver.get("https://maps.google.com/");

				setUpPage(cp.workLocation, cp.homeLocation);
				
				System.out.println("Select appropriate day and press <ENTER> in the console window ...");
				System.in.read();  // TODO : temporary to allow manual click on appropriate day of the week.
				
				Calendar ts = Calendar.getInstance();
				ts.set(Calendar.DAY_OF_WEEK, cp.dayOfWeek);
				ts.set(Calendar.HOUR_OF_DAY, 4);
				ts.set(Calendar.MINUTE, 0);
				ts.add(Calendar.HOUR, 0); // force Calendar internal recalc.
				while (ts.get(Calendar.HOUR_OF_DAY) < 20) {
					java.util.AbstractMap.SimpleEntry<Integer, Integer> newDuration = this.collectDuration(ts);
					writeDuration(cp, ts, newDuration);
					ts.add(Calendar.MINUTE, 10);
				}
			} finally {
				if (driver != null) {
					driver.quit();
					driver = null;
				}
			}
		}
	}

	private void writeDuration(CollectionParams cp, Calendar ts, java.util.AbstractMap.SimpleEntry<Integer, Integer> p)
			throws Exception {
		BufferedWriter out = this.getWriter(cp.routeId, true);
		Integer average = (p.getKey() + p.getValue()) / 2;
		String s = outputDateFormat.format(ts.getTime()) + "\t" + p.getKey() + "\t" + average + "\t" + p.getValue()
				+ System.getProperty("line.separator");
		out.write(s);
		out.close();
	}

	public void run() {
		Date start = new Date();
		try {
			log("Starting ...");
			loadCollectionParams();
			collectDurations();
		} catch (Exception e) {
			log(e);
		} finally {
			if (driver != null) {
				driver.quit();
				driver = null;
			}
			log("Finished, started at : " + start.toString());
		}
	}

	private void log(Exception e) {
		e.printStackTrace();
		System.out.println(new Date().toString() + "\t" + e);
	}

	private void log(String s) {
		System.out.println(new Date().toString() + "\t" + s);
	}

	public static void main(String[] args) {
		new DurationCollector(args).run();
	}
}
