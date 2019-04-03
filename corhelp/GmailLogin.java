package Utils;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class GmailLogin
{
	@FindBy(id = "Email")
	private WebElement email;
	
	@FindBy(id = "Passwd")
	private WebElement password;
	
	@FindBy(id = "signIn")
	private WebElement Signin;
	
	@FindBy(id="next")
	private WebElement next;
	
	@FindBy(id = "gmail-sign-in")
	private WebElement SigninButton;
	
	private Config testConfig;
	
	public GmailLogin(Config testConfig)
	{
		this.testConfig = testConfig;
		Browser.navigateToURL(testConfig, "https://mail.google.com/mail/&hl=en");
		PageFactory.initElements(this.testConfig.driver, this);
		Browser.waitForPageLoad(testConfig, email);
	}
	
	public GmailVerification Login(String userName, String passWord)
	{
		// Enter email credentials
		Element.enterData(testConfig, email, userName, "User Name");
		Element.click(testConfig, next, "click on next button");
		Browser.wait(testConfig,2);
		Element.enterData(testConfig, password, passWord, "Password");
		
		Element.click(testConfig, Signin, "Signing in");
		Browser.wait(testConfig, 5);
		return new GmailVerification(testConfig);
	}
}
