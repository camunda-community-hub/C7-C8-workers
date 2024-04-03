package org.camunda.c7c8worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan


public class WorkerApplication {
  public static void main(String[] args) {

    SpringApplication.run(WorkerApplication.class, args);
    // thanks to Spring, the class AutomatorStartup.init() is active.
  }
}
