package org.camunda.c7c8worker;

import jakarta.annotation.PostConstruct;
import org.camunda.c7c8worker.baseworker.BaseWorker;
import org.camunda.c7c8worker.bpmnengine.BpmnEngine;
import org.camunda.c7c8worker.bpmnengine.BpmnEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@ConfigurationPropertiesScan


public class WorkerApplication {
  Logger logger = LoggerFactory.getLogger(WorkerApplication.class);

  /**
   * This is tbe main application
   * @param args
   */
  public static void main(String[] args) {
    SpringApplication.run(WorkerApplication.class, args);
  }

  @Autowired
  BpmnEngineFactory bpmnEngineFactory;


  @Autowired
  private ApplicationContext applicationContext;



  @PostConstruct
  public void startUp() {
    // first, connect all Camunda server
    bpmnEngineFactory.initialize();

    List<BpmnEngine> listEngines = bpmnEngineFactory.getListEngines();
    // Second, detect and start workers
    Map<String, BaseWorker> beansOfType = applicationContext.getBeansOfType(BaseWorker.class);
    for(BaseWorker baseWorker : beansOfType.values()) {
      // Create one worker per engine
      for (BpmnEngine bpmnEngine : listEngines) {
        String workerId = baseWorker.getType();
        bpmnEngine.registerServiceTask(workerId, baseWorker);
        logger.info("Worker [{}] register in engine [{}]", baseWorker.getType(), bpmnEngine);
      }
    }
  }
}
