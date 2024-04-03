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
To create your own worker in the io.camunda.workers package, you can follow a similar structure 
as the ping worker. Here's an example of how you can create a simple worker:

In this example:

Replace "myWorkerTopic" with the topic name that your worker subscribes to.
Inside the handle() method, you can access process variables using externalTask.getVariable("variableName").
Implement your business logic within the handle() method.
Use externalTaskService.complete(externalTask) to complete the task successfully.
Use externalTaskService.fail(externalTask) or externalTaskService.bpmnError(externalTask, errorCode) to handle failures or BPMN errors respectively.
Ensure that you have the necessary dependencies set up for your project to compile and run successfully. You may need to include Camunda BPM client libraries or any other dependencies required by your business logic.