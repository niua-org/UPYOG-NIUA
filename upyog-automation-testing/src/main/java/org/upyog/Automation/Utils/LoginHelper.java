package org.upyog.Automation.Utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LoginHelper {

    private static final Logger logger =
            LoggerFactory.getLogger(LoginHelper.class);

    public static void citizenLogin(WebDriver driver,
                                    WebDriverWait wait,
                                    JavascriptExecutor js,
                                    String baseUrl,
                                    String mobile,
                                    String otp,
                                    String city)
            throws InterruptedException {

        driver.get(baseUrl);

        System.out.println(
                "CURRENT URL = "
                        + driver.getCurrentUrl()
        );

        System.out.println(
                "PAGE TITLE = "
                        + driver.getTitle()
        );

        // ==========================
        // MOBILE
        // ==========================
        CommonActions.fillInput(
                wait,
                "mobileNumber",
                mobile
        );


        // ==========================
        // CHECKBOX
        // ==========================
        WebElement checkbox = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(
                                "input[type='checkbox'].form-field"
                        )
                )
        );

        if (!checkbox.isSelected()) {

            js.executeScript(
                    "arguments[0].click();",
                    checkbox
            );
        }


        // ==========================
        // NEXT
        // ==========================
        CommonActions.clickButtonByText(
                driver,
                wait,
                js,
                "Next"
        );

        // ==========================
        // OTP
        // ==========================
        List<WebElement> otpInputs = wait.until(
                ExpectedConditions
                        .visibilityOfAllElementsLocatedBy(
                                By.cssSelector(
                                        "input.input-otp"
                                )
                        )
        );

        for (int i = 0;
             i < otp.length() && i < otpInputs.size();
             i++) {

            otpInputs.get(i)
                    .sendKeys(
                            String.valueOf(
                                    otp.charAt(i)
                            )
                    );
        }



        // ==========================
        // OTP NEXT
        // ==========================
        CommonActions.clickButtonByText(
                driver,
                wait,
                js,
                "Next"
        );
        logger.info("selecting cityyyyy");


        // ==========================
        // CITY
        // ==========================

        CommonActions.selectCity(driver,
                wait,
                js,
                city);


        logger.info("City selected properly");

         //==========================
         //CONTINUE
         //==========================
        CommonActions.clickButtonByText(
                driver,
                wait,
                js,
                "Continue"
        );

        logger.info(
                "Citizen Login Completed"
        );


        //Thread.sleep(5000);

        // ==========================
// LANGUAGE (optional)
// ==========================
//        try {
//
//            WebElement languageOption = wait.until(
//                    ExpectedConditions.elementToBeClickable(
//                            By.xpath(
//                                    "//*[contains(text(),'English')]"
//                            )
//                    )
//            );
//
//            js.executeScript(
//                    "arguments[0].scrollIntoView({block:'center'});",
//                    languageOption
//            );
//
//            new org.openqa.selenium.interactions.Actions(driver)
//                    .moveToElement(languageOption)
//                    .click()
//                    .perform();
//
//            CommonActions.clickButtonByText(
//                    driver,
//                    wait,
//                    js,
//                    "Continue"
//            );
//
//            System.out.println("Language selected");
//
//        } catch (Exception e) {
//
//            System.out.println("Language screen not shown");
//        }

    // ==========================
    // CITY
    // ==========================
//    WebElement city1Option = wait.until(
//            ExpectedConditions.elementToBeClickable(
//                    By.xpath(
//                            "//*[contains(text(),'" + city + "')]/ancestor::*[contains(@class,'radio-wrap')]"
//                    )
//            )
//    );
//
//        js.executeScript(
//                "arguments[0].scrollIntoView({block:'center'});",
//                city1Option
//        );
//
//        js.executeScript(
//                "arguments[0].click();",
//                city1Option
//        );
//
//        System.out.println("City selected properly");
//
//
//    // ==========================
//    // CONTINUE
//    // ==========================
//        CommonActions.clickButtonByText(
//    driver,
//    wait,
//    js,
//            "Continue"
//            );
//
//        System.out.println(
//                "Citizen Login Completed"
//                );
       }
}