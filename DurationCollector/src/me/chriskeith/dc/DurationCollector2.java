package me.chriskeith.dc;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import java.util.regex.*;

public class DurationCollector2 {
	private WebDriver driver;
	private String baseUrl;
	private Pattern p;

	@Before
	public void setUp() throws Exception {
		p = Pattern.compile("[0-9]+");
		driver = new FirefoxDriver();
		baseUrl = "https://maps.google.com/";
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	@Test
	public void collectDurations() throws Exception {
		System.out.println(collectDuration());
	}

	private int collectDuration() throws Exception {
		driver.get(baseUrl + "/");
		driver.findElement(By.id("d_launch")).click();
		driver.findElement(By.id("d_d")).clear();
		driver.findElement(By.id("d_d")).sendKeys("3200 Bridge Parkway, Redwood City, CA");
		driver.findElement(By.id("d_daddr")).clear();
		driver.findElement(By.id("d_daddr")).sendKeys("395 Bancroft Ave, San Leandro, CA");
		driver.findElement(By.id("d_sub")).click();
		List<WebElement> wList = driver.findElements(By.xpath("//span[contains(text(), \"In current traffic: \")]"));
		int minutes = 999999;
		for (WebElement w : wList) {
			Matcher m = p.matcher(w.getText());
			if (m.find()) {
				int newVal = Integer.parseInt(m.group());
				if (newVal < minutes) {
					minutes = newVal;
				}
			}
		}
		return minutes;
	}
	
	@After
	public void tearDown() throws Exception {
		driver.quit();
	}
}
