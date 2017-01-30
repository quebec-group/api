package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.LinkedHashMap;

// uk.ac.cam.cl.quebec.api.TestDB::handleRequest
public class TestDB implements RequestHandler<LinkedHashMap<String, String>, String> {
    @Override
    public String handleRequest(LinkedHashMap<String, String> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Serving request, input : " + input);

        StringBuilder builder = new StringBuilder();
        for (String key : input.keySet()) {
            builder.append("< ").append(key).append(", ").append(input.get(key)).append(" >\n");
        }
        
        return String.valueOf("Input " + builder.toString());
    }
}