package uk.ac.cam.cl.quebec.api.json;

import org.json.simple.JSONArray;
import org.neo4j.driver.internal.value.RelationshipValue;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;

import java.util.List;

public class ResultFormatter {

    // Base of every successful result
    public static JSON successJson() {
        return new JSON();
    }

    // Gets a list of events, format
    // events : list
    //      videos : Standard video format
    //      likes : bool, does the current user like this event
    //      likesCount : int, number of likes
    //      creator : Standard user format
    //      members : list
    //          Standard user format
    public static JSON getEventsFromResults(StatementResult result, String callerID) {
        JSON response = successJson();
        JSONArray events = new JSONArray();

        while (result.hasNext()) {
            Record record = result.next();
            JSON event = eventFromValue(record.get("events"));

            // Videos
            JSONArray videosJson = new JSONArray();
            record.get("videos")
                    .asList(ResultFormatter::videoFromValue)
                    .forEach(videosJson::add);

            event.put("videos", videosJson);

            event.put("likes", false);

            // Members
            JSONArray membersJson = new JSONArray();
            Value members  = record.get("members");
            int likesCount = 0;

            for (int i = 0; i < members.size(); i++) {
                Value member = members.get(i);
                JSON user = userFromValue(member.get("member"));

                // Likes relation
                if (doesRelationshipExist(member.get("likes"))) {
                    likesCount++;
                    if (member.get("member").get("userID").asString().equals(callerID)) {
                        event.put("likes", true);
                    }
                }

                // Creator
                if (doesRelationshipExist(member.get("created"))) {
                    event.put("creator", user);
                }

                membersJson.add(user);
            }

            event.put("likesCount", likesCount);
            event.put("members", membersJson);

            events.add(event);
        }

        response.put("events", events);

        return response;
    }

    // Result : bool, does relation r exist?
    public static JSON doesRelationExist(StatementResult result) {
        JSON json = successJson();
        json.put("result", false);
        while (result.hasNext()) {
            json.put("result", ResultFormatter.doesRelationshipExist(result.next().get("r")));
        }

        return json;
    }

    // users : list
    //      Standard user info
    //      iFollow : bool, do I follow that user?
    public static JSON getUsersFromResultWithFollowingInfo(StatementResult result) {
        JSON response = successJson();
        JSONArray users = new JSONArray();

        while (result.hasNext()) {
            Record record = result.next();
            Value userValue = record.get("users");
            JSON user = userFromValue(userValue);

            user.put("iFollow", doesRelationshipExist(record.get("followsRelation")));

            users.add(user);
        }

        response.put("users", users);

        return response;
    }

    // array of userIDs
    public static JSONArray getUsersIDs(StatementResult result) {
        JSONArray users = new JSONArray();

        while (result.hasNext()) {
            List<Object> userValues =  result.next().get("users").asList();
            userValues.forEach(users::add);
        }

        return users;
    }

    // Takes a return value from the result, converts it to its natural java form
    // Then stores it under that name
    public static JSON getReturnValue(StatementResult result, String valueName) {
        JSON json = successJson();
        while (result.hasNext()) {
            json.put(valueName, result.next().get(valueName).asObject());
        }

        return json;
    }

    // Gets a single user in its standard form
    public static JSON getUser(StatementResult result) {
        JSON user = successJson();

        while (result.hasNext()) {
            user = userFromValue(result.next().get("user"));
        }

        return user;
    }

    private static Boolean doesRelationshipExist(Value value) {
        return value instanceof RelationshipValue;
    }

    // Standard video format
    //      videoID : int
    //      videoPath : full S3 path of video (excluding bucket)
    //      thumbnailPath : full S3 path of thumbnail (excluding bucket), may be null
    private static JSON videoFromValue(Value value) {
        JSON video = new JSON();

        video.put("videoID", value.get("videoID").asInt());
        video.put("videoPath", value.get("S3ID").asString());
        video.put("thumbnailPath", value.get("thumbnailS3Path", ""));

        return video;
    }

    // Standard user format
    //      userID : string, users Cognito ID
    //      name : string
    //      email : string
    //      profileID : full S3 path of profile picture (excluding bucket), may be null
    private static JSON userFromValue(Value user) {
        JSON json = new JSON();
        json.put("userID", user.get("userID").asString());
        json.put("name", user.get("name").asString());
        json.put("email", user.get("email").asString());
        json.put("profileID", user.get("profileThumbnailS3Path", ""));

        return json;
    }

    // Standard event format
    //      eventID : int
    //      title : string
    //      location : string
    //      time : string
    private static JSON eventFromValue(Value value) {
        JSON event = new JSON();

        event.put("eventID", value.get("eventID").asInt());
        event.put("title", value.get("title").asString());
        event.put("location", value.get("location").asString());
        event.put("time", value.get("time").asString());

        return event;
    }
}
