package org.upyog.Automation.Modules;

import org.upyog.Automation.Utils.ConfigReader;

public class AdvBookingConfigDemo {
    public void runDemo() {

        String url = ConfigReader.get("citizen.base.url");
        String mobile = ConfigReader.get("citizen.mobile.number");

        System.out.println("URL from config: " + url);
        System.out.println("Mobile from config: " + mobile);
    }
}
