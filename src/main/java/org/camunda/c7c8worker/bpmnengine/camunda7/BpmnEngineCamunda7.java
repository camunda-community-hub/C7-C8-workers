package org.camunda.c7c8worker.bpmnengine.camunda7;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.c7c8worker.baseworker.BaseWorker;
import org.camunda.c7c8worker.bpmnengine.BpmnEngine;
import org.camunda.c7c8worker.bpmnengine.EngineException;
import org.camunda.c7c8worker.bpmnengine.JobInformation;
import org.camunda.c7c8worker.configuration.BpmnEngineList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

/**
 * connection to a Camunda 7 server. This is one object created by the engine, and then one "init() " call.
 * After, all methods are call in a multi-threads environment
 */
public class BpmnEngineCamunda7 implements BpmnEngine {

  public static final int SEARCH_MAX_SIZE = 100;
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda7.class);
  private final String serverUrl;
  private final int workerMaxJobsActive;
  private final boolean logDebug;

  /**
   * @param serverDefinition definition to connect to the server
   * @param logDebug         if true, operation will be log as debug level
   */
  public BpmnEngineCamunda7(BpmnEngineList.BpmnServerDefinition serverDefinition, boolean logDebug) {
    this.serverUrl = serverDefinition.camunda7ServerUrl;
    this.workerMaxJobsActive = serverDefinition.workerMaxJobsActive;
    this.logDebug = logDebug;
    init();
  }

  /**
   * P
   *
   * @param serverUrl is  "http://localhost:8080/engine-rest"
   */
  public BpmnEngineCamunda7(String serverUrl, String userName, String password, boolean logDebug) {
    this.serverUrl = serverUrl;
    this.workerMaxJobsActive = 1;
    this.logDebug = logDebug;
    init();
  }

  @Override
  public void init() {
// Nothing to do for C7
  }

  @Override
  public void connection() throws EngineException {
    // No connection is necessary
  }

  @Override
  public void disconnection() throws EngineException {
    // nothing to do here
  }

  /**
   * Engine is ready. If not, a connection() method must be call
   *
   * @return true if ready
   */
  public boolean isReady() {
    return true;
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  Service task                                                        */
  /*                                                                      */
  /* ******************************************************************** */
  @Override
  public RegisteredTask registerServiceTask(String workerId,
                                            BaseWorker baseWorker) {

    if (!(baseWorker instanceof ExternalTaskHandler)) {
      logger.error("handler is not a externalTaskHandler implementation, can't register the worker [{}], topic [{}]",
          workerId, baseWorker.getType());
      return null;
    }
    RegisteredTask registeredTask = new RegisteredTask();

    ExternalTaskClient client = ExternalTaskClient.create()
        .baseUrl(serverUrl)
        .workerId(workerId)
        .maxTasks(Math.max(workerMaxJobsActive,1))
        .lockDuration(baseWorker.getLockTime().toMillis())
        .asyncResponseTimeout(baseWorker.getAsyncResponseTime())
        .backoffStrategy(baseWorker.getBackoffStrategy())
        .build();

    registeredTask.topicSubscription = client.subscribe(baseWorker.getType())
        .lockDuration(baseWorker.getLockTime().toMillis())
        .handler(baseWorker)
        .open();
    return registeredTask;

  }

  public void extendLockTime( JobInformation jobInformation, Duration duration) {}


  @Override
  public void complete(JobInformation jobInformation, Map<String, Object> variables)
      throws EngineException {

    if (logDebug) {
      logger.info("BpmnEngine7.executeServiceTask: activityId[{}]", jobInformation.externalTask.getActivityId());
    }
    try {
      jobInformation.externalTaskService.complete(jobInformation.externalTask, variables);
    } catch (Exception e) {
      throw new EngineException("Can't execute taskId["+jobInformation.externalTask.getActivityId()+"]", e );
    }
  }

  @Override
  public void fail(JobInformation jobInformation,
                   int nbRetries,
                   String errorMessage,
                   String errorDetails,
                   Duration retryTimeout,
                   Map<String, Object> variables) throws EngineException {
    jobInformation.externalTaskService.handleFailure(jobInformation.externalTask, errorMessage, errorDetails, nbRetries, retryTimeout.toMillis());
  }

  @Override
  public void throwBpmnError(JobInformation jobInformation,
                             String errorCode,
                             String errorMsg,
                             Map<String, Object> variables) throws EngineException {
    jobInformation.externalTaskService.handleBpmnError(jobInformation.externalTask, errorCode, errorMsg, variables);

  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  get server definition                                               */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public BpmnEngineList.CamundaEngine getTypeCamundaEngine() {
    return BpmnEngineList.CamundaEngine.CAMUNDA_7;
  }

  @Override
  public String getSignature() {
    return BpmnEngineList.CamundaEngine.CAMUNDA_7 + " " + "serverUrl[" + serverUrl + "]";
  }

  @Override
  public int getWorkerExecutionThreads() {
    return workerMaxJobsActive;
  }


}
