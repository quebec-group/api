package uk.ac.cam.cl.quebec.api.face;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.neo4j.driver.v1.exceptions.ClientException;
import uk.ac.cam.cl.quebec.api.DBManager;

// uk.ac.cam.cl.quebec.api.face.EventProcessedHandler::handleRequest
public class EventProcessedHandler implements RequestHandler<EventProcessedLambdaInput, LambdaOutput> {
    private DBManager db = new DBManager();

    @Override
    public LambdaOutput handleRequest(EventProcessedLambdaInput input, Context context) {
        LambdaOutput response;

        try {
            db.addUsersToEvent(input.getEventID(), input.getMembers());
            db.setVideoThumbnail(input.getVideoID(), input.getThumbnailS3Path());
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
