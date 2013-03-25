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

public class DurationCollector2 {
	private WebDriver driver;
	private Pattern p;
	private String personId;
	private String homeLocation;
	private String workLocation;

	private int collectDuration(String origin, String destination)
			throws Exception {
		driver.get("https://maps.google.com/");
		driver.findElement(By.id("d_launch")).click();
		driver.findElement(By.id("d_d")).clear();
		driver.findElement(By.id("d_d")).sendKeys(origin);
		driver.findElement(By.id("d_daddr")).clear();
		driver.findElement(By.id("d_daddr")).sendKeys(destination);
		driver.findElement(By.id("d_sub")).click();
		List<WebElement> wList = driver.findElements(By
				.xpath("//span[contains(text(), \"In current traffic: \")]"));
		int minutes = Integer.MAX_VALUE;
		for (WebElement w : wList) {
			Matcher m = p.matcher(w.getText());
			if (m.find()) {
				int newVal = Integer.parseInt(m.group());
				if (newVal < minutes) {
					minutes = newVal;
				}
			}
		}
		driver.close();
		return minutes;
	}

	private void collect() throws Exception {
		int previousDuration = -1;
		String origin;
		String destination;
		String direction = "to_home";
		String filePathString = "/temp/" + personId + "_commuteTimes.csv";
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
				int newDuration = this.collectDuration(origin, destination);
				if ((newDuration < Integer.MAX_VALUE) && (newDuration != previousDuration)) {
					// TODO : write the previous duration so as not to lose a data point.
					String s = direction + "\t" + new Date().toString() + "\t"
							+ newDuration;
					out.write(s + "\n");
					out.flush();
					System.out.println(s);
					previousDuration = newDuration;
				}
				
				// Sample every two minutes.
				// For The Future : randomize the sleep time.
				Thread.sleep(2 * 60 * 1000);
			}
		} finally {
			out.close();
		}
	}

	public void run(String[] args) {
		if (args.length < 3) {
			this.homeLocation = "395 Bancroft Avenue, San Leandro, CA 94577";
			this.workLocation = "3200 Bridge Pkwy Redwood City, CA";
			this.personId = "ckeith";
		} else {
			this.homeLocation = args[0];
			this.workLocation = args[1];
			this.personId = args[2];
		}
		try {
			p = Pattern.compile("[0-9]+");
			driver = new FirefoxDriver();
			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			collect();
			driver.quit();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void main(String[] args) {
		new DurationCollector2().run(args);
	}
}
