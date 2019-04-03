
package Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.io.TemporaryFilesystem;
import org.openqa.selenium.remote.SessionId;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.asserts.SoftAssert;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.selendroid.SelendroidCapabilities;
import io.selendroid.SelendroidLauncher;
import ru.yandex.qatools.allure.annotations.Attachment;

public class Config
{
	
	public enum SheetToBeUsed
	{
		Common, Money, Product, ProductProduction, ACS, POS, MoneyRefund
	}
	
	// parameters that can be overridden through command line and are same for
	// all executing tests
	public static String BrowserName;
	public static String Environment;
	public static String MobileUAFlag;
	public static String ResultsDir;
	public static String PlatformName;
	public static String RemoteAddress;
	public static String ProjectName;
	public static String BrowserVersion;
	public static String BuildId;
	public static String MobilePlatFormName;
	public static String simulatorEnabled;
	public static String ADBPLATFORM = "adb shell getprop ro.build.version.release";
	public String ADBLISTDEVICES = "adb devices";
	public String ADBSERVERKILL = "adb kill-server";
	public String ADBSERVERSTART = "adb start-server";
	public String ADBUNINSTALLAPK = "adb uninstall ";
	public String ADBCLEARDATAAPK = "adb shell pm clear ";
    public String adminId = null;
	public String aggregatorId = null;
	public String pitUserId = null;
	public AppiumDriver<MobileElement> appiumDriver;
	protected AndroidDriver<MobileElement> androidDriver;
	protected IOSDriver<MobileElement> iosDriver;
	Process appiumServer;
	public String APPIUMSERVERSTART = "C://Appium//node.exe C://Appium//node_modules//appium//bin//appium.js";
	public Connection connection = null;
	public String customerId = null;
	public Connection DBConnection = null;
	public boolean debugMode = false;
	public static String RunType;
	public static String fileSeparator = File.separator;
	
	// parameters different for every test
	public WebDriver driver;
	public String downloadPath = null;
	public boolean enableScreenshot = true;
	public boolean endExecutionOnfailure = false;
	public boolean isMobile = false;
	
	public boolean logToStandardOut = true;
	public String merchantId = null;
	public Connection MoneyAsyncDbConnection = null;
	// Connection objects to be used in DataBase.java
	public Connection MoneyAuthDbConnection = null;
	public Connection MoneyCmsDbConnection = null;
	public Connection PayuBizWebSitedDB = null;
	
	public Connection KVaultDBConnection = null;
	public Connection MoneyUserMgmtConnection = null;
	public Connection PerformanceDBConnection = null;
	public Connection AccuracyDBConnection = null;
	public Connection MoneyVaultDbConnection = null;
	public Connection MoneyWebServiceDbConnection = null;
	public Connection MoneyAnalyticsDataDBConnection = null;
	public Connection ACSDBAutomationConnection = null;
	public Connection CredDBAutomationConnection = null;
	public Connection ACSDBConnection = null;
	public Connection CredOnBoardingDBConnection = null;
	public Connection CredAutomationDBConnection = null;
	public Connection CredCibilDBConnection = null;
	public Connection RupayDBConnection = null;
	public Connection ACSAuthDBConnection = null;
	public Connection ACSAuthDashBoardConnection = null;
	public Connection OptimusCredLMSDBConnection = null;
	public Connection OptimusCRMDBConnection = null;
	
	public String NODESERVERKILL = "taskkill /f /im node.exe";
	
	public boolean recordPageHTMLOnFailure = false;
	// stores the run time properties (different for every test)
	Properties runtimeProperties;
	
	public SelendroidCapabilities selCap;
	public SelendroidLauncher selLaunch;

	public SoftAssert softAssert;
	public static HashMap<String, TestDataReader> testDataReaderHashMap = new HashMap<String, TestDataReader>();
	public static HashMap<Integer, HashMap<String, String>> genericErrors = new HashMap<Integer, HashMap<String, String>>();
	TestDataReader testDataReaderObj;
	public boolean remoteExecution;


	String testEndTime;
	
	public String testLog;
	
	public Method testMethod;
	String testName;
	
	public boolean testResult;
	
	// package fields
	String testStartTime;
	
	public String previousPage="";
	public SessionId session=null;
	
	public Config(Method method)
	{
		try
		{
			endExecutionOnfailure = true;
			
			this.testMethod = method;
			this.testResult = true;
			this.connection = null;
			this.testLog = "";
			this.softAssert = new SoftAssert();
			// Read the Config file
			Properties property = new Properties();
			
			String path = System.getProperty("user.dir") + fileSeparator + "Parameters" + fileSeparator + "Config.properties";
			
			if (debugMode)
				logComment("Read the configuration file:-" + path);
			FileInputStream fn = new FileInputStream(path);
			property.load(fn);
			fn.close();
			
			// override the environment value if passed through ant command line
			if (!(Environment == null || Environment.isEmpty()))
				property.put("Environment", Environment.toLowerCase());
			
			// override environment if declared in @TestVariables
			TestVariables testannotation = null;
			if(method != null)
			{
				testannotation = ObjectUtils.firstNonNull(method.getAnnotation(TestVariables.class), method.getDeclaringClass().getAnnotation(TestVariables.class));
				if (testannotation != null)
				{
					String environment = testannotation.environment();
					if(!environment.isEmpty())
					{
						logComment("Running on " + environment.toLowerCase() + " environment");
						property.put("Environment", environment.toLowerCase());
					}

					String applicationName = testannotation.applicationName();
					if (!applicationName.isEmpty())
					{
						logComment("Loading settings for application " + applicationName);

						path = System.getProperty("user.dir") + fileSeparator + "Parameters" + fileSeparator + applicationName + ".properties";
						logComment("Read the environment file:- " + path);

						fn = new FileInputStream(path);
						property.load(fn);
						fn.close();
					}
				}
			}
			
			path = System.getProperty("user.dir") + fileSeparator + "Parameters" + fileSeparator + property.get("Environment") + ".properties";
			logComment("Read the environment file:- " + path);
			
			fn = new FileInputStream(path);
			property.load(fn);
			fn.close();
						
			this.runtimeProperties = new Properties();
			Enumeration<Object> em = property.keys();
			while (em.hasMoreElements())
			{
				String str = (String) em.nextElement();
				putRunTimeProperty(str, (String) property.get(str));
			}
			
			this.debugMode = (getRunTimeProperty("DebugMode").toLowerCase().equals("true")) ? true : false;
			this.logToStandardOut = (getRunTimeProperty("LogToStandardOut").toLowerCase().equals("true")) ? true : false;
			this.recordPageHTMLOnFailure = (getRunTimeProperty("RecordPageHTMLOnFailure").toLowerCase().equals("true")) ? true : false;
			
			// override run time properties if passed through ant command line
			if (!(MobilePlatFormName == null || MobilePlatFormName.isEmpty()))
				putRunTimeProperty("platformNameMobile", MobilePlatFormName);
			
			if (!(BrowserName == null || BrowserName.isEmpty()))
				putRunTimeProperty("Browser", BrowserName);
			
			if (!(ProjectName == null || ProjectName.isEmpty()))
				putRunTimeProperty("ProjectName", ProjectName);
			
			if (!(PlatformName == null || PlatformName.isEmpty())&& !PlatformName.equalsIgnoreCase("Local") && !getRunTimeProperty("PlatformName").equalsIgnoreCase("Android") && !getRunTimeProperty("PlatformName").equalsIgnoreCase("iOS"))
		 	putRunTimeProperty("PlatformName", PlatformName);
						
			if (!(RemoteAddress == null || RemoteAddress.isEmpty()) && !RemoteAddress.equalsIgnoreCase("null"))
			{
			 	putRunTimeProperty("RemoteAddress", RemoteAddress);
			 	putRunTimeProperty("RemoteExecution", "true");		
			}

			if (!(BuildId == null || BuildId.isEmpty()))
			 	putRunTimeProperty("BuildId", BuildId);
			
			if (!(BrowserVersion == null || BrowserVersion.isEmpty()))
			 	putRunTimeProperty("BrowserVersion", BrowserVersion);
			
			if(!(RunType == null || RunType.isEmpty()))
				putRunTimeProperty("RunType", RunType);
			else
			{
				RunType = getRunTimeProperty("RunType");
			}
			
			if (!(ResultsDir == null || ResultsDir.isEmpty()))
			{
				putRunTimeProperty("ResultsDir", ResultsDir);
			}
			else
			{
				// Set the full path of results dir
				String resultsDir = System.getProperty("user.dir") + getRunTimeProperty("ResultsDir");
				logComment("Results Directory is:- " + resultsDir);
				putRunTimeProperty("ResultsDir", resultsDir);
				
			}
			
			// override the MobileUAFlag value if passed through command line
			if (!(MobileUAFlag == null || MobileUAFlag.isEmpty()))
				putRunTimeProperty("MobileUAFlag", MobileUAFlag);
			
			if (!(simulatorEnabled == null || simulatorEnabled.isEmpty()))
			{
				putRunTimeProperty("simulatorEnabled", simulatorEnabled);
			}
			
			
			// override environment if declared in @TestVariables
			if (testannotation != null)
			{
				String mobileUAFlag = testannotation.MobileUAFlag();
				if(!mobileUAFlag.isEmpty())
				{
					putRunTimeProperty("MobileUAFlag",mobileUAFlag );
				}
			}
			//TODO Uncomment for android web execution
//			if (getRunTimeProperty("MobileUAFlag").equals("true"))
//			{
//				putRunTimeProperty("browser", "android_web");
//			}
								
			// Set the full path of test data sheet
			String testDataSheet = System.getProperty("user.dir") + getRunTimeProperty("TestDataSheet");
			if (debugMode)
				logComment("Test data sheet is:-" + testDataSheet);
			putRunTimeProperty("TestDataSheet", testDataSheet);
			
			// Set the full path of checkout page
			if (getRunTimeProperty("checkoutPage") != null)
			{
				String checkoutPage = System.getProperty("user.dir") + getRunTimeProperty("checkoutPage");
				if (debugMode)
					logComment("Checkout page is:-" + checkoutPage);
				putRunTimeProperty("checkoutPage", checkoutPage);
			}
			
			if (testannotation != null)
			{
				String remote = testannotation.remoteExecution();
				if(!remote.isEmpty())
				{
					putRunTimeProperty("RemoteExecution", remote);
				}
			}
			
			endExecutionOnfailure = false;
			remoteExecution = (getRunTimeProperty("RemoteExecution").toLowerCase().equals("true")) ? true : false;
			isMobile = (((getRunTimeProperty("Browser").equals("android_web") || getRunTimeProperty("Browser").equals("android_native")) && getRunTimeProperty("RemoteExecution").equals("true")) || getRunTimeProperty("MobileUAFlag").equals("true"));
			
			
			if(testMethod != null)
			{
				String folderName = testMethod.getName();
				if(remoteExecution)
				{
					RemoteAddress = getRunTimeProperty("RemoteAddress");
					downloadPath =  fileSeparator + fileSeparator + RemoteAddress + fileSeparator + "Downloads" + fileSeparator + folderName;
				}
				else
				{
					downloadPath = System.getProperty("user.home") + fileSeparator + "Downloads" + fileSeparator + folderName;
				}

				boolean status = Helper.createFolder(downloadPath);

				if(status)
				{
					downloadPath = downloadPath + fileSeparator;
				}
				else
				{
					System.out.println("Something went Wrong.!! Error in Creating Folder -" + downloadPath + " switching to predefined download Path - " + System.getProperty("user.home") + fileSeparator + "Downloads" + fileSeparator);
					downloadPath = System.getProperty("user.home") + fileSeparator + "Downloads" + fileSeparator;
				}
			}
		}
		catch (IOException e)
		{
			logException(e);
		}
	}
	
	@Attachment(value = "Logs For \"{0}\"", type = "text/html")
	public String attachLogs(String testName)
	{
		return this.testLog;
	}
	
	/**
	 * Create TestDataReader object for the given sheet and cache it can be
	 * fetched using - getCachedTestDataReaderObject()
	 * 
	 * @param sheetName
	 */
	private void cacheTestDataReaderObject(String sheetName, String path)
	{
		if (testDataReaderHashMap.get(path + sheetName) == null)
		{
			testDataReaderObj = new TestDataReader(this, sheetName, path);
			testDataReaderHashMap.put(path + sheetName, testDataReaderObj);
		}
	}
	
	public void closeAppium(ITestResult result)
	{
		logToStandardOut = true;
		if (result.getThrowable() != null)
			logException(result.getThrowable());
		Appium.close(this);
	}
	
	public void closeBrowser()
	{
		logToStandardOut = true;
		
		Browser.quitBrowser(this);
		driver = null;
	}
	
	public void closeBrowser(ITestResult result)
	{
		try
		{
			Browser.closeBrowser(this);
		}
		catch (Exception e)
		{
		}
		
		try
		{
			Browser.quitBrowser(this);
		}
		catch (Exception ex)
		{
		}
		
		try
		{
			driver.switchTo().defaultContent();
			Browser.closeBrowser(this);
		}
		catch (Exception e)
		{
		}
		
		try
		{
			driver.switchTo().defaultContent();
			Browser.quitBrowser(this);
		}
		catch (Exception ex)
		{
		}
		
		driver = null;
	}
	
	/**
	 * End Test
	 * @param result - ITestResult
	 */
	public void endTest(ITestResult result)
	{
		testEndTime = Helper.getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
		
		endExecutionOnfailure = false;
		enableScreenshot = false;
		recordPageHTMLOnFailure = false;
		
		List<String> reporterOutput = Reporter.getOutput(result);
		if(this.testStartTime != null)
		{
			long minutes = 0;
			long seconds = 0;
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String minuteOrMinutes = " ";
			String secondOrSeconds = "";
			try
			{
				long timeinMillis = (dateFormat.parse(testEndTime).getTime() - dateFormat.parse(this.testStartTime).getTime())/1000;
				minutes = timeinMillis/60;
				seconds = timeinMillis%60;
				if(minutes > 1)
					minuteOrMinutes = "s ";
				if(seconds > 1)
					secondOrSeconds = "s";
			}
			catch(Exception e)
			{}

			if(!Helper.listContainsString(reporterOutput, "<font color='Blue'><B>Total time taken by Test '" + getTestName() + "' : '"))
				logComment("<font color='Blue'><B>Total time taken by Test '" + getTestName() + "' : '" + minutes + " minute" + minuteOrMinutes + seconds + " second" + secondOrSeconds + "' </B></font>");
		}
		
		if (!testResult)
		{
			if(!Helper.listContainsString(reporterOutput, "<B>Failure occured in test '" + getTestName() + "' Ended on '"))
				logFail("<B>Failure occured in test '" + getTestName() + "' Ended on '" + testEndTime + "'</B>");
		}
		else
		{
			if(!Helper.listContainsString(reporterOutput, "<B>Test Passed '" + getTestName() + "' Ended on '"))
				logPass("<B>Test Passed '" + getTestName() + "' Ended on '" + testEndTime + "'</B>");
		}
	}
	
	/**
	 * Get the cached TestDataReader Object for the given sheet. If it is not
	 * cached, it will be cached for future use
	 * 
	 * To read datasheet other than TestDataSheet, pass filename and sheet name separated by dot (i.e filename.sheetname)
	 * 
	 * @param sheetName
	 * @return TestDataReader object or null if object is not in cache
	 */
	public TestDataReader getCachedTestDataReaderObject(String sheetName)
	{	
		String path = getRunTimeProperty("TestDataSheet");
		if(sheetName.contains("."))
		{	
			path=System.getProperty("user.dir")+getRunTimeProperty(sheetName.split("\\.")[0]);
			sheetName=sheetName.split("\\.")[1];
			
		}
		return getCachedTestDataReaderObject(sheetName, path);
	}
	
	
	/**
	 * Get the cached TestDataReader Object for the given sheet in the excel
	 * specified by path. If it is not cached, it will be cached for future use
	 * 
	 * @param sheetName
	 * @param path
	 *            Path of excel sheet to read
	 * @return TestDataReader object or null if object is not in cache
	 */
	public TestDataReader getCachedTestDataReaderObject(String sheetName, String path)
	{
		TestDataReader obj = testDataReaderHashMap.get(path + sheetName);
		// Object is not in the cache
		if (obj == null)
		{
			// cache for future use
			synchronized(Config.class)
			{
				cacheTestDataReaderObject(sheetName, path);
				obj = testDataReaderHashMap.get(path + sheetName);
			}
		}
		return obj;
	}
	
	/**
	 * Get the Run Time Property value
	 * 
	 * @param key
	 *            key name whose value is needed
	 * @return value of the specified key
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<JSONObject> getJSONArrayListFromRunTimeProperty(String key)
	{
		String keyName = key.toLowerCase();
		ArrayList<JSONObject> value;
		try
		{
			value = (ArrayList<JSONObject>) runtimeProperties.get(keyName);
			if (debugMode)
				logComment("Reading Run-Time key-" + keyName + " value:-'" + value + "'");
		}
		catch (Exception e)
		{
			if (debugMode)
			{
				logComment(e.toString());
				logComment("'" + key + "' not found in Run Time Properties");
			}
			return null;
		}
		return value;
	}
	
	/**
	 * Get the Run Time Property value
	 * 
	 * @param key
	 *            key name whose value is needed
	 * @return value of the specified key
	 */
	public Object getObjectRunTimeProperty(String key)
	{
		String keyName = key.toLowerCase();
		Object value = "";
		try
		{
			value = runtimeProperties.get(keyName);
			if (debugMode)
				logComment("Reading Run-Time key-" + keyName + " value:-'" + value + "'");
		}
		catch (Exception e)
		{
			if (debugMode)
			{
				logComment(e.toString());
				logComment("'" + key + "' not found in Run Time Properties");
			}
			return null;
		}
		return value;
	}
	
	/**
	 * Refreshes the cache for the given sheet in excel, and gets TestDataReader
	 * Object Also it will be cached for future use
	 * 
	 * @param sheetName
	 * @param path
	 *            Path of excel sheet to read
	 * @return TestDataReader object or null if object is not in cache
	 */
	public TestDataReader getRefreshedTestDataReaderObject(String sheetName, String path)
	{
		TestDataReader obj = new TestDataReader(this, sheetName, path);
		// cache for future use
		testDataReaderHashMap.put(path + sheetName, obj);
		obj = testDataReaderHashMap.get(path + sheetName);
		
		return obj;
	}
	
	/**
	 * Get the Run Time Property value
	 * 
	 * @param key
	 *            key name whose value is needed
	 * @return value of the specified key
	 */
	public String getRunTimeProperty(String key)
	{
		String keyName = key.toLowerCase();
		String value = "";
		try
		{
			value = runtimeProperties.get(keyName).toString();
			value = Helper.replaceArgumentsWithRunTimeProperties(this, value);
			if (debugMode)
				logComment("Reading Run-Time key-" + keyName + " value:-'" + value + "'");
		}
		catch (Exception e)
		{
			if (debugMode)
			{
				logComment(e.toString());
				logComment("'" + key + "' not found in Run Time Properties");
			}
			
			return null;
		}
		return value;
	}
	
	public String getTestName()
	{
		return testName;
	}
	
	public boolean getTestCaseResult()
	{
		return testResult;
	}
	
	public void logComment(String message)
	{
		if(message != null && message.contains("window.onload"))
		{
			//This is done to remove the unnecessary redirecting of results page when viewing the reports, this will not affect the flow of testcase
			Log.Comment("Message to be print contains 'window.onload' so replacing it by '*window.onload*' before printing.", this);
			message = message.replace("window.onload", "*window.onload*");
		}
		Log.Comment(message, this);
	}
	
	public void logHighLight(String message)
	{
		Log.Comment(message, this, "HotPink");
	}
	
	public void logException(Throwable e)
	{
		testResult = false;
		String fullStackTrace = ExceptionUtils.getFullStackTrace(e);
		Log.Fail(fullStackTrace, this);
	}
	
	public void logFail(String message)
	{
		testResult = false;
		Log.Fail(message, this);
	}
	
	public <T> void logFail(String what, T expected, T actual)
	{
		testResult = false;
		String message = "Expected '" + what + "' was :-'" + expected + "'. But actual is '" + actual + "'";
		Log.Fail(message, this);
	}
	
	/*
	 * Method to log redmine id for failed test cases
	 */
	public void logRedmineID(String issueID)
	{
		Log.Comment("<B><font color='Red'>" + ">>>>>>>>>> Test case failed due to KNOWN bug in Application <<<<<<<<<<" + "</font></B>", this);
		Log.Comment("<B>RedmineID</B>:- <a href="+"http://redmine.payu.in/issues/" + issueID + " target='_blank' >" +"http://redmine.payu.in/issues/"+issueID+"</a>", this);
	}
	
	public void logFail(String what, String expected, String actual)
	{
		testResult = false;
		String projectName = this.getRunTimeProperty("ProjectName");
		String message = null;
		
		if(projectName != null && projectName.equalsIgnoreCase("acs"))
			message = "Expected '" + what + "' was :-\n<br/>" + StringUtils.replaceEach(expected, new String[] { "&", "\"", "<", ">" }, 
					new String[] { "&amp;", "&quot;", "&lt;", "&gt;" }).replace("&lt;br/&gt;", "\n<br/>") + ".\n<br/>But actual is :-\n<br/>" + StringUtils.replaceEach(actual, new String[] { "&", "\"", "<", ">" }, 
							new String[] { "&amp;", "&quot;", "&lt;", "&gt;" }).replace("&lt;br/&gt;", "\n<br/>") + "";
		else
			message = "Expected '" + what + "' was :-'" + expected + "'. But actual is '" + actual + "'";
		
		Log.Fail(message, this);
	}
	
	public void logWarning(String what, String expected, String actual)
	{
		//testResult = false;
		String message = "Expected '" + what + "' was :-'" + expected + "'. But actual is '" + actual + "'";
		Log.Warning(message, this);
	}
	
	public void logFailureException(Throwable e)
	{
		testResult = false;
		Log.Failfinal(ExceptionUtils.getFullStackTrace(e), this);
	}
	
	public void logPass(String message)
	{
		Log.Pass(message, this);
	}
	
	public <T> void logPass(String what, T actual)
	{
		String message = "Verified '" + what + "' as :-'" + actual + "'";
		Log.Pass(message, this);
	}
	
	public void logPass(String what, String actual)
	{
	String message = StringUtils.replaceEach(actual, new String[] { "&", "\"", "<", ">" }, new String[] { "&amp;", "&quot;", "&lt;", "&gt;" });
	message = "Verified '" + what + "' as :-'" + message + "'";
	Log.Pass(message, this);
	}
	
	public void logWarning(String message)
	{
		Log.Warning(message, this);
	}
	
	public void logWarning(String message, boolean logPageInfo)
	{
		Log.Warning(message, this, logPageInfo);
	}
	
	public void openApplication()
	{
		int tries=3;
		while(tries>0){
			this.appiumDriver = Appium.openApplication(this);	
			if(this.appiumDriver == null)
			{
				this.logComment("<-----APPLICATION not Launched properly, as appiumDriver is NULL,Trying again ----->");
				tries--;
			}
			else
			{
				this.logComment("<-----APPLICATION LAUNCHED SUCCESSFULLY----->");
				tries=0;
			}	
		}
		if(this.appiumDriver == null)
		{
			this.logFail("<-----APPLICATION not Launched properly, as appiumDriver is NULL ----->");
		}	
		this.driver = this.appiumDriver;
	}
	
	public void openBrowser()
	{
		int retryCnt = 3;
		while (this.driver == null && retryCnt > 0)
		{
			try
			{
				this.driver = Browser.openBrowser(this);
				printNodeIpAddress(this.session, "browser");
				
			}
			catch (Exception e)
			{
				Log.Warning("Retrying the browser launch:-" + e.getLocalizedMessage(), this);
			}
			if (this.driver == null)
			{
				if (this.remoteExecution)
				{
					Log.Comment("Deleting temporary files from folder", this);
					TemporaryFilesystem tempFS = TemporaryFilesystem.getTmpFsBasedOn(new File((File.separator + File.separator + Config.RemoteAddress + File.separator + "SeleniumTemp" + File.separator + getRunTimeProperty("BuildId") + File.separator)+this.getTestName()));
					tempFS.deleteTemporaryFiles();
					Browser.wait(this, 30);
				}
				retryCnt--;
				if (retryCnt == 0)
				{
					logFail("Browser could not be opened");
					Assert.assertTrue(false);
				}
				Browser.wait(this, 2);
			}
			
		}
		endExecutionOnfailure = false;
	}
	
	public void openBrowserInMobile()
	{
		this.appiumDriver = Appium.openBrowser(this);
		this.driver = this.appiumDriver;
	};
	
	/**
	 * Add the given key ArrayListJSONObject pair in the Run Time Properties
	 */
	public void putJSONArrayListInRunTimeProperty(String key, ArrayList<JSONObject> table)
	{
		String keyName = key.toLowerCase();
		runtimeProperties.put(keyName, table);
		if (debugMode)
			logComment("Putting Run-Time key-" + keyName + " value:-'" + table.toString() + "'");
	}
	
	/**
	 * Add the given key value pair in the Run Time Properties
	 * 
	 * @param key
	 * @param value
	 */
	public void putRunTimeProperty(String key, Object value)
	{
		String keyName = key.toLowerCase();
		runtimeProperties.put(keyName, value);
		if (debugMode)
			logComment("Putting Run-Time key-" + keyName + " value:-'" + value + "'");
	}
	
	/**
	 * Add the given key value pair in the Run Time Properties
	 * 
	 * @param key
	 * @param value
	 */
	public void putRunTimeProperty(String key, String value)
	{
		String keyName = key.toLowerCase();
		runtimeProperties.put(keyName, value);
		if (debugMode)
		{
				logComment("Putting Run-Time key-" + keyName + " value:-'" + value + "'");
		}
	}
	
	/**
	 * Removes the given key from the Run Time Properties
	 * 
	 * @param key
	 */
	public void removeRunTimeProperty(String key)
	{
		String keyName = key.toLowerCase();
		if (debugMode)
			logComment("Removing Run-Time key-" + keyName);
		runtimeProperties.remove(keyName);
	}
	
	
	public void update_TestDataSheet_value(SheetToBeUsed sheetToBeUsed)
	{
		// Set the full path of test data sheet
		String path = "";
		String userDir = System.getProperty("user.dir");
		
		switch (sheetToBeUsed)
		{
			case ProductProduction:
				path = userDir.substring(0, userDir.lastIndexOf(fileSeparator) + 1) + "Product//Parameters//PRODUCTION.xls";
				break;
			
			case Product:
				path = userDir.substring(0, userDir.lastIndexOf(fileSeparator) + 1) + "Product//Parameters//ProductTestData.xls";
				break;
			
			case Money:
				path = userDir.substring(0, userDir.lastIndexOf(fileSeparator) + 1) + "Money//Parameters//MoneyTestData.xls";
				break;
				
			case Common:
				path = userDir.substring(0, userDir.lastIndexOf(fileSeparator) + 1) + "Common//Parameters//CommonTestData.xls";
				break;
				
			case ACS:
				path = userDir.substring(0, userDir.lastIndexOf(fileSeparator) + 1) + "ACS//Parameters//ACSTestData.xls";
				break;
				
			case POS:
				path = userDir.substring(0, userDir.lastIndexOf(fileSeparator) + 1) + "bizposmwautomation//Parameters//POSTestData.xlsx";
				break;
								
			case MoneyRefund:
				path = userDir.substring(0, userDir.lastIndexOf(fileSeparator) + 1) + "Money//Parameters//RefundTestData.xlsx";
				break;
				
			default:// do nothing
				break;
		}
		if (debugMode)
			logComment("Updated path of Test data sheet is :-" + path);
		putRunTimeProperty("TestDataSheet", path);
		
	}
	
	/**
	 * This method will hit the hub and call the api of hub to get the ip address of machine where our test case is executing
	 * 
	 * @param session {@link SessionId}
	 * @author shishir.dwivedi
	 */
	public void printNodeIpAddress(SessionId session, String calledFor)
	{
		String[] hostAndPort = new String[2];
		String errorMsg = "Failed to acquire remote webdriver node and port info. Root cause: ";
		String hostName= this.getRunTimeProperty("RemoteAddress");
		int port=4444;
		if(session==null)
		{
			//this.logComment("Session ID not found: It seems like this execution is Local execution");
			return;
		}
		else
		{
			try
			{
				HttpHost host = new HttpHost(hostName, port);
				HttpClient client = HttpClientBuilder.create().build();
				//DefaultHttpClient client = new DefaultHttpClient();
				URL sessionURL = new URL("http://" + hostName + ":" + port + "/grid/api/testsession?session=" + session);
				BasicHttpEntityEnclosingRequest r = new BasicHttpEntityEnclosingRequest("POST", sessionURL.toExternalForm());
				org.apache.http.HttpResponse response =  client.execute(host, r);
				JSONObject myjsonobject = extractObject(response);
				String url = Helper.getAttributeValueFromJson(this, myjsonobject, "proxyId");
				URL myURL = new URL(url);
				
				if ((myURL.getHost() != null) && (myURL.getPort() != -1))
				{
					hostAndPort[0] = myURL.getHost();
					if(calledFor.equalsIgnoreCase("browser"))
						this.logComment("<font color='Blue'><B>Test Case Executing at :</B> "+hostAndPort[0] + "</font>");
					else if(calledFor.equals("mobile"))
					{
						sessionURL = new URL("http://" + hostName + ":" + port + "/grid/api/proxy?id="+url);
						r = new BasicHttpEntityEnclosingRequest("POST", sessionURL.toExternalForm());
						response =  client.execute(host, r);
						myjsonobject = extractObject(response);

						String mobileName = Helper.getAttributeValueFromJson(this, myjsonobject, "mobileName");
						String deviceId = Helper.getAttributeValueFromJson(this, myjsonobject, "deviceId");
						this.logComment("<font color='Blue'><B>Test Case Executing at :</B> "+hostAndPort[0] + " <B>on Mobile : </B> '" + mobileName + "' <B>with deviceId :</B> " + deviceId + "</font>");
						putRunTimeProperty("deviceID", deviceId);
						putRunTimeProperty("mobileName", mobileName);
					}
					hostAndPort[1] = Integer.toString(myURL.getPort());
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(errorMsg, e);
			}
		}
	}
	
	/**
	 * This method will parse the json response and extract machine ip from json which is returned after calling api
	 * @param resp {@link HttpResponse}
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 * @author shishir.dwivedi
	 */
	private static JSONObject extractObject(HttpResponse resp) throws IOException, JSONException
	{
		BufferedReader rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
		StringBuffer s = new StringBuffer();
		String line;
		while ((line = rd.readLine()) != null)
		{
			s.append(line);
		}
		rd.close();
		JSONObject objToReturn = new JSONObject(s.toString());
		return objToReturn;
	}
	
	/**
	 * Get mobile os name
	 * @return os name
	 */
	public String getMobileOsName()
	{
		if(iosDriver != null)
			return "ios";
		else if (androidDriver != null)
			return "android";
		else
			return "unknown";
	}
}
