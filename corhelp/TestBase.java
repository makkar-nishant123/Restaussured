package Utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.asserts.SoftAssert;

import Utils.Config.SheetToBeUsed;
import Utils.DataBase.DatabaseType;

@Listeners(Utils.TestListener.class)
public class TestBase
{
	protected final static long DEFAULT_TEST_TIMEOUT = 600000;
	protected static ThreadLocal<Config[]> threadLocalConfig = new ThreadLocal<Config[]>();
	//private String log=null;
	
	@DataProvider(name = "GetMobileTestConfig")
	public Object[][] GetMobileTestConfig(Method method)
	{
		Config testConf = new Config(method);
		testConf.testName = method.getDeclaringClass().getName() + "." + method.getName();
		testConf.testStartTime = Helper.getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
		
		if (method.isAnnotationPresent(TestVariables.class))
		{
			// Create a object of annotation
			Annotation annotation = method.getAnnotation(TestVariables.class);
			TestVariables annotationObj = (TestVariables) annotation;
			testConf.putRunTimeProperty("newCommandTimeout", annotationObj.newCommandTimeout());
		}
		
		testConf.openApplication();
		threadLocalConfig.set(new Config[] { testConf });
		
		return new Object[][] { { testConf } };
	}
	
	/**
	 * This method is used to implement data driven approach. Number of
	 * parameters of testcase should equal to data column excluding config
	 * object
	 * 
	 * @param method
	 * @return double dimension String array of data including Config class
	 *         object.
	 */
	@DataProvider(name = "GetTestDataForCred")
	public Object[][] getCredTestData(Method method)
	{
		Config dataConfig = (Config) GetTestConfig(method)[0][0];
		Object[][] object = null;
		// if method is annotated with @TestVariables else throw error message
		if (method.isAnnotationPresent(TestVariables.class))
		{
			// Create a object of annotation
			Annotation annotation = method.getAnnotation(TestVariables.class);
			TestVariables annotationObj = (TestVariables) annotation;
			
			// Get length of 'datasheetRowNumber' annotation
			int rowLength = annotationObj.dataSheetRowNumber().length;
			TestDataReader readerData=dataConfig.getCachedTestDataReaderObject(annotationObj.dataSheetName());
			// Get number of parameter passed except expected columns
			int colLength = readerData.getColumnNumExceptExpected();
			object = new Object[rowLength][2];//col length set = 2 as, no of parameters to be passed in Test Case, i.e. Api parameters and Config
			// Get required test data in a Hashmap
			for (int row = 0; row < rowLength; row++)
			{
				HashMap<String,String> parameters = new HashMap<>();
				int col = 0;
				for (; col < colLength; col++)
				{			
					parameters.put(readerData.GetHeaderData(col),
							readerData.GetData(annotationObj.dataSheetRowNumber()[row], readerData.GetHeaderData(col), false));
				}
				object[row][0] =  parameters;
				object[row][1] = dataConfig;
			}
		}
		else
		{
			String message = "<<-------ANNOTATIONS NOT DECLARED ABOVE METHOD------->>";
			dataConfig.logFail(message);
		}
		
		return object;
	}
	
	@DataProvider(name = "GetMobileAndWebTestConfig")
	public Object[][] GetMobileAndWebTestConfig(Method method)
	{
		Config secondaryConfig = new Config(method);
		Config testConf = new Config(method);
		testConf.testName = secondaryConfig.testName = method.getDeclaringClass().getName() + "." + method.getName();
		testConf.testStartTime = secondaryConfig.testStartTime = Helper.getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
		
		if (method.isAnnotationPresent(TestVariables.class))
		{
			// Create a object of annotation
			Annotation annotation = method.getAnnotation(TestVariables.class);
			TestVariables annotationObj = (TestVariables) annotation;
			testConf.putRunTimeProperty("newCommandTimeout", annotationObj.newCommandTimeout());
		}
		
		testConf.openApplication();
		threadLocalConfig.set(new Config[] { testConf, secondaryConfig});
		
		return new Object[][] { { testConf, secondaryConfig } };
	}
	
	/**
	 * used by TransactionEmail test cases
	 * 
	 * @param method
	 * @return
	 */
	@DataProvider(name = "GetRequestRefundTestConfig")
	public Object[][] GetRequestRefundTestConfig(Method method)
	{
		Config testConf = new Config(method);
		testConf.testName = method.getDeclaringClass().getName() + "." + method.getName();
		testConf.testStartTime = Helper.getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
		threadLocalConfig.set(new Config[] { testConf });
		
		return new Object[][] { { testConf, new String[] { "partial", "full" } } };
	}
	
	@DataProvider(name = "GetTestConfig")
	public Object[][] GetTestConfig(Method method)
	{
		Config testConf = new Config(method);
		testConf.testName = method.getDeclaringClass().getName() + "." + method.getName();
		testConf.testStartTime = Helper.getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
		threadLocalConfig.set(new Config[] { testConf });
		return new Object[][] { { testConf } };
	}
	
	@DataProvider(name = "GetTwoBrowserTestConfig")
	public Object[][] GetTwoBrowserTestConfig(Method method)
	{
		Config testConf = new Config(method);
		Config secondaryConfig = new Config(method);
		
		testConf.testName = secondaryConfig.testName = method.getDeclaringClass().getName() + "." + method.getName();
		testConf.testStartTime = secondaryConfig.testStartTime = Helper.getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
		
		if (method.isAnnotationPresent(TestVariables.class))
		{
			// Create a object of annotation
			Annotation annotation = method.getAnnotation(TestVariables.class);
			TestVariables annotationObj = (TestVariables) annotation;
			testConf.putRunTimeProperty("newCommandTimeout", annotationObj.newCommandTimeout());
		}
		
		threadLocalConfig.set(new Config[] { testConf, secondaryConfig });
		
		return new Object[][] { { testConf, secondaryConfig } };
	}
	
	/**
	 * used by TransactionEmail test cases
	 * 
	 * @param method
	 * @return
	 */
	@DataProvider(name = "GetUrlTestConfig")
	public Object[][] GetUrlTestConfig(Method method)
	{
		Config testConf = new Config(method);
		testConf.testName = method.getDeclaringClass().getName() + "." + method.getName();
		testConf.testStartTime = Helper.getCurrentDateTime("yyyy-MM-dd HH:mm:ss");
		threadLocalConfig.set(new Config[] { testConf });
		
		return new Object[][] { { testConf, new String[] { "ivr", "invoice" } } };
	}
	
	@BeforeSuite(alwaysRun = true)
	@Parameters({ "environment" })
	public void beforeSuiteSetup(@Optional String environment)
	{
		Config.Environment = environment;
		Config testConfig = new Config(null);
		try
		{
			testConfig.logComment("Inside before suite......");
			String projectName = testConfig.getRunTimeProperty("projectName");
			if(projectName != null && projectName.equalsIgnoreCase("money"))
			{
				String env = testConfig.getRunTimeProperty("environment");
				if(env != null && !env.toLowerCase().contains("production"))
				{
					testConfig.update_TestDataSheet_value(SheetToBeUsed.Common);
					String sqlSheetName = "SQLForMoney";
	
					TestDataReader tdr = testConfig.getCachedTestDataReaderObject(sqlSheetName);
					//Disable all generic rules
					DataBase.executeUpdateQuery(testConfig, tdr.GetData(18, "Query"), DatabaseType.MoneyAuthDB);
					//Remove autoApproval and isBanned from all categories
					DataBase.executeUpdateQuery(testConfig, tdr.GetData(19, "Query"), DatabaseType.MoneyAuthDB);
					//Delete all FailureMessage from `analyticsdata` db
					DataBase.executeUpdateQuery(testConfig, tdr.GetData(20, "Query"), DatabaseType.MoneyAnalyticsDataDB);
				}
			}
		}
		catch(Exception e)
		{
			testConfig.logComment(e.getMessage());
		}
		testConfig.logComment("Exiting before suite......");
	}
	
	@BeforeClass(alwaysRun = true)
	@Parameters({ "browser", "environment", "testngOutputDir", "MobileUAFlag", "PlatformName", "RemoteAddress", "BrowserVersion", "RunType", "ProjectName", "BuildId", "platformNameMobile", "simulatorEnabled" })
	public void InitializeParameters(@Optional String browser, @Optional String environment, @Optional String testngOutputDir, @Optional String MobileUAFlag, @Optional String PlatformName, @Optional String RemoteAddress, @Optional String BrowserVersion, @Optional String RunType, @Optional String ProjectName, @Optional String BuildId, @Optional String platformNameMobile, @Optional String simulatorEnabled)
	{
		Config.BrowserName = browser;
		Config.Environment = environment;
		Config.ResultsDir = testngOutputDir;
		Config.MobileUAFlag = MobileUAFlag;
		Config.PlatformName = PlatformName;
		Config.RemoteAddress = RemoteAddress;
		Config.BrowserVersion = BrowserVersion;
		Config.RunType = RunType;
		Config.ProjectName = ProjectName;
		Config.BuildId = BuildId; 
		Config.MobilePlatFormName = platformNameMobile;
		Config.simulatorEnabled = simulatorEnabled;
	}
	
	@AfterMethod(alwaysRun = true)
	public void tearDown(ITestResult result)
	{
		tearDownHelper(result, true);
	}
	
	/**
	 * This method is used to implement data driven approach. Number of
	 * parameters of testcase should equal to data column excluding config
	 * object
	 * 
	 * @param method
	 * @return double dimension String array of data including Config class
	 *         object.
	 */
	@DataProvider(name = "GetTestDataFromExcel")
	public Object[][] getTestData(Method method)
	{
		Object[][] object = null;
		Config dataConfig = (Config) GetTestConfig(method)[0][0];
		
		// if method is annotated with @TestVariables else throw error message
		if (method.isAnnotationPresent(TestVariables.class))
		{
			// Create a object of annotation
			Annotation annotation = method.getAnnotation(TestVariables.class);
			TestVariables annotationObj = (TestVariables) annotation;
			
			// Get length of 'datasheetRowNumber' annotation
			int rowLength = annotationObj.dataSheetRowNumber().length;
			
			// Get number of parameter passed
			int colLength = method.getParameterCount();
			
			// Get all test data in double dimension array
			//String[][] data = FileHandler.replica(dataConfig.getRunTimeProperty("TestDataSheet"), annotationObj.dataSheetName());
		
			object = new Object[rowLength][colLength];
			TestDataReader readerData=dataConfig.getCachedTestDataReaderObject(annotationObj.dataSheetName());
			// Get required test data in a two dimension array and append
			// 'dataConfig' object at the end.
			for (int row = 0; row < rowLength; row++)
			{
				int col = 0;
				for (; col < colLength - 1; col++)
				{				
					object[row][col]=readerData.GetData(annotationObj.dataSheetRowNumber()[row], readerData.GetHeaderData(col + 1));				
				}
				object[row][col] = dataConfig;
			}
		}
		else
		{
			String message = "<<-------ANNOTATIONS NOT DECLARED ABOVE METHOD------->>";
			dataConfig.logFail(message);
		}
		return object;
	}
	
	protected void tearDownHelper(ITestResult result, Boolean clearConfig) {
		
		Config[] testConfigs = threadLocalConfig.get();
		Config testConf = testConfigs[0];
		String tearDownValue = null;
		if(testConf.getRunTimeProperty("skipTearDown")==null)
					tearDownValue = "false";
		else
					tearDownValue = testConf.getRunTimeProperty("skipTearDown");
				
		tearDownHelper(result, clearConfig, tearDownValue);
		
	}
	
	protected void tearDownHelper(ITestResult result, Boolean clearConfig,String tearDownValue)
	{
		String testcaseName = "NullConfig";
		Config[] testConfigs = threadLocalConfig.get();
			
		
		if (testConfigs != null)
		  if(tearDownValue.equals("false")) {
			  for (Config testConf : testConfigs)
				{ 
					if (testConf != null)
					{
						testcaseName = testConf.getTestName();
						testConf.logComment("<------ AfterMethod started for : " + testConf +" "+ testConf.getTestName() + " ------>");
						User user = new User();
						if (testConf.customerId != null)
							user.markCustomerFree(testConf, testConf.customerId);
						if (testConf.merchantId != null)
							user.markMerchantFree(testConf, testConf.merchantId);
						if (testConf.adminId != null)
							user.markAdminFree(testConf, testConf.adminId);
						if (testConf.aggregatorId != null)
							user.markAggregatorFree(testConf, testConf.aggregatorId);
						if (testConf.pitUserId != null)//PIT User
							user.markPITUserFree(testConf, testConf.pitUserId);
						
						try
						{
							if (testConf.MoneyAuthDbConnection != null)
								testConf.MoneyAuthDbConnection.close();
							
							if (testConf.MoneyVaultDbConnection != null)
								testConf.MoneyVaultDbConnection.close();
							
							if (testConf.MoneyCmsDbConnection != null)
								testConf.MoneyCmsDbConnection.close();
							
							if (testConf.MoneyUserMgmtConnection != null)
								testConf.MoneyUserMgmtConnection.close();

							if (testConf.PerformanceDBConnection != null)
								testConf.PerformanceDBConnection.close();
							
							if (testConf.KVaultDBConnection != null)
								testConf.KVaultDBConnection.close();
							
							if (testConf.connection != null)
								testConf.connection.close();
						}
						catch (SQLException e)
						{
							testConf.logComment("<------Exception is SQL------>" + e.getMessage());
							e.printStackTrace();
						}
						
						if (testConf.appiumDriver != null)
						{
							testConf.closeAppium(result);
							Appium.stopAppiumServer(testConf);
						}
						else
						{
							//insertLogsInAccuracyDB(testConf);
							testConf.closeBrowser(result);
						}
						
						testConf.logComment("<------ AfterMethod ended for : " + testConf +" "+ testConf.getTestName() + " ------>");
						
						/**
						 * flag to save testConfig variable for data driven test cases
						 */ 
						if (clearConfig)
							{
								testConf.runtimeProperties.clear();
								testConf = null;
							}
						else
							{
								// reset config data so that old failure data is not passed to next test case in data driven test cases
								// reset more data if needed
								// like testResult etc.
								testConf.softAssert = new SoftAssert();
							}
						
					}
					else
					{
						System.out.println("testConfig object not found");
					}
				}
		  } 
		  else {
			  for(Config testConf : testConfigs) {
						testConf.softAssert = new SoftAssert();
			  }
		  }
			
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Date startDate = new Date();
		System.out.println("<B>Test '" + testcaseName + "' Ended on '" + dateFormat.format(startDate) + "'</B>");
	}
	
	
	@DataProvider(name="multipleExecution")
	public Object[][] getStaticPageList(Method method) {
		Object[][] object = null;
		Config dataConfig = (Config) GetTestConfig(method)[0][0];
		
		if (method.isAnnotationPresent(TestVariables.class)) {
			
			Annotation annotation = method.getAnnotation(TestVariables.class);
			TestVariables annotationObj = (TestVariables) annotation;
						
			TestDataReader readerData = dataConfig.getCachedTestDataReaderObject(annotationObj.dataSheetName());
			int rowLength = readerData.getRecordsNum();
			
			
			int colLen; 
			colLen = 0;
			
			while(readerData.GetData(0, readerData.GetHeaderData(colLen)) != null && readerData.GetData(0, readerData.GetHeaderData(colLen)).isEmpty() == false && !readerData.GetData(0, readerData.GetHeaderData(colLen)).equalsIgnoreCase("{skip}")){
				colLen++;	
			}
			
			object = new Object[rowLength-1][2];//object=new Object[rowLength][colLength];
			//Add 'dataConfig' object at the beginning and Get required test data in a two dimension array and append .			
			
			for(int row = 0; row <rowLength-1; row++)
			{
				object[row][0] = dataConfig;
				HashMap<String, String> staticPageList = new HashMap<String, String>();
				for(int col = 0; col < colLen; col++)
				{					
					staticPageList.put(readerData.GetData(0, readerData.GetHeaderData(col)), readerData.GetData(row+1, readerData.GetHeaderData(col)));					
					
				}
				if(row==rowLength-2)
					staticPageList.put("tearDown", "1");
				else
				staticPageList.put("tearDown", "0");
				object[row][1] = staticPageList;
			}
			
		}
		else
		{
			String message= "<<-------ANNOTATIONS NOT DECLARED ABOVE METHOD------->>";
			dataConfig.logFail(message);
		}
		
		return object;
	}
	
	
	/**
	 * @param method
	 *
	 * Data provider for Api
	 * Reads particular row from excel sheet and returns key value pair
	 * @return config, HashMap of compulsory field, HashMap of Api Parameters.
	 */
	@DataProvider(name = "GetApiTestDataFromExcel")
	public Object[][] getApiTestData(Method method) {				
		Object[][] object = null;	
		Config dataConfig = (Config) GetTestConfig(method)[0][0];
				
		// if method is annotated with @TestVariables else throw error message
		if (method.isAnnotationPresent(TestVariables.class))
		{
			//Create a object of annotation
			Annotation annotation = method.getAnnotation(TestVariables.class);
			TestVariables annotationObj = (TestVariables) annotation;	
			//Get length of 'datasheetRowNumber' annotation
			int rowLength = annotationObj.dataSheetRowNumber().length;	
			
			TestDataReader readerData=dataConfig.getCachedTestDataReaderObject(annotationObj.dataSheetName());
			
			int colLen;//data[0].length;//length in excel sheet 
			colLen = 0;			
			
			//get total parameters 
			while(readerData.GetData(0, readerData.GetHeaderData(colLen)) != null && readerData.GetData(0, readerData.GetHeaderData(colLen)).isEmpty() == false && !readerData.GetData(0, readerData.GetHeaderData(colLen)).equalsIgnoreCase("{skip}")){
				colLen++;	
			}
			object = new Object[rowLength][3];//object=new Object[rowLength][colLength];
			//Add 'dataConfig' object at the beginning and Get required test data in a two dimension array and append .			
			
			for(int row = 0; row <rowLength; row++)
			{
				object[row][0] = dataConfig;
				int numberOfCompulsoryFieldinApiSheetBeforeParameters = 8;
				HashMap<String, String> apiResponseDetails = new HashMap<String, String>();
				for(int col = 0; col < numberOfCompulsoryFieldinApiSheetBeforeParameters; col++)
				{					
					apiResponseDetails.put(readerData.GetData(0, readerData.GetHeaderData(col)), readerData.GetData(annotationObj.dataSheetRowNumber()[row], readerData.GetHeaderData(col)));					
				}
				object[row][1] = apiResponseDetails;
				HashMap<String, String> apiParameters = new HashMap<String, String>();
				for(int col = numberOfCompulsoryFieldinApiSheetBeforeParameters; col < colLen; col++)
				{
					apiParameters.put(readerData.GetData(0, readerData.GetHeaderData(col)), readerData.GetData(annotationObj.dataSheetRowNumber()[row], readerData.GetHeaderData(col)));					
				}
				object[row][2] = apiParameters;	
			}
		}
		else
		{
			String message= "<<-------ANNOTATIONS NOT DECLARED ABOVE METHOD------->>";
			dataConfig.logFail(message);
		}
		
		return object;
	}
	
	/**
	 * This method is used to implement data driven approach to read complete excel data sheet 
	 * for a test method. Each excel sheet row represents one test case  
	 * @param method
	 * @return TestCaseName, Config instance, Hashmap of test data
	 */
	@DataProvider(name = "GetCompleteExcelTestData")
	public Object[][] getCompleteExcelTestData(Method method) {				
		Object[][] object = null;	
		Config dataConfig = (Config) GetTestConfig(method)[0][0];
				
		// if method is annotated with @TestVariables else throw error message
		if (method.isAnnotationPresent(TestVariables.class))
		{
			//Create a object of annotation
			Annotation annotation = method.getAnnotation(TestVariables.class);
			TestVariables annotationObj = (TestVariables) annotation;	
			
			TestDataReader readerData=dataConfig.getCachedTestDataReaderObject(annotationObj.dataSheetName());
			
			int colLen; 
			colLen = 0;
			
			//get total column numbers i.e., parameters 
			colLen = readerData.getColumnNum();
			
			//get total data Rows
			int rowLen = readerData.getRecordsNum();
			
			//object[x][0] -- test case name
			//object[x][1] -- test config object
			//object[x][2] -- hashmap of test data
			object = new Object[rowLen - 1][3];			
			
			for(int row = 1; row <rowLen; row++) {
			
				//Test case name
				object[row-1][0] = readerData.GetData(row, readerData.GetHeaderData(1));
				
				//testconfig object
				object[row-1][1] = dataConfig;
							
				//0th row has row no, 1st row has testcase name/description
				LinkedHashMap<String, String> inputDetails = new LinkedHashMap<String, String>();
				for(int col = 2; col < colLen; col++) {					
					inputDetails.put(readerData.GetData(0, readerData.GetHeaderData(col)), readerData.GetData(row, readerData.GetHeaderData(col)));					
				}
				object[row-1][2] = inputDetails;
			}
		}
		else
		{
			String message= "<<-------Test Variables annotations missing------->>";
			dataConfig.logFail(message);
		}
		
		return object;
	}

	//@AfterSuite(alwaysRun = true)
	public void testKillFirefox() throws IOException
	{
		if(Config.RunType.equalsIgnoreCase("official"))
		{
			System.out.println("****Closing all Firefox instances****");
			String path = System.getProperty("user.dir") + "\\..\\Common\\Prerequisite\\QuitAllFirefox.bat";
			Runtime.getRuntime().exec("cmd /c start " + path);

			System.out.println("****Closing all Chrome instances****");
			path = System.getProperty("user.dir") + "\\..\\Common\\Prerequisite\\QuitAllChrome.bat";
			Runtime.getRuntime().exec("cmd /c start " + path);

			System.out.println("****Closing all IE instances****");
			path = System.getProperty("user.dir") + "\\..\\Common\\Prerequisite\\QuitAllIE.bat";
			Runtime.getRuntime().exec("cmd /c start " + path);
		}
		else
		{
			System.out.println("****No browser is closed, running test cases on local machine****");
		}
	}
	
	/**
	 * This method will fetch log of each test case will insert into Accuracy DB.
	 * @param testConf
	 */
	private void insertLogsInAccuracyDB(Config testConf){
		String testResult="fail";
		if(testConf.testResult){
			testResult="pass";
		}
		String query="insert into productlogs (test_case_name,test_log,test_case_status,test_case_area,addedon) values (\""+testConf.testName+"\""+",\""+testConf.testLog+"\",\""+testResult+"\","+null+",\""+testConf.testEndTime+"\");";
		DBHelperForAccuracyDB.executeQuery(query, DatabaseType.AutomationDB);	
		testConf.logComment("Rows Inserted Suucessful in productlogs table");
	}
}
