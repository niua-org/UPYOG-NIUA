# Spring Boot AdminLTE

A **Spring Boot** application integrated with the **AdminLTE 2** template to facilitate development.

## **Technologies Used**
- **Spring Boot 3.0**
- **Thymeleaf**
- **Spring Security**
- **JPA (Java Persistence API)**
- **PostgreSQL + PostGIS** (for spatial data support)
- **OpenLayers (OSM)** (for mapping functionalities)
- **GeoServer** (for map layers and spatial data visualization)

## **Project Structure**
### **Main Files for Maps**
- `Main.js` – Handles map-related functionalities.
- `Map.html` – Displays the map interface.

### **GeoServer Integration**
- GeoServer is used to serve map layers.
- Configurations are done to integrate OpenLayers with GeoServer for spatial data visualization.

## **Setup & Installation**
### **Prerequisites**
Make sure you have the following installed:
- **Java 17+**
- **Maven**
- **PostgreSQL with PostGIS Extension**
- **Node.js and NPM** (for managing frontend dependencies)

### **Running Locally**
1. **Clone the repository**
   ```sh
   git clone <repository-url>
   cd <project-directory>
   ```

2. **Install Bower (for managing frontend dependencies)**
   Run the following command in the project's root directory:
   ```sh
   npm install -g bower
   bower install
   ```

3. **Configure Database**
   Update `application.properties` with your PostgreSQL and PostGIS credentials:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

4. **Run the Application**
   ```sh
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access the Application**
   Open your browser and navigate to:
   ```
   http://localhost:8084
   ```

## **Additional Notes**
- Ensure `bower_components` is installed properly; otherwise, UI elements may not load correctly.
- If using GeoServer, configure layers and data sources properly to visualize maps in the application.

