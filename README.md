# C7-C8-workers
Template to develop a worker which can communicate to C7 or C8 engine

# Introduction

Migrating from Camunda 7 to Camunda 8 involves several steps, one of which is transitioning 
from using Java Delegates pattern for service tasks to External Tasks pattern. 

Here's an explanation of why this transition is necessary and how it can be done:

Java Delegates to External Tasks: In Camunda 7, service tasks can be implemented using Java Delegates,
where Java classes implement the org.camunda.bpm.engine.delegate.JavaDelegate interface. 
These classes define the logic to be executed when the service task is reached in the process. 
However, in Camunda 8, the only approach is to use External Tasks for implementing service tasks.

Java Delegates: These are tightly coupled with the process engine and execute within the same JVM as the engine. 
They are synchronous and executed within the same transaction as the process.

External Tasks: External Tasks, on the other hand, are executed asynchronously and independently 
from the process engine. They allow for greater flexibility and scalability, as they can be executed 
by separate worker applications. This decoupling of business logic from the process engine provides 
advantages such as improved fault tolerance and easier scaling.

Migration of External Tasks: Once the business logic has been migrated from Java Delegates to External Tasks, 
the next step is to migrate these External Tasks from Camunda 7 to Camunda 8. 

While the principle of using External Tasks remains the same, there may be differences in the APIs 
between Camunda 7 and Camunda 8.

API Differences: Although the core principle of using External Tasks remains consistent, 
there may be changes in the specific APIs, methods, or configurations between different versions 
of Camunda. This means that code written for Camunda 7's External Tasks may need to be updated 
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
your application remains resilient to future changes in the workflow engine. 
You can adopt new features or migrate to newer versions of Camunda at your own pace, 
without disrupting your existing business logic.

# How to implement it?

## Principle

To create your own worker in the `org.camunda.c5c8worker.worker` package, you can follow a similar structure 
as the `DelayWorker` worker. Here's an example of how you can create a simple worker.

Follow this procedure
* the worker must be a @Component

This is the way the SpringBoot application detect it and open a worker

* Implement the `getType()` method

This method return the type of the worker

* Implement the `executeWorker` method

The worker, in Camunda 7 or Camunda 8, will call this method.
All information are avaible in the `JobInformation` object. You can access a method via `getVariable()` 

````java
    String delayPolicy = (String) jobInformation.getVariable(DELAY_POLICY);
````

At the end, worker must call `complete()`, `fail()` or `throwBpmnError()`method. 
These methods are available in the `BaseWorker`class.

## Additional

Check the `BaseWorker` class. Multiple method are available to parametrize the worker.
Some methods are used only on Camunda 8, for example `isStreamEnabled()`. This option is valid only 
for a Camunda 8 server upper than 8.3.

# Connect to Camunda server

Your application need to connect to a Camunda server, 7 or 8.
Actually, this application can connect to any server, at the same time: it's possible for example
to give a Camunda 7 server and a Camunda 8 server. The application will open a worker to the Camunda 7 server, and a second worker on the Camunda 8 worker.

The list of server can be give on multiple way

## List of servers in application.yaml

## Connection string of servers in application.yaml or environment variable


# Generate a JAR file for a Camunda 8 run time
It's possible to generate a JAR file, to be deploy in a Camunda 8 run time, like the Cherry Runtime
(visit https://github.com/camunda-community-hub/zeebe-cherry-runtime)

To generate the JAR file, execute

````shell
mvn install
````
A jar file is generated under the target folder.

Copy this JAR file and upload it on the Cherry runtime. Workers will be detected.

