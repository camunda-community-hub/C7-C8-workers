package org.camunda.c7c8worker.baseworker;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.c7c8worker.bpmnengine.BpmnEngine;
import org.camunda.c7c8worker.bpmnengine.BpmnEngineFactory;
import org.camunda.c7c8worker.bpmnengine.EngineException;
import org.camunda.c7c8worker.bpmnengine.JobInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class BaseWorker implements ExternalTaskHandler, JobHandler {

  private final Logger logger = LoggerFactory.getLogger(BaseWorker.class);

  /**
   * C7 handler
   * @param externalTask external task ID
   * @param externalTaskService external task service
   */
  @Override
  public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
    BpmnEngine engineCamunda7 = BpmnEngineFactory.getInstance().getCamunda7();
    JobInformation jobInformation = new JobInformation(externalTask, externalTaskService, engineCamunda7);

    try {
      executeWorker(jobInformation);

    } catch(Exception e) {
      // C7 worker is not supposed to throw an exception
      try {
        logger.error("Exception during the job execution, move to FAIL. JobId[{}] ActivityId[{}] : {}",
            externalTask.getId(),
            externalTask.getActivityId(),
            e.getMessage());
        fail(jobInformation, 0, Collections.emptyMap());
      }catch (Exception eFail) {
        logger.error("Can't fail the job JobId[{}] ActivityId[{}] : {}",
            externalTask.getId(),
            externalTask.getActivityId(),
            eFail.getMessage());
      }
    }
  }
  /**
   * C8 handler
   * @param jobClient JobClient
   * @param activatedJob Job to execute
   * @throws Exception In case of error
   */
  @Override
  public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {
    BpmnEngine engineCamunda8 = BpmnEngineFactory.getInstance().getCamunda8();
    JobInformation jobInformation = new JobInformation(jobClient, activatedJob,engineCamunda8 );
    executeWorker(jobInformation);
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  Information on workers                                              */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * return the type of the worker. It will be register behind this type
   * @return the worker type
   */
  public abstract String getType();

  /**
   * Return the list of variable to fetch. if null, all process variables will be fetched
   * @return list of variables to fetch
   */
  public List<String> getListFetchVariables() {
    return null;
  }

  /*
  When a job is fetch, how many times it must be lock?
   */
  public long getLockTimeInMs() {
    return 1000;
  }
  /*
  When the job fail, it will wait this time before a new tentative is done
   */
  public long getRetryBackoffInMs(){
    return 100;
  }

  /**
   * Specify the number of active job.
   * @return the number. If 0, then the value defined in the YAML file is used
   */
  public int getMaxJobActives() {
    return 0;
  }

  /**
   * Specify the number of thread (C8 valid only)
   * @return the number. If 0, then the value defined in the YAML file is used
   */
  public int getNumberOfThread() {
    return 0;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Execution                                                           */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Execute a job. When a job is ready to be executed, it will be call by this method
   * The worker should call a jobInformation.complete(), fail(), throwBpmnError() method
   * @param jobInformation job to execute
   * @throws Exception throw an exception in case of errors. It is not expected to throw an exception, and the worker will be "failed" if a exception is detected
   */
  public abstract void executeWorker(JobInformation jobInformation) throws Exception;



  /**
   * Extend the job lock
   *
   * @param duration duration to extend
   */
  public void extendLockTime(JobInformation jobInformation, Duration duration) {
    jobInformation.getBpmnEngine().extendLockTime(jobInformation, duration);
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Finish the work                                                     */
  /*                                                                      */
  /*  Different method exist to finish a job:                             */
  /*  - complete : job is successful                                      */
  /*  - fail: job fail, and should be retried (or not)                    */
  /*  - throwBPMNError throw a BPMN Error                                 */
  /* ******************************************************************** */

  public void complete(JobInformation jobInformation, Map<String, Object> variablesToSubmit) throws EngineException {
    jobInformation.getBpmnEngine().complete(jobInformation, variablesToSubmit);
  }

  public void fail(JobInformation jobInformation,int nbRetry, Map<String, Object> variables) throws EngineException {
    jobInformation.getBpmnEngine().fail(jobInformation, nbRetry, variables);
  }

  public void throwBpmnError(JobInformation jobInformation, String errorCode, String errorMessage, Map<String, Object> variables) throws EngineException {
    jobInformation.getBpmnEngine().throwBpmnError(jobInformation, errorCode, errorMessage, variables);
  }

}
