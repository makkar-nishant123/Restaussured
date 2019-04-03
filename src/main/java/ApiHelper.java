import static com.jayway.restassured.RestAssured.given;
import java.io.File;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

public class ApiHelper {

	/**
	 * Hitting URL and getting response.
	 * @param command - API Name / Command .
	 * @param Input - Input is which we are sending in Body of API.
	 * @param contenttype - Format of request  data , can send null if not present.
	 * @param params - parameter in request , can send null if  not present.
	 * @param file - file in request  , can send null if not present.
	 * @return
	 */
	public static Object test_api(String command, String Input,
			ContentType contenttype, HashMap<String, String> params, File file) {

		// Framing Url to send Request (Base URL Should be configurable )
		String baseUrl = "http://10.5.3.245/MercuryServices/rest/mercuryview/";
		String URL = baseUrl + command;

		// Initializing request parameter.
		RequestSpecification reqspec;

		//Creating Request type.
		if (file == null)
			reqspec = given().contentType(contenttype);
		else
			reqspec = given().multiPart(file);

		// Add request parameters 
		if (params != null) {
			for (String key : params.keySet())
				reqspec = reqspec.param(key, params.get(key));

		}

		//Add request body.
		if (Input != null)
			reqspec = reqspec.body(Input);

		//Logging Request.
		reqspec = reqspec.log().all();

		//Calling API with POST Call (Customize this with Switch for more protocols).
		Response response = reqspec.when().post(URL).then().log().all()
				.statusCode(200).extract().response();

		//Initialize for returniing response.
		Object returnObj = null;
		String stringResponse = response.asString();

		// Checking for Json format. Assumption if a string contains { in Response then it will be treated as Json String}.

		if (stringResponse.contains("{") && stringResponse.contains("}")) {
			try {
				returnObj = new JSONObject(stringResponse);
			} catch (JSONException e) {
				// Check for OfferSycn API
				String convertString = stringResponse;
				/*
				 * convertString = stringResponse.replaceAll("\\\"", "");
				 * convertString = convertString.replaceAll("\\\\", "\\\"");
				 */
				try {
					returnObj = new JSONObject(convertString);
				} catch (JSONException e1) {
					try {
						returnObj = new JSONArray(stringResponse);
					} catch (JSONException e2) {
						e2.printStackTrace();
						return null;
					}
				}
			}
		} else {
			// If not Json then return String
			returnObj = stringResponse;
		}
		return returnObj;
	}
}