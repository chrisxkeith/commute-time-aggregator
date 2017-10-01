package com.ckkeith.duration_collector;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

	final boolean isDebug;
	final int sleepFactor = 2; // increase for slower computers.
	final int sleepSeconds = 30 * sleepFactor;
	final private String dirForResults;

	final private Pattern digitPattern = Pattern.compile("[0-9]+");

	// A format that will convert to a date when pasted into a Google spreadsheet.
	final private SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	// Formats that will convert to format for the Maps web page.
	final private SimpleDateFormat gMapsTimeFormat = new SimpleDateFormat("HH:mm a");

	final private List<CollectionParams> collectionParams = new ArrayList<CollectionParams>();

	private WebDriver driver = null;
	private String otherCollectionParamsFileName = null;
	private int totalCalls;

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
		String inputFileName = d + File.separator + "Documents" + File.separator + "routeInfo.txt";
		File f = new File(inputFileName);
		if (f.exists()) {
			otherCollectionParamsFileName = inputFileName;
		}
		totalCalls = 0;
	}

	String getDayOfWeek(int dayOfWeek) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
		return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
	}

	private void loadCollectionParams(String start, String end, String id, int startDOW, int endDOW) throws Exception {
		collectionParams.add(new CollectionParams(/* name of data set file */ "to_" + id, /* start location */ start,
				/* destination location */ end, startDOW, endDOW));
		collectionParams.add(new CollectionParams(/* name of data set file */ "from_" + id,
				/* destination location */ end, /* start location */ start, startDOW, endDOW));
	}

	private void loadCollectionParams() throws Exception {
		collectionParams.clear();
		if (otherCollectionParamsFileName != null) {
			File otherCollectionParams = new File(otherCollectionParamsFileName);
			if (otherCollectionParams.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(otherCollectionParamsFileName));
				try {
					String personId = br.readLine();
					while (personId != null) {
						String home = br.readLine();
						if (home == null) {
							throw new RuntimeException("Home not specified for: " + personId);
						}
						String work = br.readLine();
						if (work == null) {
							throw new RuntimeException("Work not specified for: " + personId);
						}
						String firstDayOfWeek = br.readLine();
						String lastDayOfWeek = br.readLine();
						collectionParams.add(new CollectionParams(personId, home, work,
								Integer.parseInt(firstDayOfWeek), Integer.parseInt(lastDayOfWeek)));
						personId = br.readLine();
					}
				} finally {
					br.close();
				}
			}
		}
		loadCollectionParams("343 Kenilworth Avenue, San Leandro, CA", "2415 Bay Rd, Redwood City, CA 94063",
				"CK_TechShop_Redwood_City", Calendar.SUNDAY, Calendar.SATURDAY);
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

	// TODO : Any way to replace all calls to Thread.sleep() ?
	private void setUpPage(CollectionParams cp, int dayOfWeek) throws Exception {
		WebDriverWait wait = new WebDriverWait(driver, sleepSeconds, 1000);
		wait.until(ExpectedConditions.elementToBeClickable(By.id("searchboxinput")));
		driver.findElement(By.id("searchboxinput")).sendKeys(cp.workLocation + "\n");

		// wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("section-hero-header-directions")));
		Thread.sleep(10 * 1000);
		driver.findElement(By.className("section-hero-header-directions")).click();

		WebElement currentElement = driver.switchTo().activeElement();
		currentElement.sendKeys(cp.homeLocation + "\n");

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()[contains(.,\"Leave now\")]]")));
		driver.findElement(By.xpath("//*[text()[contains(.,\"Leave now\")]]")).click();

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()[contains(.,\"Depart at\")]]")));
		driver.findElement(By.xpath("//*[text()[contains(.,\"Depart at\")]]")).click();
		clickOnDayOfWeek(dayOfWeek);
		// manuallySetDayOfWeek(cp, dayOfWeek);
	}

	// TODO : add the 'name' of the route.
	private java.util.AbstractMap.SimpleEntry<Integer, Integer> collectDuration(Calendar ts) throws Exception {
		WebDriverWait wait = new WebDriverWait(driver, sleepSeconds, 1000);

		wait.until(ExpectedConditions.elementToBeClickable(By.name("transit-time")));
		WebElement timeEl = driver.findElement(By.name("transit-time"));
		timeEl.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		timeEl.sendKeys(Keys.chord(Keys.DELETE));
		timeEl.sendKeys(this.gMapsTimeFormat.format(ts.getTime()));
		timeEl.sendKeys(Keys.chord(Keys.ENTER));
		totalCalls++;
		Thread.sleep(5000);

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
		String s = cp.routeId + "\tminimum\taverage\tmaximum" + System.getProperty("line.separator");
		out.write(s);
		out.close();
	}

	private String getPath(CollectionParams cp, int dayOfWeek) {
		return dirForResults + "/" + cp.toString(dayOfWeek) + ".txt";
	}

	private void setupFile(CollectionParams cp, int dayOfWeek) throws Exception {
		File f = new File(getPath(cp, dayOfWeek));
		if (f.exists()) {
			f.delete();
		}
		writeHeader(cp, dayOfWeek);
	}

	private void collectDurations() throws Exception {
		for (CollectionParams cp : collectionParams) {
			try {
				for (int dayOfWeek = cp.startDayOfWeek; dayOfWeek <= cp.endDayOfWeek; dayOfWeek++) {
					initBrowserDriver();
					setupFile(cp, dayOfWeek);
					driver.get("https://maps.google.com/");

					setUpPage(cp, dayOfWeek);

					Calendar ts = Calendar.getInstance();
					ts.set(Calendar.DAY_OF_WEEK, dayOfWeek);
					ts.set(Calendar.HOUR_OF_DAY, 4);
					ts.set(Calendar.MINUTE, 0);
					ts.add(Calendar.HOUR, 0); // force Calendar internal recalc.
					while (ts.get(Calendar.HOUR_OF_DAY) < 20) {
						try {
							java.util.AbstractMap.SimpleEntry<Integer, Integer> newDuration = this.collectDuration(ts);
							if ((newDuration.getKey() != Integer.MAX_VALUE)
									&& (newDuration.getValue() != Integer.MAX_VALUE)) {
								writeDuration(cp, ts, newDuration, dayOfWeek);
							} else {
								log("Invalid data for : " + ts);
							}
						} catch (Exception e) {
							log("Inside while : " + e);
							if (driver.getPageSource().contains("Sorry, we could not calculate directions from ")) {
								ts.add(Calendar.MINUTE, 10);
								continue; // Unknown why this happens, try getting data for the next day...
							}
						}
						ts.add(Calendar.MINUTE, 10);
					}
				}
			} catch (Exception e) {
				log(e);
			} finally {
				if (driver != null) {
					driver.quit();
					driver = null;
				}
			}
		}
	}

	private void writeDuration(CollectionParams cp, Calendar ts, java.util.AbstractMap.SimpleEntry<Integer, Integer> p,
			int dayOfWeek) throws Exception {
		BufferedWriter out = this.getWriter(cp, true, dayOfWeek);
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
		log(e.toString());
	}

	private void log(String s) {
		// TODO : also write to log file
		System.out.println(new Date().toString() + "\t" + s + " totalCalls : " + totalCalls);
	}

	public static void main(String[] args) {
		new DurationCollector(args).run();
	}
}
