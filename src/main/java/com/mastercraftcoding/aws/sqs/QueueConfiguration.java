package com.mastercraftcoding.aws.sqs;

import org.springframework.beans.factory.InitializingBean;

/**
 * The QueueConfiguration class is used to configure and reference an SQS Queue using the Master Craft Coding
 * AWS SQS library.  Once this Spring Bean has been created, it will validate it's configuration to ensure
 * that the AWS SQS system will most likely accept the configuration.
 * <p>
 * A QueueConfiguration instance can be established one of two ways.  First, be using a Spring Bean XML configuration
 * file where each property is set in turn.  The alternate way to create a QueueConfiguration is you are using
 * code-based Spring configuration is to use the QueueConfiguration.Builder subclass.  This class is accessed
 * using the QueueConfiguration.builder static method.
 * <p>
 * {@code
 * return QueueConfiguration.builder()
 *     .queueBaseName( "TestQueue" )
 *     .isFifoQueue( false )
 *     .createDeadLetterQueue( false )
 *     .build();
 * }
 */
public class QueueConfiguration implements InitializingBean {

    private QueueUserConfiguration userConfiguration = new QueueUserConfiguration();
    private QueueSystemConfiguration systemConfiguration = new QueueSystemConfiguration();

    private class QueueUserConfiguration {

        /**
         * queueBaseName is the base queue name that is requested by the user.  This name will be
         * added to for the queueManager's RuntimeEnvironment and ApplicationName to construct the
         * full name of the queue as seen by the AWS SQS system.
         * @See generateFullQueueName
         */
        private String queueBaseName;

        /**
         * isFifoQueue determines if this QueueConfiguration represents a First-In-First-Out queue
         * or a "Standard" queue.  FIFO queues require extra information be set on the QueueConfiguration
         * in order to be accepted by AWS.
         *
         * @see MessageSendContext
         */
        private boolean isFifoQueue;

        /**
         * The createDeadLetterQueue flag is used to signal that you wish to create not only this queue,
         * but a "dead letter" queue that will contain messages that fail to be properly processed out of
         * the main queue name.  If this flag is set, that you must also set the deadLetterRetryCount
         * and deadLetterMaxReceiveCount fields.
         * @see <a href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html">AWS Dead Letter Queue Docs</a>
         */
        private boolean createDeadLetterQueue;

        /**
         * deadLetterRetryCount specifies the number of times that SQS will attempt to delivery any
         * given message.  If the message fails to be processed more than "deadLetterRetryCount" times,
         * then the message will be moved into the associated "dead letter" queue for review.
         */
        private int deadLetterRetryCount;

        /**
         * receiveMessageWaitTimeSeconds is a user configured setting that determines how many seconds
         * a receiveMessage call will wait before returning messages from the associated queue.  By default,
         * this value is zero, indicating "short polling".  Messages are returned that are immediately available.
         * If this value is non-zero, then the Amazon SQS servers will wait for the configured number of
         * seconds and return any messages that are available or become available within that time window.
         */
        private int receiveMessageWaitTimeSeconds;

        /**
         * messageVisibilityTimeoutSeconds is the number of seconds that a message will become "invisible"
         * to other requests for messages from this queue once a message has been pulled for processing.
         * @see <a href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html">AWS Visibility Timeout Docs</a>
         */
        private int messageVisibilityTimeoutSeconds = 30; // Default matches AWS' default timeout.

        /**
         * messageRetentionPeriodSeconds is the length of time that AWS SQS will retain a message in this queue.  If
         * the message has not been processed within that time frame, it is removed from the queue and lost, or moved to
         * it's associated "dead letter" queue.
         */
        private int messageRetentionPeriodSeconds = 345600; // 4 days - Default matches AWS' default timeout.

        /**
         * deadLetterRetentionPeriodSeconds is the length of time that AWS SQS will retain a message in this queue. If
         * the message has not been processed within that time frame, it is removed from the queue and lost.
         */
        private int deadLetterRetentionPeriodSeconds = 345600; // 4 days - Default matches AWS' default timeout.

        public String getQueueBaseName() {
            return queueBaseName;
        }

        public void setQueueBaseName(String queueBaseName) {
            this.queueBaseName = queueBaseName;
        }

        public boolean isFifoQueue() {
            return isFifoQueue;
        }

        public void setFifoQueue(boolean fifoQueue) {
            isFifoQueue = fifoQueue;
        }

        public boolean isCreateDeadLetterQueue() {
            return createDeadLetterQueue;
        }

        public void setCreateDeadLetterQueue(boolean createDeadLetterQueue) {
            this.createDeadLetterQueue = createDeadLetterQueue;
        }

        public int getDeadLetterRetryCount() {
            return deadLetterRetryCount;
        }

        public void setDeadLetterRetryCount(int deadLetterRetryCount) {
            this.deadLetterRetryCount = deadLetterRetryCount;
        }

        public int getReceiveMessageWaitTimeSeconds() {
            return receiveMessageWaitTimeSeconds;
        }

        public void setReceiveMessageWaitTimeSeconds(int receiveMessageWaitTimeSeconds) {
            this.receiveMessageWaitTimeSeconds = receiveMessageWaitTimeSeconds;
        }

        public int getMessageVisibilityTimeoutSeconds() {
            return messageVisibilityTimeoutSeconds;
        }

        public void setMessageVisibilityTimeoutSeconds(int messageVisibilityTimeoutSeconds) {
            this.messageVisibilityTimeoutSeconds = messageVisibilityTimeoutSeconds;
        }

        public int getMessageRetentionPeriodSeconds() {
            return messageRetentionPeriodSeconds;
        }

        public void setMessageRetentionPeriodSeconds(int messageRetentionPeriodSeconds) {
            this.messageRetentionPeriodSeconds = messageRetentionPeriodSeconds;
        }

        public int getDeadLetterRetentionPeriodSeconds() {
            return deadLetterRetentionPeriodSeconds;
        }

        public void setDeadLetterRetentionPeriodSeconds(int deadLetterRetentionPeriodSeconds) {
            this.deadLetterRetentionPeriodSeconds = deadLetterRetentionPeriodSeconds;
        }
    }

    class QueueSystemConfiguration {

        /**
         * owningQueueManager instance is set by the QueueManager once this configuration
         * is registered with a given QueueManager by calling registerQueue or by the Spring
         * initialization code (afterPropertiesSet) of the QueueManager.
         */
        private QueueManager owningQueueManager;

        /**
         * queueUrl contains the URL that is used to access the AWS SQS queue represented by this
         * QueueConfiguration instance.  This value to not available until set by the QueueManager
         * class upon successful QueueConfiguration registration.
         */
        private String queueUrl;

        /**
         * queueArn contains the assigned "Amazon Resource Name" for this queue.  After a QueueConfiguration
         * instance is registered with a QueueManager, the ARN for the queue is obtained.  It is
         * required by some API calls, including setting up the relationship between the main queue and
         * any requested "dead letter" queue.
         */
        private String queueArn;

        /**
         * queueDeadLetterUrl contains the URL required to access any associated "dead letter" queue
         * if one was requested by this QueueConfiguration instance.
         */
        private String queueDeadLetterUrl;

        /**
         * queueDeadLetterArn contains the "Amazon Resource Name" for the associated "dead letter" queue,
         * if a dead letter queue was requested.  It is set by the QueueManager upon registration of this
         * QueueConfiguration instance.
         */
        private String queueDeadLetterArn;

        QueueManager getOwningQueueManager() {
            return owningQueueManager;
        }

        public void setOwningQueueManager(QueueManager owningQueueManager) {

            // Make sure this QueueConfiguration isn't already owned by another
            // QueueManager instance.
            if( this.owningQueueManager != null ) {
                throw new IllegalStateException(String.format("The SQS Queue %1$s is already registered with another QueueManager instance.", toString()));
            }

            this.owningQueueManager = owningQueueManager;
        }

        String getQueueUrl() {
            return queueUrl;
        }

        public void setQueueUrl(String queueUrl) {
            this.queueUrl = queueUrl;
        }

        String getQueueArn() {
            return queueArn;
        }

        public void setQueueArn(String queueArn) {
            this.queueArn = queueArn;
        }

        String getQueueDeadLetterUrl() {
            return queueDeadLetterUrl;
        }

        public void setQueueDeadLetterUrl(String queueDeadLetterUrl) {
            this.queueDeadLetterUrl = queueDeadLetterUrl;
        }

        String getQueueDeadLetterArn() {
            return queueDeadLetterArn;
        }

        public void setQueueDeadLetterArn(String queueDeadLetterArn) {
            this.queueDeadLetterArn = queueDeadLetterArn;
        }
    }

    /* package */ QueueSystemConfiguration getSystemConfiguration() {
        return this.systemConfiguration;
    }

    public QueueUserConfiguration getUserConfiguration() {
        return this.userConfiguration;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        validate();
    }

    public static class Builder {

        QueueConfiguration returnConfiguration;

        public Builder() {
            returnConfiguration = new QueueConfiguration();
        }

        public Builder deadLetterRetentionPeriodSeconds(int deadLetterRetentionPeriodSeconds) {
            returnConfiguration.userConfiguration.setDeadLetterRetentionPeriodSeconds(deadLetterRetentionPeriodSeconds);
            return this;
        }

        public Builder messageRetentionPeriodSeconds(int messageRetentionPeriodSeconds ) {
            returnConfiguration.userConfiguration.setMessageRetentionPeriodSeconds(messageRetentionPeriodSeconds);
            return this;
        }

        public Builder queueBaseName(String queueBaseName) {
            returnConfiguration.userConfiguration.setQueueBaseName(queueBaseName);
            return this;
        }

        public Builder isFifoQueue(boolean isFifoQueue) {
            returnConfiguration.userConfiguration.setFifoQueue(isFifoQueue);
            return this;
        }

        public Builder createDeadLetterQueue(boolean createDeadLetterQueue) {
            returnConfiguration.userConfiguration.setCreateDeadLetterQueue(createDeadLetterQueue);
            return this;
        }

        public Builder deadLetterRetryCount(int deadLetterRetryCount) {
            returnConfiguration.userConfiguration.setDeadLetterRetryCount(deadLetterRetryCount);
            return this;
        }

        public Builder receiveMessageWaitTimeSeconds(int receiveMessageWaitTimeSeconds){
            returnConfiguration.userConfiguration.setReceiveMessageWaitTimeSeconds(receiveMessageWaitTimeSeconds);
            return this;
        }

        public Builder messageVisibilityTimeoutSeconds(int messageVisibilityTimeoutSeconds) {
            returnConfiguration.userConfiguration.setMessageVisibilityTimeoutSeconds(messageVisibilityTimeoutSeconds);
            return this;
        }

        public QueueConfiguration build() {

            returnConfiguration.validate();

            return returnConfiguration;
        }
    }

    private void validate() {
        // If we have a dead letter queue, we have to have set a max retry count.
        if( userConfiguration.isCreateDeadLetterQueue() ) {
            if( userConfiguration.getDeadLetterRetryCount() == 0 ) {
                throw new IllegalStateException("The request queue configuration has a dead letter queue but 'deadLetterRetryCount' has not been set.");
            }
        }
    }

    public String getQueueBaseName() {
        return userConfiguration.getQueueBaseName();
    }

    public void setQueueBaseName(String queueBaseName) {
        userConfiguration.setQueueBaseName(queueBaseName);
    }

    public boolean isFifoQueue() {
        return userConfiguration.isFifoQueue();
    }

    public void setFifoQueue(boolean isFifoQueue) {
        userConfiguration.setFifoQueue(isFifoQueue);
    }

    public boolean isCreateDeadLetterQueue() {
        return userConfiguration.isCreateDeadLetterQueue();
    }

    public void setCreateDeadLetterQueue( boolean isDeadLetterQueue ) {
        userConfiguration.setCreateDeadLetterQueue(isDeadLetterQueue);
    }

    public int getDeadLetterRetryCount() {
        return userConfiguration.getDeadLetterRetryCount();
    }

    public void setDeadLetterRetryCount(int retryCount) {
        userConfiguration.setDeadLetterRetryCount(retryCount);
    }

    public String getQueueUrl() {
        return systemConfiguration.getQueueUrl();
    }

    public String getQueueArn() {
        return systemConfiguration.getQueueArn();
    }

    public String getQueueDeadLetterUrl() {
        return systemConfiguration.getQueueDeadLetterUrl();
    }

    public QueueManager getOwningQueueManager() {
        return systemConfiguration.getOwningQueueManager();
    }

    public String getQueueDeadLetterArn() {
        return systemConfiguration.getQueueDeadLetterArn();
    }

    /* package */ String generateFullQueueName(RuntimeEnvironment runtimeEnvironment, String applicationName) {
        return runtimeEnvironment.toString() + "_" + applicationName + "_" + userConfiguration.getQueueBaseName() + (userConfiguration.isFifoQueue() ? ".fifo" : "");
    }

    /* package */ String generateFullDeadLetterQueueName(RuntimeEnvironment runtimeEnvironment, String applicationName) {
        return runtimeEnvironment.toString() + "_" + applicationName + "_" + userConfiguration.getQueueBaseName() + "_DeadLetter" + (userConfiguration.isFifoQueue() ? ".fifo" : "");
    }

    public int getReceiveMessageWaitTimeSeconds() {
        return userConfiguration.getReceiveMessageWaitTimeSeconds();
    }

    public void setReceiveMessageWaitTimeSeconds(int receiveMessageWaitTimeSeconds) {
        userConfiguration.setReceiveMessageWaitTimeSeconds(receiveMessageWaitTimeSeconds);
    }

    public int getMessageVisibilityTimeoutSeconds() {
        return userConfiguration.getMessageVisibilityTimeoutSeconds();
    }

    public void setMessageVisibilityTimeoutSeconds(int messageVisibilityTimeoutSeconds) {
        userConfiguration.setMessageVisibilityTimeoutSeconds(messageVisibilityTimeoutSeconds);
    }

    public int getMessageRetentionPeriodSeconds() {
        return userConfiguration.getMessageRetentionPeriodSeconds();
    }

    public void setMessageRetentionPeriodSeconds(int messageRetentionPeriodSeconds) {
        userConfiguration.setMessageRetentionPeriodSeconds(messageRetentionPeriodSeconds);
    }

    public int getDeadLetterRetentionPeriodSeconds() {
        return userConfiguration.getDeadLetterRetentionPeriodSeconds();
    }

    public void setDeadLetterRetentionPeriodSeconds(int deadLetterRetentionPeriodSeconds) {
        userConfiguration.setDeadLetterRetentionPeriodSeconds(deadLetterRetentionPeriodSeconds);
    }
}
