import static com.jayway.restassured.RestAssured.given;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;

/**
 * Created by NMakkar on 1/18/2017.
 */
public class test_RestAPI {

	/**
	 * Test Script for get data API.
	 */

	@Test
	public void getdata_api() {
		// API Name or Command .
		String Command = "getData";

		// Input (Can be in different format) we are passing it as String.
		String Input = "{\"regionList\":[],\"functionalGroupList\":[{\"id\":\"1400004\",\"name\":\"Technical Support\"}],\"organizationList\":[{\"id\":\"1000\",\"name\":\"Cox\"}],\"lenguage\":\"1100001\",\"firstHitFlag\":\"false\",\"langList\":[{\"id\":\"1100001\",\"name\":\"English\"}],\"timezone\":\"1\",\"lobList\":[{\"id\":\"1200004\",\"name\":\"Residential\"}],\"subLobList\":[{\"id\":\"1300006\",\"name\":\"Residential\"}],\"subfunctionsList\":[{\"id\":\"1500146\",\"name\":\"Technical Support - Technical Support - Data\",\"fgId\":\"1400004\"}],\"functionsList\":[{\"id\":\"1600634\",\"name\":\"Residential - Technical Support - Data - Rancho Mission Viejo Data Tech\"}],\"coeList\":[],\"startTime\":\"0000\",\"endTime\":\"2330\",\"viewDetail\":{\"baseViewId\":\"VIEW_3\",\"viewId\":\"VIEW_3\",\"customStatus\":\"false\"}}";

		// Calling Method for API execution.
		ApiHelper.test_api(Command, Input, ContentType.JSON, null, null);
	}
	
	public static void main(String[] args) {


		// API Name or Command .
				String Command = "getData";

				// Input (Can be in different format) we are passing it as String.
				String Input = "{\"regionList\":[],\"functionalGroupList\":[{\"id\":\"1400004\",\"name\":\"Technical Support\"}],\"organizationList\":[{\"id\":\"1000\",\"name\":\"Cox\"}],\"lenguage\":\"1100001\",\"firstHitFlag\":\"false\",\"langList\":[{\"id\":\"1100001\",\"name\":\"English\"}],\"timezone\":\"1\",\"lobList\":[{\"id\":\"1200004\",\"name\":\"Residential\"}],\"subLobList\":[{\"id\":\"1300006\",\"name\":\"Residential\"}],\"subfunctionsList\":[{\"id\":\"1500146\",\"name\":\"Technical Support - Technical Support - Data\",\"fgId\":\"1400004\"}],\"functionsList\":[{\"id\":\"1600634\",\"name\":\"Residential - Technical Support - Data - Rancho Mission Viejo Data Tech\"}],\"coeList\":[],\"startTime\":\"0000\",\"endTime\":\"2330\",\"viewDetail\":{\"baseViewId\":\"VIEW_3\",\"viewId\":\"VIEW_3\",\"customStatus\":\"false\"}}";

				// Calling Method for API execution.
				ApiHelper.test_api(Command, Input, ContentType.JSON, null, null);
		
	    
	  
	}
	
}