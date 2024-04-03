package org.camunda.c7c8worker.worker;

import org.camunda.c7c8worker.baseworker.BaseWorker;
import org.camunda.c7c8worker.bpmnengine.JobInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Declare the worker as a component, then it will be detected by the application and register as a worker
 */
@Component
public class DelayWorker extends BaseWorker {
  public static final String DELAY_POLICY = "delayPolicy";
  public static final String DELAY_SLEEP_IN_MS = "delaySleepInMs";
  public static final String ERROR_CODE = "errorCode";
  public static final String ERROR_MESSAGE = "errorMessage";
  public static final String INITIAL_RETRY = "initialRetry";
  private final Logger logger = LoggerFactory.getLogger(DelayWorker.class);

  @Override
  public String getType() {
    return "delay-worker";
  }

  @Override
  public List<String> getListFetchVariables() {
    return List.of(DELAY_POLICY,DELAY_SLEEP_IN_MS,ERROR_CODE,ERROR_MESSAGE,INITIAL_RETRY);
  }

  @Override
  public void executeWorker(JobInformation jobInformation) throws Exception {

    Map<String, Object> variables = new HashMap<>();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    variables.put("ExecutedStamp ", formatter.format(new Date()));

    String delayPolicy = (String) jobInformation.getVariable(DELAY_POLICY);

    if ("COMPLETE".equalsIgnoreCase(delayPolicy)) {
      long delaySleepInMs = getValueLong(jobInformation.getVariable(DELAY_SLEEP_IN_MS), 0L);
      logger.error("Delay-worker job[{}] : COMPLETE Sleep[{}]", jobInformation.getJobId(), delaySleepInMs);
      Thread.sleep(delaySleepInMs);

      complete(jobInformation, variables);
    } else if ("BPMNERROR".equalsIgnoreCase(delayPolicy)) {
      String errorCode = (String) jobInformation.getVariable(ERROR_CODE);
      String errorMessage = (String) jobInformation.getVariable(ERROR_MESSAGE);
      logger.error("Delay-worker job[{}] : BPMNERROR errorCode [{}]errorMessage[{}]", jobInformation.getJobId(),
          errorCode, errorMessage);
      throwBpmnError(jobInformation, errorCode, errorMessage, variables);
    } else if ("FAIL".equalsIgnoreCase(delayPolicy)) {
      long retry = getValueLong(jobInformation.getVariable(INITIAL_RETRY), 1L);
      if (jobInformation.getRetries() != 0)
        retry = jobInformation.getRetries() - 1;

      logger.error("Delay-worker job[{}] : FAIL retry[{}]", jobInformation.getJobId(), retry);
      fail(jobInformation, (int) retry, variables);
    } else {
      logger.error("No policy was set up, so just complete");
      complete(jobInformation, variables);
    }
  }

  /**
   * Return a long value
   * @param value value to transform
   * @param defaultValue default value if the value is null
   * @return a Long
   */
  private Long getValueLong(Object value, Long defaultValue) {
    if (value == null)
      return defaultValue;

    if (value instanceof Long valueLong)
      return valueLong;
    try {
      return Long.valueOf(value.toString());
    }catch(Exception e) {
      logger.error("Can't transform[{}] to Long return defaultValue[{}]", value, defaultValue);
      return defaultValue;
    }
  }
}
