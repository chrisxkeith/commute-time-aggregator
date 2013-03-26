package me.chriskeith.dc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import java.util.regex.*;

/**
 * Ping google maps for estimated commute duration. Write duration and time stamp to tab-separated file.
 * @author ckeith
 * 
 * Don't manually close Firefox window.
 * Don't switch networks (e.g., log into a VPN).
 */
public class DurationCollector2 {
	private Pattern digitPattern;
	private String personId;
	private String homeLocation;
	private String workLocation;

	private int collectDuration(WebDriver driver, String origin, String destination)
			throws Exception {
		driver.get("https://maps.google.com/");
		driver.findElement(By.id("d_launch")).click();
		driver.findElement(By.id("d_d")).clear();
		driver.findElement(By.id("d_d")).sendKeys(origin);
		driver.findElement(By.id("d_daddr")).clear();
		driver.findElement(By.id("d_daddr")).sendKeys(destination);
		driver.findElement(By.id("d_sub")).click();
		
		// GMaps can show multiple alternative routes. Use the quickest.
		List<WebElement> wList = driver.findElements(By
				.xpath("//span[contains(text(), \"In current traffic: \")]"));
		int minutes = Integer.MAX_VALUE;
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
		return minutes;
	}

	private void collect(WebDriver driver) throws Exception {
		int previousDuration = -1;
		Date previousDate = null;
		String origin;
		String destination;
		String direction = "to_home";
		// TODO : create the directory
		String filePathString = "/temp/" + personId + "_commuteTimes.txt";
		FileWriter fstream;
		File f = new File(filePathString);
		if (f.exists()) {
			fstream = new FileWriter(filePathString, true);
		} else {
			fstream = new FileWriter(filePathString);
		}
		BufferedWriter out = new BufferedWriter(fstream);
		try {
			while (true) {
				// Assume : Java VM is running in the appropriate time zone.
				// TODO : if a weekend day, sleep until midnight Monday.
				int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				if (hour < 13) { // switch directions at 12 noon.
					origin = this.homeLocation;
					destination = this.workLocation;
					if (direction.equals("to_home")) {
						previousDuration = -1;
					}
					direction = "to_work";
				} else {
					origin = this.workLocation;
					destination = this.homeLocation;
					if (direction.equals("to_work")) {
						previousDuration = -1;
					}
					direction = "to_home";
				}
				int newDuration = this.collectDuration(driver, origin, destination);
				if ((newDuration < Integer.MAX_VALUE) && (newDuration != previousDuration)) {
					if (previousDate != null) {
						writeDuration(out, direction, previousDate, previousDuration);
					}
					writeDuration(out, direction, new Date(), newDuration);
					previousDuration = newDuration;
				}
				previousDate = new Date();
				
				// Sample every two minutes.
				// For The Future : randomize the sleep time.
				Thread.sleep(2 * 60 * 1000);
			}
		} finally {
			out.close();
		}
	}

	private void writeDuration(BufferedWriter out, String direction, Date date, int duration) throws Exception {
		String s = direction + "\t" + date.toString() + "\t" + duration;
		out.write(s + System.getProperty("line.separator"));
		out.flush();
		System.out.println(s);

	}
	
	private void runWithRetries() {
		for (int retries = 0; retries < 5; retries++) {
			WebDriver driver = null;
			try {
				driver = new FirefoxDriver();
				driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
				collect(driver);
			} catch (Exception e) {
				System.out.println(new Date().toString() + System.getProperty("line.separator") + e);
			} finally {
				if (driver != null) {
					driver.quit();
				}
			}
		}
	}
	
	public void run(String[] args) {
		if (args.length < 3) {
			this.homeLocation = "368 MacArthur Blvd, San Leandro, CA 945777";
			this.workLocation = "3200 Bridge Pkwy Redwood City, CA";
			this.personId = "ckeith";
		} else {
			this.homeLocation = args[0];
			this.workLocation = args[1];
			this.personId = args[2];
		}
		digitPattern = Pattern.compile("[0-9]+");
		runWithRetries();
	}

	public static void main(String[] args) {
		new DurationCollector2().run(args);
	}
}
