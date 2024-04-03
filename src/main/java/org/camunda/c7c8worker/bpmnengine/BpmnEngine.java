package org.camunda.c7c8worker.bpmnengine;

import io.camunda.zeebe.client.api.worker.JobWorker;
import org.camunda.bpm.client.topic.TopicSubscription;
import org.camunda.c7c8worker.configuration.BpmnEngineList;

import java.time.Duration;
import java.util.Map;

public interface BpmnEngine {

  /**
   * init the engine.
   */
  void init();

  void connection() throws EngineException;

  void disconnection() throws EngineException;

  /**
   * Engine is ready. If not, a connection() method must be call
   *
   * @return true if the engine is ready
   */
  boolean isReady();




  /* ******************************************************************** */
  /*                                                                      */
  /*  Service tasks                                                       */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * @param workerId        workerId
   * @param topic           topic to register
   * @param lockTime        lock time for the job
   * @param jobHandler      C7: must implement ExternalTaskHandler. C8: must implement JobHandler
   * @param backoffSupplier backOffStrategy
   * @return list of Service Task
   */
  RegisteredTask registerServiceTask(String workerId,
                                     String topic,
                                     Duration lockTime,
                                     Object jobHandler,
                                     FixedBackoffSupplier backoffSupplier);

  /**
   * Extend the lock duration
   * @param duration duration to extend the lock
   */
   void extendLockTime(JobInformation jobInformation, Duration duration);

    /**
     * Complete a service task
     *
     * @param jobInformation JobInformation
     * @param variables      variable to updates
     * @throws EngineException in case of error
     */
  void complete(JobInformation jobInformation, Map<String, Object> variables) throws EngineException;

  /**
   * Fail a service task
   *
   * @param jobInformation JobInformation
   * @param nbRetry        number of retry
   * @param variables      variable to updates
   * @throws EngineException in case of error
   */
  void fail(JobInformation jobInformation, int nbRetry, Map<String, Object> variables) throws EngineException;

  /**
   * Fail a service task
   *
   * @param jobInformation JobInformation
   * @param errorCode      error code
   * @param errorMsg       error message
   * @param variables      variable to updates
   * @throws EngineException in case of error
   */
  void throwBpmnError(JobInformation jobInformation, String errorCode, String errorMsg, Map<String, Object> variables)
      throws EngineException;




  /* ******************************************************************** */
  /*                                                                      */
  /*  get server definition                                               */
  /*                                                                      */
  /* ******************************************************************** */

  BpmnEngineList.CamundaEngine getTypeCamundaEngine();

  /**
   * return the signature of the engine, to log it for example
   *
   * @return signature of the engine
   */
  String getSignature();

  int getWorkerExecutionThreads();

  class RegisteredTask {
    public TopicSubscription topicSubscription;
    public JobWorker jobWorker;

    public boolean isNull() {
      return topicSubscription == null && jobWorker == null;
    }

    public boolean isClosed() {
      if (jobWorker != null)
        return jobWorker.isClosed();
      return topicSubscription == null;
    }

    public void close() {
      if (jobWorker != null)
        jobWorker.close();
      if (topicSubscription != null) {
        topicSubscription.close();
        topicSubscription = null;
      }
    }
  }

}
