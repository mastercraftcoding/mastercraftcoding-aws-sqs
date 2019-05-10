package com.mastercraftcoding.aws.sqs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration( classes = {TestConfiguration_FifoDeadLetter.class} )
@TestPropertySource("file:${user.home}/mcc/mcc.properties")
public class QueueManagerTests_FifoDeadLetter extends CreateAndDestroySqsQueuesBase {

    @Test
    @DirtiesContext
    public void testQueueManagerSetupFifoDeadLetter() throws Exception {
        performCreateAndDestroyTest();
    }
}
