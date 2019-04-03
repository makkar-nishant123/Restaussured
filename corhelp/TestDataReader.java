package Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author nitin
 */
public class TestDataReader
{
	
	String filename;
	
	private FileOutputStream fileOut = null;
	
	private FileInputStream fis = null;
	
	String path;
	private String sheetName;
	private Config testConfig;
	
	private ArrayList<List<String>> testData;
	
	public TestDataReader(Config testConfig)
	{
		this.testConfig = testConfig;
	}
	
	TestDataReader(Config testConfig, String sheetName)
	{
		String path = testConfig.getRunTimeProperty("TestDataSheet");
		readFile(testConfig, sheetName, path);
	}
	
	protected TestDataReader(Config testConfig, String sheetName, String path)
	{
		readFile(testConfig, sheetName, path);
	}
	
	private String convertHSSFCellToString(HSSFCell cell, FormulaEvaluator evaluator)
	{
		String value = null;
		try
		{
			if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
			{
				value = Double.toString(cell.getNumericCellValue());
			}
			else
				if (cell.getCellType() == Cell.CELL_TYPE_STRING)
				{
					value = cell.getRichStringCellValue().toString();
				}
				else
					if (cell.getCellType() == Cell.CELL_TYPE_FORMULA)
					{
						HSSFDataFormatter formatter = new HSSFDataFormatter();
						value = formatter.formatCellValue(cell, evaluator); 
					}
					else
						if (cell.getCellType() == Cell.CELL_TYPE_ERROR)
						{
							value = "";
						}
						else
							if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN)
							{
								value = Boolean.toString(cell.getBooleanCellValue());
							}
							else
								if (cell.getCellType() == Cell.CELL_TYPE_BLANK)
								{
									value = "";
								}
			
		}
		catch (NullPointerException ex)
		{
			value = "";
		}
		return value;
	}
	
	private String convertXSSFCellToString(XSSFCell cell)
	{
		String value = null;
		try
		{
			if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC)
			{
				value = Double.toString(cell.getNumericCellValue());
			}
			else
				if (cell.getCellType() == Cell.CELL_TYPE_STRING)
				{
					value = cell.getRichStringCellValue().toString();
				}
				else
					if (cell.getCellType() == Cell.CELL_TYPE_ERROR)
					{
						value = "";
					}
					else
						if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN)
						{
							value = Boolean.toString(cell.getBooleanCellValue());
						}
						else
							if (cell.getCellType() == Cell.CELL_TYPE_BLANK)
							{
								value = "";
							}
			
		}
		catch (NullPointerException ex)
		{
			value = "";
		}
		return value;
	}
	
	public String GetcolumnData(String column, String value, String path, int columnNum, Boolean newPennyFlow)
	{
		String data = "";
		column = column.trim();
		List<String> headerRow = testData.get(0);
		HSSFWorkbook workbook = null;
		HSSFSheet sheet = null;
		filename = path;
		
		try
		{
			fis = new FileInputStream(filename);
			workbook = new HSSFWorkbook(fis);
			sheet = workbook.getSheet(sheetName);
			/*
			 * while (workbook.) { XSSFRow row = (XSSFRow) rows.next(); List
			 * <String> data = new ArrayList<String>(); for(int
			 * z=0;z<row.getLastCellNum();z++) { String str
			 * =convertXSSFCellToString((XSSFCell)row.getCell(z));
			 * data.add(str); } testData.add(data); }
			 */
			
			if (headerRow.get(columnNum).equals(column))
			{
				
				int rowNum = 0;
				
				int totalRows = getRecordsNum();
				if (newPennyFlow)
				{
					totalRows = totalRows - 1;
				}
				System.out.println(totalRows);
				for (rowNum = totalRows; rowNum >= 1; --rowNum)
				{
					
					data = GetData(rowNum, column);
					if (data.equals(value))
					{
						data = GetData(rowNum, column);
						String row = String.valueOf(rowNum);
						testConfig.putRunTimeProperty("Row", row);
						testConfig.logPass(column + " contains the value " + value);
						break;
					}
				}
			}
			
		}
		catch (FileNotFoundException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return data;
	}
	
	/**
	 * Gets the data value in column=correspondingColumnToRead for the row where
	 * value=inputColumnValue and column=inputColumn
	 * 
	 * @param inputColumn
	 *            Column name corresponding to which value is to be read
	 * @param inputColumnValue
	 *            Column value for which value is to be read
	 * @param correspondingColumnToRead
	 *            Column name whose value is required
	 * @return corresponding column value
	 */
	public String GetCorrespondingColumnValue(String inputColumn, String inputColumnValue, String correspondingColumnToRead)
	{
		String correspondingColumnValue = "";
		
		List<String> headerRow = testData.get(0);
		for (int col = 0; col < headerRow.size(); col++)
		{
			if (headerRow.get(col).equalsIgnoreCase(inputColumn))
			{
				try
				{
					int row = 1;
					List<String> rowToRead = null;
					String inputval = null;
					while (true)
					{
						rowToRead = testData.get(row);
						inputval = rowToRead.get(col);
						if (inputval.equalsIgnoreCase(inputColumnValue))
						{
							correspondingColumnValue = GetData(row, correspondingColumnToRead);
							return correspondingColumnValue;
						}
						row++;
					}
					
				}
				catch (Exception e)
				{
					return "";
				}
			}
		}
		
		return correspondingColumnValue;
	}
	
	/**
	 * Returns the Excel sheet data value. It will get the current environment
	 * and read the value in column 'column-<environment>', if it blank then it
	 * will return value in 'column' (returns {skip} if the excel value is
	 * blank, which means no operation)
	 * 
	 * @param row
	 *            Excel Row number to read
	 * @param column
	 *            Excel column name to read
	 * @return The value read
	 */
	public String GetCurrentEnvironmentData(int row, String column)
	{
		String env = testConfig.getRunTimeProperty("Environment");
		String value = GetData(row, column + "-" + env);
		if (value.equalsIgnoreCase("{skip}"))
		{
			value = GetData(row, column);
		}
		return value;
	}
	
	/**
	 * Returns the Excel sheet data value (returns {skip} if the excel value is
	 * blank, which means no operation)
	 * 
	 * @param row
	 *            Excel Row number to read
	 * @param column
	 *            Excel column name to read
	 * @return The value read
	 */
	public String GetData(int row, String column){
		return GetData(row, column, true);
	}
	public String GetData(int row, String column, boolean isTrim)
	{
		String data = "";
		List<String> headerRow = testData.get(0);
		List<String> dataRow = testData.get(row);
		
		for (int i = 0; i < headerRow.size(); i++)
		{
			if (headerRow.get(i).equalsIgnoreCase(column))
			{
				try
				{
					data = dataRow.get(i);
				}
				catch (IndexOutOfBoundsException e)
				{
					data = "";
				}
				break;
			}
		}
		
		if(isTrim)
			data = data.trim();
		
		if (data.equals(""))
		{
			data = "{skip}";
			return data;
		}
		else
		{
			if (data.contains("{empty}"))
				data = data.replace("{empty}", "");
			if (data.contains("{space}"))
				data = data.replace("{space}", " ");
			
			while (data.contains("{random"))
			{
				int start = data.indexOf("Num:") + 4;
				int end = data.indexOf("}");
				int length = Integer.parseInt(data.substring(start, end));
				
				if (data.contains("{randomAlphaNum:" + length + "}"))
					data = data.replace("{randomAlphaNum:" + length + "}", Helper.generateRandomAlphaNumericString(length));
				if (data.contains("{randomAlphabetsNum:" + length + "}"))
					data = data.replace("{randomAlphabetsNum:" + length + "}", Helper.generateRandomAlphabetsString(length));
				if (data.contains("{randomNum:" + length + "}"))
					data = data.replace("{randomNum:" + length + "}", Long.toString(Helper.generateRandomNumber(length)));
			}
		}
		
		if (testConfig.debugMode)
			Log.Comment("Reading '" + sheetName + "' row-" + row + " column-" + column + " value:-'" + data + "'", testConfig);
		return data;
	}
	
	/**
	 * Returns the Excel header value
	 * 
	 * @param row
	 *            Excel Row number to read
	 * @param column
	 *            Excel column name to read
	 * @return The value read
	 */
	public String GetHeaderData(int i)
	{
		String data = "";
		List<String> headerRow = testData.get(0);
		List<String> dataRow = testData.get(0);
		
		try
		{
			data = dataRow.get(i);
		}
		catch (IndexOutOfBoundsException e)
		{
			data = "";
		}
		
		data = data.trim();

		if (data.equals(""))
		{
			data = "{skip}";
			return data;
		}
		else
		{
			if (data.contains("{empty}"))
				data = data.replace("{empty}", "");
			if (data.contains("{space}"))
				data = data.replace("{space}", " ");
			
			if (data.contains("{random"))
			{
				int start = data.indexOf("Num:") + 4;
				int end = data.indexOf("}");
				int length = Integer.parseInt(data.substring(start, end));
				
				if (data.contains("{randomAlphaNum:" + length + "}"))
					data = data.replace("{randomAlphaNum:" + length + "}", Helper.generateRandomAlphaNumericString(length));
				if (data.contains("{randomNum:" + length + "}"))
					data = data.replace("{randomNum:" + length + "}", Long.toString(Helper.generateRandomNumber(length)));
			}
		}
		
		return data;
	}
	
	/**
	 * This method returns the number of records present in the datasheet
	 * @return number of records
	 */
	public int getRecordsNum()
	{
		return testData.size();
	}
	
	/**
	 * This method returns the number of columns of the datasheet
	 * (It counts the header and returns the number)
	 * @return number of columns
	 */
	public int getColumnNum(){
		List<String> headerRow = testData.get(0);
		return headerRow.size();
	}
	
	/**
	 * This method returns the number of columns of the datasheet except expected values
	 * (It counts the header and returns the number)
	 * @return number of columns
	 */
	public int getColumnNumExceptExpected(){
		List<String> headerRow = testData.get(0);
		List<String> headerRowExceptExpected = new ArrayList<String>();
		for(int i = 0; i < headerRow.size(); i++){
			if(!headerRow.get(i).contains("Expected"))
				headerRowExceptExpected.add(headerRow.get(i));
			else
				if(testConfig.debugMode)
					testConfig.logComment("Skipping the expected column" + headerRow.get(i));
		}
		return headerRowExceptExpected.size();
	}
	
	
	// Ignore number format exception in reading String/Float value from excel
	// if excel cell value is blank and format is not string
	public String ignoreNumberFormatException(String returnType, String value)
	{
		if (value.equalsIgnoreCase("{skip}"))
		{
			switch (returnType)
			{
				case "stringType":
					return "";
					
				case "floatType":
					return "0.0";
					
				default:
					return value;
			}
		}
		else
			return value;
	}
	
	private void readFile(Config testConfig, String sheetName, String path)
	{
		this.testConfig = testConfig;
		int index = path.lastIndexOf("//");
		if (index != -1)
			testConfig.logComment("Read:-'" + path.substring(path.lastIndexOf("//")) + "', Sheet:- '" + sheetName + "'");
		else
			testConfig.logComment("Read:-'" + path + "', Sheet:- '" + sheetName + "'");
		
		filename = path;
		testData = new ArrayList<List<String>>();
		
		try
		{
			if (filename.endsWith(".xls"))
			{
				try
				{
					HSSFWorkbook workbook = null;
					HSSFSheet sheet = null;
					
					fis = new FileInputStream(filename);
					
					try
					{
						workbook = new HSSFWorkbook(fis);
					}
					catch(OutOfMemoryError e)
					{
						//Catching "java.lang.OutOfMemoryError: GC overhead limit exceeded" Exception
						 
						//Print the jvm heap size
						long heapSize = Runtime.getRuntime().totalMemory();
						testConfig.logFail("******************** Heap Size of machine is = " + heapSize + " ********************");
						
						testConfig.logException(e);
						e.printStackTrace();
					}
					
					sheetName = sheetName.trim();
					sheet = workbook.getSheet(sheetName);
					FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
					
					if (sheet == null)
						testConfig.logFail("No sheetName:- " + sheetName + " found.");
					
					
					Iterator<Row> rows = sheet.rowIterator();
					while (rows.hasNext())
					{
						HSSFRow row = (HSSFRow) rows.next();
						List<String> data = new ArrayList<String>();
						for (int z = 0; z < row.getLastCellNum(); z++)
						{
							String str = convertHSSFCellToString(row.getCell(z), evaluator);
							data.add(str);
						}
						testData.add(data);
					}
				}
				catch (IOException e) // Invalid header signature; read
										// 0x6576206C6D783F3C, expected
										// 0xE11AB1A1E011CFD0
				// because the exported xls file is actually a Open XML file
				{
					try
					{
						File fXmlFile = new File(filename);
						DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
						DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
						Document doc = dBuilder.parse(fXmlFile);
						
						doc.getDocumentElement().normalize();
						
						NodeList rows = doc.getElementsByTagName("Row");
						
						for (int temp = 0; temp < rows.getLength(); temp++)
						{
							Node row = rows.item(temp);
							List<String> data = new ArrayList<String>();
							NodeList cols = row.getChildNodes();
							for (int z = 0; z < cols.getLength(); z++)
							{
								Node col = cols.item(z);
								String str = col.getTextContent();
								data.add(str);
							}
							testData.add(data);
						}
					}
					catch (Exception e1)
					{
						testConfig.logException(e1);
					}
				}
				this.sheetName = sheetName;
			}
			else
				if (filename.endsWith(".xlsx"))
				{
					XSSFWorkbook workbook = null;
					XSSFSheet sheet = null;
					
					fis = new FileInputStream(filename);
					
					workbook = new XSSFWorkbook(fis);
					sheet = workbook.getSheet(sheetName);
					Iterator<Row> rows = sheet.rowIterator();
					while (rows.hasNext())
					{
						XSSFRow row = (XSSFRow) rows.next();
						List<String> data = new ArrayList<String>();
						for (int z = 0; z < row.getLastCellNum(); z++)
						{
							String str = convertXSSFCellToString(row.getCell(z));
							data.add(str);
						}
						testData.add(data);
					}
					this.sheetName = sheetName;
				}
				else
					if (filename.endsWith(".csv"))
					{
						
						BufferedReader CSVFile = null;
						String dataRow = null;
						ArrayList<String> datatemp = new ArrayList<String>();
						try
						{
							CSVFile = new BufferedReader(new FileReader(path));
						}
						catch (FileNotFoundException e)
						{
							e.printStackTrace();
						}
						
						try
						{
							dataRow = CSVFile.readLine();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
						
						while (dataRow != null)
						{
							
							String[] dataArray = dataRow.split(",");
							
							List<String> data = new ArrayList<String>();
							for (int z = 0; z < dataArray.length; z++)
							{
								String str = dataArray[z];
								data.add(str);
							}
							testData.add(data);
							
							try
							{
								dataRow = CSVFile.readLine();
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
						try
						{
							CSVFile.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
		}
		catch (FileNotFoundException e)
		{
			testConfig.logException(e);
		}
		catch (IOException e)
		{
			testConfig.logException(e);
		}
		finally
		{
			if (fis != null)
			{
				try
				{
					fis.close();
				}
				catch (IOException e)
				{
					testConfig.logException(e);
				}
			}
		}
	}
	
	/**
	 * Method to read CSV files
	 * 
	 * @param fileName
	 *            CSV file to read
	 * @param searchKey
	 *            to be searched in csv file
	 * @param value
	 *            to be updated or appended
	 */
	
	public String readWriteCSVFile(String fileName, String searchKey, int location, String header, String value)
	{
		String dataRow = null;
		String path = null;
		try
		{
			BufferedReader readCSVFile = null;
			readCSVFile = new BufferedReader(new FileReader(fileName));
			
			// Get the first data row
			dataRow = readCSVFile.readLine();
			String headerRow = dataRow;
			int headerLength = dataRow.split(",").length;
			int dataLength = 0;
			// Initialize an array list
			ArrayList<String> row = new ArrayList<String>();
			
			while (dataRow != null)
			{
		
				if (dataRow.contains(searchKey))
				{
					// Append the data required in the paymentId matched row
					ArrayList<Object> newDataInCSV = new ArrayList<Object>();
					String[] data = dataRow.split(",");
					for (String string : data) {
						newDataInCSV.add(string);
					}
					newDataInCSV.add(location, value);
					if(data.length>13)
						newDataInCSV.remove(13);
					
					dataRow = "";
					String putData = "";
					dataLength = newDataInCSV.size();;
					
					for (int i = newDataInCSV.size() - 1; i >= 0; i--)
					{
						if (i == newDataInCSV.size() - 1)
						{
							putData = newDataInCSV.get(i).toString();
						}
						else
						{
							putData = newDataInCSV.get(i) + ",";
						}
						dataRow = putData + dataRow;
					}
					
					//Adding a comma for couponString
					//Only For Discount Cases
					if(dataLength < headerLength){
						dataRow = dataRow + ",";
					}
					row = new ArrayList<String>();
					row.add(headerRow + "\n" + dataRow);
					
					// Create a csv file
					path = testConfig.downloadPath;
					fileName = "NewFile.csv";
					FileWriter writeCSVFile = new FileWriter(path + fileName, false);
					for (String str : row)
					{
						writeCSVFile.write(str);
						writeCSVFile.write("\n");
						writeCSVFile.flush();
					}
					
					// Close the FileWriter
					writeCSVFile.close();
					break;
					
				}
				else
				{
					// Make the same row for new csv file
					row.add(dataRow);
					dataRow = readCSVFile.readLine();
				}
			}
			// Close the fileReader
			readCSVFile.close();
		}
		catch (Exception e1)
		{
			testConfig.logException(e1);
		}
		return path + fileName;
	}
	
	public void setCellData(int row, String column, String data, String description) throws IOException
	{
		HSSFWorkbook workbook = null;
		HSSFSheet sheet = null;
		HSSFRow dataRow = null;
		HSSFCell cell = null;
		int colNum = -1;
		if (row <= 0)
			testConfig.logFail("Invalid row number:" + row);
		List<String> headerRow = testData.get(0);
		for (int i = 0; i < headerRow.size(); i++)
		{
			if (headerRow.get(i).equalsIgnoreCase(column))
			{
				try
				{
					colNum = i;
					if (colNum == -1)
						testConfig.logFail("Invalid column name:" + column);
					
				}
				catch (IndexOutOfBoundsException e)
				{
					testConfig.logFail(e.getMessage());
				}
				
			}
		}
		fis = new FileInputStream(filename);
		workbook = new HSSFWorkbook(fis);
		sheet = workbook.getSheet(sheetName);
		dataRow = sheet.getRow(row);
		cell = dataRow.getCell(colNum);
		cell.setCellValue(data.trim());
		fileOut = new FileOutputStream(filename);
		workbook.write(fileOut);
		fileOut.close();
		if (testConfig.debugMode)
			testConfig.logComment("Writing " + description + " in the sheet '" + sheetName + "' in the column " + column + " value:-'" + data + "'");
		
	}
	
	/**
	 * Method to read and write CSV files
	 * 
	 * @param fileName
	 *            CSV file to read
	 * @param searchKey
	 *            to be searched in csv file
	 * @param value
	 *            to be updated or appended
	 * @param newFileName
	 *            to write
	 * @throws Exception
	 */
	
	public String writeCSVFile(String value, String newFileName)
	{
		String path = null;
		try
		{
			// Initialize an array list
			ArrayList<String> row = new ArrayList<String>();
			
			row.add(value);
			
			// Create a file
			path = testConfig.downloadPath;
			FileWriter writeCSVFile = new FileWriter(path + newFileName, false);
			for (String str : row)
			{
				writeCSVFile.write(str);
				writeCSVFile.write("\n");
				writeCSVFile.flush();
			}
			
			// Close the FileWriter
			writeCSVFile.close();
		}
		
		catch (Exception e1)
		{
			testConfig.logException(e1);
		}
		return path + newFileName;
	}

}