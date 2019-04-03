package Test.API;

import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import Utils.Config;
import Utils.Config.SheetToBeUsed;
import Utils.Helper;
import Utils.TestDataReader;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

public class BizAPIHelper {

	/**
	 * Enum For Product Web Services
	 *
	 */
	public enum ProductWebServices {
		// Define API and row Numbers here
		 	check_payment(1),
		 	refundTransaction(3), 
			verify_payment(4),
			cancel_refund_transaction(7),
			get_user_cards(10),
			cod_cancel(12),
			cod_verify(13),
			cod_settled(14),
			create_invoice(21),
			save_user_card(23),
			edit_user_card(24),
			delete_user_card(25),
			get_user_cards_hashes(28),
			get_settlement_details(31), 
			getAllRefundsFromTxnIds(32), 
			getPaymentGateways(33),
			refundDateAmount(34), 
			validateCardNumber(35),
			checkNewActiveMerchants(36), 
			refundStatus(37), 
			updateMerUtr(38), 
			offerSyncingFromPayuMoneyToPayu(39),
			getIssuingBankDownBins(40), 
			caseWithPayuidAsChargeBack(41), 
			getIssuingBankStatus(43), 
			eligibleBinsForEMI(44), 
			merchantLoginDetails(46), 
			checkOfferDetails(47),
			check_action_status(48), 
			get_payment_status(49), 
			check_offer_status(50), 
			update_requests(51), 
			getNetbankingStatus(52),
			udf_update(53),
			OPIM_Upload_InvoiceAPI(54),
		 	get_merchant_ibibo_codes(55);

			
		 	
		private int row;

		// Constructor
		private ProductWebServices(final int row) {
			this.row = row;
		}

		// return enum value
		public int getRowNumber() {
			return row;
		};
	}

	public enum APIList{
		CreateReconTemplateRule,
		ModilfyReconTemplateRule,
		deleteReconTemplateRule,
		FileParserAPI,
		getReconFileTemplateRules
	}
	
	public enum APIMethod
	{
		post,get
	}

	/**
	 * Will Execute the API and the return Map<String,String> as response If
	 * there are more than one Matching key in Map than do numbering them
	 * starting with 0
	 * 
	 * @param testConfig
	 * @param webServiceRow
	 * @param key
	 * @param salt
	 * @return
	 * @throws JSONException
	 */
	public static HashMap<String, Object> executeAPIandReturnResponse(Config testConfig, int webServiceRow, String merchantLoginDetailsRow) {
		TestDataReader merchantLoginDetails = testConfig.getCachedTestDataReaderObject("MerchantLoginDetails");
		String key = merchantLoginDetails.GetData(Integer.parseInt(merchantLoginDetailsRow), "key");
		String salt = merchantLoginDetails.GetData(Integer.parseInt(merchantLoginDetailsRow), "salt");
		return (jsonParser(executePayuProductWebService(testConfig, webServiceRow, key, salt)));
	}

	/**
	 * This Function Will execute the WebService Return Count of Json object in
	 * Json array corresponding to specifickey in JSON object return by
	 * WebService API.
	 * 
	 * @param testConfig
	 * @param webServiceRow
	 * @param merchantLoginDetailsRow
	 * @param specifickey
	 *            --key which has value as Json Array
	 * @return
	 * @throws JSONException
	 */
	public static int executeAPIandVerifyArrayObjectCount(Config testConfig, int webServiceRow, String merchantLoginDetailsRow, String specifickey)
			throws JSONException {
		TestDataReader merchantLoginDetails = testConfig.getCachedTestDataReaderObject("MerchantLoginDetails");
		String key = merchantLoginDetails.GetData(Integer.parseInt(merchantLoginDetailsRow), "key");
		String salt = merchantLoginDetails.GetData(Integer.parseInt(merchantLoginDetailsRow), "salt");
		return (jsonArrayCount((executePayuProductWebService(testConfig, webServiceRow, key, salt)), specifickey));
	}
	/**
	 * 
	 * @param testConfig
	 * @param webServiceRow
	 * @param key
	 * @param salt
	 * @return
	 */
	//Method is calling another method for APIs not having files.
	public static Object executePayuProductWebService(Config testConfig, int webServiceRow, String key, String salt) {
	
		return executePayuProductWebService( testConfig,  webServiceRow,  key,  salt , null );
	}

	/**
	 * Will create a request Format for Web Service,Send the Request to Web
	 * Server and return response in JSON format
	 * 
	 * @param testConfig
	 * @param webServiceRow
	 *            WebService Row in WebService Sheet to know which API is going
	 *            to Execute
	 * @param key
	 *            Merchant's Key
	 * @param salt
	 *            Merchant Salt
	 * @return Will Return JSON Object of Response
	 */
	public static Object executePayuProductWebService(Config testConfig, int webServiceRow, String key, String salt ,File file ) {
		// Save the current testDataSheetName (in case code is called from other
		// project, will be rolledback after function exit)
		String testDataSheetName = testConfig.getRunTimeProperty("TestDataSheet");
		testConfig.update_TestDataSheet_value(SheetToBeUsed.Product);

		// Getting Url to send Request
		String baseUrl = testConfig.getRunTimeProperty("PayUWebServiceUrlForApi");

		// Reading WebService Sheet Data
		TestDataReader webServiceData = testConfig.getCachedTestDataReaderObject("WebServices");

		// Getting name of API
		String command = webServiceData.GetData(webServiceRow, "Command");

		// Get Webservice params, replace run Time Arguments With Actual Values
		String excelParameters = webServiceData.GetData(webServiceRow, "Parameters").trim();
		String parameters = Helper.replaceArgumentsWithRunTimeProperties(testConfig, excelParameters);

		String[] params = parameters.split("&");
		String var1 = "";
		try {
			var1 = params[0].split("=")[1];
		} catch (Exception e) {
			// var1 will remain ""
		}
		// Creating String for Calculating Hash
		String postString = key + "|" + command + "|" + var1 + "|" + salt;

		// Calling Hash Generation function
		String hash = Helper.calculateHash(postString);

		// Add request parameters
		
		RequestSpecification reqspec ;
		
		if(file == null) 
			reqspec = given()
						.contentType(ContentType.URLENC);
		else 
			reqspec = given()
						.multiPart(file);
		
		reqspec = reqspec.
					param("key", key).
					param("command", command).
					param("hash", hash);
		
		// append var1, var2, etc.
		for (int i = 0; i < params.length; i++) {
			String arg0 = "";
			String arg1 = "";
			try {
				arg0 = params[i].split("=")[0];
			} catch (Exception e) {
				// key is empty, so move to next parameter
				continue;
			}

			try {
				arg1 = params[i].split("=")[1];
			} catch (Exception e) {
				// arg1 (param value) will remain ""
			}

			reqspec = reqspec.
					param(arg0, arg1);
			
			
		}

		reqspec = reqspec.
					log().all();

		Response response = reqspec.
								when().
									post(baseUrl).
								then().
									log().all().
									statusCode(200).
									extract().response();

		Object returnObj = null;
		String stringResponse = response.asString();
		// Checking for Json format. Assumption if a string contains { in Response then it will be treated as Json String
		if(stringResponse.contains("{") && stringResponse.contains("}"))
		{
				try 
				{
					returnObj = new JSONObject(stringResponse);					
				}
				catch (JSONException e) 
				{
					// Check for OfferSycn API
					String convertString = stringResponse;
					convertString = stringResponse.replaceAll("\\\"", "");
					convertString = convertString.replaceAll("\\\\", "\\\"");
					try 
					{
						returnObj = new JSONObject(convertString);
					}
					catch (JSONException e1) 
					{
						try 
						{
							returnObj = new JSONArray(stringResponse);					
						}
						catch (JSONException e2) 
						{
							e2.printStackTrace();
							return null;
						}
					}
				}
		}
		else
		{
			// If not Json then return String
			returnObj = stringResponse;
		}

		// reset TestDataSheet to original value
		testConfig.putRunTimeProperty("TestDataSheet", testDataSheetName);
		return returnObj;
	}

	/**
	 * This Function Will Excute The Payu product Web Services And Return Object
	 * response
	 * 
	 * @param testConfig
	 * @param webServiceAPI
	 *            Enum for different APIs
	 * @param key
	 *            Merchant Key
	 * @param salt
	 *            Merchant Salt
	 * @param array
	 *            Variable length Array for variable which use in corresponding
	 *            API
	 * @return JSONObject of webService Response
	 */
	public static Object executePayuProductWebService(Config testConfig, ProductWebServices webServiceAPI, String key, String salt, String... parameters) {
		Object obj = null;
		// String response = null;
		switch (webServiceAPI) {
		case verify_payment:
			testConfig.putRunTimeProperty("transactionId", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.verify_payment.getRowNumber(), key, salt);
			break;
		case check_payment:
			testConfig.putRunTimeProperty("mihpayid", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.check_payment.getRowNumber(), key, salt);
			break;
		// Using this just Send UserCredentials (Example:-ra:ra )and cardToken
		case delete_user_card:
			testConfig.putRunTimeProperty("userCredentials", parameters[0]);
			testConfig.putRunTimeProperty("cardToken", parameters[1]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.delete_user_card.getRowNumber(), key, salt);
			break;
		case save_user_card:
			testConfig.putRunTimeProperty("userCredentials", parameters[0]);
			testConfig.putRunTimeProperty("cardName", parameters[1]);
			testConfig.putRunTimeProperty("cardMode", parameters[2]);
			testConfig.putRunTimeProperty("cardType", parameters[3]);
			testConfig.putRunTimeProperty("nameOnCard", parameters[4]);
			testConfig.putRunTimeProperty("cardNo", parameters[5]);
			testConfig.putRunTimeProperty("cardExpMon", parameters[6]);
			testConfig.putRunTimeProperty("cardExpYr", parameters[7]);
			obj=executePayuProductWebService(testConfig, ProductWebServices.save_user_card.getRowNumber(), key, salt);
		 	break;
		case edit_user_card:
 			testConfig.putRunTimeProperty("userCredentials", parameters[0]);
 			testConfig.putRunTimeProperty("cardName", parameters[1]);
			testConfig.putRunTimeProperty("cardMode", parameters[2]);
 			testConfig.putRunTimeProperty("cardType", parameters[3]);
 			testConfig.putRunTimeProperty("nameOnCard", parameters[4]);
 			testConfig.putRunTimeProperty("cardNo", parameters[5]);
 			testConfig.putRunTimeProperty("cardExpMon", parameters[6]);
 			testConfig.putRunTimeProperty("cardExpYr", parameters[7]);
 			testConfig.putRunTimeProperty("cardToken", parameters[8]);
 			obj=executePayuProductWebService(testConfig, ProductWebServices.edit_user_card.getRowNumber(), key, salt);
 		 	break;
		case get_user_cards_hashes:
 			testConfig.putRunTimeProperty("user_credentials", parameters[0]);
 			obj=executePayuProductWebService(testConfig, ProductWebServices.get_user_cards_hashes.getRowNumber(), key, salt);
 			break;
		case get_settlement_details:
			testConfig.putRunTimeProperty("date", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.get_settlement_details.getRowNumber(), key, salt);
			break;
		case get_user_cards:
			testConfig.putRunTimeProperty("user_credentials", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.get_user_cards.getRowNumber(), key, salt);
			break;
		case getAllRefundsFromTxnIds:
			testConfig.putRunTimeProperty("transactionId", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.getAllRefundsFromTxnIds.getRowNumber(), key, salt);
			break;
		case getPaymentGateways:
			testConfig.putRunTimeProperty("transactionId", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.getPaymentGateways.getRowNumber(), key, salt);
			break;
		case refundDateAmount:
			testConfig.putRunTimeProperty("mihpayid", parameters[0]);
			testConfig.putRunTimeProperty("token", parameters[1]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.refundDateAmount.getRowNumber(), key, salt);
			break;
		case validateCardNumber:
			testConfig.putRunTimeProperty("cardno", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.validateCardNumber.getRowNumber(), key, salt);
			break;
		case checkNewActiveMerchants:
			testConfig.putRunTimeProperty("date", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.checkNewActiveMerchants.getRowNumber(), key, salt);
			break;
		case refundTransaction:
			testConfig.putRunTimeProperty("mihpayid", parameters[0]);
			testConfig.putRunTimeProperty("token", parameters[1]);
			testConfig.putRunTimeProperty("amount", parameters[2]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.refundTransaction.getRowNumber(), key, salt);
			break;
		case cancel_refund_transaction:
			testConfig.putRunTimeProperty("mihpayid", parameters[0]);
			testConfig.putRunTimeProperty("token", parameters[1]);
			testConfig.putRunTimeProperty("amount", parameters[2]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.cancel_refund_transaction.getRowNumber(), key, salt);
			break;
		case refundStatus:
			testConfig.putRunTimeProperty("transactionId", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.refundStatus.getRowNumber(), key, salt);
			break;
		case updateMerUtr:
			testConfig.putRunTimeProperty("merchantid", parameters[0]);
			testConfig.putRunTimeProperty("utr", parameters[1]);
			testConfig.putRunTimeProperty("txnData", parameters[2]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.updateMerUtr.getRowNumber(), key, salt);
			break;
		case offerSyncingFromPayuMoneyToPayu:
			testConfig.putRunTimeProperty("paisamid", parameters[0]);
			testConfig.putRunTimeProperty("cashback_max_value", parameters[1]);
			testConfig.putRunTimeProperty("cashback_percent", parameters[2]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.offerSyncingFromPayuMoneyToPayu.getRowNumber(), key, salt);
			break;
		case getIssuingBankDownBins:
			testConfig.putRunTimeProperty("bankname", parameters[0]);
			testConfig.putRunTimeProperty("getPartialDownIb", parameters[1]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.getIssuingBankDownBins.getRowNumber(), key, salt);
			break;
		case caseWithPayuidAsChargeBack:
			testConfig.putRunTimeProperty("chargebacktype", parameters[0]);
			testConfig.putRunTimeProperty("payuids", parameters[1]);
			testConfig.putRunTimeProperty("amount", parameters[2]);
			testConfig.putRunTimeProperty("refnum", parameters[3]);
			testConfig.putRunTimeProperty("force", parameters[4]);
			testConfig.putRunTimeProperty("approvalstatus", parameters[5]);
			int rownbr = 0;
			if (parameters[0].equalsIgnoreCase("chargeBack")) {
				testConfig.putRunTimeProperty("debittype", parameters[6]);
				rownbr = ProductWebServices.caseWithPayuidAsChargeBack.getRowNumber();
			} else if ((parameters[0].equalsIgnoreCase("chargeBackReversal"))) {
				rownbr = 42;
			} else {
				testConfig.logFail("Invalid Var 1");
			}
			obj = executePayuProductWebService(testConfig, rownbr, key, salt);
			break;
		case getIssuingBankStatus:
			testConfig.putRunTimeProperty("cardNo", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.getIssuingBankStatus.getRowNumber(), key, salt);
			break;

		case eligibleBinsForEMI:
			testConfig.putRunTimeProperty("bank", parameters[0]);
			testConfig.putRunTimeProperty("bankname", parameters[1]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.getIssuingBankStatus.getRowNumber(), key, salt);
			break;
		case merchantLoginDetails:
			testConfig.putRunTimeProperty("startdate", parameters[0]);
			testConfig.putRunTimeProperty("enddate", parameters[1]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.merchantLoginDetails.getRowNumber(), key, salt);
			break;
		case check_action_status:
			testConfig.putRunTimeProperty("idnumber", parameters[0]);
			testConfig.putRunTimeProperty("idname", parameters[1]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.check_action_status.getRowNumber(), key, salt);
			break;
		case get_payment_status:
			testConfig.putRunTimeProperty("transactionid", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.get_payment_status.getRowNumber(), key, salt);
			break;
		case update_requests:
			testConfig.putRunTimeProperty("requestid", parameters[1]);
			testConfig.putRunTimeProperty("bankRef", parameters[2]);
			testConfig.putRunTimeProperty("amount", parameters[3]);
			testConfig.putRunTimeProperty("actiontype", parameters[4]);
			testConfig.putRunTimeProperty("newStatus", parameters[5]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.update_requests.getRowNumber(), key, salt);
			break;
		case getNetbankingStatus:
			testConfig.putRunTimeProperty("NBBin", parameters[0]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.getNetbankingStatus.getRowNumber(), key, salt);
			break;
		case OPIM_Upload_InvoiceAPI:
			 testConfig.putRunTimeProperty("invoiceid", parameters[0]);
		     testConfig.putRunTimeProperty("txnid", parameters[1]);
		     testConfig.putRunTimeProperty("upload_type", parameters[2]);
		     testConfig.putRunTimeProperty("upload_file_type" , parameters[3]);
		     testConfig.putRunTimeProperty("file",parameters[4]);
		     File file =new  File(parameters[4]);
			obj = executePayuProductWebService(testConfig, ProductWebServices.OPIM_Upload_InvoiceAPI.getRowNumber(), key, salt , file);
			break;
		case cod_cancel:
			testConfig.putRunTimeProperty("mihpayid", parameters[0]);
			testConfig.putRunTimeProperty("token", parameters[1]);
			testConfig.putRunTimeProperty("Amount", parameters[2]);
		 	obj=executePayuProductWebService(testConfig, ProductWebServices.cod_cancel.getRowNumber(), key, salt);
		 	break;
		case cod_verify:
			testConfig.putRunTimeProperty("mihpayid", parameters[0]);
			testConfig.putRunTimeProperty("token", parameters[1]);
			testConfig.putRunTimeProperty("Amount", parameters[2]);
		 	obj=executePayuProductWebService(testConfig, ProductWebServices.cod_verify.getRowNumber(), key, salt);
		 	break;
		case cod_settled:
			testConfig.putRunTimeProperty("mihpayid", parameters[0]);
			testConfig.putRunTimeProperty("token", parameters[1]);
			testConfig.putRunTimeProperty("Amount", parameters[2]);
			obj=executePayuProductWebService(testConfig, ProductWebServices.cod_settled.getRowNumber(), key, salt);
			break;
		case udf_update:
			testConfig.putRunTimeProperty("txnid", parameters[0]);
			testConfig.putRunTimeProperty("Udf1", parameters[1]);
			testConfig.putRunTimeProperty("Udf2", parameters[2]);
			testConfig.putRunTimeProperty("Udf3", parameters[3]);
			testConfig.putRunTimeProperty("Udf4", parameters[4]);
			testConfig.putRunTimeProperty("Udf5", parameters[5]);
			obj = executePayuProductWebService(testConfig,ProductWebServices.udf_update.getRowNumber(), key, salt);
			break;
		case create_invoice:
			int rownnumber = 0;
			
			if (parameters[0].equalsIgnoreCase("Mandatory"))
				rownnumber = ProductWebServices.create_invoice.getRowNumber();
			else if (parameters[0].equalsIgnoreCase("AllFields"))
				rownnumber = 20;
			else 
				testConfig.logFail("Invalid Var 1");
			
			obj = executePayuProductWebService(testConfig, rownnumber, key, salt);
			break;
		case get_merchant_ibibo_codes:
			testConfig.putRunTimeProperty("var1", parameters[0]);
			testConfig.putRunTimeProperty("ibibo_code", parameters[1]);
			obj = executePayuProductWebService(testConfig,ProductWebServices.get_merchant_ibibo_codes.getRowNumber(), key, salt);
			break;
		default:
			testConfig.logFail("No API Found by this Name");
			break;
		}
		return obj;
	}

	/**
	 * This Function will take JsonObject and a key and Return Count of Json
	 * object in Json array corresponding to specifickey in JSON object return
	 * by WebService API.
	 * 
	 * @param jobject
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	public static int jsonArrayCount(Object object, String key) throws JSONException {
		JSONObject jobject = (JSONObject) object;
		@SuppressWarnings("unchecked")
		Iterator<Object> keys = jobject.keys();
		int count = -1;
		while (keys.hasNext()) {
			String jsonkey = (String) keys.next();
			if (jsonkey.equalsIgnoreCase(key)) {
				count = ((JSONArray) jobject.get(jsonkey)).length();
			}

		}
		return count;
	}

	/**
	 * Create A Map Of key Value Out Of JSONObject If there is JSONObject as a
	 * Value .it Will insert those JSONObject Values in Same Map
	 * 
	 * @param jobject
	 *            JSONObject
	 * @return Return a Map<String,String> with Entries Filled
	 * @throws JSONException
	 */
	public static HashMap<String, Object> jsonParser(Object jobject) {
		if (jobject == null)

			return null;
		int count = 0;
		int flag = 0;
		HashMap<String, Object> jsonMap = new HashMap<String, Object>();
		Stack<Object> stack = new Stack<Object>();
		JSONObject obj1 = null;
		try {
			// pushing the main Jason Object in the stack
			if (jobject.getClass().getName().equals("org.json.JSONArray")) {
				JSONArray jsonobj = (JSONArray) (jobject);
				obj1 = jsonobj.getJSONObject(0);
			} else if (jobject.getClass().getName().equals("org.json.JSONObject")) {
				obj1 = (JSONObject) (jobject);
			}
			stack.push(obj1);
			while (!stack.isEmpty()) {
				if (flag == 1) {
					count++;
					flag = 0;
				}
				JSONObject obj = (JSONObject) stack.pop();
				// Getting Iterator Of Json Object Keys
				@SuppressWarnings("unchecked")
				Iterator<Object> keys = obj.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					// If key's Value is Json Object Itself ,if true than push
					// it in
					// the Stack else Put it in the Map
					if (obj.get(key) instanceof JSONObject)
						stack.push(obj.get(key));
					else if (obj.get(key) instanceof JSONArray) {
						for (int i = 0; i < ((JSONArray) (obj.get(key))).length(); i++) {
							stack.push(((JSONArray) (obj.get(key))).getJSONObject(i));
						}
					} else {
						String value = obj.get(key).toString();
						// If Key is Already In the Map Than do the Numbering of
						// Keys like (status ,status1,status2)
						if (jsonMap.containsKey(key)) {
							key = key + Integer.toString(count);
							flag = 1;
						}
						jsonMap.put(key, value);
					}
				}
			}
		} catch (JSONException e) {
			jsonMap = null;
		}
		return jsonMap;
	}

	/**
	 * Method to execute API with params
	 * @param testConfig
	 * @param apiList
	 * @param apimethod
	 * @param reqspec
	 * @return
	 */
	public static Response executeGenericAPI(Config testConfig,APIList apiList,APIMethod apimethod,int rownbr,int expectedAPIStatus)
	{
		RequestSpecification reqspec = null;
		Response response = null;
		String baseUrl = getAPIBaseURL(testConfig,apiList);
		reqspec = getAPIparamters(testConfig,apiList,rownbr);
		reqspec = reqspec.log().all();
		switch(apimethod)
		{
		case get:
			response = reqspec.header(new Header("Accept", "application/json")).when().get(baseUrl).then().log().all().statusCode(expectedAPIStatus).extract().response();
			break;
		case post:
			response = reqspec.header(new Header("Accept", "application/json")).when().post(baseUrl).then().log().all().statusCode(expectedAPIStatus).extract().response();
			break;
		}	
		return response;
	}

	private static RequestSpecification getAPIparamters(Config testConfig,APIList apiList,int rowNbr) {
		
		TestDataReader testDataReader = testConfig.getCachedTestDataReaderObject("RuleCreationInput");
		RequestSpecification reqspec=null;
		reqspec = given().contentType(ContentType.URLENC);
		
		switch(apiList)
		{
			case CreateReconTemplateRule:
						
				String par1=testDataReader.GetData(rowNbr,"pgId");
				String par2=testDataReader.GetData(rowNbr,"filePattern");
				String par3=testDataReader.GetData(rowNbr,"separator");
				String par4=testDataReader.GetData(rowNbr,"fileExtension");
				String par5=testDataReader.GetData(rowNbr,"fileType");
				String par6=testDataReader.GetData(rowNbr,"headerPresent");
				String par7=testDataReader.GetData(rowNbr,"columnCount");
				String par8=testDataReader.GetData(rowNbr,"columns");
				String par9=testDataReader.GetData(rowNbr,"datePattern");
				
				reqspec.param("pgId",par1).param("filePattern",par2).param("separator",par3).param("fileExtension",par4).param("fileType",par5).param("headerPresent",par6).param("columnCount",par7).param("columns",par8).param("datePattern",par9);
				break;
				
			case deleteReconTemplateRule:
				par1=testDataReader.GetData(rowNbr,"pgId");
				par2=testDataReader.GetData(rowNbr,"fileExtension");
				par3=testDataReader.GetData(rowNbr,"fileType");
				
				reqspec.param("pgId",par1).param("fileExtension",par2).param("fileType",par3);
				break;
				
			case FileParserAPI:
				par1=testConfig.getRunTimeProperty("fileName");
				par2=testDataReader.GetData(rowNbr,"pgId");
				par3=testDataReader.GetData(rowNbr,"pgReconId");
				reqspec.param("filePath", "/usr/local/BizReconFiles/"+par1).param("pgId", par2).param("pgReconId",par3);
				break;
			case ModilfyReconTemplateRule:
				
				break;
			case getReconFileTemplateRules:
				
				break;
		}
		return reqspec;
	}

	/**
	 * Method to get URL of API
	 * @param testConfig
	 * @param apiList
	 * @return
	 */
	public static String getAPIBaseURL(Config testConfig,APIList apiList)
	{
		String baseUrl = null;
		String refernceURL = null;
		String ip = testConfig.getRunTimeProperty("CoherenceServerIP");
		String port = testConfig.getRunTimeProperty("CoherenceServerPort");
		baseUrl = "http://" + ip + ":" + port;
		switch(apiList)
		{
			case CreateReconTemplateRule:
				refernceURL = "/bizTreasury/admin/setReconFileTemplateRules";
					break;
			case deleteReconTemplateRule:
				refernceURL = "/bizTreasury/admin/deleteReconFileTemplateRules";
				break;
			case FileParserAPI:
				refernceURL = "/bizTreasury/int/readRawReconFile";
				break;
			case ModilfyReconTemplateRule:
				refernceURL = "/bizTreasury/admin/setReconFileTemplateRules";
				break;
			case getReconFileTemplateRules:
				refernceURL = "/bizTreasury/admin/getReconFileTemplateRules";
				break;
		}
		baseUrl =  baseUrl+ refernceURL;
		return baseUrl;
	}
}
