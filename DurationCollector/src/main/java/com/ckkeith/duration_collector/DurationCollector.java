package com.ckkeith.duration_collector;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import java.text.SimpleDateFormat;

/**
 * Scrape google maps for estimated commute duration.
 * Write duration and time stamp to tab-separated file.
 * To get better data while running:
 * - Don't manually close browser window.
 * - Don't switch networks (e.g., log into a VPN).
 * - Make sure that your browser is up-to-date.
 * Assume : Java VM is running in the appropriate time zone.
 */
public class DurationCollector {

	class CollectionParams {
		String personId;
		String homeLocation;
		String workLocation;

		CollectionParams(String personId, String homeLocation,
				String workLocation) {
			this.personId = personId;
			this.homeLocation = homeLocation;
			this.workLocation = workLocation;
		}
	}

	final boolean isDebug;
	final private String dirForResults;
	final private String otherCollectionParamsFileName;

	// A format that will convert to a date when pasted into a Google
	// spreadsheet.
	final private SimpleDateFormat outputDateFormat = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm");

	final private List<CollectionParams> collectionParams = new ArrayList<CollectionParams>();

	private WebDriver driver = null;

	public DurationCollector(String[] args) {
		isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
			    getInputArguments().toString().indexOf("jdwp") >= 0;
		String d = System.getProperty("user.home");
		if (d == null || d.length() == 0) {
			throw new IllegalArgumentException("Unable to determine user.home directory");
		}
		String pathEnd = File.separator + "Documents" + File.separator + "Github" + File.separator
				+ "commute-time-aggregator" + File.separator + "DurationCollector" + File.separator + "data";
		File dir = new File(d + pathEnd);
		if (!dir.exists()) {
			throw new RuntimeException("Unable to find: " + dir.getAbsolutePath());
		}
		dirForResults = d + pathEnd;
		otherCollectionParamsFileName = null; // d + File.separator + "personIds.txt";
	}

	private int minutesFromString(String s) {
		int minutes = 0;
		if (s.contains(" h ")) {
			String[] c = s.split(" h ");
			minutes += Integer.parseInt(c[0]);
			s = c[1];
		}
		s = s.replaceAll("min", "");
		return minutes + Integer.parseInt(s);
	}
	
	private int collectDuration(String origin, String destination, String timeStamp)
			throws Exception {
		initFireFoxDriver();
		driver.get("https://maps.google.com/");
		driver.findElement(By.id("searchboxinput")).sendKeys(destination + "\n");
		driver.findElement(By.className("section-hero-header-directions")).click();
		WebElement currentElement = driver.switchTo().activeElement();
		currentElement.sendKeys(origin + "\n");
		driver.findElement(By.xpath("//*[text()[contains(.,\"Text: Leave now\")]]")).click();
		driver.findElement(By.xpath("//*[text()[contains(.,\"Text: Depart at\")]]")).click();
		driver.findElement(By.name("transit-time")).sendKeys(timeStamp);
		
		// Google Maps can show multiple alternative routes. Use the first.
		List<WebElement> wList = driver
				.findElements(By
						.xpath("//span[contains(text(), \"typically\")]"));

		WebElement w = wList.get(0);
		String durations = w.findElement(By.xpath("//span[contains(text(), \"min\")]")).getText();
		String[] rangeLimits = durations.split("-");
		return minutesFromString(rangeLimits[0]) + minutesFromString(rangeLimits[1]);
	}

	private void initFireFoxDriver() {
		if (driver == null) {
			driver = new FirefoxDriver();
			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		}
	}

	private BufferedWriter getWriter(String personId, boolean doAppend) throws Exception {
		String filePathString = dirForResults + "/" + personId
				+ "_commuteTimes.txt";
		FileWriter fstream;
		File f = new File(filePathString);
		if (f.exists()) {
			fstream = new FileWriter(filePathString, doAppend);
		} else {
			fstream = new FileWriter(filePathString);
		}
		return new BufferedWriter(fstream);
	}

	private void collectDurations() throws Exception {
		for (CollectionParams cp : collectionParams) {
			for (int hour = 4 ; hour < 20; hour++) {
				for (int minute = 0; minute < 60; minute += 10) {
					String origin;
					String destination;
					String ampm;
					if (hour < 12) { // switch directions at 12 noon.
						origin = cp.homeLocation;
						destination = cp.workLocation;
						ampm =" AM";
					} else {
						origin = cp.workLocation;
						destination = cp.homeLocation;
						ampm =" PM";
					}
					String timeStr = hour + ":" + minute + ampm;
					int newDuration = this.collectDuration(origin, destination, timeStr);
					writeDuration(cp, newDuration);
				}
			}
		}
	}

	private void writeDuration(CollectionParams cp, int duration) throws Exception {
		Calendar now = Calendar.getInstance();
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);
		String durationStr = "";
		BufferedWriter out = this.getWriter(cp.personId, true);
		if ((0 < duration) && (duration < Integer.MAX_VALUE)) {
			durationStr = new Integer(duration).toString();
		}
		String s = outputDateFormat.format(new Date(now.getTimeInMillis()))
				+ "\t" + durationStr + System.getProperty("line.separator");
		out.write(s);
		out.close();
	}

	private void loadCollectionParams() throws Exception {
		collectionParams.clear();
		if (otherCollectionParamsFileName != null) {
			File otherCollectionParams = new File(otherCollectionParamsFileName);
			if (otherCollectionParams.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(
						otherCollectionParamsFileName));
				try {
					String personId = br.readLine();
					while (personId != null) {
						String home = br.readLine();
						if (home == null) {
							throw new RuntimeException("Home not specified for: "
									+ personId);
						}
						String work = br.readLine();
						if (work == null) {
							throw new RuntimeException("Work not specified for: "
									+ personId);
						}
						collectionParams.add(new CollectionParams(personId, home,
								work));
						personId = br.readLine();
					}
				} finally {
					br.close();
				}
			}
		}
		// Add this one last, so the browser shows it (to help
		// debugging/monitoring).
		collectionParams.add(new CollectionParams("toMtTam",
				"343 Kenilworth Avenue, San Leandro, CA 94577",
				"Mt Tamalpais, California 94941"));
	}

	public void run() {
		try {
			loadCollectionParams();
			collectDurations();
		} catch (Exception e) {
			log(e);
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	private void log(Exception e) {
		e.printStackTrace();
		System.out.println(new Date().toString() + " " + e);
	}

	public static void main(String[] args) {
		new DurationCollector(args).run();
	}
}
