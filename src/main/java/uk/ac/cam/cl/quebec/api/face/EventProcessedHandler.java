package uk.ac.cam.cl.quebec.api.face;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.neo4j.driver.v1.exceptions.ClientException;
import uk.ac.cam.cl.quebec.api.DBManager;
import uk.ac.cam.cl.quebec.api.SNSWrapper;

import java.util.List;

// uk.ac.cam.cl.quebec.api.face.EventProcessedHandler::handleRequest
public class EventProcessedHandler implements RequestHandler<EventProcessedLambdaInput, LambdaOutput> {
    private DBManager db = new DBManager();
    private SNSWrapper sns = new SNSWrapper();

    @Override
    public LambdaOutput handleRequest(EventProcessedLambdaInput input, Context context) {
        LambdaOutput response;

        try {
            db.setVideoThumbnail(input.getVideoID(), input.getThumbnailS3Path());

            List<String> members = input.getMembers();
            List<SNSUser> users = db.addUsersToEventAndGetArns(input.getEventID(), members);
            SNSUser creator = db.getCreator(input.getEventID());

            for (SNSUser user : users) {
                sns.notifyAddedToEvent(user, creator.getName());
            }

            sns.notifyEventFinished(creator, members.size());

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
