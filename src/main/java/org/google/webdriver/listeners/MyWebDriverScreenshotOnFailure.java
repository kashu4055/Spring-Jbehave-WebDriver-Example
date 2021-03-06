package org.google.webdriver.listeners;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.AfterScenario.Outcome;
import org.jbehave.core.annotations.ScenarioType;
import org.jbehave.core.failures.PendingStepFound;
import org.jbehave.core.failures.UUIDExceptionWrapper;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.web.selenium.RemoteWebDriverProvider;
import org.jbehave.web.selenium.WebDriverProvider;
import org.jbehave.web.selenium.WebDriverSteps;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

public class MyWebDriverScreenshotOnFailure extends WebDriverSteps {
	public static final String DEFAULT_SCREENSHOT_PATH_PATTERN = "{0}/screenshots/failed-scenario-{1}.png";
	protected final StoryReporterBuilder reporterBuilder;
	protected final String screenshotPathPattern;

	public MyWebDriverScreenshotOnFailure(WebDriverProvider driverProvider) {
		this(driverProvider, new StoryReporterBuilder());
	}

	public MyWebDriverScreenshotOnFailure(WebDriverProvider driverProvider,
			StoryReporterBuilder reporterBuilder) {
		this(driverProvider, reporterBuilder, DEFAULT_SCREENSHOT_PATH_PATTERN);
	}

	public MyWebDriverScreenshotOnFailure(WebDriverProvider driverProvider,
			StoryReporterBuilder reporterBuilder, String screenshotPathPattern) {
		super(driverProvider);
		this.reporterBuilder = reporterBuilder;
		this.screenshotPathPattern = screenshotPathPattern;
	}

	@AfterScenario(uponType = ScenarioType.EXAMPLE, uponOutcome = Outcome.FAILURE)
	public void afterScenarioFailure(UUIDExceptionWrapper uuidWrappedFailure) {
		if (uuidWrappedFailure instanceof PendingStepFound) {
			return; // we don't take screen-shots for Pending Steps
		}
		String screenshotPath = screenshotPath(uuidWrappedFailure.getUUID());
		String currentUrl = "[unknown page title]";
		try {
			currentUrl = driverProvider.get().getCurrentUrl();
		} catch (Exception e) {
		}
		boolean savedIt = false;
		try {
			savedIt = saveScreenshotTo(screenshotPath);
		} catch (RemoteWebDriverProvider.SauceLabsJobHasEnded e) {
			System.err
					.println("Screenshot of page '"
							+ currentUrl
							+ "' has **NOT** been saved. The SauceLabs job has ended, possibly timing out on their end.");
			return;
		} catch (Exception e) {
			System.err.println("Screenshot of page '" + currentUrl
					+ ". Will try again. Cause: " + e.getMessage());
			// Try it again. WebDriver (on SauceLabs at least?) has blank-page
			// and zero length files issues.
			try {
				savedIt = saveScreenshotTo(screenshotPath);
			} catch (Exception e1) {
				System.err.println("Screenshot of page '" + currentUrl
						+ "' has **NOT** been saved to '" + screenshotPath
						+ "' because error '" + e.getMessage()
						+ "' encountered. Stack trace follows:");
				e.printStackTrace();
				return;
			}
		}
		if (savedIt) {
			System.out.println("Screenshot of page '" + currentUrl
					+ "' has been saved to '" + screenshotPath + "' with "
					+ new File(screenshotPath).length() + " bytes");
		} else {
			System.err
					.println("Screenshot of page '"
							+ currentUrl
							+ "' has **NOT** been saved. If there is no error, perhaps the WebDriver type you are using is not compatible with taking screenshots");
		}
	}

	protected String screenshotPath(UUID uuid) {
		return MessageFormat.format(screenshotPathPattern,
				reporterBuilder.outputDirectory(), uuid);
	}

	private boolean saveScreenshotTo(String screenshotPath) {
		File screen = ((TakesScreenshot) getDriverProvider().get())
				.getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(screen, new File(screenshotPath));
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}
