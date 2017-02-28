package uk.ac.cam.cl.quebec.api.face;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.neo4j.driver.v1.exceptions.ClientException;
import uk.ac.cam.cl.quebec.api.DBManager;

// uk.ac.cam.cl.quebec.api.face.ProfileProcessedHandler::handleRequest
public class ProfileProcessedHandler implements RequestHandler<ProfileProcessedLambdaInput, LambdaOutput> {
    private DBManager db = new DBManager();

    @Override
    public LambdaOutput handleRequest(ProfileProcessedLambdaInput input, Context context) {
        LambdaOutput response;

        try {
            db.setVideoThumbnail(input.getVideoID(), input.getS3ID());
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
