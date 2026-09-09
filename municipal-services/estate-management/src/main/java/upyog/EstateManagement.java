package upyog;


import org.egov.tracer.config.TracerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Import({ TracerConfiguration.class })
@SpringBootApplication
@ComponentScan(basePackages = { "upyog", "upyog.web.controllers" , "upyog.config"})
@EnableFeignClients(basePackages = "upyog.*")
public class EstateManagement {


    public static void main(String[] args) throws Exception {
        SpringApplication.run(EstateManagement.class, args);
    }

}
