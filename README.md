# AWS SQS Library
The `mastercraftcoding-aws-sqs` library is intended to aid in the use of Amazon SQS for
Java Spring-based projects.

## Configuration Properties

**`aws.sqs.accessKeyId`** - This value should be set to the Amazon Web Services access key assigned to your AWS account.
**`aws.sqs.secretAccessKey`** - This is the secret key that is associated with the above "accessKeyId".

## Security Rights
The account associated with the above security information must have all access rights to the SQS service
for the functionality in this library to work properly.

## Getting Started
The Master Craft Coding AWS SQS library requires two pieces of information.  

1.  You must create an instance of `com.mastercraftcoding.aws.sqs.QueueManager` that is
configured to control access to the AWS SQS using the credentials defined above.

2.  You must create an instance of a `com.mastercraftcoding.aws.sqs.QueueConfiguration` that 
describes each queue your application will be using.  This is true for both reading 
messages from a queue, and writing message into a queue.

The `QueueManager` and `QueueConfiguration` classes support both methods of Spring Bean
instantiation - xml file configuration and code based configuration.  See the unit test code for further
examples of each.

### QueueManager
**Code Based Spring Bean Creation**
```java
public class MyConfiguration {

    @Bean
    public QueueManager queueManager() {

        // Create the QueueManager instance we want to use.
        return new QueueManager(Region.US_EAST_1, RuntimeEnvironment.IntegrationTesting, "MyApplicationName");
    }
}
```

**XML Based Spring Bean Creation**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
    
    <bean id="queueManager" class="com.mastercraftcoding.aws.sqs.QueueManager" scope="singleton">
        <constructor-arg type="software.amazon.awssdk.regions.Region" value="US_EAST_1"/>
        <constructor-arg type="com.mastercraftcoding.aws.sqs.RuntimeEnvironment" value="IntegrationTesting"/>
        <constructor-arg value="MyApplicationName"/>
    </bean>
</beans>
```

### QueueConfiguration

**Code Based Spring Bean Creation**

```java
public class MyConfiguration {

    @Bean("EmailQueueConfiguration")
    public QueueConfiguration emailQueue() {

        return QueueConfiguration.builder()
                .queueBaseName("EmailQueue")
                .isFifoQueue(false)
                .createDeadLetterQueue(false)
                .receiveMessageWaitTimeSeconds(20)
                .build();
    }
}
```

**XML Based Spring Bean Creation**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="EmailQueueConfiguration" class="com.mastercraftcoding.aws.sqs.QueueConfiguration">
        <property name="queueBaseName" value="EmailQueue"/>
        <property name="fifoQueue" value="false"/>
        <property name="createDeadLetterQueue" value="false"/>
        <property name="receiveMessageWaitTimeSeconds" value="20"/>
    </bean>
</beans>
```