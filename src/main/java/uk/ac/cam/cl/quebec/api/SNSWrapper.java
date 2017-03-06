package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.PublishRequest;
import uk.ac.cam.cl.quebec.api.face.SNSUser;

public class SNSWrapper {
    private AmazonSNS sns;
    private static String ALL_ARN = "arn:aws:sns:eu-west-1:926867918335:quebec_alldevices_MOBILEHUB_1062763500";

    public SNSWrapper() {
        sns = AmazonSNSClientBuilder.defaultClient();
    }

    public void notifyAddedToEvent(SNSUser user, String eventCreator) {
        push(user.getArn(), "You've been added to " + eventCreator + "'s event!");
    }

    public void notifyFollowed(String arn, String followerName) {
        push(arn, followerName + " followed you!");
    }

    public void notifyTrainingComplete(SNSUser user) {
        push(user.getArn(), "Training video processed");
    }

    public void notifyEventFinished(SNSUser creator, int numFound) {
        push(creator.getArn(), "Your event has finished processing. Tagged " + numFound + "people.");
    }

    public void sendMessageToAll(String message) {
        push(ALL_ARN, message);
    }

    private void push(String arn, String message) {
        if (isArnValid(arn)) {
            PublishRequest request = new PublishRequest();
            request.setTargetArn(arn);
            request.setMessage(message);

            sns.publish(request);
        }
    }

    private boolean isArnValid(String arn) {
        if (arn != null && !arn.isEmpty()) {
            GetEndpointAttributesRequest request = new GetEndpointAttributesRequest();
            request.setEndpointArn(arn);
            GetEndpointAttributesResult result = sns.getEndpointAttributes(request);
            return result.getAttributes().getOrDefault("Enabled", "false").equals("true");
        }

        return false;
    }
}
