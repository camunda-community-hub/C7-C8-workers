package org.camunda.c7c8worker.bpmnengine;
/**
 * This is the encapsulated information for Worker, C7 or C8
 */

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobInformation {

  private Logger logger = LoggerFactory.getLogger(JobInformation.class);

  public JobClient jobClient;
  public ActivatedJob activatedJob;
  public ExternalTask externalTask;
  public ExternalTaskService externalTaskService;
  public int retries;

  private final BpmnEngine bpmnEngine;

  public JobInformation(JobClient jobClient, ActivatedJob activatedJob, BpmnEngine bpmnEngine) {
    this.jobClient = jobClient;
    this.activatedJob = activatedJob;
    this.retries = activatedJob.getRetries();
    this.bpmnEngine = bpmnEngine;
  }

  public JobInformation(ExternalTask externalTask, ExternalTaskService externalTaskService, BpmnEngine bpmnEngine) {
    this.externalTask = externalTask;
    this.externalTaskService = externalTaskService;
    this.retries = externalTask.getRetries();
    this.bpmnEngine = bpmnEngine;
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  Information                                                         */
  /*                                                                      */
  /*  Get information on job and engine                                   */
  /* ******************************************************************** */

  /**
   * if the worker need to know if the engine is a C8 engine
   *
   * @return true if the engine who submitted the job is a C8
   */
  public boolean isC8Engine() {
    return jobClient != null;
  }

  /**
   * if the worker need to know if the engine is a C7 engine
   *
   * @return true if the engine who submitted the job is a C7
   */
  public boolean isC7Engine() {
    return externalTask != null;
  }

  /**
   * Return the BPMN Engine behind the job
   *
   * @return the BPM Engine
   */
  public BpmnEngine getBpmnEngine() {
    return bpmnEngine;
  }

  /**
   * return the number of retries on the job
   *
   * @return number of retries
   */
  public int getRetries() {
    return retries;
  }

  public String getJobId() {
    if (externalTask != null)
      return externalTask.getId();
    if (activatedJob != null)
      return String.valueOf(activatedJob.getKey());
    logger.error("GetJobId [{}] no information about the job");
    return null;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  get Variables                                                       */
  /*                                                                      */
  /*  Get process variables                                               */
  /* ******************************************************************** */

  /**
   * Get a variable
   *
   * @return a variable
   */
  public Object getVariable(String name) {
    if (externalTask != null)
      return externalTask.getVariable(name);
    if (activatedJob != null)
      return activatedJob.getVariablesAsMap().get(name);
    logger.error("GetVariable [{}] no information about the job", name);
    return null;
  }


}
