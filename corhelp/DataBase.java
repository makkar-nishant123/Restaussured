package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataBase
{
	public enum DatabaseType
	{
		MoneyAsyncDB(1), MoneyAuthDB(2), MoneyCmsDB(3), MoneyVaultDB(4), MoneyWebServiceDB(5), Offline(6), Online(7), PayuMoneyAutomation(8), 
		PerformanceDB(9), AccuracyDB(10),OfflineCrypto(11), AutomationDB(12), AggregatedDB(13),PayuBizWebsiteDb(14), KVaultDB(15), ACSAutomationDB(16), 
		ACSDB(17), POSInvoice(18), MoneyAnalyticsDataDB(19), CREDOnBoardingDB(20), CredCreditCardDB(21), CredAutomationDB(22),CredCibilDB(23), RupayDB(24),
		ACSAuth(25),ACSAuthDashboard(26),OptimusCredLMSDB(27),OptimusCRMSDB(28);

		public final int values;

		DatabaseType(final int value){
			this.values = value;
		}
	};

	static Connection connection;

	public static Map<String, String> addToRunTimeProperties(Config testConfig, ResultSet sqlResultSet)
	{
		HashMap<String, String> mapData = new HashMap<String, String>();
		try
		{
			ResultSetMetaData meta = sqlResultSet.getMetaData();
			for (int col = 1; col <= meta.getColumnCount(); col++)
			{
				try
				{
					String columnName = meta.getColumnLabel(col);
					String columnValue = sqlResultSet.getObject(col).toString();

					//Code to handle TINYINT case
					if(meta.getColumnTypeName(col).equalsIgnoreCase("TINYINT"))
						columnValue = Integer.toString(sqlResultSet.getInt(col));

					mapData.put(columnName, columnValue);
				}
				catch (Exception e)
				{
					mapData.put(meta.getColumnLabel(col), "");
				}
			}
		}
		catch (SQLException e)
		{
			testConfig.logException(e);
		}

		Set<String> keys = mapData.keySet();
		for (String key : keys)
		{
			testConfig.putRunTimeProperty(key, mapData.get(key));
		}
		return mapData;
	}

	/**
	 * Executes the select db query for OFFLINE db and returns complete
	 * Resultset
	 * 
	 * @param Config
	 *            test config instance
	 * @param sqlRow
	 *            row number of the 'Query' column of 'SQL' sheet of Test data
	 *            excel having the query to be executed
	 * @return ResultSet -- Complete Result which is fetched is returned
	 */
	public static ResultSet executeSelectQuery(Config testConfig, int sqlRow, DatabaseType dbType)
	{
		// Read the Query column of SQL sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject("SQL");
		String selectQuery = sqlData.GetData(sqlRow, "Query");
		selectQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, selectQuery);
		Log.Comment("Executing the query - '" + selectQuery + "'", testConfig);
		return executeSelectQuery(testConfig, selectQuery, dbType);
	}

	/**
	 * Executes the select db query for OFFLINE db, and saves the result in
	 * Config.runtimeProperties as well as returns Map
	 * 
	 * @param Config
	 *            test config instance
	 * @param sqlRow
	 *            row number of the 'Query' column of 'SQL' sheet of Test data
	 *            excel having the query to be executed
	 * @param rowNumber
	 *            row number to be returned (use 1 for first row and -1 for last
	 *            row)
	 * @return Map containing key:value pairs of specified row
	 */
	public static Map<String, String> executeSelectQuery(Config testConfig, int sqlRow, int rowNumber)
	{
		// Read the Query column of SQL sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject("SQL");
		String selectQuery = sqlData.GetData(sqlRow, "Query");
		selectQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, selectQuery);
		Log.Comment("Executing the query - '" + selectQuery + "'", testConfig);
		return executeSelectQuery(testConfig, selectQuery, rowNumber, DatabaseType.Offline);
	}


	/**
	 * Pick query from Given Sheet and Executes the select db query for OFFLINE db, and returns Map of response data. 
	 * 
	 * @param Config
	 *            test config instance
	 * @param sqlRow
	 *            row number of the 'Query' column of 'SQL' sheet of Test data
	 *            excel having the query to be executed
	 * @param rowNumber
	 *            row number to be returned (use 1 for first row and -1 for last
	 *            row)
	 * @param sheetname will be combination of filename and sheetname separated by dot (i.e : filename.sheetname )          
	 * @return Map containing key:value pairs of specified row
	 */
	public static Map<String, String> executeSelectQuery(Config testConfig, int sqlRow, int rowNumber,String sheetname)
	{
		// Read the Query column of given sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject(sheetname);
		String selectQuery = sqlData.GetData(sqlRow, "Query");
		selectQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, selectQuery);
		Log.Comment("Executing the query - '" + selectQuery + "'", testConfig);
		return executeSelectQuery(testConfig, selectQuery, rowNumber, DatabaseType.Offline);
	}

	/**
	 * Pick query from Given Sheet and Executes the select db query for OFFLINE db, and returns Map of response data. 
	 * 
	 * @param Config
	 *            test config instance
	 * @param sqlRow
	 *            row number of the 'Query' column of 'SQL' sheet of Test data
	 *            excel having the query to be executed
	 * @param rowNumber
	 *            row number to be returned (use 1 for first row and -1 for last
	 *            row)
	 * @param sheetname will be combination of filename and sheet name separated by dot (i.e : filename.sheetname ) 
	 * @param  path will be path for sheet containing Queries      
	 * @return Map containing key:value pairs of specified row
	 */
	public static Map<String, String> executeSelectQuery(Config testConfig, int sqlRow, int rowNumber,String sheetname,String path)
	{

		// Read the Query column of given sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject(sheetname,path);
		String selectQuery = sqlData.GetData(sqlRow, "Query");
		selectQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, selectQuery);
		Log.Comment("Executing the query - '" + selectQuery + "'", testConfig);
		return executeSelectQuery(testConfig, selectQuery, rowNumber, DatabaseType.Offline);
	}

	/**
	 * Executes the select db query, and saves the result in
	 * Config.runtimeProperties as well as returns Map
	 * 
	 * @param Config
	 *            test config instance
	 * @param sqlRow
	 *            row number of the 'Query' column of 'SQL' sheet of Test data
	 *            excel having the query to be executed
	 * @param rowNumber
	 *            row number to be returned (use 1 for first row and -1 for last
	 *            row)
	 * @return Map containing key:value pairs of specified row
	 */
	public static Map<String, String> executeSelectQuery(Config testConfig, int sqlRow, int rowNumber, DatabaseType dbType)
	{
		// Read the Query column of SQL sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject("SQL");
		String selectQuery = sqlData.GetData(sqlRow, "Query");
		selectQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, selectQuery);
		Log.Comment("Executing the query - '" + selectQuery + "'", testConfig);
		return executeSelectQuery(testConfig, selectQuery, rowNumber, dbType);
	}

	/**
	 * Executes the select db query and return the complete Result Set
	 * 
	 * @param Config
	 *            test config instance
	 * @param selectQuery
	 *            query to be executed
	 * @param DatabaseType
	 *            online/offline
	 * @return Resultset
	 */

	public static ResultSet executeSelectQuery(Config testConfig, String selectQuery, DatabaseType dbType)
	{
		Date startDate = new Date();

		selectQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, selectQuery);
		Statement stmt = null;
		ResultSet resultSet = null;
		try
		{
			stmt = getConnection(testConfig, dbType).createStatement();
			resultSet = stmt.executeQuery(selectQuery);
		}
		catch (SQLException e)
		{
			testConfig.logException(e);
		}

		if (null == resultSet)
			testConfig.logWarning("No data was returned for this query");

		Date endDate = new Date();
		double timeDifference = (endDate.getTime() - startDate.getTime()) / 1000.00;

		if(timeDifference > 60)
			testConfig.logComment("<B>Time taken to run this query in minutes : " + timeDifference/60 + "</B>");
		//else
		//testConfig.logComment("Time taken to run this query in seconds : " + timeDifference);

		return resultSet;
	}


	private static HashMap<Integer, HashMap<String, String>> executeQueryHelper(Config testConfig,ResultSet resultSet)
	{
		// Convert that ResultSet into a HashMap
		HashMap<Integer, HashMap<String, String>> rowMapData = new HashMap<Integer, HashMap<String, String>>();
		//Starting Row Number 
		int row=1;
		try
		{
			while (resultSet.next())
			{			
				ResultSetMetaData meta = resultSet.getMetaData();

				HashMap<String, String> colMapData = new HashMap<String, String>();
				for (int col = 1; col <= meta.getColumnCount(); col++)
				{
					try{
						colMapData.put(meta.getColumnLabel(col), resultSet.getObject(col).toString());
					}
					catch(NullPointerException e){
						colMapData.put(meta.getColumnLabel(col), "");
					}
				}
				rowMapData.put(row,colMapData);	
				row++;
			}
		}catch (SQLException e){
			testConfig.logException(e);}
		catch(NullPointerException e){
			testConfig.logWarning("No data was returned for this query");
			rowMapData=null;
		}
		return rowMapData;
	}

	/**
	 * This Method is used to return all the rows return by a select query in a HashMap Structure
	 * 	Map<String,String> --> Map<Column Name,Column Data>
	 * @param testConfig
	 * @param DataBaseType  type 
	 * @param sqlRow Row number of SQl Query in dataSheet 
	 * @return HashMap <Integer, Map<String,String>>
	 * 	Integer --> Row Numbers
	 * 	Map->Column Name And Values 
	 */
	public static HashMap<Integer, HashMap<String, String>> executeSelectQuery(Config testConfig,DatabaseType type,int sqlRow,String sheetname)
	{	
		// Fetch Complete Result Set

		ResultSet resultSet=executeSelectQuery(testConfig, sqlRow, type,sheetname);
		return executeQueryHelper(testConfig,resultSet);
	}

	/**
	 * This Method is used to return all the rows return by a select query in a HashMap Structure
	 * 	Map<String,String> --> Map<Column Name,Column Data>
	 * @param testConfig
	 * @param DataBaseType  type 
	 * @param sqlRow Row number of SQl Query in dataSheet 
	 * @return HashMap <Integer, Map<String,String>>
	 * 	Integer --> Row Numbers
	 * 	Map->Column Name And Values 
	 */
	public static HashMap<Integer, HashMap<String, String>> executeSelectQuery(Config testConfig,DatabaseType type,int sqlRow)
	{	
		// Fetch Complete Result Set

		ResultSet resultSet=executeSelectQuery(testConfig, sqlRow, type);
		return executeQueryHelper(testConfig,resultSet);
	}

	/**
	 * Executes the select db query, and saves the result in
	 * Config.runtimeProperties as well as returns Map
	 * 
	 * @param Config
	 *            test config instance
	 * @param selectQuery
	 *            query to be executed
	 * @param rowNumber
	 *            row number to be returned (use 1 for first row and -1 for last
	 *            row)
	 * @return Map containing key:value pairs of specified row
	 */
	public static Map<String, String> executeSelectQuery(Config testConfig, String selectQuery, int rowNumber, DatabaseType dbType)
	{
		Date startDate = new Date();
		selectQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, selectQuery);

		Statement stmt = null;
		ResultSet resultSet = null;
		try
		{
			stmt = getConnection(testConfig, dbType).createStatement();
			resultSet = stmt.executeQuery(selectQuery);
		}
		catch (SQLException e)
		{
			testConfig.logException(e);
		}
		catch (NullPointerException ne) 
		{
			testConfig.endExecutionOnfailure = true;
			testConfig.logFail("<-----Unable to Create Connection With Database!! Please check your Internet----->");
		}
		Map<String, String> resultMap = null;

		int row = 1;
		try
		{
			if (rowNumber == -1)
			{
				if (resultSet.last())
					resultMap = addToRunTimeProperties(testConfig, resultSet);
			}
			else
			{
				while (resultSet.next())
				{
					if (row == rowNumber)
					{
						resultMap = addToRunTimeProperties(testConfig, resultSet);
						break;
					}
					else
					{
						row++;
					}
				}
			}
		}
		catch (SQLException e)
		{
			testConfig.logException(e);
		}
		catch (NullPointerException ne) 
		{
			testConfig.logWarning("<----------------No Data returned by Query!! Please check---------------->");
		}
		finally
		{
			try
			{
				resultSet.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				testConfig.logException(e);
			}
		}

		if (null == resultMap)
			testConfig.logWarning("No data was returned for this query");

		Date endDate = new Date();
		double timeDifference = (endDate.getTime() - startDate.getTime()) / 1000.00;

		if(timeDifference > 60)
			testConfig.logComment("<B>Time taken to run this query in minutes : " + timeDifference/60 + "</B>");
		//else
		//testConfig.logComment("Time taken to run this query in seconds : " + timeDifference);

		return resultMap;
	}

	/**
	 * Executes the select db query, and saves the result in
	 * Config.runtimeProperties as well as returns Map
	 * 
	 * @param Config
	 *            test config instance
	 * @param sqlRow
	 *            row number of the 'Query' column of 'SQL' sheet of Test data
	 *            excel having the query to be executed
	 * @param rowNumber
	 *            row number to be returned (use 1 for first row and -1 for last
	 *            row)
	 * @return Map containing key:value pairs of specified row
	 */
	public static int executeUpdateQuery(Config testConfig, int sqlRow, DatabaseType dbType)
	{		
		// Read the Query column of SQL sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject("SQL");
		String updateQuery = sqlData.GetData(sqlRow, "Query");

		return executeUpdateQuery(testConfig, updateQuery, dbType);
	}
	
	/**
	 * 
	 * @param testConfig
	 * @param sqlRow
	 * @param dbType
	 * @return
	 */
	public static int executeUpdateQuery(Config testConfig,  String sheetPath, int sqlRow, DatabaseType dbType)
	{		
		// Read the Query column of SQL sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject("SQL",sheetPath);
		String updateQuery = sqlData.GetData(sqlRow, "Query");

		return executeUpdateQuery(testConfig, updateQuery, dbType);
	}

	/**
	 * Executes the update db query
	 * 
	 * @param Config
	 *            test config instance
	 * @param updateQuery
	 *            query to be executed
	 * @return number of rows affected
	 */
	public static int executeUpdateQuery(Config testConfig, String updateQuery, DatabaseType dbType)
	{
		Date startDate = new Date();

		Statement stmt = null;
		int rows = 0;
		try
		{
			stmt = getConnection(testConfig, dbType).createStatement();
			updateQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, updateQuery);

			if(testConfig.getRunTimeProperty("replaceNULLInQuery") != null && testConfig.getRunTimeProperty("replaceNULLInQuery").equalsIgnoreCase("true"))
			{
				if(updateQuery.contains("'(null)'") || updateQuery.contains("'(NULL)'") || updateQuery.contains("'null'") || updateQuery.contains("'NULL'"))
				{
					updateQuery = updateQuery.replace("'(null)'", "NULL").replace("'(NULL)'", "NULL").replace("'null'", "NULL").replace("'NULL'", "NULL");
				}
			}

			Log.Comment("\nExecuting the update query - '" + updateQuery + "'", testConfig);
			rows = stmt.executeUpdate(updateQuery);
		}
		catch (SQLException e)
		{
			testConfig.logException(e);
		}
		finally
		{
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException e)
				{
					testConfig.logException(e);
				}
			}
		}
		if (0 == rows)
			testConfig.logWarning("No rows were updated by this query");

		Date endDate = new Date();
		double timeDifference = (endDate.getTime() - startDate.getTime()) / 1000.00;

		if(timeDifference > 60)
			testConfig.logComment("<B>Time taken to run this query in minutes : " + timeDifference/60 + "</B>");
		//else
		//testConfig.logComment("Time taken to run this query in seconds : " + timeDifference);

		return rows;
	}
	/**
	 * Execute Query From Given Sheet
	 * @param testConfig
	 * @param sqlRow
	 * @param dbType
	 * @param sheetname
	 * @return
	 */
	public static ResultSet executeSelectQuery(Config testConfig, int sqlRow, DatabaseType dbType,String sheetname)
	{
		// Read the Query column of SQL sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject(sheetname);
		String selectQuery = sqlData.GetData(sqlRow, "Query");
		selectQuery = Helper.replaceArgumentsWithRunTimeProperties(testConfig, selectQuery);
		Log.Comment("Executing the query - '" + selectQuery + "'", testConfig);
		return executeSelectQuery(testConfig, selectQuery, dbType);
	}

	/**
	 * Creates database connection using the Config parameters -
	 * 'DBConnectionString', 'DBConnectionUsername' and 'DBConnectionPassword'
	 * 
	 * @param Config
	 *            test config instance
	 * @return Db Connection
	 */
	private static Connection getConnection(Config testConfig, DatabaseType dbType)
	{
		Connection con = null;
		String connectString = null;
		String userName = null;
		String password = null;
		try
		{
			switch (dbType)
			{
			case Online:
				connectString = testConfig.getRunTimeProperty("OnlineDBConnectionString");
				testConfig.logComment("Connecting to ONLINE db:-" + connectString);
				break;
			case Offline:
				connectString = testConfig.getRunTimeProperty("OfflineDBConnectionString");
				testConfig.logComment("Connecting to OFFLINE db:-" + connectString);
				break;
			case PayuMoneyAutomation:
				connectString = testConfig.getRunTimeProperty("PayuMoneyAutomationDBConnectionString");
				break;
			case PerformanceDB:
				connectString = "jdbc:mysql://10.100.93.245/performancedb";
				testConfig.putRunTimeProperty("PerformanceDBConnectionUsername", "RootUser");
				testConfig.putRunTimeProperty("PerformanceDBConnectionPassword", "RootUser");
				break;
			case AccuracyDB:
				connectString = "jdbc:mysql://10.100.93.245/accuracy";
				testConfig.putRunTimeProperty("AccuracyDBConnectionUsername", "accuracy");
				testConfig.putRunTimeProperty("AccuracyDBConnectionPassword", "accuracy");
				break;
			case MoneyAuthDB:
				connectString = testConfig.getRunTimeProperty("PaisaDBConnectionString");
				break;
			case MoneyVaultDB:
				connectString = testConfig.getRunTimeProperty("PaisaVaultDBConnectionString");
				break;
			case MoneyCmsDB:
				connectString = testConfig.getRunTimeProperty("PaisaCMSDBConnectionString");
				break;
			case MoneyAsyncDB:
				connectString = testConfig.getRunTimeProperty("PaisaAsyncDBConnectionString");
				break;
			case MoneyWebServiceDB:
				connectString = testConfig.getRunTimeProperty("PaisaWebServiceDBConnectionString");
				break;
			case MoneyAnalyticsDataDB:
				connectString = testConfig.getRunTimeProperty("PaisaAnalyticsDataDBConnectionString");
				break;
			case OfflineCrypto:
				connectString = testConfig.getRunTimeProperty("OfflineDBConnectionStringForCryptoDB");
				testConfig.logComment("Connecting to ONLINE db:-" + connectString);
				break;
			case AggregatedDB:
				connectString = testConfig.getRunTimeProperty("AggregatedDBConnectionString");
				break;
			case PayuBizWebsiteDb:
				connectString = testConfig.getRunTimeProperty("PayuBizWebSiteDBConnectionString");
				break;
			case KVaultDB:
				connectString = testConfig.getRunTimeProperty("KVaultDBConnectionString");
				break;
			case ACSAutomationDB:
				connectString = testConfig.getRunTimeProperty("ACSDBAutomationConnectionString");
				break;
			case ACSDB:
				connectString = testConfig.getRunTimeProperty("ACSDBConnectionString");
				break;
			case POSInvoice:
				connectString = testConfig.getRunTimeProperty("InvoiceDBConnectionString");
				break;
			case CredCreditCardDB:
				connectString = testConfig.getRunTimeProperty("CreditCardDBConnectionString");
				break;
			case CREDOnBoardingDB:
				connectString = testConfig.getRunTimeProperty("CredOnBoardingDBConnectionString");
				break;
			case CredAutomationDB:
				connectString = testConfig.getRunTimeProperty("CredDBAutomationConnectionString");
				break;
			case CredCibilDB:
				connectString = testConfig.getRunTimeProperty("CredCibilDBConnectionString");
				break;
			case RupayDB:
				connectString = testConfig.getRunTimeProperty("RupayDBConnectionString");
				break;	
			case ACSAuth:
				connectString = testConfig.getRunTimeProperty("ACSAuthDBConnectionString");
				break;
			case ACSAuthDashboard:
				connectString = testConfig.getRunTimeProperty("ACSDashboardDBConnectionString");
				break;
			case OptimusCredLMSDB:
				connectString = testConfig.getRunTimeProperty("OptimusCredLMSDBConnectionString");
				break;
			case OptimusCRMSDB:
				connectString = testConfig.getRunTimeProperty("OptimusCRMConnectionString");
				break;

			default:
				break;
			}

			if (dbType == DatabaseType.MoneyAuthDB || dbType == DatabaseType.MoneyVaultDB || dbType == DatabaseType.MoneyCmsDB 
					|| dbType == DatabaseType.MoneyAsyncDB || dbType == DatabaseType.MoneyWebServiceDB || dbType == DatabaseType.MoneyAnalyticsDataDB)
			{
				userName = testConfig.getRunTimeProperty("PaisaDBConnectionUsername");
				password = testConfig.getRunTimeProperty("PaisaDBConnectionPassword");

				switch (dbType)
				{
				case MoneyAuthDB:
					if (testConfig.MoneyAuthDbConnection != null && !testConfig.MoneyAuthDbConnection.isClosed())
						return testConfig.MoneyAuthDbConnection;
					testConfig.logComment("Connecting to Money db:-" + connectString);
					connectString = testConfig.getRunTimeProperty("PaisaDBConnectionString");
					testConfig.MoneyAuthDbConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.MoneyAuthDbConnection;
					break;

				case MoneyVaultDB:
					if (testConfig.MoneyVaultDbConnection != null  && !testConfig.MoneyVaultDbConnection.isClosed())
						return testConfig.MoneyVaultDbConnection;
					testConfig.logComment("Connecting to Money db:-" + connectString);
					connectString = testConfig.getRunTimeProperty("PaisaVaultDBConnectionString");
					testConfig.MoneyVaultDbConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.MoneyVaultDbConnection;
					break;

				case MoneyCmsDB:
					if (testConfig.MoneyCmsDbConnection != null && !testConfig.MoneyCmsDbConnection.isClosed())
						return testConfig.MoneyCmsDbConnection;
					testConfig.logComment("Connecting to Money db:-" + connectString);
					connectString = testConfig.getRunTimeProperty("PaisaCMSDBConnectionString");
					testConfig.MoneyCmsDbConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.MoneyCmsDbConnection;
					break;
				case MoneyAsyncDB:
					if (testConfig.MoneyAsyncDbConnection != null && !testConfig.MoneyAsyncDbConnection.isClosed())
						return testConfig.MoneyAsyncDbConnection;
					testConfig.logComment("Connecting to Money db:-" + connectString);
					connectString = testConfig.getRunTimeProperty("PaisaAsyncDBConnectionString");
					testConfig.MoneyAsyncDbConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.MoneyAsyncDbConnection;
					break;
				case MoneyWebServiceDB:
					if (testConfig.MoneyWebServiceDbConnection != null && !testConfig.MoneyWebServiceDbConnection.isClosed())
						return testConfig.MoneyWebServiceDbConnection;
					testConfig.logComment("Connecting to Money db:-" + connectString);
					connectString = testConfig.getRunTimeProperty("PaisaWebServiceDBConnectionString");
					testConfig.MoneyWebServiceDbConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.MoneyWebServiceDbConnection;
					break;
				case MoneyAnalyticsDataDB:
					if (testConfig.MoneyAnalyticsDataDBConnection != null && !testConfig.MoneyAnalyticsDataDBConnection.isClosed())
						return testConfig.MoneyAnalyticsDataDBConnection;
					testConfig.logComment("Connecting to MoneyAnalyticsData db:-" + connectString);
					connectString = testConfig.getRunTimeProperty("PaisaAnalyticsDataDBConnectionString");
					testConfig.MoneyAnalyticsDataDBConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.MoneyAnalyticsDataDBConnection;
					break;
				case Offline:
					break;
				case Online:
					break;
				case PayuMoneyAutomation:
					break;
				case OfflineCrypto:
					break;
				default:
					break;
				}
				if (testConfig.debugMode)
					testConfig.logComment("Connection succeeded");
			}

			else if(dbType == DatabaseType.ACSDB || dbType == DatabaseType.ACSAuth || dbType == DatabaseType.ACSAuthDashboard 
					|| dbType == DatabaseType.RupayDB){
				userName = testConfig.getRunTimeProperty("ACSDBConnectionUsername");
				password = testConfig.getRunTimeProperty("ACSDBConnectionPassword");

				switch (dbType)
				{
				case ACSDB:
					if (testConfig.ACSDBConnection != null && !testConfig.ACSDBConnection.isClosed())
						return testConfig.ACSDBConnection;
					testConfig.logComment("Connecting to ACS db:-" + connectString);
					testConfig.ACSDBConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.ACSDBConnection;
					break;
				case ACSAuth:
					if (testConfig.ACSAuthDBConnection != null && !testConfig.ACSAuthDBConnection.isClosed())
						return testConfig.ACSAuthDBConnection;
					testConfig.logComment("Connecting to ACS Auth db:-" + connectString);
					testConfig.ACSAuthDBConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.ACSAuthDBConnection;
					break;
				case ACSAuthDashboard:
					if (testConfig.ACSAuthDashBoardConnection != null && !testConfig.ACSAuthDashBoardConnection.isClosed())
						return testConfig.ACSAuthDashBoardConnection;
					testConfig.logComment("Connecting to ACS Dashboard db:-" + connectString);
					testConfig.ACSAuthDashBoardConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.ACSAuthDashBoardConnection;
					break;
				case RupayDB:
					if (testConfig.RupayDBConnection != null && !testConfig.RupayDBConnection.isClosed())
						return testConfig.RupayDBConnection;
					testConfig.logComment("Connecting to ACS Rupay db:-" + connectString);
					testConfig.RupayDBConnection = DriverManager.getConnection(connectString, userName, password);
					con = testConfig.RupayDBConnection;
					break;
				default:
					testConfig.logFail("No such db exists in ACS");
					break;
				}
				if (testConfig.debugMode)
					testConfig.logComment("Connection succeeded");
			}
			else
				if (dbType == DatabaseType.PayuMoneyAutomation)
				{
					if (testConfig.MoneyUserMgmtConnection != null && !testConfig.MoneyUserMgmtConnection.isClosed())
						return testConfig.MoneyUserMgmtConnection;
					userName = testConfig.getRunTimeProperty("PayuMoneyAutomationDBConnectionUsername");
					password = testConfig.getRunTimeProperty("PayuMoneyAutomationDBConnectionPassword");
					if ((testConfig.connection != testConfig.MoneyUserMgmtConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to PayuMoney Automation db:-" + connectString);
						testConfig.MoneyUserMgmtConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.MoneyUserMgmtConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}
				else if (dbType == DatabaseType.PerformanceDB)
				{
					if (testConfig.PerformanceDBConnection != null && !testConfig.PerformanceDBConnection.isClosed())
						return testConfig.PerformanceDBConnection;
					userName = testConfig.getRunTimeProperty("PerformanceDBConnectionUsername");
					password = testConfig.getRunTimeProperty("PerformanceDBConnectionPassword");
					if ((testConfig.connection != testConfig.PerformanceDBConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to Performance DB:-" + connectString);
						testConfig.PerformanceDBConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.PerformanceDBConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}
				else if (dbType == DatabaseType.AccuracyDB)
				{
					if (testConfig.AccuracyDBConnection != null && !testConfig.AccuracyDBConnection.isClosed() )
						return testConfig.AccuracyDBConnection;
					userName = testConfig.getRunTimeProperty("AccuracyDBConnectionUsername");
					password = testConfig.getRunTimeProperty("AccuracyDBConnectionPassword");
					if ((testConfig.connection != testConfig.AccuracyDBConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to Accuracy DB:-" + connectString);
						testConfig.AccuracyDBConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.AccuracyDBConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}
				else if (dbType == DatabaseType.KVaultDB)
				{
					if (testConfig.KVaultDBConnection != null && !testConfig.KVaultDBConnection.isClosed())
						return testConfig.KVaultDBConnection;

					userName = testConfig.getRunTimeProperty("KVaultDBConnectionUsername");
					password = testConfig.getRunTimeProperty("KVaultDBConnectionPassword");
					if ((testConfig.connection != testConfig.KVaultDBConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to KVault db:-" + connectString);
						testConfig.KVaultDBConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.KVaultDBConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}
				else if (dbType == DatabaseType.ACSAutomationDB)
				{
					if (testConfig.ACSDBAutomationConnection != null && !testConfig.ACSDBAutomationConnection.isClosed())
						return testConfig.ACSDBAutomationConnection;
					userName = testConfig.getRunTimeProperty("ACSDBAutomationConnectionUsername");
					password = testConfig.getRunTimeProperty("ACSDBAutomationConnectionPassword");
					if ((testConfig.connection != testConfig.ACSDBAutomationConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to Automation ACS db:-" + connectString);
						testConfig.ACSDBAutomationConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.ACSDBAutomationConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}

				else if (dbType == DatabaseType.CREDOnBoardingDB)
				{
					if (testConfig.CredOnBoardingDBConnection != null && !testConfig.CredOnBoardingDBConnection.isClosed())
						return testConfig.CredOnBoardingDBConnection;
					userName = testConfig.getRunTimeProperty("CredDBConnectionUsername");
					password = testConfig.getRunTimeProperty("CredDBConnectionPassword");
					if ((testConfig.connection != testConfig.CredOnBoardingDBConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to Cred On Boarding db:-" + connectString);
						testConfig.CredOnBoardingDBConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.CredOnBoardingDBConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}
				else if (dbType == DatabaseType.CredCreditCardDB)
				{
					if (testConfig.CredDBAutomationConnection != null && !testConfig.CredDBAutomationConnection.isClosed())
						return testConfig.CredDBAutomationConnection;
					userName = testConfig.getRunTimeProperty("CredDBConnectionUsername");
					password = testConfig.getRunTimeProperty("CredDBConnectionPassword");
					if ((testConfig.connection != testConfig.CredDBAutomationConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to Automation CreditCard db:-" + connectString);
						testConfig.CredDBAutomationConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.CredDBAutomationConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}
				else if (dbType == DatabaseType.CredAutomationDB)
				{
					if (testConfig.CredAutomationDBConnection != null && !testConfig.CredAutomationDBConnection.isClosed())
						return testConfig.CredAutomationDBConnection;
					userName = testConfig.getRunTimeProperty("CredDBAutomationConnectionUsername");
					password = testConfig.getRunTimeProperty("CredDBAutomationConnectionPassword");
					if ((testConfig.connection != testConfig.CredAutomationDBConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to Cred Automation db:-" + connectString);
						testConfig.CredAutomationDBConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.CredAutomationDBConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}

				else if (dbType == DatabaseType.CredCibilDB)
				{
					if (testConfig.CredCibilDBConnection != null && !testConfig.CredCibilDBConnection.isClosed())
						return testConfig.CredCibilDBConnection;
					userName = testConfig.getRunTimeProperty("CredCibilDBConnectionUsername");
					password = testConfig.getRunTimeProperty("CredCibilDBConnectionPassword");
					if ((testConfig.connection != testConfig.CredCibilDBConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to Cred CIbil db:-" + connectString);
						testConfig.CredCibilDBConnection = DriverManager.getConnection(connectString, userName, password);
						con = testConfig.CredCibilDBConnection;
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}

				else if(dbType == DatabaseType.OptimusCredLMSDB)
				{
					if (testConfig.OptimusCredLMSDBConnection != null && !testConfig.OptimusCredLMSDBConnection.isClosed())
						return testConfig.OptimusCredLMSDBConnection;
					userName = testConfig.getRunTimeProperty("OptimusCredLMSDBConnectionUsername");
					password = testConfig.getRunTimeProperty("OptimusCredLMSDBConnectionPassword");
					if ((testConfig.connection != testConfig.OptimusCredLMSDBConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
					{
						testConfig.logComment("Connecting to Cred LMS DB:-" + connectString);

						System.out.println(connectString+"connectString");
						System.out.println(userName+"userName");
						System.out.println(password+"password");
						try{
							testConfig.OptimusCredLMSDBConnection = DriverManager.getConnection(connectString, userName, password);
							con = testConfig.OptimusCredLMSDBConnection;
						}
						catch (Exception e)
						{
							System.out.println(e.getMessage());
						}
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
						}
					}
							
				else if(dbType == DatabaseType.OptimusCRMSDB)
				{
				if (testConfig.OptimusCRMDBConnection != null && !testConfig.OptimusCRMDBConnection.isClosed())
				return testConfig.OptimusCRMDBConnection;
				userName = testConfig.getRunTimeProperty("OptimusCredLMSDBConnectionUsername");
				password = testConfig.getRunTimeProperty("OptimusCredLMSDBConnectionPassword");
				if ((testConfig.connection != testConfig.OptimusCRMDBConnection) || (testConfig.connection == null || testConfig.connection.isClosed()))
				{
				testConfig.logComment("Connecting to Cred LMS DB:-" + connectString);
				try{


				testConfig.OptimusCRMDBConnection = DriverManager.getConnection(connectString, userName, password);
				con = testConfig.OptimusCRMDBConnection;

						}
						catch (Exception e)
						{
							System.out.println(e.getMessage());
						}
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
				}

				else
				{
					userName = testConfig.getRunTimeProperty("DBConnectionUsername");
					password = testConfig.getRunTimeProperty("DBConnectionPassword");

					con = DriverManager.getConnection(connectString, userName, password);
					if (con != null)
					{
						if (testConfig.debugMode)
							testConfig.logComment("Connection succeeded");
					}
					else
						testConfig.logFail("Unable to establish connection");
				}

			try
			{
				Class.forName(testConfig.getRunTimeProperty("DBConnectionDriver"));
			}
			catch (ClassNotFoundException e)
			{
				con = null;
				testConfig.logException(e);
			}
		}
		catch (SQLException e)
		{
			testConfig.logException(e);
		}

		testConfig.connection = con;
		return testConfig.connection;
	}
	/**
	 * Executes detele query in DB
	 * @param testConfig : test config instance
	 * @param sqlRow : row number of sql query in excel
	 * @param dbType : type of DB
	 * @return 
	 */
	public static int executeDeleteQuery(Config testConfig, int sqlRow, DatabaseType dbType)
	{		
		// Read the Query column of SQL sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject("SQL");
		String deleteQuery = sqlData.GetData(sqlRow, "Query");

		return executeUpdateQuery(testConfig, deleteQuery, dbType);
	}

	/**
	 * This method converts resultset to list
	 * 
	 * @param resultset
	 *            SQL resultSet
	 * @return sql data in list<hashmap<string,string>
	 */
	public static List<HashMap<String, String>> convertResultSetToList(Config testConfig, ResultSet rs)
	{
		List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

		try
		{
			ResultSetMetaData md = rs.getMetaData();
			int columns = md.getColumnCount();

			while (rs.next())
			{
				HashMap<String, String> row = new HashMap<String, String>(columns);
				for (int i = 1; i <= columns; ++i)
				{
					row.put(md.getColumnLabel(i), rs.getString(i));
				}
				list.add(row);
			}
		}
		catch(SQLException e)
		{
			testConfig.logComment(e.getMessage());
		}

		return list;
	}

	/**
	 * This method is used to run a query on a provided DB with given connection string, username and password.
	 * @param query
	 * @param connectString
	 * @param userName
	 * @param password
	 * @return
	 */
	public static ResultSet executeQueryWithoutClosingConnection(String query, String connectionString, String username, String password)
	{
		Date startDate = new Date();
		Statement stmt = null;
		ResultSet resultSet = null;
		try
		{
			connection = DriverManager.getConnection(connectionString, username, password);
			stmt = connection.createStatement();
			resultSet = stmt.executeQuery(query);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		if (null == resultSet)
			System.out.println("No data was returned for this query");

		Date endDate = new Date();
		double timeDifference = (endDate.getTime() - startDate.getTime()) / 1000.00;

		if(timeDifference > 60)
			System.out.println("<B>Time taken to run this query in minutes : " + timeDifference/60 + "</B>");
		else
			System.out.println("Time taken to run this query in seconds : " + timeDifference);
		return resultSet;
	}

	/**
	 * Close the database connection, if open.
	 */
	public static void closeDatabaseConnection()
	{
		if(connection != null)
		{
			try
			{
				connection.close();
				connection = null;
				System.out.println("Database connection closed successfully.");
			}
			catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Executes insert query in DB
	 * @param testConfig 	test config instance
	 * @param sqlRow 		row number of sql query in excel
	 * @param dbType		type of DB
	 * @return
	 */
	public static int executeInsertQuery(Config testConfig, int sqlRow, DatabaseType dbType)
	{
		// Read the Query column of SQL sheet of Test data excel
		TestDataReader sqlData = testConfig.getCachedTestDataReaderObject("SQL");
		String insertQuery = sqlData.GetData(sqlRow, "Query");

		return executeUpdateQuery(testConfig, insertQuery, dbType);
	}
}