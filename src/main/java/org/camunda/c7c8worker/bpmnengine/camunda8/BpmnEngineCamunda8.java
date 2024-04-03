package org.camunda.c7c8worker.bpmnengine.camunda8;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;
import org.camunda.c7c8worker.bpmnengine.BpmnEngine;
import org.camunda.c7c8worker.bpmnengine.EngineException;
import org.camunda.c7c8worker.bpmnengine.FixedBackoffSupplier;
import org.camunda.c7c8worker.bpmnengine.JobInformation;
import org.camunda.c7c8worker.configuration.BpmnEngineList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;

public class BpmnEngineCamunda8 implements BpmnEngine {

  public static final int SEARCH_MAX_SIZE = 100;
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda8.class);

  private BpmnEngineList.BpmnServerDefinition serverDefinition;
  /**
   * It is not possible to search user task for a specfic processInstance. So, to realize this, a marker is created in each process instance. Retrieving the user task,
   * the process instance can be found and correction can be done
   */
  private ZeebeClient zeebeClient;
  // Default
  private BpmnEngineList.CamundaEngine typeCamundaEngine = BpmnEngineList.CamundaEngine.CAMUNDA_8;

  private BpmnEngineCamunda8() {
  }

  /**
   * Constructor from existing object
   *
   * @param serverDefinition server definition
   * @param logDebug         if true, operation will be log as debug level
   */
  public static BpmnEngineCamunda8 getFromServerDefinition(BpmnEngineList.BpmnServerDefinition serverDefinition,
                                                           boolean logDebug) {
    BpmnEngineCamunda8 bpmnEngineCamunda8 = new BpmnEngineCamunda8();
    bpmnEngineCamunda8.serverDefinition = serverDefinition;
    return bpmnEngineCamunda8;

  }

  /**
   * Constructor to specify a Self Manage Zeebe Address por a Zeebe Saas
   *
   * @param zeebeSelfGatewayAddress    Self Manage : zeebe address
   * @param zeebeSelfSecurityPlainText Self Manage: Plain text
   * @param operateUrl                 URL to access Operate
   * @param operateUserName            Operate user name
   * @param operateUserPassword        Operate password
   * @param tasklistUrl                Url to access TaskList
   */
  public static BpmnEngineCamunda8 getFromCamunda8(String zeebeSelfGatewayAddress,
                                                   String zeebeSelfSecurityPlainText,
                                                   String operateUrl,
                                                   String operateUserName,
                                                   String operateUserPassword,
                                                   String tasklistUrl) {
    BpmnEngineCamunda8 bpmnEngineCamunda8 = new BpmnEngineCamunda8();
    bpmnEngineCamunda8.serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    bpmnEngineCamunda8.serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;
    bpmnEngineCamunda8.serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    bpmnEngineCamunda8.serverDefinition.zeebeGatewayAddress = zeebeSelfGatewayAddress;
    bpmnEngineCamunda8.serverDefinition.zeebeSecurityPlainText = zeebeSelfSecurityPlainText;

    return bpmnEngineCamunda8;
  }

  /**
   * Constructor to specify a Self Manage Zeebe Address por a Zeebe Saas
   *
   * @param zeebeSaasCloudRegister  Saas Cloud Register information
   * @param zeebeSaasCloudRegion    Saas Cloud region
   * @param zeebeSaasCloudClusterId Saas Cloud ClusterID
   * @param zeebeSaasCloudClientId  Saas Cloud ClientID
   * @param zeebeSaasClientSecret   Saas Cloud Client Secret
   * @param operateUrl              URL to access Operate
   * @param operateUserName         Operate user name
   * @param operateUserPassword     Operate password
   * @param tasklistUrl             Url to access TaskList
   */
  public static BpmnEngineCamunda8 getFromCamunda8SaaS(

      String zeebeSaasCloudRegister,
      String zeebeSaasCloudRegion,
      String zeebeSaasCloudClusterId,
      String zeebeSaasCloudClientId,
      String zeebeSaasOAuthUrl,
      String zeebeSaasAudience,
      String zeebeSaasClientSecret,
      String operateUrl,
      String operateUserName,
      String operateUserPassword,
      String tasklistUrl) {
    BpmnEngineCamunda8 bpmnEngineCamunda8 = new BpmnEngineCamunda8();
    bpmnEngineCamunda8.serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    bpmnEngineCamunda8.serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;


    /*
     * SaaS Zeebe
     */
    bpmnEngineCamunda8.serverDefinition.zeebeSaasRegion = zeebeSaasCloudRegion;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasClusterId = zeebeSaasCloudClusterId;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasClientId = zeebeSaasCloudClientId;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasClientSecret = zeebeSaasClientSecret;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasOAuthUrl = zeebeSaasOAuthUrl;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasAudience = zeebeSaasAudience;

    return bpmnEngineCamunda8;
  }

  @Override
  public void init() {
    // nothing to do there
  }

  @Override
  public void connection() throws EngineException {

    final String defaultAddress = "localhost:26500";
    final String envVarAddress = System.getenv("ZEEBE_ADDRESS");

    // connection is critical, so let build the analysis
    StringBuilder analysis = new StringBuilder();
    boolean isOk = true;

    isOk = stillOk(serverDefinition.name, "ZeebeConnection", analysis, false, isOk);
    this.typeCamundaEngine = this.serverDefinition.serverType;

    final ZeebeClientBuilder clientBuilder;

    // ---------------------------- Camunda Saas
    if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(this.typeCamundaEngine)) {
      String gatewayAddressCloud =
          serverDefinition.zeebeSaasClusterId + "." + serverDefinition.zeebeSaasRegion + ".zeebe.camunda.io:443";
      stillOk(gatewayAddressCloud, "GatewayAddress", analysis, false, true);
      stillOk(serverDefinition.zeebeSaasClientId, "ClientId", analysis, false, true);

      /* Connect to Camunda Cloud Cluster, assumes that credentials are set in environment variables.
       * See JavaDoc on class level for details
       */
      isOk = stillOk(serverDefinition.zeebeSaasOAuthUrl, "OAutorisationServerUrl", analysis, true, isOk);
      isOk = stillOk(serverDefinition.zeebeSaasClientId, "ClientId", analysis, true, isOk);
      isOk = stillOk(serverDefinition.zeebeSaasClientSecret, "ClientSecret", analysis, true, isOk);

      try {
        String audience = serverDefinition.zeebeSaasAudience != null ? serverDefinition.zeebeSaasAudience : "";
        OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder() // formatting
            .authorizationServerUrl(serverDefinition.zeebeSaasOAuthUrl)
            .audience(audience)
            .clientId(serverDefinition.zeebeSaasClientId)
            .clientSecret(serverDefinition.zeebeSaasClientSecret)
            .build();

        clientBuilder = ZeebeClient.newClientBuilder()
            .gatewayAddress(gatewayAddressCloud)
            .credentialsProvider(credentialsProvider);

      } catch (Exception e) {
        zeebeClient = null;
        throw new EngineException(
            "Bad credential [" + serverDefinition.name + "] Analysis:" + analysis + " fail : " + e.getMessage());
      }

      typeCamundaEngine = BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS;

      //---------------------------- Camunda 8 Self Manage
    } else if (serverDefinition.zeebeGatewayAddress != null && !this.serverDefinition.zeebeGatewayAddress.trim()
        .isEmpty()) {
      isOk = stillOk(serverDefinition.zeebeGatewayAddress, "GatewayAddress", analysis, true, isOk);

      // connect to local deployment; assumes that authentication is disabled
      clientBuilder = ZeebeClient.newClientBuilder()
          .gatewayAddress(serverDefinition.zeebeGatewayAddress)
          .usePlaintext();
      typeCamundaEngine = BpmnEngineList.CamundaEngine.CAMUNDA_8;
    } else
      throw new EngineException("Invalid configuration");

    // ---------------- connection
    try {
      isOk = stillOk(serverDefinition.workerExecutionThreads, "ExecutionThread", analysis, false, isOk);

      analysis.append(" ExecutionThread[");
      analysis.append(serverDefinition.workerExecutionThreads);
      analysis.append("] MaxJobsActive[");
      analysis.append(serverDefinition.workerMaxJobsActive);
      analysis.append("] ");
      if (serverDefinition.workerMaxJobsActive == -1) {
        serverDefinition.workerMaxJobsActive = serverDefinition.workerExecutionThreads;
        analysis.append("No workerMaxJobsActive defined, align to the number of threads, ");
      }
      if (serverDefinition.workerExecutionThreads > serverDefinition.workerMaxJobsActive) {
        logger.error(
            "Camunda8 [{}] Incorrect definition: the workerExecutionThreads {} must be <= workerMaxJobsActive {} , else ZeebeClient will not fetch enough jobs to feed threads",
            serverDefinition.name, serverDefinition.workerExecutionThreads, serverDefinition.workerMaxJobsActive);
      }

      if (!isOk)
        throw new EngineException("Invalid configuration " + analysis.toString());

      clientBuilder.numJobWorkerExecutionThreads(serverDefinition.workerExecutionThreads);
      clientBuilder.defaultJobWorkerMaxJobsActive(serverDefinition.workerMaxJobsActive);

      analysis.append("Zeebe connection...");
      zeebeClient = clientBuilder.build();

      // simple test
      Topology join = zeebeClient.newTopologyRequest().send().join();

      // Actually, if an error arrived, an exception is thrown
      analysis.append(join != null ? "successfully," : "error");


      logger.info(analysis.toString());

    } catch (Exception e) {
      zeebeClient = null;
      throw new EngineException(
          "Can't connect to Server[" + serverDefinition.name + "] Analysis:" + analysis + " fail : " + e.getMessage());
    }
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
    return zeebeClient != null;
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  User tasks                                                          */
  /*                                                                      */
  /* ******************************************************************** */

  /* ******************************************************************** */
  /*                                                                      */
  /*  Service tasks                                                       */
  /*                                                                      */
  /* ******************************************************************** */
  @Override
  public RegisteredTask registerServiceTask(String workerId,
                                            String topic,
                                            Duration lockTime,
                                            Object jobHandler,
                                            FixedBackoffSupplier backoffSupplier) {
    if (!(jobHandler instanceof JobHandler)) {
      logger.error("handler is not a JobHandler implementation, can't register the worker [{}], topic [{}]", workerId,
          topic);
      return null;
    }
    if (topic == null) {
      logger.error("topic must not be null, can't register the worker [{}]", workerId);
      return null;

    }
    RegisteredTask registeredTask = new RegisteredTask();

    JobWorkerBuilderStep1.JobWorkerBuilderStep3 step3 = zeebeClient.newWorker()
        .jobType(topic)
        .handler((JobHandler) jobHandler)
        .timeout(lockTime)
        .name(workerId);

    if (backoffSupplier != null) {
      step3.backoffSupplier(backoffSupplier);
    }
    registeredTask.jobWorker = step3.open();
    return registeredTask;
  }

  public void extendLockTime(JobInformation jobInformation, Duration duration){}

  /* ******************************************************************** */
  /*                                                                      */
  /*  Complete operations                                                       */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public void complete(JobInformation jobInformation, Map<String, Object> variables) throws EngineException {
    try {
      zeebeClient.newCompleteCommand(jobInformation.activatedJob.getKey()).variables(variables).send().join();
    } catch (Exception e) {
      throw new EngineException("Can't execute service task " + e.getMessage());
    }
  }

  /**
   * Fail a service task
   *
   * @param jobInformation JobInformation
   * @param nbRetry        number of retry
   * @param variables      variable to updates
   * @throws EngineException in case of error
   */
  @Override
  public void fail(JobInformation jobInformation, int nbRetry, Map<String, Object> variables) throws EngineException {
    try {
      zeebeClient.newFailCommand(jobInformation.activatedJob.getKey())
          .retries(nbRetry)
          .variables(variables)
          .send()
          .join();
    } catch (Exception e) {
      throw new EngineException("Can't execute service task " + e.getMessage());
    }
  }

  /**
   * Fail a service task
   *
   * @param jobInformation JobInformation
   * @param errorCode      error BPMN code
   * @param errorMessage   error BPMN Message
   * @param variables      variable to updates
   * @throws EngineException in case of error
   */
  @Override
  public void throwBpmnError(JobInformation jobInformation,
                             String errorCode,
                             String errorMessage,
                             Map<String, Object> variables) throws EngineException {
    try {
      zeebeClient.newThrowErrorCommand(jobInformation.activatedJob.getKey())
          .errorCode(errorCode)
          .errorMessage(errorMessage)
          .variables(variables)
          .send()
          .join();
    } catch (Exception e) {
      throw new EngineException("Can't execute service task " + e.getMessage());
    }

  }

  @Override
  public BpmnEngineList.CamundaEngine getTypeCamundaEngine() {
    return typeCamundaEngine;
  }


  @Override
  public String getSignature() {
    String signature = typeCamundaEngine.toString() + " ";
    if (typeCamundaEngine.equals(BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS))
      signature +=
          "Cloud ClientId[" + serverDefinition.zeebeSaasClientId + "] ClusterId[" + serverDefinition.zeebeSaasClusterId
              + "]";
    else
      signature += "Address[" + serverDefinition.zeebeGatewayAddress + "]";
    signature += " numJobWorkerExecutionThreads[" + serverDefinition.workerExecutionThreads + "] workerMaxJobsActive["
        + serverDefinition.workerMaxJobsActive + "]";
    return signature;
  }


  @Override
  public int getWorkerExecutionThreads() {
    return serverDefinition != null ? serverDefinition.workerExecutionThreads : 0;
  }


  public ZeebeClient getZeebeClient() {
    return zeebeClient;
  }

  /**
   * add in analysis and check the consistence
   *
   * @param value       value to check
   * @param message     name of parameter
   * @param analysis    analysis builder
   * @param check       true if the value must not be null or empty
   * @param wasOkBefore previous value, is returned if this check is Ok
   * @return previous value is ok false else
   */
  private boolean stillOk(Object value, String message, StringBuilder analysis, boolean check, boolean wasOkBefore) {
    analysis.append(message);
    analysis.append(" [");
    analysis.append(value);
    analysis.append(" ]");

    if (check) {
      if (value == null || (value instanceof String && ((String) value).isEmpty())) {
        analysis.append("No ");
        analysis.append(message);
        return false;
      }
    }
    return wasOkBefore;
  }

}
