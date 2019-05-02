package com.mastercraftcoding.aws.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MessageHandlerDriverThread implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MessageHandlerDriverThread.class);

    private QueueManager queueManager;
    private Thread driverThread;
    private MessageHandler messageHandler;
    private QueueConfiguration targetQueue;
    private String[] messageAttributeNames;

    public MessageHandlerDriverThread(QueueManager queueManager, MessageHandler messageHandler, String[] messageAttributeNames) {
        this.queueManager = queueManager;
        this.messageHandler = messageHandler;
        this.targetQueue = this.messageHandler.getTargetQueueConfiguration();
        this.messageAttributeNames = messageAttributeNames != null ? messageAttributeNames : new String[0];

        this.driverThread = new Thread(this, "MessageHandlerDriverThread - " + queueManager.fullQueueName(targetQueue));
        this.driverThread.setDaemon(true);
        this.driverThread.start();
    }

    public void stop() {
        this.driverThread.interrupt();

        try {
            this.driverThread.join(1000);
        }
        catch(InterruptedException e) {
            // The join timed out...well, just let the application exit
        }
    }

    @Override
    public void run() {

        if(log.isInfoEnabled()) {
            log.info(String.format("Message Driver Thread Starting - %1$s", queueManager.fullQueueName(targetQueue)));
        }

        while(!Thread.interrupted()) {
            try {
                List<MessageReceiveContext> successfulMessageList = new ArrayList<>();

                // Read in the next batch of messages
                MessageReceiveContext[] returnedMessages = queueManager.readMessages(targetQueue, messageAttributeNames, 10);
                for( MessageReceiveContext nextMessage : returnedMessages ) {

                    try {
                        if( log.isDebugEnabled()) {
                            log.debug(String.format("Processing message with id %1$s", nextMessage.getMessageId()));
                        }

                        // Ask the messageHandler to process this message.
                        MessageHandlerStatus status = messageHandler.handleMessage(nextMessage);

                        if( log.isDebugEnabled()) {
                            log.debug(String.format("Successfully processed message with id %1$s", nextMessage.getMessageId()));
                        }

                        // If the message was processed successfully...
                        if( status == MessageHandlerStatus.Message_Processed_Successfully ) {
                            successfulMessageList.add(nextMessage);
                        }
                    }
                    catch( Throwable t ) {
                        log.error(t.getMessage(), t);
                    }
                }

                // Tell SQS to delete the messages that we have successfully processed.
                queueManager.deleteMessages(targetQueue, successfulMessageList);

                if( log.isDebugEnabled()) {
                    for(MessageReceiveContext nextMessage : successfulMessageList) {
                        log.debug(String.format("Successfully removed message from queue %1$s", nextMessage.getMessageId()));
                    }
                }
            }
            catch( Throwable t ) {
                log.error(t.getMessage(), t);

                if(Thread.interrupted()) {
                    break;
                }
            }
        }

        if( log.isInfoEnabled()) {
            log.info(String.format("Message Driver Thread Stopping - %1$s", queueManager.fullQueueName(targetQueue)));
        }
    }
}
