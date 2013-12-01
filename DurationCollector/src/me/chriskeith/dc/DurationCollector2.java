package me.chriskeith.dc;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import java.util.regex.*;
import java.text.SimpleDateFormat;

/**
 * Ping google maps for estimated commute duration. Write duration and time
 * stamp to tab-separated file. To get better data while running:
 * (1) Don't manually close Firefox window.
 * (2) Don't switch networks (e.g., log into a VPN).
 * (3) Make sure that your Firefox is up-to-date.
 * (4) Use pre-2013 version of Google Maps (may happen automatically with Firefox?)
 * Assume : Java VM is running in the appropriate time zone.
 * 
 * @author ckeith
 */
public class DurationCollector2 {

	class CollectionParams {
		String personId;
		String homeLocation;
		String workLocation;
		Calendar previousSlot;

		CollectionParams(String personId, String homeLocation,
				String workLocation) {
			this.personId = personId;
			this.homeLocation = homeLocation;
			this.workLocation = workLocation;
			this.previousSlot = null;
		}
	}

	final boolean isDebug;

	final private String dirForResults;
	final private Pattern digitPattern = Pattern.compile("[0-9]+");
	final private String otherCollectionParamsFileName;
	final private int firstHour = 4; // 4 am
	final private int lastHour = 20; // 8 pm

	// Sample every two minutes.
	final private int minuteInterval = 2;

	// A format that will convert to a date when pasted into a Google
	// spreadsheet.
	final private SimpleDateFormat outputDateFormat = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm");
	final private SimpleDateFormat fullDateFormat = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss.SSS");
	final private List<CollectionParams> collectionParams = new ArrayList<CollectionParams>();

	private WebDriver driver = null;

	public DurationCollector2(String[] args) {
		isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
			    getInputArguments().toString().indexOf("jdwp") >= 0;
		String d = System.getProperty("user.home");
		if (d == null || d.length() == 0) {
			throw new IllegalArgumentException("Unable to determine user.home directory");
		}
		String pathEnd = File.separator + "Google Drive" + File.separator + "Commute Time Raw Data";
		File dir = new File(d + pathEnd);
		if (dir.exists()) {
			d += pathEnd;
		} else {
			d += File.separator + "My Documents" + pathEnd;
			dir = new File(d);
			if (!dir.exists()) {
				throw new RuntimeException("Unable to find: " + dir.getAbsolutePath());
			}
		}
		dirForResults = d;
		otherCollectionParamsFileName = d + File.separator + "personIds.txt";
	}

	private int collectDuration(String origin, String destination)
			throws Exception {
		initFireFoxDriver();
		int minutes = Integer.MAX_VALUE;
		try {
			driver.get("https://maps.google.com/");
			driver.findElement(By.id("d_launch")).click();
			driver.findElement(By.id("d_d")).clear();
			driver.findElement(By.id("d_d")).sendKeys(origin);
			driver.findElement(By.id("d_daddr")).clear();
			driver.findElement(By.id("d_daddr")).sendKeys(destination + "\n");
			
			// Started being erratic...
			// Replaced "Get Directions" click with \n above.
			// driver.findElement(By.id("d_sub")).click();

			// GMaps can show multiple alternative routes. Use the quickest.
			List<WebElement> wList = driver
					.findElements(By
							.xpath("//span[contains(text(), \"In current traffic: \")]"));
			for (WebElement w : wList) {
				Matcher m = digitPattern.matcher(w.getText());
				if (m.find()) {
					int newVal = Integer.parseInt(m.group());
					if (m.find()) {
						// First number was hour(s).
						newVal = (newVal * 60) + Integer.parseInt(m.group());
					}
					if (newVal < minutes) {
						minutes = newVal;
					}
				}
			}
		} catch (Exception e) {
			// Switching networks (e.g., logging into a VPN)
			// can cause an exception.
			// Shut down the driver and Firefox
			// and restart for the next time slot.
			log("During WebDriver interaction", e);
			if (driver != null) {
				try {
					driver.quit();
				} catch (Exception e2) {
					log("During driver.quit()", e);
				}
				driver = null;
			}
			initFireFoxDriver();
		}
		return minutes;
	}

	private void log(String s, Object o) {
		System.out.println(new Date().toString() + "\t" + s);
		if (o != null) {
			System.out.println(o.toString());
		}
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
		String origin;
		String destination;
		while (true) {
			sleepUntilNextSnapshot();
			int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			for (CollectionParams cp : collectionParams) {
				if (hour < 12) { // switch directions at 12 noon.
					origin = cp.homeLocation;
					destination = cp.workLocation;
				} else {
					origin = cp.workLocation;
					destination = cp.homeLocation;
				}
				int newDuration = this.collectDuration(origin, destination);
				writeDuration(cp, newDuration);
			}
		}
	}

	private int getDayIncrement(Calendar now) {
		if (isDebug) {
			return 0;
		}
		int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
		int dayIncrement = 0;
		if (dayOfWeek == Calendar.SATURDAY) {
			dayIncrement = 2;
		} else if (dayOfWeek == Calendar.SUNDAY) {
			dayIncrement = 1;
		} else {
			int hour = now.get(Calendar.HOUR_OF_DAY);
			if (hour < firstHour) {
				dayIncrement = 1;
			} else if (lastHour <= hour) {
				if (dayOfWeek == Calendar.FRIDAY) {
					dayIncrement = 3;
				} else {
					dayIncrement = 1;
				}
			}
		}
		return dayIncrement;
	}

	private Calendar getNextSlot(Calendar now, int dayIncrement) {
		Calendar start = (Calendar) now.clone();
		if (dayIncrement > 0) {
			// If outside the range during which to record durations, sleep
			// until within the range.
			// Assume (till proved otherwise) that this will this work on the
			// last day of the year.
			start.set(Calendar.DAY_OF_YEAR, start.get(Calendar.DAY_OF_YEAR)
					+ dayIncrement);
			start.set(Calendar.HOUR_OF_DAY, 4);
			start.set(Calendar.MINUTE, 0);

		} else {
			// Round to previous minute instant.
			int lastInstant = (start.get(Calendar.MINUTE) / minuteInterval)
					* minuteInterval;
			// Sync to next minuteInterval instant.
			start.set(Calendar.MINUTE, (lastInstant + minuteInterval));
		}
		start.set(Calendar.SECOND, 0);
		start.set(Calendar.MILLISECOND, 0);
		return start;
	}
	
	private void sleepUntilNextSnapshot() throws Exception {
		Calendar now = Calendar.getInstance();
		int dayIncrement = getDayIncrement(now);
		Calendar start = getNextSlot(now, dayIncrement);
		if (dayIncrement > 0) {
			// Reload in case we've manually edited the file containing the list
			// of routes.
			loadCollectionParams();
			System.out
					.println("About to sleep until "
							+ outputDateFormat.format(new Date(start
									.getTimeInMillis())));
		}
		long millis = start.getTimeInMillis() - now.getTimeInMillis();
		Thread.sleep(millis);
	}

	private void writeDuration(CollectionParams cp, int duration) throws Exception {
		Calendar now = Calendar.getInstance();
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);
		String durationStr = "";
		BufferedWriter out = this.getWriter(cp.personId, true);
		if (cp.previousSlot != null) {
			cp.previousSlot.add(Calendar.MINUTE, this.minuteInterval);
			// If we didn't get a value, write empty slot(s) to keep slots
			// across different days in sync.
			while (cp.previousSlot.before(now)) {
				String s = outputDateFormat.format(new Date(cp.previousSlot
						.getTimeInMillis()))
						+ "\t\t"
						+ fullDateFormat.format(new Date(cp.previousSlot
								.getTimeInMillis()))
						+ "\t"
						+ fullDateFormat.format(new Date(now
								.getTimeInMillis()))
						+ System.getProperty("line.separator");
				out.write(s);
				cp.previousSlot.add(Calendar.MINUTE, this.minuteInterval);
			}
		}
		if ((0 < duration) && (duration < Integer.MAX_VALUE)) {
			durationStr = new Integer(duration).toString();
		}
		String s = outputDateFormat.format(new Date(now.getTimeInMillis()))
				+ "\t" + durationStr + System.getProperty("line.separator");
		out.write(s);
		out.close();
		cp.previousSlot = now;
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
		collectionParams.add(new CollectionParams("ChristopherKeith",
				"368 MacArthur Blvd, San Leandro, CA 94577",
				"2623 Camino Ramon, San Ramon, CA, USA"));
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
		System.out.println(new Date().toString() + e);
	}

	public static void main(String[] args) {
		new DurationCollector2(args).run();
	}
}
