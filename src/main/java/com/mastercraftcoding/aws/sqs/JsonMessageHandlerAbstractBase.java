package com.mastercraftcoding.aws.sqs;

import com.mastercraftcoding.aws.sqs.utility.JsonUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;

public abstract class JsonMessageHandlerAbstractBase implements MessageHandler, InitializingBean {

    private final static Logger log = LoggerFactory.getLogger(JsonMessageHandlerAbstractBase.class);

    private List<Class> validMessageClasses;

    private String[] desiredMessageAttributes;
    private String jsonJavaTypeFieldName;

    public JsonMessageHandlerAbstractBase(Class ... messageClasses) {
        validMessageClasses = new ArrayList<>();
        for(Class nextClass : messageClasses) {
            validMessageClasses.add(nextClass);
        }
        setJsonJavaTypeFieldName("javaType");
    }

    public JsonMessageHandlerAbstractBase(String[] desiredMessageAttributes, Class ... messageClasses) {
        this.desiredMessageAttributes = desiredMessageAttributes;
        validMessageClasses = new ArrayList<>();
        for(Class nextClass : messageClasses) {
            validMessageClasses.add(nextClass);
        }
        setJsonJavaTypeFieldName("javaType");
    }
    @Override
    public void afterPropertiesSet() throws Exception {

        Class[] messageClassList = new Class[validMessageClasses.size()];
        int i = 0;
        for( Class nextMessageClass : validMessageClasses ) {
            messageClassList[i++] = nextMessageClass;
        }
    }

    public String getJsonJavaTypeFieldName() {
        return jsonJavaTypeFieldName;
    }

    public void setJsonJavaTypeFieldName(String jsonJavaTypeFieldName) {
        this.jsonJavaTypeFieldName = jsonJavaTypeFieldName;
    }

    @Override
    public String[] getDesiredMessageAttributes() {
        return desiredMessageAttributes;
    }

    public List<Class> getValidMessageClasses() {
        return validMessageClasses;
    }

    public void setValidMessageClasses(List<Class> validMessageClasses) {
        this.validMessageClasses = validMessageClasses;
    }

    public void setDesiredMessageAttributes(String[] desiredMessageAttributes) {
        this.desiredMessageAttributes = desiredMessageAttributes;
    }

    @Override
    public MessageHandlerStatus handleMessage(MessageReceiveContext messageReceiveContext) {

        // Attempt to deserialize the incoming message
        try {
            // Parse the incoming payload as a parse tree
            Object message = JsonUtility.typedJsonToObject(jsonJavaTypeFieldName, messageReceiveContext.getMessageText());
            if( message == null ) {
                throw new IllegalArgumentException("Null message submitted to endpoint.");
            }

            // Make sure this is a legal message for this endpoint
            if(!validMessageClasses.contains(message.getClass())) {
                throw new IllegalArgumentException("This endpoint does not support messages of this type.");
            }

            return handleObjectMessage(messageReceiveContext, message);

        } catch (Exception e) {
            log.error( "Failed to parse incoming message as a JSON object.  Exception follows.");
            log.error(e.getMessage(),e);
            return MessageHandlerStatus.Message_Processing_Failed;
        }
    }

    protected abstract MessageHandlerStatus handleObjectMessage(MessageReceiveContext messageReceiveContext,
                                                                Object messageObject);
}
