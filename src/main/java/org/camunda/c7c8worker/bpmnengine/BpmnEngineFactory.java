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

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * This can't be a Component, to be used in AutomatorAPI
 */
public class BpmnEngineFactory {

  private static final BpmnEngineFactory bpmnEngineFactory = new BpmnEngineFactory();
  Map<BpmnEngineList.CamundaEngine, BpmnEngine> cacheEngine = new EnumMap<>(BpmnEngineList.CamundaEngine.class);

  public static BpmnEngineFactory getInstance() {
    return bpmnEngineFactory;
  }

  public BpmnEngine getEngineFromConfiguration(BpmnEngineList.BpmnServerDefinition serverDefinition,
                                               boolean logDebug)
      throws EngineException {
    BpmnEngine engine = cacheEngine.get(serverDefinition.serverType);
    if (engine != null)
      return engine;

    // instantiate and initialize the engine now
    synchronized (this) {
      engine = cacheEngine.get(serverDefinition.serverType);
      if (engine != null)
        return engine;

      engine = switch (serverDefinition.serverType) {
        case CAMUNDA_7 -> new BpmnEngineCamunda7(serverDefinition, logDebug);

        case CAMUNDA_8 -> BpmnEngineCamunda8.getFromServerDefinition(serverDefinition, logDebug);

        case CAMUNDA_8_SAAS -> BpmnEngineCamunda8.getFromServerDefinition( serverDefinition, logDebug);


      };

      engine.init();
      engine.connection();
      cacheEngine.put(serverDefinition.serverType, engine);
    }
    return engine;
  }

  public BpmnEngine getCamunda7() {
    return cacheEngine.get(BpmnEngineList.CamundaEngine.CAMUNDA_7);
  }

  public BpmnEngine getCamunda8() {
    Optional<BpmnEngine> camunda8= cacheEngine.values().stream().filter(t->
    {return ! t.getTypeCamundaEngine().equals(BpmnEngineList.CamundaEngine.CAMUNDA_7);}).findFirst();
    return  camunda8.isPresent()? camunda8.get() : null;
  }

}
