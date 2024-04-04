# C7-C8-workers
Template to develop a worker who can communicate with the C7 or C8 engine

# Introduction

Migrating from Camunda 7 to Camunda 8 involves several steps, one of which is transitioning
from using the Java Delegates pattern for service tasks to the External Tasks pattern.

Here's an explanation of why this transition is necessary and how it can be done:

## Java Delegates to External Tasks

In Camunda 7, service tasks can be implemented using Java Delegates,
where Java classes implement the org.camunda.bpm.engine.delegate.JavaDelegate interface.
These classes define the logic to be executed when the service task is reached.
However, in Camunda 8, the only approach is to use external tasks to implement service tasks.

## Main difference between the Java Delegates and External task are:


**Java Delegates**: These are tightly coupled with the process engine and execute within the same JVM as the engine.
They are synchronous and executed within the same transaction as the process.

**External Tasks**: External Tasks, on the other hand, are executed asynchronously and independently
from the process engine. They allow for greater flexibility and scalability, as separate worker applications can execute them. Decoupling business logic from the process engine provides
advantages such as improved fault tolerance and easier scaling.

## The API is different beetween Camunda 7 and Camunda 8

While the principle of using External Tasks remains the same, there may be differences in the APIs
between Camunda 7 and Camunda 8.

This means that code written for Camunda 7's External Tasks may need to be updated
to work with Camunda 8.

# This project

Using an interface to abstract away the specifics of the Camunda version and decouple
your business logic from the workflow engine has several advantages:

## Version Agnosticism
By designing your business code to interact with Camunda through an interface, you create a layer
of abstraction that shields your code from the underlying changes in Camunda versions.
This means your business logic remains unaffected by upgrades or migrations between different
versions of Camunda.

## Code Maintenance
Having only one codebase to maintain simplifies the development and maintenance process.
You don't need to manage separate code branches or versions for different Camunda versions,
reducing complexity and potential errors.

## Bug Isolation and Reporting
During the migration process or while running on different Camunda versions simultaneously,
any bugs or issues encountered in your business logic can be isolated from the Camunda version
itself. This means you can address and fix bugs within your code independently of the
Camunda version, without needing to coordinate with the Camunda development team.

## Flexibility and Scalability
Abstracting away the Camunda version through an interface provides flexibility to adapt to
future changes or new versions of Camunda without impacting your business logic.
It also allows for easier scaling and integration with other systems, as your business code
remains independent and modular.

## Testing and Validation
With a version-agnostic approach, you can test your business logic independently of the
Camunda version, ensuring its correctness and reliability. This separation of concerns makes it
easier to validate the behavior of your business processes across different Camunda environments.

## Future-Proofing
As Camunda continues to evolve, having a version-agnostic business codebase ensures that
Your application will remain resilient to future changes in the workflow engine.
You can adopt new features or migrate to newer versions of Camunda at your own pace
without disrupting your existing business logic.

# How to implement it?

## Principle

To create your worker in the `org.camunda.c5c8worker.worker` package, you can follow a similar structure
as the `DelayWorker` worker. Here's an example of how you can create a simple worker.

Follow this procedure
* The worker must be a `@Component`

It is the way the SpringBoot application detects it and opens a worker.

* Implement the `getType()` method

This method returns the type of the worker.

* Implement the `executeWorker()` method

In Camunda 7 or Camunda 8, the worker will call this method.
All information is available in the `JobInformation` object. You can access a method via `getVariable()`

````
    String delayPolicy = (String) jobInformation.getVariable(DELAY_POLICY);
````

In the end, the worker must call the `complete()`, `fail()`, or `throwBpmnError()` method.
These methods are available in the `BaseWorker'class.

## Additional

Check the `BaseWorker` class. Multiple methods are available to parametrize the worker.
Some methods are used only on Camunda 8, such as `isStreamEnabled()`. This option is valid only
for a Camunda 8 server upper than 8.3.

# Connect to the Camunda server

Your application needs to connect to a Camunda server, 7 or 8.
This application can connect to any server simultaneously: it's possible
to give a Camunda 7 server and a Camunda 8 server. The application will open a worker on the Camunda 7 server and a second worker on the Camunda 8 worker.

The list of servers can be given in multiple ways.

## List of servers in the application.yaml

In application.yaml, describe a server list under the variable c7c8worker.serverList

Each record must have
* a type: `camunda7`, `camunda8`, `camunda8Saas`
* a name to identify the server

Then, other parameters depend on the type.

### Camunda 7

Provide
* URL to connect the server
* max Jobs Active to get the number of active jobs

### Camunda 8

Provide
* The zeebeGatewayAddress to access the Camunda 8 Zeebe Gateway

Then, the worker details
* the number of workers thread
* max Jobs Active to get the number of active jobs

### Camunda 8 Saas

Provide information to connect the Saas:
* Region
* clusterId
* clientId
* secret
* oAuthUrl
* audience

Then, the worker details
* the number of workers thread
* max Jobs Active to get the number of active jobs

### example:

`````yaml
c7c8worker:
serversList:
- name: "camunda7Emeraud"
type: "camunda7"
url: "http://localhost:8080/engine-rest"
workerMaxJobsActive: 20

    - name: "Camunda8Ruby"
      type: "camunda8"      
      zeebeGatewayAddress: "127.0.0.1:26500"
      workerExecutionThreads: 10
      workerMaxJobsActive: 10

    - name: "Camunda8Grena"
      type: "camunda8saas"      
      region: "bru-2"
      clusterId: "4b..e2"
      clientId: "bs..6a"
      secret: "-E..ZG"
      oAuthUrl: "https://login.cloud.camunda.io/oauth/token"
      audience: "zeebe.camunda.io"
      workerExecutionThreads: 10
      workerMaxJobsActive: 10
`````

## Connection string of servers in the application.yaml or environment variable
Running the application as a Docker image in a Helm cluster makes it easier to provide the connection string as an environment variable, not to rebuild the image with a new application.yaml

Leave the `serverList` empty, and provide the connection string under the variable `c7c8worker.serversConnection`

Each server connection is separated by a semi-column (`;`) 

Each parameter in a server connection is separated by a comma (`,`)

The parameter starts with the server type, and other parameters depend on the server type.
Then, the previous list of servers is

`````yaml
c7c8worker:
  serversConnection : "camunda7Emeraud,camunda7,http://localhost:8080/engine-rest,20;Camunda8Ruby,camunda8,127.0.0.1:26500,10,10;Camunda8Grena,camunda8saas,bru-2,4b..e2,bs..6a,-E..ZG,https://login.cloud.camunda.io/oauth/token,zeebe.camunda.io,10,10"
`````

Using a Docker image, the value can be pass
````
   environment: 
      - C7C8WORKER_SERVERSCONNECTION="camunda7Emeraud,camunda7,http://localhost:8080/engine-rest,20;Camunda8Ruby,camunda8,127.0.0.1:26500,10,10;Camunda8Grena,camunda8saas,bru-2,4b..e2,bs..6a,-E..ZG,https://login.cloud.camunda.io/oauth/token,zeebe.camunda.io,10,10"
 
````


# Generate a JAR file for a Camunda 8 run time

It's possible to generate a JAR file to be deployed in a Camunda 8 run time, like the [Cherry Runtime](https://github.com/camunda-community-hub/zeebe-cherry-runtime)

To generate the JAR file, execute:

````shell
mvn install
````
A jar file is generated under the target folder.

Copy this JAR file and upload it on the Cherry runtime. Workers will be detected.

