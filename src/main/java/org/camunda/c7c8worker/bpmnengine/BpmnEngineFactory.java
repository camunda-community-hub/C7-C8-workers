/* ******************************************************************** */
/*                                                                      */
/*  BpmnEngineFactory                                                    */
/*                                                                      */
/*  Generate the client to the engine                                    */
/* ******************************************************************** */
package org.camunda.c7c8worker.bpmnengine;


import org.camunda.c7c8worker.bpmnengine.camunda7.BpmnEngineCamunda7;
import org.camunda.c7c8worker.bpmnengine.camunda8.BpmnEngineCamunda8;
import org.camunda.c7c8worker.configuration.BpmnEngineList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
/**
 * This can't be a Component, to be used in AutomatorAPI
 */
public class BpmnEngineFactory {

  static Logger logger = LoggerFactory.getLogger(BpmnEngineFactory.class);

  List<BpmnEngine> listEngines = new ArrayList<>();

@Autowired
  BpmnEngineList bpmEngineList;

  /**
   * This method must be call to initialize the factory
   * Note: this is not a PostConstruct because we give this operation to the application, to get control on the order of the execution
   *
   */
  public void initialize() {
    bpmEngineList.initialize();
    for (BpmnEngineList.BpmnServerDefinition serverDefinition : bpmEngineList.getListServers())
    {
      try {
        BpmnEngine engine = getEngineFromConfiguration(serverDefinition, false);
        engine.init();
        engine.connection();
        listEngines.add( engine);
      } catch (EngineException e) {
        logger.error("During intializing server [{}] : {}", serverDefinition.toString(), e.getMessage());
      }
    }
  }

  public List<BpmnEngine> getListEngines() {
    return listEngines;
  }

  private BpmnEngine getEngineFromConfiguration(BpmnEngineList.BpmnServerDefinition serverDefinition,
                                               boolean logDebug)
      throws EngineException {
    return switch (serverDefinition.serverType) {
        case CAMUNDA_7 -> new BpmnEngineCamunda7(serverDefinition, logDebug);

        case CAMUNDA_8 -> BpmnEngineCamunda8.getFromServerDefinition(serverDefinition, logDebug);

        case CAMUNDA_8_SAAS -> BpmnEngineCamunda8.getFromServerDefinition( serverDefinition, logDebug);


      };
  }

  public BpmnEngine getCamunda7() {
    Optional<BpmnEngine> camunda8= listEngines.stream().filter(t->
        t.getTypeCamundaEngine().equals(BpmnEngineList.CamundaEngine.CAMUNDA_7)).findFirst();
    return  camunda8.isPresent()? camunda8.get() : null;
  }

  public BpmnEngine getCamunda8() {
    Optional<BpmnEngine> camunda8= listEngines.stream().filter(t->
    ! t.getTypeCamundaEngine().equals(BpmnEngineList.CamundaEngine.CAMUNDA_7)).findFirst();
    return  camunda8.isPresent()? camunda8.get() : null;
  }

}
