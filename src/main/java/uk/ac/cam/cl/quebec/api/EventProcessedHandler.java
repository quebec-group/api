package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.neo4j.driver.v1.exceptions.ClientException;

// uk.ac.cam.cl.quebec.api.EventProcessedHandler::handleRequest
public class EventProcessedHandler implements RequestHandler<EventProcessedLambdaInput, LambdaOutput> {
    private DBManager db = new DBManager();

    @Override
    public LambdaOutput handleRequest(EventProcessedLambdaInput input, Context context) {
        LambdaOutput response;

        try {
            db.addUsersToEvent(input.getEventID(), input.getMembers());
            response = new LambdaOutput(true);
        } catch (ClientException e) {
            if (context != null) {
                context.getLogger().log(e.getMessage());
            }
            response = new LambdaOutput(false, e.getMessage());
        }

        return response;
    }
}
