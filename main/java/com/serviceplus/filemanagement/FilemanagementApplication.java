package com.serviceplus.filemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.serviceplus.filemanagement")
@EnableDiscoveryClient
@EnableFeignClients
public class FilemanagementApplication  extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(FilemanagementApplication.class, args);
	}

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(FilemanagementApplication.class);
    }

}
