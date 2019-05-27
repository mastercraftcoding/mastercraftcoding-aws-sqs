package com.mastercraftcoding.aws.sqs;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class QueueManager implements ApplicationContextAware, InitializingBean, DisposableBean {

    private ApplicationContext applicationContext;
    private SqsClient sqsClient;
    private Region targetRegion;
    private final RuntimeEnvironment runtimeEnvironment;
    private final String applicationName;

    @Value("${aws.sqs.accessKeyId}")
    private String awsAccessKeyId;

    @Value("${aws.sqs.secretAccessKey}")
    private String awsSecretAccessKey;

    private final HashSet<QueueConfiguration> registeredQueues;
    private final HashSet<MessageHandlerDriverThread> messageHandlerDriverThreads;

    public QueueManager(Region targetRegion, RuntimeEnvironment runtimeEnvironment, String applicationName) {
        this.targetRegion = targetRegion;
        this.runtimeEnvironment = runtimeEnvironment;
        this.applicationName = applicationName;
        this.registeredQueues = new HashSet<>();
        this.messageHandlerDriverThreads = new HashSet<>();
    }

    public QueueManager(Region targetRegion, String runtimeEnvironmentName, String applicationName) {
        this.runtimeEnvironment = RuntimeEnvironment.createCustomEnvironment(runtimeEnvironmentName);
        this.applicationName = applicationName;
        this.registeredQueues = new HashSet<>();

        this.sqsClient = SqsClient.builder()
                .region(targetRegion)
                .build();
        this.messageHandlerDriverThreads = new HashSet<>();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {

        this.sqsClient = SqsClient.builder()
                .region(targetRegion)
                .credentialsProvider(() -> new AwsCredentials() {
                    @Override
                    public String accessKeyId() {
                        return awsAccessKeyId;
                    }

                    @Override
                    public String secretAccessKey() {
                        return awsSecretAccessKey;
                    }
                })
                .build();

        // Make sure things were injected properly
        if( applicationContext == null ) {
            throw new IllegalStateException("Failed to autowire applicationContext");
        }

        // Use the application context to obtain a reference to all QueueConfiguration
        // objects.
        Map<String, QueueConfiguration> queueConfigurations = applicationContext.getBeansOfType(QueueConfiguration.class);
        if( queueConfigurations == null || queueConfigurations.isEmpty() ) {
            return; // Nothing to do.
        }

        // Loop through the list of queue configuration objects.
        for( QueueConfiguration nextQueueConfiguration : queueConfigurations.values()) {
            registerQueue(nextQueueConfiguration);
        }

        // Find all of the beans that implement the MessageHandler
        // interfaces.
        Map<String, MessageHandler> messageHandlerMap = applicationContext.getBeansOfType(MessageHandler.class);
        for(MessageHandler nextHandler : messageHandlerMap.values()) {
            messageHandlerDriverThreads.add(new MessageHandlerDriverThread(this, nextHandler, nextHandler.getDesiredMessageAttributes()));
        }
    }

    public RuntimeEnvironment getRuntimeEnvironment() {
        return runtimeEnvironment;
    }
    public String getApplicationName() {
        return applicationName;
    }

    public synchronized void registerQueue(QueueConfiguration queueConfiguration) {

        // See if a queue by this name already exists
        if (registeredQueues.contains(queueConfiguration)) {
            throw new IllegalArgumentException(String.format("A queue with the name '%1$s' is already registered.", queueConfiguration.getQueueBaseName()));
        }

        // Make sure this QueueConfiguration isn't already registered with someone else.
        if( queueConfiguration.getOwningQueueManager() != null ) {
            throw new IllegalArgumentException("This QueueConfiguration is already registered with another QueueManager instance.");
        }

        // Add the queue to the tracking hashmap
        registeredQueues.add(queueConfiguration);

        // Set the QueueConfiguration's owner to this queue manager
        queueConfiguration.getSystemConfiguration().setOwningQueueManager(this);

        // Make sure the queue in question has been created with AWS
        // First, if we need a dead letter queue, create it first since we have to
        // link it to the main queue.
        if( queueConfiguration.isCreateDeadLetterQueue()) {
            createQueue(queueConfiguration, true);
        }

        // Create the primary queue
        createQueue(queueConfiguration, false);

        // Attach the two together if we created a dead letter queue
        if( queueConfiguration.isCreateDeadLetterQueue()) {
            attachDeadLetterQueue(queueConfiguration);
        }
    }

    public String fullQueueName(QueueConfiguration queueConfiguration) {

        if( queueConfiguration == null ) {
            throw new IllegalArgumentException("queueConfiguration may not be null.");
        }

        return queueConfiguration.generateFullQueueName(this.runtimeEnvironment, this.applicationName);
    }

    public String fullDeadLetterQueueName(QueueConfiguration queueConfiguration) {

        if( queueConfiguration == null ) {
            throw new IllegalArgumentException("queueConfiguration may not be null.");
        }

        return queueConfiguration.generateFullDeadLetterQueueName(this.runtimeEnvironment, this.applicationName);
    }

    private void createQueue(QueueConfiguration queueConfiguration, boolean isDeadLetterQueue) {

        // ------------------------------------------------------
        // Set up the attributes for this queue
        // ------------------------------------------------------
        HashMap<QueueAttributeName, String> attributes = new HashMap<>();
        if( queueConfiguration.isFifoQueue() ) {
            attributes.put(QueueAttributeName.FIFO_QUEUE, "True");
        }

        // Set the default polling length for the queue
        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS,
                Integer.toString(queueConfiguration.getReceiveMessageWaitTimeSeconds()));

        // Set the visibility timeout for messages in this queue
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT,
                Integer.toString(queueConfiguration.getMessageVisibilityTimeoutSeconds()));

        // Set the message retention period
        attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD,
                Integer.toString(
                        isDeadLetterQueue ? queueConfiguration.getDeadLetterRetentionPeriodSeconds()
                                : queueConfiguration.getMessageRetentionPeriodSeconds()));

        // ------------------------------------------------------
        // Request to create the given queue name.
        // ------------------------------------------------------
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(
                        isDeadLetterQueue ?
                                queueConfiguration.generateFullDeadLetterQueueName(this.runtimeEnvironment, this.applicationName) :
                                queueConfiguration.generateFullQueueName(this.runtimeEnvironment, this.applicationName))
                .attributes(attributes)
                .build();

        CreateQueueResponse createResponse = sqsClient.createQueue(createQueueRequest);
        throwOnFailure(createResponse);

        // Update the configuration with the URL that we need to communicate
        // with the given queue.
        String targetQueueUrl = createResponse.queueUrl();
        if( isDeadLetterQueue ) {
            queueConfiguration.getSystemConfiguration().setQueueDeadLetterUrl(createResponse.queueUrl());
        }
        else {
            queueConfiguration.getSystemConfiguration().setQueueUrl(createResponse.queueUrl());
        }

        // ------------------------------------------------------
        // Get the ARN for this queue so we can link it to the
        // dead letter queue.
        // ------------------------------------------------------
        GetQueueAttributesRequest queueAttributesRequest = GetQueueAttributesRequest.builder()
                .queueUrl(targetQueueUrl)
                .attributeNames(QueueAttributeName.QUEUE_ARN)
                .build();

        GetQueueAttributesResponse queueAttributesResponse = sqsClient.getQueueAttributes(queueAttributesRequest);
        throwOnFailure(createResponse);

        // Store the queueArn back to the configuration
        if( isDeadLetterQueue ) {
            queueConfiguration.getSystemConfiguration().setQueueDeadLetterArn(queueAttributesResponse.attributes().get(QueueAttributeName.QUEUE_ARN));
        }
        else {
            queueConfiguration.getSystemConfiguration().setQueueArn(queueAttributesResponse.attributes().get(QueueAttributeName.QUEUE_ARN));
        }
    }

    /* package */ SqsClient SqsClient() {
        return this.sqsClient;
    }

    private void attachDeadLetterQueue(QueueConfiguration queueConfiguration) {

        // ------------------------------------------------------
        // Set the REDRIVE_POLICY on the given queue
        // ------------------------------------------------------
        HashMap<QueueAttributeName, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.REDRIVE_POLICY, "{\"maxReceiveCount\":\"" + queueConfiguration.getDeadLetterRetryCount() + "\", \"deadLetterTargetArn\":\""
                + queueConfiguration.getQueueDeadLetterArn() + "\"}");

        SetQueueAttributesRequest queueAttributesRequest = SetQueueAttributesRequest.builder()
                .queueUrl(queueConfiguration.getQueueUrl())
                .attributes(attributes)
                .build();

        // Verify that the call was successful
        SetQueueAttributesResponse queueAttributesResponse = sqsClient.setQueueAttributes(queueAttributesRequest);
        throwOnFailure(queueAttributesResponse);
    }

    public synchronized void deleteQueue(QueueConfiguration targetQueue) {

        // Validate the incoming parameter
        if(targetQueue == null) {
            throw new IllegalArgumentException("targetQueue may not be null.");
        }

        // First, make sure that the QueueConfiguration is associated with this queue manager instance.
        if(!registeredQueues.contains(targetQueue) ) {
            throw new IllegalStateException("the given target queue has not been registered with this QueueManager instance.");
        }

        // Delete the target queue
        deleteQueue(targetQueue.getQueueUrl());

        // If there is an attached dead letter queue, delete it as well.
        if( targetQueue.isCreateDeadLetterQueue() ) {
            deleteQueue(targetQueue.getQueueDeadLetterUrl());
        }

        // Remove the registry entry
        registeredQueues.remove(targetQueue);
    }

    private void deleteQueue(String queueUrl) {
        DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();

        DeleteQueueResponse deleteQueueResponse = sqsClient.deleteQueue(deleteQueueRequest);
        throwOnFailure(deleteQueueResponse);
    }

    public synchronized List<String> queueUrlList() {
        ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder()
                .queueNamePrefix(queueNamePrefix())
                .build();

        ListQueuesResponse listQueuesResponse = sqsClient.listQueues(listQueuesRequest);
        throwOnFailure(listQueuesResponse);

        return new ArrayList<>(listQueuesResponse.queueUrls());
    }

    private String queueNamePrefix() {
        return this.runtimeEnvironment.toString() + "_" + this.applicationName + "_";
    }

    public MessageReceiveContext[] readMessages(QueueConfiguration targetQueue, String[] messageAttributeNames, int maxMessageCount) {

        // Make sure this QueueConfiguration is registered with this QueueManager
        if(targetQueue.getOwningQueueManager() != this) {
            throw new IllegalArgumentException("The given targetQueue is not registered with this QueueManager instance.");
        }

        // Make sure the maxMessageCount is in range
        if( maxMessageCount > 10 ) {
            throw new IllegalArgumentException("maxMessageCount cannot exceed 10.");
        }

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(targetQueue.getQueueUrl())
                .maxNumberOfMessages(maxMessageCount)
                .waitTimeSeconds(targetQueue.getReceiveMessageWaitTimeSeconds())
                .visibilityTimeout(targetQueue.getMessageVisibilityTimeoutSeconds())
                .messageAttributeNames(messageAttributeNames)
                .build();

        ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveMessageRequest);
        throwOnFailure(receiveMessageResponse);

        List<Message> messageList = receiveMessageResponse.messages();
        if( messageList == null || messageList.isEmpty() ) {
            return new MessageReceiveContext[0];
        }

        MessageReceiveContext[] returnMessages = new MessageReceiveContext[messageList.size()];
        int messageOffset = 0;
        for(Message nextMessage : messageList) {

            returnMessages[messageOffset] = MessageReceiveContext.builder()
                    .messageId(nextMessage.messageId())
                    .receiptHandle(nextMessage.receiptHandle())
                    .messageText(nextMessage.body())
                    .queueConfiguration(targetQueue)
                    .customAttributes(nextMessage.messageAttributes())
                    .build();

            messageOffset++;
        }

        return returnMessages;
    }

    /* package */ boolean deleteMessages(QueueConfiguration targetQueue, MessageReceiveContext messageContext) {

        // Make sure this QueueConfiguration is registered with this QueueManager
        if(targetQueue.getOwningQueueManager() != this) {
            throw new IllegalArgumentException("The given targetQueue is not registered with this QueueManager instance.");
        }

        // Make sure the incoming messageContext is valid
        if( messageContext == null ) {
            throw new IllegalArgumentException("messageContext may not be null.");
        }

        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .receiptHandle(messageContext.getReceiptHandle())
                .queueUrl(targetQueue.getQueueUrl())
                .build();

        DeleteMessageResponse deleteMessageResponse = sqsClient.deleteMessage(deleteMessageRequest);
        throwOnFailure(deleteMessageResponse);

        return true;
    }

    /* package */ List<MessageReceiveContext> deleteMessages(QueueConfiguration targetQueue, List<MessageReceiveContext> messagesToDelete) {

        // Make sure this QueueConfiguration is registered with this QueueManager
        if(targetQueue.getOwningQueueManager() != this) {
            throw new IllegalArgumentException("The given targetQueue is not registered with this QueueManager instance.");
        }

        // Make sure there's something to do.
        if( messagesToDelete == null || messagesToDelete.isEmpty()) {
            return new ArrayList<>();
        }

        List<DeleteMessageBatchRequestEntry> messageDeleteEntries = new ArrayList<>();
        for(MessageReceiveContext nextMessage : messagesToDelete) {
            DeleteMessageBatchRequestEntry nextEntry = DeleteMessageBatchRequestEntry.builder()
                    .receiptHandle(nextMessage.getReceiptHandle())
                    .id(nextMessage.getMessageId())
                    .build();
            messageDeleteEntries.add(nextEntry);
        }

        DeleteMessageBatchRequest deleteMessageBatchRequest = DeleteMessageBatchRequest.builder()
                .queueUrl(targetQueue.getQueueUrl())
                .entries(messageDeleteEntries)
                .build();

        DeleteMessageBatchResponse deleteMessageBatchResponse = sqsClient.deleteMessageBatch(deleteMessageBatchRequest);
        throwOnFailure(deleteMessageBatchResponse);

        List<MessageReceiveContext> failed = new ArrayList<>();

        if( deleteMessageBatchResponse.failed() != null ) {
            for( BatchResultErrorEntry nextResult : deleteMessageBatchResponse.failed()) {
                failed.add(messagesToDelete.stream().filter( mc -> mc.getMessageId().equals(nextResult.id())).findFirst().get());
            }
        }

        return failed;
    }

    public QueueStatistics currentMessageCount(QueueConfiguration queueConfiguration) {

        GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder()
                .queueUrl(queueConfiguration.getQueueUrl())
                .attributeNamesWithStrings(
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES.toString(),
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED.toString(),
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE.toString())
                .build();

        GetQueueAttributesResponse getQueueAttributesResponse = sqsClient.getQueueAttributes(getQueueAttributesRequest);
        throwOnFailure(getQueueAttributesResponse);

        String messageCountString = getQueueAttributesResponse.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
        String delayedMessageCountString = getQueueAttributesResponse.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED);
        String hiddenMessageCountString = getQueueAttributesResponse.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE);

        int messageCount = 0;
        int delayedMessageCount = 0;
        int hiddenMessageCount = 0;

        if( messageCountString != null ) {
            try { messageCount = Integer.parseInt(messageCountString);}catch(Throwable t) {}
        }
        if( delayedMessageCountString != null ) {
            try { delayedMessageCount = Integer.parseInt(delayedMessageCountString); } catch( Throwable t ) {}
        }
        if( hiddenMessageCountString != null ) {
            try { hiddenMessageCount = Integer.parseInt(hiddenMessageCountString); } catch( Throwable t ) {}
        }

        return new QueueStatistics(messageCount, delayedMessageCount, hiddenMessageCount);
    }

    private void throwOnFailure(SqsResponse sqsResponse) {
        SdkHttpResponse sdkHttpResponse = sqsResponse.sdkHttpResponse();

        if(!sdkHttpResponse.isSuccessful()) {

            String statusText = sdkHttpResponse.statusText().isPresent() ? sdkHttpResponse.statusText().get() : "";

            throw SqsException.builder()
                    .message(statusText)
                    .statusCode(sdkHttpResponse.statusCode())
                    .build();
        }
    }

    @Override
    public void destroy() {
        for(MessageHandlerDriverThread nextDriverThread : messageHandlerDriverThreads) {
            nextDriverThread.stop();
        }
    }
}
