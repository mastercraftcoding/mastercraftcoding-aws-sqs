package com.mastercraftcoding.aws.sqs;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MessageSendContext {

    private QueueConfiguration queueConfiguration;

    private String messageFifoDeduplicationId;
    private String messageFifoGroupId;
    private String messageText;
    private int messageDelaySeconds;
    private HashMap<String, MessageAttributeValue> customAttributes = new HashMap<>();

    private MessageSendContext() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private MessageSendContext messageSendContext;

        private Builder() {
            this.messageSendContext = new MessageSendContext();
        }

        public Builder messageFifoDeduplicationId(String messageFifoDeduplicationId) {
            messageSendContext.messageFifoDeduplicationId = messageFifoDeduplicationId;
            return this;
        }

        public Builder messageFifoGroupId(String messageFifoGroupId) {
            messageSendContext.messageFifoGroupId = messageFifoGroupId;
            return this;
        }

        public Builder messageText(String messageText) {
            messageSendContext.messageText = messageText;
            return this;
        }

        public Builder queueConfiguration(QueueConfiguration queueConfiguration) {
            messageSendContext.queueConfiguration = queueConfiguration;
            return this;
        }

        public Builder messageDelaySeconds(int messageDelaySeconds) {
            messageSendContext.messageDelaySeconds = messageDelaySeconds;
            return this;
        }

        public Builder customAttribute(String name, MessageAttributeValue value) {
            messageSendContext.customAttributes.put(name,value);
            return this;
        }

        public Builder customAttributes(Map<String, MessageAttributeValue> attributes) {
            messageSendContext.customAttributes.putAll(attributes);
            return this;
        }

        /* package */ MessageSendContext buildReceivedMessage() {
            return messageSendContext;
        }

        public MessageSendContext build() {

            if(messageSendContext.queueConfiguration == null ) {
                throw new IllegalStateException("queueConfiguration must be set to the SQS Queue that is the destination or origination point.");
            }

            // For FIFO queues, you must have set a groupid and dedup id.
            if(messageSendContext.queueConfiguration.isFifoQueue()) {
                if( messageSendContext.messageFifoDeduplicationId == null ) {
                    throw new IllegalStateException("messageFifoDeduplicationId must be set for FIFO queues.");
                }

                if( messageSendContext.messageFifoGroupId == null) {
                    throw new IllegalStateException("messageFifoGroupId must be set for FIFO queues.");
                }
            }

            // Make sure that the message text has been set.
            if( messageSendContext.messageText == null) {
                throw new IllegalStateException("messageText must be set.");
            }

            return messageSendContext;
        }
    }

    public String getMessageFifoDeduplicationId() {
        return messageFifoDeduplicationId;
    }

    public String getMessageFifoGroupId() {
        return messageFifoGroupId;
    }

    public String getMessageText() {
        return messageText;
    }

    public int getMessageDelaySeconds() {
        return messageDelaySeconds;
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

    /* package */ QueueConfiguration getQueueConfiguration() { return this.queueConfiguration; }

    public void sendMessage() {

        // Create and message builder and set the message body text.
        SendMessageRequest.Builder sendMessageRequestBuilder = SendMessageRequest.builder();
        sendMessageRequestBuilder.messageBody(getMessageText());

        // if this is a FIFO queue, then a deduplication id and a messageFifoGroupId must be set.
        if( getQueueConfiguration().isFifoQueue()) {
            sendMessageRequestBuilder.messageDeduplicationId(getMessageFifoDeduplicationId());
            sendMessageRequestBuilder.messageGroupId(getMessageFifoGroupId());
        }

        // Set the message delivery delay
        sendMessageRequestBuilder.delaySeconds(getMessageDelaySeconds());

        // Set up the custom attributes
        HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        for( String nextAttributeName : getCustomAttributeNames()) {
            MessageAttributeValue nextAttributeValue = getCustomAttribute(nextAttributeName);
            messageAttributes.put(nextAttributeName, nextAttributeValue);
        }
        sendMessageRequestBuilder.messageAttributes(messageAttributes);
        sendMessageRequestBuilder.queueUrl(getQueueConfiguration().getQueueUrl());

        // Build the message class.
        SendMessageRequest sendMessageRequest = sendMessageRequestBuilder.build();

        // Send the request to AWS SQS
        SendMessageResponse sendMessageResponse = getQueueConfiguration().getOwningQueueManager().SqsClient().sendMessage(sendMessageRequest);
        if(!sendMessageResponse.sdkHttpResponse().isSuccessful()) {
            throw new IllegalStateException(sendMessageResponse.sdkHttpResponse().statusText().get());
        }
    }
}
