package org.camunda.c7c8worker.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@PropertySource("classpath:application.yaml")
@Configuration
public class ConfigurationStartup {
  static Logger logger = LoggerFactory.getLogger(ConfigurationStartup.class);

  @Value("#{'${c7c8worker.workers:}'.split(';')}")
  private List<String> listWorkers;

  public List<String> getListWorkers() {
    return listWorkers;
  }

  private List<String> recalibrateAfterSplit(List<String> originalList) {
    if (originalList.size() == 1 && originalList.get(0).isEmpty())
      return Collections.emptyList();
    return originalList;
  }
}
