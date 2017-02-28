package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SQSWrapper {
    private static String VIDEO_QUEUE = "https://sqs.eu-west-1.amazonaws.com/926867918335/processing-queue";
    private AmazonSQS sqs;

    public SQSWrapper() {
        sqs = AmazonSQSClientBuilder.defaultClient();
    }

    public void sendTrainingVideo(String S3ID, String userID, Integer videoID) {
        JsonObject json = new JsonObject();

        json.addProperty("type", "Training Video");
        json.addProperty("S3ID", S3ID);
        json.addProperty("userID", userID);
        json.addProperty("videoID", videoID);

        sqs.sendMessage(VIDEO_QUEUE, json.toString());
    }

    public void sendEventVideo(String videoS3Path, int eventID, int videoID, JsonArray usersToMatch) {
        JsonObject json = new JsonObject();

        json.addProperty("type", "Event Video");
        json.addProperty("S3ID", videoS3Path);
        json.addProperty("eventID", eventID);
        json.addProperty("videoID", videoID);
        json.add("usersToMatch", usersToMatch);

        sqs.sendMessage(VIDEO_QUEUE, json.toString());
    }
}
