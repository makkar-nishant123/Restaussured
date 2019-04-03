package Utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.frontendtest.components.ImageComparison;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;

public class ImageHelper
{
	public enum RespectiveToElement
	{
		None, AboveElement, BelowElement, LeftToElement, InsideAnElement
	}
	
	/**
	 * This function is used to verify if UI is distorted or not by comparing screenshot with passed image.
	 * @param testConfig
	 * @param originalImage - Image which is to be used as base image for comparison (Put it in folder : Project/Resources/ImageComparision folder)
	 */
	public static void verifyPageIsNotDistorted(Config testConfig, String originalImage)
	{
		verifyUIDistortion(testConfig, originalImage, null, RespectiveToElement.None);
	}
	
	
	/**
	 * This function is used to verify if UI, below a given Element(including Element)
	 * @param testConfig
	 * @param originalImage - Image which is to be used as base image for comparison (Put it in folder : Project/Resources/ImageComparision folder)
	 */
	public static void verifyPageIsNotDistortedBelowAnElement(Config testConfig, String originalImage, WebElement element)
	{
		verifyUIDistortion(testConfig, originalImage, element, RespectiveToElement.BelowElement);
	}
	
	
	/**
	 * This function is used to verify if UI, below a given Element(excluding Element)
	 * @param testConfig
	 * @param originalImage - Image which is to be used as base image for comparison (Put it in folder : Project/Resources/ImageComparision folder)
	 */
	public static void verifyPageIsNotDistortedAboveAnElement(Config testConfig, String originalImage, WebElement element)
	{
		verifyUIDistortion(testConfig, originalImage, element, RespectiveToElement.AboveElement);
	}
	
	
	/**
	 * This function is used to verify if UI, left to a given Element(including Element)
	 * @param testConfig
	 * @param originalImage - Image which is to be used as base image for comparison (Put it in folder : Project/Resources/ImageComparision folder)
	 */
	public static void verifyPageIsNotDistortedLeftToElement(Config testConfig, String originalImage, WebElement element)
	{
		verifyUIDistortion(testConfig, originalImage, element, RespectiveToElement.LeftToElement);
	}
	
	
	/**
	 * This function is used to verify if UI, InsideAnElement with grace of 10 pixels
	 * @param testConfig
	 * @param originalImage - Image which is to be used as base image for comparison (Put it in folder : Project/Resources/ImageComparision folder)
	 */
	public static void verifyPageIsNotDistortedInsideAnElement(Config testConfig, String originalImage, WebElement element)
	{
		verifyUIDistortion(testConfig, originalImage, element, RespectiveToElement.InsideAnElement);
	}
	

	/**
	 * This function is used to compare screenshot below any specific Element (If Element=null then will compare full image)
	 * @param testConfig
	 * @param originalImage - Image which is to be used as base image for comparison (Put it in folder : Project/Resources/ImageComparision folder)
	 * @param element
	 * @param compareAboveThisElement - Pass true to compareAboveThisElement
	 */
	private static void verifyUIDistortion(Config testConfig, String originalImage, WebElement element, RespectiveToElement respectiveToElement)
	{
		boolean result = false;
		String randomString = Helper.generateRandomAlphabetsString(5).toLowerCase();
		String originalImagePath = "Resources" + File.separator + "ImageComparision" + File.separator + originalImage;
		
		String expectedImagePath = new String(testConfig.getRunTimeProperty("ResultsDir") + File.separator + "ImageComparision" + File.separator + randomString + "_expectedImage_" + originalImage.split("[.]")[0] + ".jpg");
		String actualImagePath = new String(testConfig.getRunTimeProperty("ResultsDir") + File.separator + "ImageComparision" + File.separator + randomString + "_actualImage_" + originalImage.split("[.]")[0] + ".jpg");
		String outputImagePath = new String(testConfig.getRunTimeProperty("ResultsDir") + File.separator + "ImageComparision" + File.separator + randomString + "_outputImage_" + originalImage.split("[.]")[0] + ".jpg");
		
		try 
		{
			//Get Screenshot from Browser (Full or Partial)
			File screenshotFilePath = ((TakesScreenshot)testConfig.driver).getScreenshotAs(OutputType.FILE);
			if(element != null)
			{
				switch(respectiveToElement)
				{
					case AboveElement:
						screenshotFilePath = cropScreenshotAboveAnElement(testConfig, screenshotFilePath, element);
						break;
					case BelowElement:
						screenshotFilePath = cropScreenshotBelowAnElement(testConfig, screenshotFilePath, element);
						break;
					case LeftToElement:
						screenshotFilePath = cropScreenshotLeftToAnElement(testConfig, screenshotFilePath, element);
						break;
					case InsideAnElement:
						screenshotFilePath = cropScreenshotInsideAnElement(testConfig, screenshotFilePath, element);
						break;
				default:
					break;
				}
			}
			FileUtils.copyFile(screenshotFilePath, new File(actualImagePath));
			
			//Compare both the images
			ImageComparison imageComparison = new ImageComparison(10, 10, 0.05);
			result = imageComparison.fuzzyEqual(originalImagePath, actualImagePath, outputImagePath);
			
/*			What it does (Simplified Flow):
				1. First the two images that should be compared are divided in squares with the width and height (e.g. 10, 10 here), that you defined in the constructor.
				2. For every square in each image an average RGB-Value is calculated.
				3. If the average RGB-Values of the corresponding squares differ more than the threshold that you defined in the constructor (0.05 = 5%), the function fuzzyEqual(…) will return false.
				4. If you passed a path to save an image with the found differences a copy will be save at this path with all the differences marked with red squares.
			Note: The sensitivity of the fuzzy-equal-test will be influenced by the defined threshold as well as the size of the squares!
*/
			
			if(result)
			{
				testConfig.logPass("User Interface verified successfully. No Distortion found in : " + originalImage);
				testConfig.logComment("<B>Verified Image</B>:- <a href=" + Browser.getResultsURLOnRunTime(testConfig, outputImagePath) + " target='_blank' >" + "click here to view Verified User Interface" + "</a>");
				FileUtils.forceDelete(new File(actualImagePath));
			}
			else
			{
				Log.failure("UI of CurrentPage is different from the " + originalImage + " Image", testConfig);
				
				//Copying expectedImagePath from local to Results folder(in order to make it visible in failure logs)
				try {	Files.copy(Paths.get(originalImagePath), Paths.get(expectedImagePath), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {e.printStackTrace();}
				
				testConfig.logComment("<B>UI Distortion Results</B>:- <a href=" + Browser.getResultsURLOnRunTime(testConfig, outputImagePath) + " target='_blank' >" + "click here to view Output Image of UI Distortion" + "</a>");
				testConfig.logComment("<B>Expected Screenshot</B>:- <a href=" + Browser.getResultsURLOnRunTime(testConfig, expectedImagePath) + " target='_blank' >" + "click here to view Expected Image of User Interface" + "</a>");
				testConfig.logComment("<B>Current Screenshot</B>:- <a href=" + Browser.getResultsURLOnRunTime(testConfig, actualImagePath) + " target='_blank' >" + "click here to view Actual Image of User Interface" + "</a>");
			}
		} 
		catch (IOException e) 
		{
			testConfig.logFail("*******Exception while verifying the User Interface*******");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * This function is used to crop the screenshot blow an element (including the element)
	 * @param testConfig
	 * @param screenshotFilePath
	 * @param element
	 * @return
	 */
	private static File cropScreenshotBelowAnElement(Config testConfig, File screenshotFilePath, WebElement element)
	{
		try
		{
			BufferedImage  fullPagePhoto = ImageIO.read(screenshotFilePath);
			Point point = element.getLocation();
			
			int startPoint_X = 0;
			int widthFromStartPoint_X = fullPagePhoto.getWidth();
			
			int startPoint_Y = point.getY();
			int heightFromStartPoint_Y = fullPagePhoto.getHeight() - point.getY();
			
			BufferedImage croppedPhoto= fullPagePhoto.getSubimage(startPoint_X, startPoint_Y, widthFromStartPoint_X, heightFromStartPoint_Y);
			ImageIO.write(croppedPhoto, "png", screenshotFilePath);
			System.out.println("Image is cropped Successfully...");
		}
		catch (IOException e) 
		{
			testConfig.logWarning("*******Exception while cropping the Image below Element*******");
			e.printStackTrace();
		}
		return screenshotFilePath;
	}
	
	/**
	 * This function is used to crop the screenshot above an element (excluding the element)
	 * @param testConfig
	 * @param screenshotFilePath
	 * @param element
	 * @return
	 */
	private static File cropScreenshotAboveAnElement(Config testConfig, File screenshotFilePath, WebElement element)
	{
		try
		{
			BufferedImage  fullPagePhoto = ImageIO.read(screenshotFilePath);
			Point point = element.getLocation();
			
			int startPoint_X = 0;
			int widthFromStartPoint_X = fullPagePhoto.getWidth();
			
			int startPoint_Y = 0;
			int heightFromStartPoint_Y = point.getY();
			
			BufferedImage croppedPhoto= fullPagePhoto.getSubimage(startPoint_X, startPoint_Y, widthFromStartPoint_X, heightFromStartPoint_Y);
			ImageIO.write(croppedPhoto, "png", screenshotFilePath);
			System.out.println("Image is cropped Successfully...");
		}
		catch (IOException e) 
		{
			testConfig.logWarning("*******Exception while cropping the Image above Element*******");
			e.printStackTrace();
		}
		return screenshotFilePath;
	}
	
	
	
	/**
	 * This function is used to crop the screenshot above an element (including the element)
	 * @param testConfig
	 * @param screenshotFilePath
	 * @param element
	 * @return
	 */
	private static File cropScreenshotLeftToAnElement(Config testConfig, File screenshotFilePath, WebElement element)
	{
		try
		{
			BufferedImage  fullPagePhoto = ImageIO.read(screenshotFilePath);
			Point point = element.getLocation();
			int widthOfElement = element.getSize().width;
			
			int startPoint_X = 0;
			int widthFromStartPoint_X = point.getX() + widthOfElement + 20;
			
			int startPoint_Y = 0;
			int heightFromStartPoint_Y = fullPagePhoto.getHeight();
			
			BufferedImage croppedPhoto= fullPagePhoto.getSubimage(startPoint_X, startPoint_Y, widthFromStartPoint_X, heightFromStartPoint_Y);
			ImageIO.write(croppedPhoto, "png", screenshotFilePath);
			System.out.println("Image is cropped Successfully...");
		}
		catch (IOException e) 
		{
			testConfig.logWarning("*******Exception while cropping the Image above Element*******");
			e.printStackTrace();
		}
		return screenshotFilePath;
	}
	
	
	/**
	 * This function is used to crop the screenshot inside an element
	 * @param testConfig
	 * @param screenshotFilePath
	 * @param element
	 * @return
	 */
	private static File cropScreenshotInsideAnElement(Config testConfig, File screenshotFilePath, WebElement element)
	{
		try
		{
			BufferedImage  fullPagePhoto = ImageIO.read(screenshotFilePath);
			Point point = element.getLocation();
			
			int startPoint_X = point.getX() - 10;
			int widthFromStartPoint_X = element.getSize().width + 20;
			
			int startPoint_Y = point.getY() - 10;
			int heightFromStartPoint_Y = element.getSize().height + 20;
			
			BufferedImage croppedPhoto= fullPagePhoto.getSubimage(startPoint_X, startPoint_Y, widthFromStartPoint_X, heightFromStartPoint_Y);
			ImageIO.write(croppedPhoto, "png", screenshotFilePath);
			System.out.println("Image is cropped Successfully...");
		}
		catch (IOException e) 
		{
			testConfig.logWarning("*******Exception while cropping the Image Inside the Element*******");
			e.printStackTrace();
		}
		return screenshotFilePath;
	}
	
	public static void setWindowSize(Config testConfig, int width, int height)
	{
		testConfig.driver.manage().window().maximize();
		Dimension fullScreen = testConfig.driver.manage().window().getSize();
		
		testConfig.logComment("FullScreen Size of Browser : "+fullScreen.toString());
		
		if(height == -1)
			height = fullScreen.getHeight();
		
		if(width == -1)
			width = fullScreen.getWidth();
		
		testConfig.driver.manage().window().setPosition(new Point(0,0));
		testConfig.driver.manage().window().setSize(new Dimension(width, height));
		
		Dimension after = testConfig.driver.manage().window().getSize();
		testConfig.logComment("Custom Size of Browser : "+after.toString());
	}
}