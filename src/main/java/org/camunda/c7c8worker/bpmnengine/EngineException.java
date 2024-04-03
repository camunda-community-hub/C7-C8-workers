package org.camunda.c7c8worker.bpmnengine;


public class EngineException extends Exception {
  public int code;
  public String message;

  public EngineException(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public EngineException(String message) {
    this.message = message;
  }

  public EngineException(String message, Exception exception) {
    this.message = message + " : " + exception.getMessage();
  }

  public String getMessage() {
    return message;
  }

  public int getCode() {
    return code;
  }
}
