# UPYOG Selenium Test Automation

## Overview
This project contains automated test scripts for the UPYOG pet registration and Trade License applications using Selenium WebDriver with Java. The automation suite simulates user interactions with the web application to verify functionality of both pet registration and Trade License workflows.

## Features
- Automated login with OTP verification
- City selection
- Navigation to pet registration module
- Form filling for pet owner details
- Pet type and breed selection
- Trade License application processing workflow

## Prerequisites
- Java JDK 8 or higher
- Maven
- Chrome browser
- ChromeDriver (matching your Chrome version)

## Setup
1. Ensure ChromeDriver is installed at `/usr/local/bin/chromedriver` or update the path in the code
2. Build the project using Maven:
   ```
   mvn clean install
   ```

## Project Structure
- `src/main/java/org/upyog/testing/Automation/PetApplication.java` - Main test script for pet registration workflow
- `src/main/java/org/upyog/Automation/Modules/TradeLicense/InboxEmpTl.java` - Trade License employee inbox workflow automation

## Test Flow

### Pet Registration
1. Login to the application using mobile number and OTP
2. Select city
3. Navigate to pet registration module
4. Fill owner details (father's name, email)
5. Select pet type and breed

### Trade License
1. Employee login with username/password
2. City selection
3. Navigate to Trade License inbox
4. Process applications through workflow steps:
   - Verify and Forward
   - Approve
   - Payment processing

## Usage
The test is configured as a Spring component with `@PostConstruct` annotation, which means it will run automatically when the Spring context is initialized.

## Notes
- The test uses a fixed mobile number (9999999999) and OTP (123456) for login
- Various Selenium techniques are implemented including:
  - Explicit waits
  - JavaScript execution
  - Actions API for complex interactions
  - Multiple locator strategies (CSS, XPath)


## Local Setup
- application.properties
  - selenium.grid.enabled=false
  - Please update all document paths in application.properties based on your local machine.
    For example:
    /Users/niua-l1232/Documents/doc.pdf should be replaced with your own system path.
  - Use IP- localhost:8080