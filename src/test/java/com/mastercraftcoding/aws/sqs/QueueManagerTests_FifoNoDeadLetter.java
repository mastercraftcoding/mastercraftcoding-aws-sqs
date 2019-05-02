package com.mastercraftcoding.aws.sqs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration( classes = {TestConfiguration_FifoNoDeadLetter.class} )
@TestPropertySource("classpath:Testing.properties")
public class QueueManagerTests_FifoNoDeadLetter extends CreateAndDestroySqsQueuesBase {

    @Test
    @DirtiesContext
    public void testQueueManagerSetupFifoNoDeadLetter() throws Exception {
        performCreateAndDestroyTest();
    }
}
