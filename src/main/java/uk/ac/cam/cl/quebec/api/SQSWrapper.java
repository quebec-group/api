package uk.ac.cam.cl.quebec.api;

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

    public void sendTrainingVideo(String userID, String S3ID) {
        JSONObject json = new JSONObject();

        json.put("type", "Training Video");
        json.put("S3ID", S3ID);
        json.put("userID", userID);

        sqs.sendMessage(VIDEO_QUEUE, json.toString());
    }

    public void sendEventVideo(String S3ID, String eventID, JSONArray usersToMatch) {
        JSONObject json = new JSONObject();

        json.put("type", "Event Video");
        json.put("S3ID", S3ID);
        json.put("eventID", eventID);

        json.put("usersToMatch", usersToMatch);

        sqs.sendMessage(VIDEO_QUEUE, json.toString());
    }
}
