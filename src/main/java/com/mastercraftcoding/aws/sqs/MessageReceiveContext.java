package com.mastercraftcoding.aws.sqs;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.*;

public class MessageReceiveContext {

    private QueueConfiguration queueConfiguration;

    private String messageId;
    private String receiptHandle;
    private String messageText;
    private final HashMap<String, MessageAttributeValue> customAttributes = new HashMap<>();

    private MessageReceiveContext() {

    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final MessageReceiveContext receiveContext;

        private Builder() {
            receiveContext = new MessageReceiveContext();
        }

        public Builder messageId( String messageId ) {
            receiveContext.messageId = messageId;
            return this;
        }

        public Builder receiptHandle( String receiptHandle ) {
            receiveContext.receiptHandle = receiptHandle;
            return this;
        }

        public Builder messageText( String messageText ) {
            receiveContext.messageText = messageText;
            return this;
        }

        public Builder customAttribute(String name, MessageAttributeValue value) {
            receiveContext.customAttributes.put(name,value);
            return this;
        }

        public Builder customAttributes(Map<String, MessageAttributeValue> attributes) {
            receiveContext.customAttributes.putAll(attributes);
            return this;
        }

        public Builder queueConfiguration(QueueConfiguration queueConfiguration) {
            receiveContext.queueConfiguration = queueConfiguration;
            return this;
        }

        public MessageReceiveContext build() {
            return receiveContext;
        }
    }

    public QueueConfiguration getQueueConfiguration() {
        return queueConfiguration;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getReceiptHandle() {
        return receiptHandle;
    }

    public String getMessageText() {
        return messageText;
    }

    public boolean hasCustomAttribute(String attributeName) {
        return customAttributes.containsKey(attributeName);
    }

    public MessageAttributeValue getCustomAttribute(String attributeName) {
        return customAttributes.get(attributeName);
    }

    public Set<String> getCustomAttributeNames() {
        return customAttributes.keySet();
    }

    public void delete() {
        queueConfiguration.getOwningQueueManager().deleteMessages(queueConfiguration, this);
    }
}
