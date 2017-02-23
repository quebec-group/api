package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.neo4j.driver.v1.exceptions.ClientException;

// uk.ac.cam.cl.quebec.api.ProfileProcessedHandler::handleRequest
public class ProfileProcessedHandler implements RequestHandler<ProfileProcessedLambdaInput, LambdaOutput> {
    private DBManager db = new DBManager();

    @Override
    public LambdaOutput handleRequest(ProfileProcessedLambdaInput input, Context context) {
        LambdaOutput response;
        try {
            db.setProfilePicture(input.getUserID(), input.getS3ID());
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
