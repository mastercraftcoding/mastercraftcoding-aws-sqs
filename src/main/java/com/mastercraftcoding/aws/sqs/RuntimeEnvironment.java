package com.mastercraftcoding.aws.sqs;

public class RuntimeEnvironment {

    public static RuntimeEnvironment Development = new RuntimeEnvironment("Development");
    public static RuntimeEnvironment UnitTest = new RuntimeEnvironment("UnitTest");
    public static RuntimeEnvironment IntegrationTesting = new RuntimeEnvironment("IntegrationTest");
    public static RuntimeEnvironment Testing = new RuntimeEnvironment("Testing");
    public static RuntimeEnvironment QualityAssurance = new RuntimeEnvironment("QualityAssurance");
    public static RuntimeEnvironment Staging = new RuntimeEnvironment("Staging");
    public static RuntimeEnvironment Production = new RuntimeEnvironment("Production");

    public static RuntimeEnvironment createCustomEnvironment(String environmentName) {
        return new RuntimeEnvironment(environmentName);
    }

    private String environmentName;

    private RuntimeEnvironment(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getEnvironmentName() {
        return this.environmentName;
    }

    @Override
    public String toString() {
        return getEnvironmentName();
    }
}
