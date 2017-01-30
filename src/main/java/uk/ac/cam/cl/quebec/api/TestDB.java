package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

// uk.ac.cam.cl.quebec.api.TestDB::testDBHandler
public class TestDB {
    public String testDBHandler(String inputJSON, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("received : " + inputJSON);
        return String.valueOf("Input was: " + inputJSON);
    }
}