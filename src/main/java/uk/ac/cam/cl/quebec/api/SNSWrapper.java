package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import uk.ac.cam.cl.quebec.api.face.SNSUser;

public class SNSWrapper {
    private AmazonSNSAsync sns;
    private static String ALL_ARN = "arn:aws:sns:eu-west-1:926867918335:quebec_alldevices_MOBILEHUB_1062763500";

    public SNSWrapper() {
        sns = AmazonSNSAsyncClientBuilder.defaultClient();
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

    private void push(String arn, String message) {
        if (isArnValid(arn)) {
            sns.publishAsync(arn, message);
        }
    }

    private boolean isArnValid(String arn) {
        return arn != null && !arn.isEmpty();
    }
}
