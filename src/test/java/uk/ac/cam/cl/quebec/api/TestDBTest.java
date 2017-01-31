package uk.ac.cam.cl.quebec.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestDBTest {
    @Test
    public void handleRequest() throws Exception {
        TestDB lambda = new TestDB();
        String result = lambda.handleRequest("Rand", null);

        assertEquals(result, "Friend: Bob, bob1 \nFriend: Andy, and1 \n");
    }

}