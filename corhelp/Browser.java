package Utils;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonParseException;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.Augmentable;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.google.common.base.Function;
import com.opera.core.systems.OperaDriver;

import Utils.DataBase.DatabaseType;
import Utils.Element.How;
import edu.umass.cs.benchlab.har.HarLog;
import edu.umass.cs.benchlab.har.tools.HarFileReader;
import ru.yandex.qatools.allure.annotations.Attachment;

public class Browser
{
	
	// This class overrides the setCompressionQuality() method to workaround
	// a problem in compressing JPEG images using the javax.imageio package.
	public static class MyImageWriteParam extends JPEGImageWriteParam
	{
		public MyImageWriteParam()
		{
			super(Locale.getDefault());
		}
		
		// This method accepts quality levels between 0 (lowest) and 1 (highest)
		// and simply converts
		// it to a range between 0 and 256; this is not a correct conversion
		// algorithm.
		// However, a proper alternative is a lot more complicated.
		// This should do until the bug is fixed.
		@Override
		public void setCompressionQuality(float quality)
		{
			if (quality < 0.0F || quality > 1.0F)
			{
				throw new IllegalArgumentException("Quality out-of-bounds!");
			}
			this.compressionQuality = 256 - (quality * 256);
		}
	}
	
	/**
	 * Brings current window on focus
	 * 
	 * @param testConfig
	 */
	public static void bringToFocus(Config testConfig)
	{
		
		String currentWindowHandle = testConfig.driver.getWindowHandle();
		((JavascriptExecutor) testConfig.driver).executeScript("alert('Test')");
		testConfig.driver.switchTo().alert().accept();
		testConfig.driver.switchTo().window(currentWindowHandle);
		
		testConfig.logComment("Brought current window to focus");
	}
	
	/**
	 * Refresh browser once
	 */
	public static void browserRefresh(Config testConfig)
	{
		//testConfig.driver.navigate().refresh();
		executeJavaScript(testConfig, "location.reload();");
		testConfig.logComment("Refreshing the browser...");
	}
	
	
	@Attachment(value = "Screenshot", type = "image/png")
	private static byte[] captureScreenshot(Config testConfig)
	{
		
		byte[] screenshot = null;
		
		try
		{
			if(Popup.isAlertPresent(testConfig, false))
			{
				Popup.ok(testConfig);
			}
			
		if (testConfig.driver.getClass().isAnnotationPresent(Augmentable.class) ||
			testConfig.driver.getClass().getName().startsWith("org.openqa.selenium.remote.RemoteWebDriver$$EnhancerByCGLIB")) 
		{
				WebDriver augumentedDriver = new Augmenter().augment(testConfig.driver);
				
				try{
					screenshot = ((TakesScreenshot) testConfig.driver).getScreenshotAs(OutputType.BYTES); 
				}
				catch(Exception e){
					testConfig.logComment("****************************exception in browser.java ***********************");
					e.printStackTrace();
				}
		}
		else
		{
			screenshot = ((TakesScreenshot)testConfig.driver).getScreenshotAs(OutputType.BYTES);
		}
		
		}
		catch (UnhandledAlertException alert)
		{
			Popup.ok(testConfig);
			testConfig.logWarning(ExceptionUtils.getFullStackTrace(alert));
		}
		catch (NoSuchWindowException NoSuchWindowExp)
		{
			testConfig.logWarning("NoSuchWindowException:Screenshot can't be taken. Probably browser is not reachable");
			//test case will end, setting this as null will prevent taking screenshot again in cleanup
			testConfig.driver = null;
		}
		catch (WebDriverException webdriverExp)
		{
			testConfig.logWarning("Unable to take screenshot:- " + ExceptionUtils.getFullStackTrace(webdriverExp));
		}
		return screenshot;
	}
	
	/**
	 * Close the current window, quitting the browser if it's the last window
	 * currently open.
	 * 
	 * @param Config
	 *            test config instance for the browser to be closed
	 */
	public static void closeBrowser(Config testConfig)
	{
		try
		{
			if (testConfig.driver != null)
			{
				testConfig.logComment("Close the browser window with URL:- " + testConfig.driver.getCurrentUrl() + ". And title as :- " + testConfig.driver.getTitle());
				testConfig.driver.close();
			}
		}
		catch (UnreachableBrowserException e)
		{
			testConfig.logWarning(ExceptionUtils.getFullStackTrace(e));
		}
	}
	
	// Reads the jpeg image in infile, compresses the image,
	// and writes it back out to outfile.
	// compressionQuality ranges between 0 and 1,
	// 0-lowest, 1-highest.
	private static void compressJpegFile(File infile, File outfile, float compressionQuality)
	{
		try
		{
			// Retrieve jpg image to be compressed
			RenderedImage rendImage = ImageIO.read(infile);
			
			// Find a jpeg writer
			ImageWriter writer = null;
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("gif");
			if (iter.hasNext())
			{
				writer = iter.next();
			}
			
			// Prepare output file
			ImageOutputStream ios = ImageIO.createImageOutputStream(outfile);
			writer.setOutput(ios);
			
			// Set the compression quality
			ImageWriteParam iwparam = new MyImageWriteParam();
			iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			iwparam.setCompressionQuality(compressionQuality);
			
			// Write the image
			writer.write(new IIOImage(rendImage, null, null));
			
			// Cleanup
			ios.flush();
			writer.dispose();
			ios.close();
		}
		
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (OutOfMemoryError outOfMemoryError)
		{
			outOfMemoryError.printStackTrace();
		}
	}
	
	/**
	 * Delete the cookies of the given browser instance
	 * 
	 * @param Config
	 *            test config instance
	 */
	public static void deleteCookies(Config testConfig)
	{
		if (testConfig.driver != null)
		{
			testConfig.logComment("Delete all cookies!!");
			testConfig.driver.manage().deleteAllCookies();
		}
	}
	
	public static void waitForPageTitleToContain(Config testConfig, String title)
	{
		testConfig.logComment("Wait for page title to contain '" + title + "'.");
		Long ObjectWaitTime = Long.parseLong(testConfig.getRunTimeProperty("ObjectWaitTime"));
		WebDriverWait wait = new WebDriverWait(testConfig.driver, ObjectWaitTime);
		wait.until(ExpectedConditions.titleContains(title));
	}
	
	/**
	 * @param testConfig
	 *            - element of Config
	 * @param path
	 *            - path of the folder where file is present
	 * @param name
	 *            - some text that is present in file name
	 * @return - file name with matching text
	 */
	public static File DesiredFileDownload(Config testConfig, String path, String name)
	{
		File fl = new File(path);
		File choise = null;
		for (int retry = 0; retry <= 15; retry++)
		{
			// storing all file names in files array
			File[] files = fl.listFiles(new FileFilter()
			{
				@Override
				public boolean accept(File file)
				{
					return file.isFile();
				}
			});
			
			// Finding the desired file
			for (File file : files)
			{
				// System.out.println(file);
				if (file.getName().contains(name) && (!file.getName().contains("part")))
				{
					choise = file;
					break;
				}
			}
			// checking if file with required name has been found out
			if (choise == null && retry == 15)
			{
				Log.Fail("File with name " + name + " does not exist in " + path, testConfig);
				break;
			}
			else
				if (choise == null)
					Browser.wait(testConfig, 1);
				else
					break;
		}
		return choise;
	}
	
	/**
	 * Executes JavaScript in the context of the currently selected frame or
	 * window in the Config driver instance.
	 * 
	 * @param javaScriptToExecute
	 *            Java Script To Execute
	 * @return If the script has a return value (i.e. if the script contains a
	 *         return statement), then the following steps will be taken: For an
	 *         HTML element, this method returns a WebElement For a decimal, a
	 *         Double is returned For a non-decimal number, a Long is returned
	 *         For a boolean, a Boolean is returned For all other cases, a
	 *         String is returned. For an array, return a List<Object> with each
	 *         object following the rules above. We support nested lists. Unless
	 *         the value is null or there is no return value, in which null is
	 *         returned
	 */
	public static Object executeJavaScript(Config testConfig, String javaScriptToExecute)
	{
		testConfig.logComment("Execute javascript:-" + javaScriptToExecute);
		JavascriptExecutor javaScript = (JavascriptExecutor) testConfig.driver;
		return javaScript.executeScript(javaScriptToExecute);
	}
	
	private static String getCallerClassName()
	{
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		for (int i = 1; i < stElements.length; i++)
		{
			StackTraceElement ste = stElements[i];
			if (!ste.getClassName().equals(Browser.class.getName()) && !ste.getClassName().contains("Helper") && ste.getClassName().indexOf("java.lang.Thread") != 0)
			{
				return ste.getClassName();
			}
		}
		return null;
	}
	
	public static String getCookieValue(Config testConfig, String cookieName)
	{
		String value = null;
		if (testConfig.driver != null)
		{
			Cookie cookie = testConfig.driver.manage().getCookieNamed(cookieName);
			if (cookie == null)
			{
				testConfig.logFail("Cookie " + cookieName + " Not found");
				return null;
			}
			value = cookie.getValue();
			testConfig.logComment("Read the cookie named '" + cookieName + "' value as '" + value + "'");
		}
		return value;
	}
	
	/**
	 * Returns the latest firebug request response. Useful for debugging failure
	 * points
	 * 
	 * @param testConfig
	 * @param request
	 * @return String[] 0 - Request, 1- Response
	 */
	public static void getFireBugResponse(Config testConfig)
	{
		File[] fileList = null;
		File filesContainer = null;
		try
		{
			String filePath = new String(testConfig.getRunTimeProperty("ResultsDir") + File.separator + "CaptureNetworkTraffic" + File.separator + "HAR_" + testConfig.getTestName());
			try
			{
				filesContainer = new File(filePath);
				fileList = filesContainer.listFiles();
				if (fileList.length == 0)
					testConfig.logWarning("Folder found but .Har files not found.");
			}
			catch (Exception e)
			{
				testConfig.logWarning("<-----.har files not found, so unable to capture the Api calls---->");
				testConfig.logComment("Path was : "+filePath);
			}
			
			if (fileList != null)
			{
				Arrays.sort(fileList, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
				
				// Update all files and rename them
				for (int i = 0; i < fileList.length; i++)
				{
					//Neglating those files which are hidden or have been already modified
					if(!fileList[i].isHidden() && fileList[i].toString().contains("+"))
					{
						HarFileReader r = new HarFileReader();
						HarLog log = r.readHarFile(fileList[i]);
						
						// Searching Something went Wrong in each file to notify the User
						if (log.toString().contains("Something went Wrong"))
						{
							testConfig.logComment("<B><font color='RED'>Warning!! 'Something went Wrong' found in the Api Calls!!</font></B>");
						}
						
						// If updated earlier then dont update
						if (!log.toString().contains("onInputData("))
						{
							RandomAccessFile file = new RandomAccessFile(fileList[i], "rw");
							byte[] text = new byte[(int) file.length()];
							file.readFully(text);
							file.seek(0);
							file.writeBytes("onInputData(");
							file.write(text);
							file.writeBytes(");");
							file.close();
							
							// Rename the file
							File newFile = new File(fileList[i].toString().replace("+", "-"));
							fileList[i].renameTo(newFile);
							
							InetAddress ip;
							String IP = null;
							ip = InetAddress.getLocalHost();
							IP = (ip.getHostAddress()).toString();
							
							
							//For new automation machine we need to do following setting on machine
	                        //open iis
	                        //Select result folder
	                        //Double click on 'Directory browsing' icon and then enable directory browsing
	                        //Double click on 'Mime Types' icon and then add ".har" as text/plain mime type
	                        							
							String fileUrl = Browser.getResultsURLOnRunTime(testConfig, newFile.toString());
							String linkName = newFile.toString().split("CaptureNetworkTraffic")[1];
							testConfig.logComment("<B>Firebug Url</B>:- <a href=http://www.softwareishard.com/har/viewer/?inputUrl=" + fileUrl + " target='_blank' >" + linkName + "</a>");
						}
					}
				}
			}
		}
		catch (JsonParseException e)
		{
			testConfig.logWarning("*****JsonParseException, so unable to capture the firebug response*****");
		}
		catch (IOException e)
		{
			testConfig.logWarning("*****IOException, so unable to capture the firebug response*****");
		}
	}
	
	/**
	 * Uses the specified method name to generate a destination file name where
	 * PageHTML can be saved
	 * 
	 * @param Config
	 *            test config instance
	 * @return file using which we can save PageHTML
	 */
	public static File getPageHTMLFile(Config testConfig)
	{
		File dest = getScreenShotDirectory(testConfig);
		return new File(dest.getPath() + File.separator + getPageHTMLFileName(testConfig.testMethod));
	}
	
	private static String getPageHTMLFileName(Method testMethod)
	{
		String nameScreenshot = testMethod.getDeclaringClass().getName() + "." + testMethod.getName();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();
		return dateFormat.format(date) + "_" + nameScreenshot + ".html";
	}
	
	private static File getScreenShotDirectory(Config testConfig)
	{
		File dest = new File(testConfig.getRunTimeProperty("ResultsDir") + File.separator + "html" + File.separator );
		
		/*
		 * Commenting out since, the current results folder will be passed by ant script and we do not need to calculate File resultsFolder = new File(System.getProperty("user.dir")+"//Results//" +
		 * Config.productName); File[] directories = resultsFolder.listFiles(new FilenameFilter() {
		 * @Override public boolean accept(File dir, String name) { return dir.isDirectory(); } }); long lastMod = Long.MIN_VALUE; File dest = null; if(directories!=null) { for(File
		 * directory:directories) { if (directory.lastModified() > lastMod) { dest = directory; lastMod = directory.lastModified(); } } }
		 */
		
		return dest;
	}
	
	/**
	 * Uses the specified method name to generate a destination file name where
	 * screenshot can be saved
	 * 
	 * @param Config
	 *            test config instance
	 * @return file using which we can call takescreenshot
	 */
	public static File getScreenShotFile(Config testConfig)
	{
		File dest = getScreenShotDirectory(testConfig);
		return new File(dest.getPath() + File.separator + getScreenshotFileName(testConfig.testMethod));
	}
	
	private static String getScreenshotFileName(Method testMethod)
	{
		String nameScreenshot = testMethod.getDeclaringClass().getName() + "." + testMethod.getName();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();
		return dateFormat.format(date) + "_" + nameScreenshot + ".png";
	}
	
	/**
	 * To return back to previous page
	 * 
	 * @param testConfig
	 * @param url
	 */
	public static void goBack(Config testConfig)
	{
		testConfig.logComment("Clicking on back button on browser");
		testConfig.driver.navigate().back();
	}
	
	public static File lastFileModified(Config testConfig, String dir)
	{
		File fl = new File(dir);
		File[] files = fl.listFiles();
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		
		return files[0];
	}
	
	/**
	 * @param testConfig
	 *            - element of Config
	 * @param path
	 *            - path of the folder where file is present
	 * @param name
	 *            - some text that is present in file name
	 * @return - file name of the last modified file with matching text
	 */
	public static File lastFileModifiedWithDesiredName(Config testConfig, String path, String name)
	{
		File fl = new File(path);
		File choise = null;
		List<File> arrayOfSortedFiles = new ArrayList<File>();
		long lastMod = Long.MIN_VALUE;
		for (int retry = 0; retry <= 5; retry++)
		{
			// making a list of files in download folder
			System.out.println("Wait for file to download");
			Browser.wait(testConfig, 5);
			File[] files = fl.listFiles(new FileFilter()
			{
				@Override
				public boolean accept(File file)
				{
					return file.isFile();
				}
			});
			// Matching names of desired file
			for (File file : files)
			{
				if (file.getName().contains(name))
					arrayOfSortedFiles.add(file);
			}
			if (arrayOfSortedFiles.size() > 0)
				break;
			else
				continue;
		}
		// Finding matching file which has been last modified
		for (File matchingfile : arrayOfSortedFiles)
		{
			if (matchingfile.lastModified() > lastMod)
			{
				choise = matchingfile;
				lastMod = matchingfile.lastModified();
			}
		}
		if (choise == null)
			Log.Fail("No File found with name" + name, testConfig);
		else
			System.out.println("The file chosen is as: " + choise.getName());
		return choise;
	}
	
	/**
	 * Navigate to driver the URL specified
	 * 
	 * @param Config
	 *            test config instance
	 * @param url
	 *            URL to be navigated
	 */
	public static void navigateToURL(Config testConfig, String url)
	{
		if (testConfig.driver == null)
		{
			testConfig.openBrowser();
		}
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date startDate = new Date();
		testConfig.logComment("Navigate to web page- '" + url + "' at:- "+dateFormat.format(startDate));
		try
		{
			testConfig.driver.get(url);
		}
		catch(UnhandledAlertException ua)
		{
			testConfig.logWarning("Alert appeared during navigation");
			if(Popup.isAlertPresent(testConfig))
				Popup.ok(testConfig);
		}
		
		if (url.contains(".payubiz.in"))
			Browser.setCookieValue(testConfig, "test_name", testConfig.getTestName(), ".payubiz.in");
		if (url.contains(".payu.in"))
			Browser.setCookieValue(testConfig, "test_name", testConfig.getTestName(), ".payu.in");
			
	}
	
	
	private static FirefoxProfile addNetExportAddOnInFirefox(Config testConfig, FirefoxProfile firefoxProfile)
	{
		if (testConfig.getRunTimeProperty("NetExport").equalsIgnoreCase("true"))
		{
			String firebugPath = System.getProperty("user.dir") + File.separator + ".." + File.separator + "Common" + File.separator + "Prerequisite" + File.separator + "firebug-2.0.16.xpi";
		 	String netExportPath = System.getProperty("user.dir") + File.separator + ".." + File.separator + "Common" + File.separator + "Prerequisite" + File.separator + "netExport-0.9b7.xpi";
		 	String exportDataAtLocation = testConfig.getRunTimeProperty("ResultsDir") + File.separator + "CaptureNetworkTraffic" + File.separator + "HAR_" + testConfig.getTestName();
			
			try
			{
				firefoxProfile.addExtension(new File(firebugPath));
				firefoxProfile.addExtension(new File(netExportPath));
				
				// Setting netExport preferences
				firefoxProfile.setPreference("extensions.firebug.netexport.alwaysEnableAutoExport", true);
				firefoxProfile.setPreference("extensions.firebug.netexport.autoExportToFile", true);
				firefoxProfile.setPreference("extensions.firebug.netexport.Automation", true);
				firefoxProfile.setPreference("extensions.firebug.netexport.showPreview", false);
				firefoxProfile.setPreference("extensions.firebug.netexport.defaultLogDir", exportDataAtLocation);
				
				// Setting Firebug preferences
				firefoxProfile.setPreference("extensions.firebug.currentVersion", "2.0");
				firefoxProfile.setPreference("extensions.firebug.addonBarOpened", true);
				firefoxProfile.setPreference("extensions.firebug.console.enableSites", true);
				firefoxProfile.setPreference("extensions.firebug.cookies.enableSites", true);
				firefoxProfile.setPreference("extensions.firebug.script.enableSites", true);
				firefoxProfile.setPreference("extensions.firebug.net.enableSites", true);
				firefoxProfile.setPreference("extensions.firebug.cookies.enableSites", true);
				firefoxProfile.setPreference("extensions.firebug.previousPlacement", 1);
				firefoxProfile.setPreference("extensions.firebug.allPagesActivation", "on");
				firefoxProfile.setPreference("extensions.firebug.onByDefault", true);
				firefoxProfile.setPreference("extensions.firebug.defaultPanelName", "net");
				firefoxProfile.setPreference("extensions.firebug.net.defaultPersist", true);
				firefoxProfile.setPreference("extensions.firebug.framePosition", "detached");
				firefoxProfile.setPreference("extensions.firebug.netRequestHeadersVisible", false);
				firefoxProfile.setPreference("extensions.firebug.netResponseHeadersVisible", false);
			}
			catch (IOException e)
			{
				testConfig.logComment("*****Exception while adding NetExport addOn in Firefox*****");
				e.printStackTrace();
			}
		}
		return firefoxProfile;
	}

	/**
	 * Opens the new browser instance using the given config
	 * 
	 * @return new browser instance
	 * @throws IOException
	 */
	public static WebDriver openBrowser(Config testConfig)
	{
		WebDriver driver = null;
		String browser = testConfig.getRunTimeProperty("Browser");
		
		//Code to handle Local Execution and Jenkins Execution WITHOUT Selenium Grid
		if (!testConfig.remoteExecution)
		{
			testConfig.logComment("Launching '" + browser + "' browser in local machine");
			
			switch (browser.toLowerCase())
			{
				case "firefox":
					//Handling the case of : unable to bind to locking port 7054 within 45 seconds
					FirefoxBinary firefoxBinary = new FirefoxBinary();
					firefoxBinary.setTimeout(java.util.concurrent.TimeUnit.SECONDS.toMillis(90));
					
					//Creating a new Firefox Profile
					FirefoxProfile firefoxProfile = new FirefoxProfile();
					firefoxProfile = setFireFoxProfile(testConfig, firefoxProfile);
					
					//Adding DesiredCapabilities
					DesiredCapabilities ffCapability = DesiredCapabilities.firefox();
					ffCapability.setCapability("firefox_profile", firefoxProfile);
					
					if (testConfig.getRunTimeProperty("isStaticPage")=="true")
						try {
							JavaScriptError.addExtension(firefoxProfile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					
					//Now Launching firefox
					driver = new FirefoxDriver(firefoxBinary, firefoxProfile, ffCapability);
					break;
				
				case "htmlunit":// for headless browser execution , used for running api test cases
					driver = new HtmlUnitDriver(true);
					break;
					
				case "phantomjs":// for headless browser execution
                    DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
                    
                    if(System.getProperty("os.name").contains("Mac"))
                    	desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,".."+File.separator+"Common"+File.separator+"Prerequisite"+File.separator+"PhantomJs"+File.separator+"ForMac"+File.separator+"bin"+File.separator+"phantomjs");
                    else
                    	desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,".."+File.separator+"Common"+File.separator+"Prerequisite"+File.separator+"PhantomJs"+File.separator+"ForWindows"+File.separator+"bin"+File.separator+"phantomjs.exe");
                    driver = new PhantomJSDriver(desiredCapabilities);
                    
					break;
					
				case "chrome":
					System.setProperty("webdriver.chrome.driver", "..\\lib\\chromedriver.exe");
					ChromeOptions chromeOptions = new ChromeOptions();
					chromeOptions.addArguments("--start-maximized");
					driver = new ChromeDriver(chromeOptions);
					break;
				case "ie":
					System.setProperty("webdriver.ie.driver", "..\\lib\\IEDriverServer.exe");
					DesiredCapabilities ieCapability = DesiredCapabilities.internetExplorer();
					ieCapability.setCapability("ignoreProtectedModeSettings", true);
					ieCapability.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
					driver = new InternetExplorerDriver(ieCapability);
					break;
					
				case "opera":
					DesiredCapabilities operaCapability = DesiredCapabilities.opera();
					operaCapability.setCapability("opera.port", -1);
					driver = new OperaDriver(operaCapability);
					break;
					
				case "android_web":
					driver = Appium.openBrowser(testConfig);
					break;
				default:
					Assert.fail(browser + "- is not supported");
			}
		}
		//Code to handle Remote Execution on Selenium Grid
		else
		{
			DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			Date startDate = new Date();
			testConfig.logComment("Launching '" + browser + "' browser in remote machine at:- "+dateFormat.format(startDate));
			
			//Changing Temp Folder path from local to master machine
			setTempProfile(testConfig, File.separator + File.separator + testConfig.RemoteAddress + File.separator +"SeleniumTemp");
			
			DesiredCapabilities capability = null;
			switch (browser.toLowerCase())
			{
				case "firefox":
					//Creating a new Firefox Profile
					FirefoxProfile firefoxProfile = new FirefoxProfile();
					firefoxProfile = setFireFoxProfile(testConfig, firefoxProfile);
					
					//Adding DesiredCapabilities
					capability = DesiredCapabilities.firefox();
					capability.setCapability("firefox_profile", firefoxProfile);
					
					//Adding JsErrorCollector extension
					if (testConfig.getRunTimeProperty("isStaticPage")=="true")
						try {
							JavaScriptError.addExtension(firefoxProfile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					break;
					
				case "phantomjs":// for headless browser execution
					capability = DesiredCapabilities.phantomjs();
                    testConfig.logComment("=========>System user directory = "+System.getProperty("user.dir"));
                    capability.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, System.getProperty("user.dir")+File.separator+".."+File.separator+"Common"+File.separator+"Prerequisite"+File.separator+"PhantomJs"+File.separator+"ForWindows"+File.separator+"bin"+File.separator+"phantomjs.exe");
					break;
					
				case "chrome":
					capability = DesiredCapabilities.chrome();
					break;
					
				case "ie":
					capability = DesiredCapabilities.internetExplorer();
					System.setProperty("webdriver.ie.driver", "..\\lib\\IEDriverServer.exe");
					capability.setCapability("ignoreProtectedModeSettings", true);
					capability.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
					break;
					
				case "safari":
					capability = DesiredCapabilities.safari();
					break;
					
				case "opera":
					capability = DesiredCapabilities.opera();
					break;
					
				case "android_web":
					return Appium.openBrowser(testConfig);
					
				default:
					Assert.fail(browser + "- is not supported");
			}
			
			capability.setCapability("version", Config.BrowserVersion);
			
			String platformName = Config.PlatformName;
			switch (platformName.toLowerCase())
			{
				case "winxp":
					capability.setPlatform(Platform.XP);
					break;
				case "win7":
					capability.setCapability("platform", Platform.VISTA); 
					capability.setCapability("name", "Windows 7");
					break;
				case "win8":
					capability.setPlatform(Platform.WIN8);
					break;
				case "winvista":
					capability.setPlatform(Platform.VISTA);
					break;
				case "linux":
					capability.setPlatform(Platform.LINUX);
					break;
				case "mac":
					capability.setPlatform(Platform.MAC);
					break;
				default:
					capability.setPlatform(Platform.ANY);
					break;
			}
			try
			{
				String remoteHostUrl = getRemoteHostUrl(testConfig);
				RemoteWebDriver remoteWebDriver = new RemoteWebDriver(new URL(remoteHostUrl + "/wd/hub"), capability);
				remoteWebDriver.setFileDetector(new LocalFileDetector());
				driver = remoteWebDriver;
				testConfig.session = remoteWebDriver.getSessionId();
			}
			catch (MalformedURLException e)
			{
				String fullStackTrace = ExceptionUtils.getFullStackTrace(e);
				testConfig.logWarning(fullStackTrace);
			}
			catch (Exception e)
			{
				String fullStackTrace = ExceptionUtils.getFullStackTrace(e);
				testConfig.logWarning(fullStackTrace);
			}
			
		}
		if (driver != null)
		{
			//Close the browser incase time taken to load a page exceed 2 min
			Long ObjectWaitTime = Long.parseLong(testConfig.getRunTimeProperty("ObjectWaitTime"));
			driver.manage().timeouts().implicitlyWait(ObjectWaitTime, TimeUnit.SECONDS);
			driver.manage().timeouts().pageLoadTimeout(ObjectWaitTime*3, TimeUnit.SECONDS);
			driver.manage().timeouts().setScriptTimeout(ObjectWaitTime*3, TimeUnit.SECONDS);
			
			//Deciding browser's size based on mobileWeb or Web
			if(!testConfig.isMobile)
			{
				//driver.manage().window().setPosition(new Point(0, 0));
				//driver.manage().window().setSize(new Dimension(334, 494));
			}
			{
				driver.manage().window().maximize();
			}
		}
		
		return driver;
	}
	
	private static String getRemoteHostUrl(Config testConfig)
	{
		String remoteHost = testConfig.getRunTimeProperty("RemoteAddress");
		if (remoteHost != null)
		{
			if (remoteHost.equalsIgnoreCase("saucelabs"))
			{
				// load sauce labs properties
				testConfig.logComment("Loading Sauce labs properies");
				
				String path = System.getProperty("user.dir") + "\\..\\Common\\Parameters\\" + "SauceLabs.properties";
				testConfig.logComment("Read the file:- " + path);
				
				try
				{
					FileInputStream fn = new FileInputStream(path);
					Properties properties = new Properties();
					properties.load(fn);
					fn.close();
					String username = properties.getProperty("sauceLabUserName");
					String accessKey = properties.getProperty("sauceLabAccessKey");
					String slUrl = properties.getProperty("sauceLabUrl");
					return "http://" + username + ":" + accessKey + slUrl;
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				return "http://" + remoteHost + ":" + testConfig.getRunTimeProperty("RemoteHostPort");
			}
		}
		throw new RuntimeException("Remost Host is set as null");
	}
	
	/**
	 * Quits this driver, closing every associated window.
	 * 
	 * @param Config
	 *            test config instance for the browser to be quit
	 */
	public static void quitBrowser(Config testConfig)
	{
		try
		{
			if (testConfig.driver != null)
			{
				if (testConfig.getRunTimeProperty("NetExport").equalsIgnoreCase("true"))
				{
				/*	String filePath = new String(System.getProperty("user.dir") + File.separator + "CaptureNetworkTraffic");
					
					File f = new File(filePath);
					File[] fList = f.listFiles();
					
					for (int i = 0; i < fList.length; i++)
					{
						if (fList[i] != null)
							fList[i].delete();
					} */
				}
				testConfig.logComment("Quit the browser");
				testConfig.driver.quit();
			}
		}
		catch (UnreachableBrowserException e)
		{
			testConfig.logWarning(ExceptionUtils.getFullStackTrace(e));
		}
	}
	
	/**
	 * Record PageHTML of the current active browser window
	 * 
	 * @param Config
	 *            test config instance
	 * @param destination
	 *            file to which Page HTML is to be saved
	 */
	public static void recordPageHTML(Config testConfig, File destination)
	{
		try
		{
			if (testConfig.driver != null)
			{
				String pageHTML;
				Boolean remoteExecution = (testConfig.getRunTimeProperty("RemoteExecution").toLowerCase().equals("true")) ? true : false;
				
				if (remoteExecution)
				{
					WebDriver augumentedDriver = new Augmenter().augment(testConfig.driver);
					pageHTML = (augumentedDriver).getPageSource();
				}
				else
				{
					pageHTML = testConfig.driver.getPageSource();
				}
				
				try
				{
					FileUtils.writeStringToFile(destination, pageHTML);
				}
				catch (IOException e)
				{
					// We are not using testConfig.logException(e) to print
					// exception here
					// testConfig.logException(e) creates a infinite loop and
					// breaks all test cases
					e.printStackTrace();
				}
				
				testConfig.logComment("<B>Page HTML</B>:- <a href=" + destination.getName() + " target='_blank' >" + destination.getName() + "</a>");
			}
		}
		catch (Exception e)
		{
			testConfig.logWarning("Unable to record Page HTML:- " + ExceptionUtils.getFullStackTrace(e));
			testConfig.recordPageHTMLOnFailure = false;
			throw e;
		}
	}
	
	public static void setCookieValue(Config testConfig, String cookieName, String cookieValue, String cookieDomain)
	{
		if(!testConfig.getRunTimeProperty("RemoteExecution").toLowerCase().equals("true"))
		{
			if (testConfig.driver != null)
			{
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, 1);
				Date tomorrow = cal.getTime();

				Cookie cookie = new Cookie(cookieName, cookieValue, cookieDomain, "/", tomorrow);
				testConfig.driver.manage().addCookie(cookie);
				if (testConfig.debugMode)
					testConfig.logComment("Added the cookie - Name: '" + cookieName + "' Value: '" + cookieValue + "' Domain: '" + cookieDomain + "' Expiry: '" + tomorrow.toString() + "'");
			}
		}
	}
	
	/**
	 * Switch the driver to the specified window
	 * 
	 * @param Config
	 *            test config instance
	 * @param windowHandle
	 *            Name of the window to be switched to
	 */
	public static void switchToGivenWindow(Config testConfig, String windowHandle)
	{
		if (testConfig.driver != null)
		{
			testConfig.logComment("Switching to the given window handle:- " + windowHandle);
			testConfig.driver.switchTo().window(windowHandle);
			testConfig.logComment("Switched to window with URL:- " + testConfig.driver.getCurrentUrl() + ". And title as :- " + testConfig.driver.getTitle());
		}
	}
	
	/**
	 * Switch the driver to the new window
	 * 
	 * @param Config
	 *            test config instance
	 * @return window handle of the old window, so that it can be switched back
	 *         later
	 */
	public static String switchToNewWindow(Config testConfig)
	{
		if (testConfig.driver != null)
		{
			testConfig.logComment("Switching to the new window");
			String oldWindow = testConfig.driver.getWindowHandle();
			
			if (testConfig.driver.getWindowHandles().size() < 2)
			{
				testConfig.logFail("No new window appeared, windows count available :-" + testConfig.driver.getWindowHandles().size());
			}
			
			for (String winHandle : testConfig.driver.getWindowHandles())
			{
				if (!winHandle.equals(oldWindow))
				{
					testConfig.driver.switchTo().window(winHandle);
					testConfig.logComment("Switched to window with URL:- " + testConfig.driver.getCurrentUrl() + ". And title as :- " + testConfig.driver.getTitle());
				}
			}
			
			return oldWindow;
		}
		return null;
	}
	
	/**
	 * Takes the screenshot of the current active browser window
	 * 
	 * @param Config
	 *            test config instance
	 * @param destination
	 *            file to which screenshot is to be saved
	 */
	public static void takeScreenShoot(Config testConfig, File destination)
	{
		try
		{
			if (testConfig.driver != null)
			{
				byte[] screenshot = null;
				
				try
				{
					screenshot = captureScreenshot(testConfig);
				}
				catch (NullPointerException ne)
				{
					testConfig.logWarning("NullPointerException:Screenshot can't be taken. Probably browser is not reachable");
					//test case will end, setting this as null will prevent taking screenshot again in cleanup
					testConfig.driver = null;
				}
				
				if (screenshot != null)
				{
					try
					{
						FileUtils.writeByteArrayToFile(destination, screenshot);
						
						float compressionQuality = (float) 0.5;
						try
						{
							compressionQuality = Float.parseFloat(testConfig.getRunTimeProperty("ScreenshotCompressionQuality"));
						}
						catch (Exception e)
						{
							// We are not using testConfig.logException(e) to print
							// exception here
							// testConfig.logException(e) creates a infinite loop
							// and breaks all test cases
							e.printStackTrace();
						}
						compressJpegFile(destination, destination, compressionQuality);
					}
					catch (IOException e)
					{
						// We are not using testConfig.logException(e) to print
						// exception here
						// testConfig.logException(e) creates a infinite loop and
						// breaks all test cases
						e.printStackTrace();
					}
				}
				/*if (testConfig.driver != null)
				{
					if (testConfig.getRunTimeProperty("APKUrl") == null)
					{
						testConfig.logComment("<B>Page URL</B>:- " + testConfig.driver.getCurrentUrl());
					}
				}*/
				if (testConfig.getRunTimeProperty("APKUrl") == null && testConfig.getRunTimeProperty("ipa") == null)
				{
					if(testConfig.driver !=null)
						testConfig.logComment("<B>Page URL</B>:- " + testConfig.driver.getCurrentUrl());
					else 
						testConfig.logComment("Driver is NULL");
				}
				String href = getResultsURLOnRunTime(testConfig, destination.getPath());
				testConfig.logComment("<B>Screenshot</B>:- <a href=" + href + " target='_blank' >" + destination.getName() + "</a>");
			}
		}
		catch (UnreachableBrowserException e)
		{
			testConfig.enableScreenshot = false;
			testConfig.logWarning("Unable to take screenshot:- " + ExceptionUtils.getStackTrace(e));
		}
		catch (Exception e)
		{
			testConfig.enableScreenshot = false;
			testConfig.logWarning("Unable to take screenshot:- " + ExceptionUtils.getStackTrace(e));
			throw e;
		}
	}
	
	public static void uploadFileWithJS(Config testConfig, String strJSLocater, String strFilePath, WebElement element)
	{
		JavascriptExecutor js = (JavascriptExecutor) testConfig.driver;
		js.executeScript(strJSLocater + ".style.display = \"block\";");
		js.executeScript(strJSLocater + ".style.visibility = 'visible';");
		js.executeScript(strJSLocater + ".style.opacity = 1;");
		//js.executeScript(strJSLocater + ".style.width = '1px';");
		//js.executeScript(strJSLocater + ".style.height = '1px';");
		element.sendKeys(strFilePath);
	}
	
	/**
	 * Verify Intermediate Page URL (which gets auto-redirected) like processing
	 * page
	 * 
	 * @param Config
	 *            test config instance
	 * @param expectedURL
	 * @return true if actual URL contains the expected URL
	 */
	public static void verifyIntermediateURL(Config testConfig, String expectedURL)
	{
		try
		{
			int retries = 3;
			String actualURL = testConfig.driver.getCurrentUrl().toLowerCase();
			expectedURL = expectedURL.toLowerCase();
			
			while (retries > 0)
			{
				if (actualURL.contains(expectedURL))
				{
					testConfig.logPass("Browser URL", actualURL);
					return;
				}
				// Browser.wait(testConfig, 1);
				actualURL = testConfig.driver.getCurrentUrl().toLowerCase();
				retries--;
			}
			testConfig.logFail("Browser URL", expectedURL, actualURL);
		}
		catch (UnreachableBrowserException e)
		{
			// testConfig.endExecutionOnfailure = true;
			testConfig.logException(e);
		}
	}
	
	/**
	 * Verify Page URL
	 * 
	 * @param Config
	 *            test config instance
	 * @param expectedURL
	 * @return true if actual URL contains the expected URL
	 */
	public static boolean verifyURL(Config testConfig, String expectedURL)
	{
		try
		{
			int retries = 30;
			String actualURL = testConfig.driver.getCurrentUrl().toLowerCase();
			expectedURL = expectedURL.toLowerCase();
			
			while (retries > 0)
			{
				if (actualURL.contains(expectedURL))
				{
					testConfig.logPass("Browser URL", actualURL);
					
					// Verify that page stays on same page (no internal
					// redirect)
					Browser.wait(testConfig, 5);
					actualURL = testConfig.driver.getCurrentUrl().toLowerCase();
					if (!actualURL.contains(expectedURL))
					{
						testConfig.logFail("Browser URL", expectedURL, actualURL);
						return false;
					}
					
					return true;
				}
				Browser.wait(testConfig, 1);
				actualURL = testConfig.driver.getCurrentUrl().toLowerCase();
				retries--;
			}
			testConfig.logFail("Browser URL", expectedURL, actualURL);
			return false;
		}
		catch (UnreachableBrowserException e)
		{
			// testConfig.endExecutionOnfailure = true;
			testConfig.logException(e);
			return false;
		}
	}
	
	/**
	 * Pause the execution for given seconds
	 * 
	 * @param seconds
	 */
	public static void wait(Config testConfig, int seconds)
	{
		int milliseconds = seconds * 1000;
		try
		{
			Thread.sleep(milliseconds);
			testConfig.logComment("Wait for '" + seconds + "' seconds");
			
		}
		catch (InterruptedException e)
		{
			
		}
	}
	
	/**
	 * Method use to wait until given url visible
	 * @param url
	 * @param maxTimeToWaitInSec
	 */
	public static void waitForUrlToDisplay(Config testConfig, String url,int maxTimeToWaitInSec)
	{
		int count = 0;
		while(!testConfig.driver.getCurrentUrl().equals(url) && count < maxTimeToWaitInSec)
		{
			count +=1;
			Browser.wait(testConfig,1);
		}
	}
	
	/**
	 * Waits for the given WebElement to appear on the specified browser
	 * instance
	 * 
	 * @param Config
	 *            test config instance
	 * @param element
	 *            element to be searched
	 */
	public static void waitForPageLoad(Config testConfig, WebElement element)
	{
		waitForPageLoad(testConfig, element, testConfig.getRunTimeProperty("ObjectWaitTime"));
	}
	
	/**
	 * Waits for the given WebElement to appear on the specified browser
	 * instance
	 * 
	 * @param Config
	 *            test config instance
	 * @param element
	 *            element to be searched
	 * @param ObjectWaitTime
	 *            - max time to wait for the object
	 */
	public static void waitForPageLoad(Config testConfig, WebElement element, String objectWaitTime)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date startDate = new Date();
		double timeTaken = 0;
		WebDriverWait wait = null;
		// Increase the timeout value
		Long ObjectWaitTime = Long.parseLong(objectWaitTime);
		String callingClassName = getCallerClassName();
		String currentPageName = callingClassName.substring(callingClassName.lastIndexOf('.') + 1);
		testConfig.logComment("Started waiting for '" + currentPageName + "' to load at:- " + dateFormat.format(startDate) + ". Wait upto " + ObjectWaitTime + " seconds.");
		
		wait = new WebDriverWait(testConfig.driver, ObjectWaitTime);
		//testConfig.driver.manage().timeouts().pageLoadTimeout(ObjectWaitTime, TimeUnit.SECONDS);
		
		// We should not use implicit and explicit wait together, so resetting
		// the implicit wait prior to using explicit wait
		testConfig.driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		
		try
		{
			//Browser.wait(testConfig, 2);
			wait.until(ExpectedConditions.visibilityOf(element));
		}
		catch (StaleElementReferenceException e)
		{
			testConfig.logWarning("StaleElementReferenceException occured, wait upto additional " + ObjectWaitTime + " seconds.");
			
			try
			{
				wait.until(ExpectedConditions.visibilityOf(element));
			}
			catch (Exception exc)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + (endDate.getTime() - startDate.getTime()) / 1000.00 + " seconds. Exiting...");
				throw exc;
			}
		}
		catch (TimeoutException e)
		{
			// adding to have extra stability, specifically for test response
			// page load
			testConfig.logWarning("'" + currentPageName + "' still not loaded, so wait upto additional " + ObjectWaitTime + " seconds.");
			try
			{
				wait.until(ExpectedConditions.visibilityOf(element));
			}
			catch (TimeoutException tm)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + timeTaken + " seconds. Exiting...");
				throw new TimeoutException("'" + currentPageName + "' did not load after waiting for " + 2 * ObjectWaitTime + " seconds");//approximate time
			}
			catch (Exception ee)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + timeTaken + " seconds. Exiting...");
				throw ee;
			}
		}
		
		catch(WebDriverException webDriverException)
		{
    		testConfig.logComment("\nWebDriverException or InterruptedException appeared, So trying again...");
			Thread.interrupted();
			
			for (int i = 1; i <= 5; i++)
			{
	            try 
	            {
	                wait.until(ExpectedConditions.visibilityOf(element));
	            } 
	            catch (Throwable exception)
	            {
	            	if(exception.getClass().toString().contains("InterruptedException"))
	            	{
	            		testConfig.logComment("InterruptedException appeared "+(i+1)+" times, So trying again...");
	            		Thread.interrupted();
	            		testConfig.logComment("***********************************************");
	                	testConfig.logComment(ExceptionUtils.getFullStackTrace(webDriverException));
	                	testConfig.logComment("***********************************************");
	            	}
	            	else if(exception.getClass().toString().contains("NoSuchElementException"))
	            	{
	            		testConfig.endExecutionOnfailure = true;
        				Date endDate = new Date();
        				timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
        				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + timeTaken + " seconds. Exiting...");
        				throw exception;
	            	}
	            	else
	            	{
	            		testConfig.logComment("\n<-----Exception in waitForPageLoad()----->");
	            		//testConfig.logComment(ExceptionUtils.getFullStackTrace(exception));
	            		throw exception;
	            	}
	            }
			}
		}
		
		// Reset to the default value in config (in case the method was called
		// with some other value like ExtendedObjectWaitTime)
		ObjectWaitTime = Long.parseLong(testConfig.getRunTimeProperty("ObjectWaitTime"));
		testConfig.driver.manage().timeouts().implicitlyWait(ObjectWaitTime, TimeUnit.SECONDS);

		Date endDate = new Date();
		double timeTaken1 = 0;
		double timeTaken2 = 0;
		timeTaken1 = (endDate.getTime() - startDate.getTime()) / 1000.00;
		
		if(testConfig.getRunTimeProperty("waitForLoader")!=null && testConfig.getRunTimeProperty("waitForLoader").equalsIgnoreCase("true"))
		{
			testConfig.putRunTimeProperty("callingFromPageLoad", "true");
			timeTaken2 = waitForHTMLToNotHaveClassBusy(testConfig);
		}
		
		timeTaken = timeTaken1 + timeTaken2;
		testConfig.logComment(currentPageName + " with Loader loaded in :- " + timeTaken + " seconds.");
		
		if(timeTaken > 120)
			testConfig.logComment("<B><font color='Red'>" + currentPageName + " is loaded after " + timeTaken/60 + " minutes.</font></B>");
	}
	
	private static void updatePerformaceDB(Config testConfig, String callingClassName, String currentPageName, double timeTaken)
	{
		//Code to record Page Load Time in db for performance related analysis
		if (testConfig.getRunTimeProperty("RecordPageLoadTime").equalsIgnoreCase("true"))
		{
			String prevPageName = testConfig.previousPage;
			testConfig.previousPage = currentPageName;
			String machineIP = "";

			try
			{
				InetAddress ip;
				ip = InetAddress.getLocalHost();
				machineIP = (ip.getHostAddress()).toString();
			}
			catch (UnknownHostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
			testConfig.putRunTimeProperty("currentPage_RunTime", currentPageName);
			testConfig.putRunTimeProperty("timeTaken_RunTime", timeTaken);
			testConfig.putRunTimeProperty("previousPage_RunTime", prevPageName);
			testConfig.putRunTimeProperty("testcaseName_RunTime", testConfig.getTestName());
			testConfig.putRunTimeProperty("machineIP_RunTime", machineIP);
					
			String testDataSheetName = testConfig.getRunTimeProperty("TestDataSheet");
			String path = "";
			int sqlRowNum = 0;
			if (callingClassName.contains("MoneyPageObject"))
			{
				path = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").lastIndexOf("\\") + 1) + "Money//Parameters//MoneyTestData.xls";
				sqlRowNum = 182;
			}
			else
			{
				path = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").lastIndexOf("\\") + 1) + "Product//Parameters//ProductTestData.xls";
				sqlRowNum = 231;
			}
			testConfig.putRunTimeProperty("TestDataSheet", path);
			DataBase.executeUpdateQuery(testConfig, sqlRowNum, DatabaseType.PerformanceDB);
			testConfig.putRunTimeProperty("TestDataSheet", testDataSheetName);
		}
	}
	
	/**
	 * Waits and accepts POP Up
	 * 
	 * @param testConfig
	 * @param pollTime
	 *            - Intervals in which Browser should be polled for alert
	 */
	public static void waitForPopUp(Config testConfig, int pollTime)
	{
		
		// Time to poll for every 5 seconds whether popup is present or not
		int threshold = 5;
		
		for (int i = 0; i < pollTime; i++)
		{
			
			// Time to poll for every 5 seconds whether popup is present or not
			if (Popup.isAlertPresent(testConfig))
			{
				Popup.ok(testConfig);
				testConfig.logComment("Alert closed successfully");
				break;
			}
			
			Browser.wait(testConfig, threshold);
		}
	}
	
	/**
	 * Wait until HTML tag do not have Busy class
	 * @param testConfig
	 */
	public static double waitForHTMLToNotHaveClassBusy(Config testConfig)
	{
		return waitForElementToLoadUntilNotHaveGivenPropertyValue(testConfig, How.css, "html", "class", "nprogress-busy");
	}
	
	/**
	 * Wait until element do not have given property value
	 * @param testConfig
	 * @param how
	 * @param what
	 * @param propertyName
	 * @param propertyValue
	 */
	public static double waitForElementToLoadUntilNotHaveGivenPropertyValue(Config testConfig, How how, String what, String propertyName, String propertyValue)
	{
		testConfig.putRunTimeProperty("disableGetPageElementLogs", "true");
		Date startDate = new Date();
		double timeTaken = 0;
		// Increase the timeout value
		Long ObjectWaitTime = Long.parseLong(testConfig.getRunTimeProperty("ObjectWaitTime"));
		String callingClassName = getCallerClassName();
		String currentPageName = callingClassName.substring(callingClassName.lastIndexOf('.') + 1);
		String callingFrom = testConfig.getRunTimeProperty("callingFromPageLoad");
		if(callingFrom==null || callingFrom.equals("false"))
		{
			testConfig.logComment("Started waiting for Not to have '" + propertyValue + "' in '" + propertyName + "' at :- " + startDate + ". Wait upto " + ObjectWaitTime + " seconds.");
			Browser.wait(testConfig, 1);
		}
		else
		{
			//testConfig.logComment("Inside Class Loader...");
		}

		FluentWait<WebDriver> wait = new FluentWait<WebDriver>(testConfig.driver);
		wait.withTimeout(ObjectWaitTime, TimeUnit.SECONDS);
		wait.pollingEvery(1, TimeUnit.SECONDS);
		try
		{
			wait.until(new Function<WebDriver, Boolean>()
			{
				public Boolean apply(WebDriver webDriver)
				{
					String className = null;
					try
					{
						className = Element.getPageElement(testConfig, how, what).getAttribute(propertyName);
					}
					catch(StaleElementReferenceException se)
					{
						className = Element.getPageElement(testConfig, how, what).getAttribute(propertyName);
					}
					
					if(className!=null && !className.contains(propertyValue))
						return true;
					else
						return false;
				}
			});

			Date endDate = new Date();
			timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
			//testConfig.logComment(currentPageName + " Loader loaded in :- " + timeTaken + " seconds.");
		}
		catch(TimeoutException e)
		{
			// adding to have extra stability, specifically for test response page load
			testConfig.logWarning(currentPageName + " Loader still not loaded, so wait upto additional " + ObjectWaitTime + " seconds.");
			try
			{
				wait.until(new Function<WebDriver, Boolean>()
				{
					public Boolean apply(WebDriver webDriver)
					{
						String className = Element.getPageElement(testConfig, how, what).getAttribute(propertyName);
						if(className!=null && !className.contains(propertyValue))
							return true;
						else
							return false;
					}
				});

				Date endDate = new Date();
				timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
				//testConfig.logComment(currentPageName + " Loader loaded in :- " + timeTaken + " seconds.");
			}
			catch (TimeoutException tm)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				testConfig.logWarning(currentPageName + " Loader NOT loaded even after :- " + (endDate.getTime() - startDate.getTime()) / 1000.00 + " seconds. Exiting...");
				throw new TimeoutException(currentPageName + " Loader did not load after waiting for " + 2 * ObjectWaitTime + " seconds");//approximate time
			}
			catch (Exception ee)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				testConfig.logWarning(currentPageName + " Loader NOT loaded even after :- " + (endDate.getTime() - startDate.getTime()) / 1000.00 + " seconds. Exiting...");
				throw ee;
			}
		}
		catch(StaleElementReferenceException se)
		{
			testConfig.logWarning("Stale Element exception occured on " + currentPageName);
		}
		catch(WebDriverException webDriverException)
		{
    		testConfig.logComment("\nWebDriverException or InterruptedException appeared, So trying again...(inside waiting for loader)");
			Thread.interrupted();
			
			for (int i = 1; i <= 5; i++)
			{
	            try 
	            {
	            	wait.until(new Function<WebDriver, Boolean>()
	        				{
	        					public Boolean apply(WebDriver webDriver)
	        					{
	        						String className = Element.getPageElement(testConfig, how, what).getAttribute(propertyName);
	        						if(className!=null && !className.contains(propertyValue))
	        							return true;
	        						else
	        							return false;
	        					}
	        				});
	            } 
	            catch (Throwable exception)
	            {
	            	if(exception.getClass().toString().contains("InterruptedException"))
	            	{
	            		testConfig.logComment("InterruptedException appeared "+(i+1)+" times, So trying again...(inside waiting for loader)");
	            		Thread.interrupted();
	            		testConfig.logComment("***********************************************");
	                	testConfig.logComment(ExceptionUtils.getFullStackTrace(webDriverException));
	                	testConfig.logComment("***********************************************");
	            	}
	            	else if(exception.getClass().toString().contains("NoSuchElementException"))
	            	{
	            		testConfig.endExecutionOnfailure = true;
        				Date endDate = new Date();
        				timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
        				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + timeTaken + " seconds. Exiting...");
        				throw exception;
	            	}
	            	else
	            	{
	            		testConfig.logComment("\n<-----Exception in waitForElementToLoadUntilNotHaveGivenPropertyValue()----->");
	            		//testConfig.logComment(ExceptionUtils.getFullStackTrace(exception));
	            		throw exception;
	            	}
	            }
			}
		}
		
		if(callingFrom==null || callingFrom.equals("false"))
		{
			testConfig.logComment("'" + propertyValue + "' from '" + propertyName + "' removed in :- " + timeTaken + " seconds.");
			Browser.wait(testConfig, 1);
		}
		else
		{
			//testConfig.logComment("Exiting Class Loader...");
		}
				
		testConfig.putRunTimeProperty("callingFromPageLoad", "false");
		testConfig.putRunTimeProperty("disableGetPageElementLogs", "false");
		return timeTaken;
	}
	
	/**
	 * Wait until element have given property value
	 * @param testConfig
	 * @param how
	 * @param what
	 * @param propertyName
	 * @param propertyValue
	 */
	public static void waitForElementToLoadWithGivenPropertyValue(Config testConfig, How how, String what, String propertyName, String propertyValue)
	{
		Browser.wait(testConfig, 1);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date startDate = new Date();

		// Increase the timeout value
		Long ObjectWaitTime = Long.parseLong(testConfig.getRunTimeProperty("ObjectWaitTime"));
		String callingClassName = getCallerClassName();
		String currentPageName = callingClassName.substring(callingClassName.lastIndexOf('.') + 1);
		testConfig.logComment("Started waiting for '" + currentPageName + "' to load at:- " + dateFormat.format(startDate) + ". Wait upto " + ObjectWaitTime + " seconds.");

		FluentWait<WebDriver> wait = new FluentWait<WebDriver>(testConfig.driver);
		wait.withTimeout(ObjectWaitTime, TimeUnit.SECONDS);
		wait.pollingEvery(1, TimeUnit.SECONDS);
		try
		{
			wait.until(new Function<WebDriver, Boolean>()
			{
				public Boolean apply(WebDriver webDriver)
				{
					String className = Element.getPageElement(testConfig, how, what).getAttribute(propertyName);
					if(className!=null && className.equals(propertyValue))
						return true;
					else
						return false;
				}
			});

			Date endDate = new Date();
			double timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
			testConfig.logComment("'" + currentPageName + "' loaded in :- " + timeTaken + " seconds.");
		}
		catch(TimeoutException e)
		{
			// adding to have extra stability, specifically for test response
			// page load
			testConfig.logWarning("'" + currentPageName + "' still not loaded, so wait upto additional " + ObjectWaitTime + " seconds.");
			try
			{
				wait.until(new Function<WebDriver, Boolean>()
				{
					public Boolean apply(WebDriver webDriver)
					{
						String className = Element.getPageElement(testConfig, how, what).getAttribute(propertyName);
						if(className!=null && className.equals(propertyValue))
							return true;
						else
							return false;
					}
				});

				Date endDate = new Date();
				double timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
				testConfig.logComment("'" + currentPageName + "' loaded in :- " + timeTaken + " seconds.");
			}
			catch (TimeoutException tm)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + (endDate.getTime() - startDate.getTime()) / 1000.00 + " seconds. Exiting...");
				throw new TimeoutException("'" + currentPageName + "' did not load after waiting for " + 2 * ObjectWaitTime + " seconds");//approximate time
			}
			catch (Exception ee)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + (endDate.getTime() - startDate.getTime()) / 1000.00 + " seconds. Exiting...");
				throw ee;
			}
		}
	}
	
	private static FirefoxProfile setFireFoxProfile(Config testConfig, FirefoxProfile firefoxProfile)
	{
		if (testConfig.isMobile)
		{
			firefoxProfile.setPreference("general.useragent.override", testConfig.getRunTimeProperty("MobileUAString"));
		}
		
		addNetExportAddOnInFirefox(testConfig, firefoxProfile);
		
		firefoxProfile.setPreference("dom.max_chrome_script_run_time", 0);
		firefoxProfile.setPreference("dom.max_script_run_time", 0);
		
		firefoxProfile.setPreference("browser.download.folderList", 2);
		firefoxProfile.setPreference("browser.download.dir", testConfig.downloadPath);
						
		// automatically download excel files
		firefoxProfile.setPreference("browser.helperApps.alwaysAsk.force", false);
		firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/x-msdos-program, application/x-unknown-application-octet-stream, application/vnd.ms-powerpoint, application/excel, application/vnd.ms-publisher, application/x-unknown-message-rfc822, application/vnd.ms-excel, application/msword, application/x-mspublisher, application/x-tar, application/zip, application/x-gzip,application/x-stuffit,application/vnd.ms-works, application/powerpoint, application/rtf, application/postscript, application/x-gtar, video/quicktime, video/x-msvideo, video/mpeg, audio/x-wav, audio/x-midi, audio/x-aiff, text/html, application/octet-stream");
		firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
		firefoxProfile.setEnableNativeEvents(false);
		
		//Turning Auto Update OFF for Firefox.
		firefoxProfile.setPreference("app.update.enabled", false);
		firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/x-msdos-program, application/x-unknown-application-octet-stream, application/vnd.ms-powerpoint, application/excel, application/vnd.ms-publisher, application/x-unknown-message-rfc822, application/vnd.ms-excel, application/msword, application/x-mspublisher, application/x-tar, application/zip, application/x-gzip,application/x-stuffit,application/vnd.ms-works, application/powerpoint, application/rtf, application/postscript, application/x-gtar, video/quicktime, video/x-msvideo, video/mpeg, audio/x-wav, audio/x-midi, audio/x-aiff, text/html, application/octet-stream");
		firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
		
		//Always allow share location
		firefoxProfile.setPreference("geo.prompt.testing", true);
		firefoxProfile.setPreference("geo.prompt.testing.allow", true);
		
		//Do not open pdf files and automatically download them
		String downloadPdf = testConfig.getRunTimeProperty("downloadPdf");
		if(downloadPdf!=null && Boolean.parseBoolean(downloadPdf)) {
			firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk","application/pdf");
			firefoxProfile.setPreference("plugin.disable_full_page_plugin_for_types","application/pdf");
			firefoxProfile.setPreference("pdfjs.disabled", true);
		}

		return firefoxProfile;		
	}
	
	/**
	 * This Method is used to set temp file Location for a Webdriver Instance.
	 * 
	 * @param testConfig
	 * @param path
	 */
	private static void setTempProfile(Config testConfig, String path)
	{
		try
		{
			String tempFolderPathForTestCase = path + File.separator + testConfig.getRunTimeProperty("BuildId") + File.separator + testConfig.getTestName();
			
			if(!new File(tempFolderPathForTestCase).isDirectory())
			{
				FileUtils.forceMkdir(new File(tempFolderPathForTestCase));
			}
			else
			{
				testConfig.logComment("Folder - " + tempFolderPathForTestCase + " already exist hence not creating a new one");
			}
			TemporaryFilesystem.setTemporaryDirectory(new File(tempFolderPathForTestCase));
			testConfig.logComment("Path - " + tempFolderPathForTestCase + " set as temp folder for this Test Case");
		}
		catch(Exception e)
		{
			testConfig.logComment("Temp file folder creation fail -- default Temp location will be used for this Test Case now");
		}
	}

	public static void waitForPageLoad(Config testConfig, WebElement element, Object pageName)
	{
		String objectWaitTime = testConfig.getRunTimeProperty("ObjectWaitTime");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date startDate = new Date();
		double timeTaken = 0;
		WebDriverWait wait = null;
		// Increase the timeout value
		Long ObjectWaitTime = Long.parseLong(objectWaitTime);
		String callingClassName = getCallerClassName();
		String currentPageName = callingClassName.substring(callingClassName.lastIndexOf('.') + 1);
		testConfig.logComment("Started waiting for '" + currentPageName + "' to load at:- " + dateFormat.format(startDate) + ". Wait upto " + ObjectWaitTime + " seconds.");
		
		wait = new WebDriverWait(testConfig.driver, ObjectWaitTime);
		//testConfig.driver.manage().timeouts().pageLoadTimeout(ObjectWaitTime, TimeUnit.SECONDS);
		
		// We should not use implicit and explicit wait together, so resetting
		// the implicit wait prior to using explicit wait
		testConfig.driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		try
		{
			wait.until(ExpectedConditions.visibilityOf(element));
		}
		catch (StaleElementReferenceException e)
		{
			testConfig.logWarning("StaleElementReferenceException occured, wait upto additional " + ObjectWaitTime + " seconds.");
			
			try
			{
				PageFactory.initElements(testConfig.driver, pageName);
				wait.until(ExpectedConditions.visibilityOf(element));
			}
			catch (Exception exc)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + (endDate.getTime() - startDate.getTime()) / 1000.00 + " seconds. Exiting...");
				throw exc;
			}
		}
		catch (TimeoutException e)
		{
			// adding to have extra stability, specifically for test response
			// page load
			testConfig.logWarning("'" + currentPageName + "' still not loaded, so wait upto additional " + ObjectWaitTime + " seconds.");
			try
			{
				wait.until(ExpectedConditions.visibilityOf(element));
			}
			catch (TimeoutException tm)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + timeTaken + " seconds. Exiting...");
				throw new TimeoutException("'" + currentPageName + "' did not load after waiting for " + 2 * ObjectWaitTime + " seconds");//approximate time
			}
			catch (Exception ee)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + timeTaken + " seconds. Exiting...");
				throw ee;
			}
		}
		catch(WebDriverException webDriverException)
		{
    		testConfig.logComment("\nWebDriverException or InterruptedException appeared, So trying again...");
			Thread.interrupted();
			
			for (int i = 1; i <= 5; i++)
			{
	            try 
	            {
	                wait.until(ExpectedConditions.visibilityOf(element));
	            } 
	            catch (Throwable exception)
	            {
	            	if(exception.getClass().toString().contains("InterruptedException"))
	            	{
	            		testConfig.logComment("InterruptedException appeared "+(i+1)+" times, So trying again...");
	            		Thread.interrupted();
	            		testConfig.logComment("***********************************************");
	                	testConfig.logComment(ExceptionUtils.getFullStackTrace(webDriverException));
	                	testConfig.logComment("***********************************************");
	            	}
	            	else if(exception.getClass().toString().contains("NoSuchElementException"))
	            	{
	            		testConfig.endExecutionOnfailure = true;
        				Date endDate = new Date();
        				timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
        				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + timeTaken + " seconds. Exiting...");
        				throw exception;
	            	}
	            	else
	            	{
	                	testConfig.logComment("\n<-----Exception in waitForPageLoad()----->");
	            		//testConfig.logComment(ExceptionUtils.getFullStackTrace(exception));
	            		throw exception;
	            	}
	            }
			}
		}
		
		// Reset to the default value in config (in case the method was called
		// with some other value like ExtendedObjectWaitTime)
		ObjectWaitTime = Long.parseLong(testConfig.getRunTimeProperty("ObjectWaitTime"));
		testConfig.driver.manage().timeouts().implicitlyWait(ObjectWaitTime, TimeUnit.SECONDS);

		Date endDate = new Date();
		double timeTaken1 = 0;
		double timeTaken2 = 0;
		timeTaken1 = (endDate.getTime() - startDate.getTime()) / 1000.00;
		
		if(testConfig.getRunTimeProperty("waitForLoader")!=null && testConfig.getRunTimeProperty("waitForLoader").equalsIgnoreCase("true"))
		{
			testConfig.putRunTimeProperty("callingFromPageLoad", "true");
			timeTaken2 = waitForHTMLToNotHaveClassBusy(testConfig);
		}
		
		timeTaken = timeTaken1 + timeTaken2;
		testConfig.logComment(currentPageName + " with Loader loaded in :- " + timeTaken + " seconds.");
		
		if(timeTaken > 120)
			testConfig.logComment("<B><font color='Red'>" + currentPageName + " is loaded after " + timeTaken/60 + " minutes.</font></B>");
	}
	
	/**
	 * This function return the URL of a file on runtime depending on LOCAL or OFFICIAL Run
	 * @param testConfig
	 * @param fileURL
	 * @return
	 */
	public static String getResultsURLOnRunTime(Config testConfig, String fileURL)
	{
		String resultsIP = "";
		if (Config.RunType.equalsIgnoreCase("official"))
			resultsIP = "http://" + testConfig.getRunTimeProperty("RemoteAddress");
		
		if (fileURL.contains("RegressionResults"))
			fileURL = fileURL.split("RegressionResults")[1];
		
		return resultsIP + fileURL;
	}
	
	/**
	 * Wait For Element To Load With Given CSSPropertyValue
	 * @param testConfig
	 * @param how
	 * @param what
	 * @param cssPropertyName
	 * @param propertyValue
	 */
	public static void waitForElementToLoadWithGivenCSSPropertyValue(Config testConfig, How how, String what, String cssPropertyName, String propertyValue)
	{
		Browser.wait(testConfig, 1);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date startDate = new Date();

		// Increase the timeout value
		Long ObjectWaitTime = Long.parseLong(testConfig.getRunTimeProperty("ObjectWaitTime"));
		String callingClassName = getCallerClassName();
		String currentPageName = callingClassName.substring(callingClassName.lastIndexOf('.') + 1);
		testConfig.logComment("Started waiting for '" + currentPageName + "' to load at:- " + dateFormat.format(startDate) + ". Wait upto " + ObjectWaitTime + " seconds.");

		FluentWait<WebDriver> wait = new FluentWait<WebDriver>(testConfig.driver);
		wait.withTimeout(ObjectWaitTime, TimeUnit.SECONDS);
		wait.pollingEvery(1, TimeUnit.SECONDS);
		try
		{
			wait.until(new Function<WebDriver, Boolean>()
			{
				public Boolean apply(WebDriver webDriver)
				{
					String actualPropertyValue = Element.getPageElement(testConfig, how, what).getCssValue(cssPropertyName);
					if(actualPropertyValue!=null && actualPropertyValue.equals(propertyValue))
						return true;
					else
						return false;
				}
			});

			Date endDate = new Date();
			double timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
			testConfig.logComment("'" + currentPageName + "' loaded in :- " + timeTaken + " seconds.");
		}
		catch(TimeoutException e)
		{
			testConfig.logWarning("'" + currentPageName + "' still not loaded, so wait upto additional " + ObjectWaitTime + " seconds.");
			try
			{
				wait.until(new Function<WebDriver, Boolean>()
				{
					public Boolean apply(WebDriver webDriver)
					{
						String actualPropertyValue = Element.getPageElement(testConfig, how, what).getCssValue(cssPropertyName);
						if(actualPropertyValue!=null && actualPropertyValue.equals(propertyValue))
							return true;
						else
							return false;
					}
				});

				Date endDate = new Date();
				double timeTaken = (endDate.getTime() - startDate.getTime()) / 1000.00;
				testConfig.logComment("'" + currentPageName + "' loaded in :- " + timeTaken + " seconds.");
			}
			catch (TimeoutException tm)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + (endDate.getTime() - startDate.getTime()) / 1000.00 + " seconds. Exiting...");
				throw new TimeoutException("'" + currentPageName + "' did not load after waiting for " + 2 * ObjectWaitTime + " seconds");//approximate time
			}
			catch (Exception ee)
			{
				testConfig.endExecutionOnfailure = true;
				Date endDate = new Date();
				testConfig.logWarning("'" + currentPageName + "' NOT loaded even after :- " + (endDate.getTime() - startDate.getTime()) / 1000.00 + " seconds. Exiting...");
				throw ee;
			}
		}
	}
	/**
	 * This method will take user input as txncdn url which need to be verified and will match against the urls which 
	 * is coming while doing transaction.
	 * @param testConfig
	 * @param expectedURL
	 * @return
	 */
	public static boolean verifyIntermediatePage(Config testConfig, String expectedURL)
	{
		try
		{
			int retries = 5000;
			String actualURL = testConfig.driver.getCurrentUrl().toLowerCase();
			expectedURL = expectedURL.toLowerCase();
			
			while (retries > 0)
			{
				if (actualURL.contains(expectedURL))
				{
					testConfig.logPass("Browser URL", actualURL);
					return true;
				}
				Browser.wait(testConfig, 0.01);
				actualURL = testConfig.driver.getCurrentUrl().toLowerCase();
				retries--;
			}
			testConfig.logFail("Verifying Txncdn url", expectedURL, actualURL);
			return false;
		}
		catch (UnreachableBrowserException e)
		{
			// testConfig.endExecutionOnfailure = true;
			testConfig.logException(e);
			return false;
		}
	}
	/**
	 * overloaded method - Pause the execution for given less than one seconds
	 * 
	 * @param seconds
	 */
	public static void wait(Config testConfig, double seconds)
	{
		int milliseconds = (int) (seconds * 1000);
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e)
		{
			testConfig.logException(e);
		}
	}
}