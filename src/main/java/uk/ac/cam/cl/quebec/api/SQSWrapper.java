package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SQSWrapper {
    private static String VIDEO_QUEUE = "https://sqs.eu-west-1.amazonaws.com/926867918335/processing-queue";
    private AmazonSQS sqs;

    public SQSWrapper() {
        sqs = AmazonSQSClientBuilder.defaultClient();
    }

    public void sendTrainingVideo(String S3ID, String userID, int videoID, Context context) {
        JSONObject json = new JSONObject();

        json.put("type", "Training Video");
        json.put("S3ID", S3ID);
        json.put("userID", userID);
        json.put("videoID", videoID);

        if (context != null) {
            context.getLogger().log("Sending: " + json.toString());
        }

        sqs.sendMessage(VIDEO_QUEUE, json.toString());
    }

    public void sendEventVideo(String videoS3Path, int eventID, int videoID, JSONArray usersToMatch, Context context) {
        JSONObject json = new JSONObject();

        json.put("type", "Event Video");
        json.put("S3ID", videoS3Path);
        json.put("eventID", eventID);
        json.put("videoID", videoID);
        json.put("usersToMatch", usersToMatch);

        if (context != null) {
            context.getLogger().log("Sending: " + json.toString());
        }

        sqs.sendMessage(VIDEO_QUEUE, json.toString());
    }
}
