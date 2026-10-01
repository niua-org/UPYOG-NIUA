package org.egov.dx.upyogdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(basePackages = "org.egov.dx.upyogdata.pfms.client")
@EnableScheduling
@SpringBootApplication
public class UpyogDataDxApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpyogDataDxApplication.class, args);
    }
}