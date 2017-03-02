package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;

public class SNSWrapper {
    private AmazonSNSAsync sns;
    private static String ALL_ARN = "arn:aws:sns:eu-west-1:926867918335:quebec_alldevices_MOBILEHUB_1062763500";

    public SNSWrapper() {
        sns = AmazonSNSAsyncClientBuilder.defaultClient();
    }

    public void notifyAddedToEvent(String arn, int eventID) {
        // Need to be able to get ARN for user
        sns.publishAsync(arn, "You've been added to an Event!" + eventID);
    }

    public void notifyFollowed(String arn, String followerID) {
        if (arn != null) {
            sns.publishAsync(arn, followerID + " Followed you!");
        }
    }
}
