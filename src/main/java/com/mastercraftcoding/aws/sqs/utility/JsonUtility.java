package com.mastercraftcoding.aws.sqs.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;

public class JsonUtility {

    private static ObjectMapper objectMapper;
    static {
        objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter();
    }

    public static Object typedJsonToObject(String javaTypeFieldName, String typedJson)
            throws IOException, ClassNotFoundException {

        // Parse the json input into a JsonNode tree
        ObjectReader objectReader = objectMapper.reader();
        JsonNode treeRoot = objectReader.readTree(typedJson);

        // Determine the java type that this object represents.
        JsonNode typeNode = treeRoot.findValue(javaTypeFieldName);
        if( typeNode == null ) {
            throw new IllegalStateException(
                    String.format("Unable to find node named '%1$s' in order to determine Json message deserialization type.", javaTypeFieldName));
        }

        // Get the class name
        String javaClassname = typeNode.asText();
        Class targetMessageClass = Class.forName(javaClassname);

        // Deserialize the json message
        return objectMapper.treeToValue(treeRoot, targetMessageClass);
    }

    public static Object jsonToObject(String json, Class objectClass) throws IOException {

        Object returnObject = objectMapper.readerFor(objectClass).readValue(json);
        return returnObject;
    }

    public static String objectToJson(Object object) throws JsonProcessingException {

        String returnJson = objectMapper.writeValueAsString(object);
        return returnJson;
    }
}
