package uk.ac.cam.cl.quebec.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.neo4j.driver.internal.value.RelationshipValue;
import org.neo4j.driver.v1.*;

import java.util.List;

public class DBManager {
    private static String DB_URL = System.getenv("dbUrl");
    private static String DB_USER = System.getenv("dbUser");
    private static String DB_PASS = System.getenv("dbPassword");

    private static Driver driver;

    private Session getSession() {
        if (driver == null) {
            driver = GraphDatabase.driver(DB_URL, AuthTokens.basic(DB_USER, DB_PASS));
        }

        return driver.session();
    }

    public JsonObject createUser(String userID, String name, String email, String arn) {
        Statement statement = new Statement(
                "MERGE (u:User {name: {name}, email: {email}, userID: {ID}, arn: {arn}}) ",
                Values.parameters("name", name, "email", email, "ID", userID, "arn", arn));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject setProfileVideo(String userID, String S3Path) {
        Statement statement = new Statement(
                // Get UUID
                "MERGE (id:UniqueId {name:'Video'}) " +
                "ON CREATE SET id.count = 1 " +
                "ON MATCH SET id.count = id.count + 1 " +
                "WITH id.count AS uid " +

                // Create video
                "MATCH (u:User {userID: {userID}}) " +
                "MERGE (v:Video {S3ID: {S3ID}, videoID: uid}) " +
                "CREATE UNIQUE (u)-[:PROFILE_PICTURE]->(v) " +
                "RETURN uid",
                Values.parameters("userID", userID, "S3ID", S3Path));

        StatementResult result = runQuery(statement);

        JsonObject response = new JsonObject();
        while (result.hasNext()) {
            response.addProperty("videoID", result.next().get("uid").asInt());
        }

        return response;

    }

    public JsonObject setProfileThumbnail(String userID, String profileThumbnailS3Path) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "SET u.profileThumbnailS3Path = {profileThumbnailS3Path} ",
                Values.parameters("userID", userID, "profileThumbnailS3Path", profileThumbnailS3Path));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject setVideoThumbnail(int videoID, String S3Path) {
        Statement statement = new Statement(
                "MATCH (v:Video {videoID: {videoID}}) " +
                "SET v.thumbnailS3Path = {S3Path} ",
                Values.parameters("videoID", videoID, "S3Path", S3Path));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject addVideoToEvent(int eventID, String S3ID) {
        Statement statement = new Statement(
                // Get UUID
                "MERGE (id:UniqueId{name:'Video'}) " +
                "ON CREATE SET id.count = 1 " +
                "ON MATCH SET id.count = id.count + 1 " +
                "WITH id.count AS uid " +

                // Create video
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MERGE (v:Video {S3ID: {S3ID}, videoID: uid}) " +
                "CREATE UNIQUE (e)-[:VIDEO]->(v) " +
                "RETURN uid",
                Values.parameters("eventID", eventID, "S3ID", S3ID));
        StatementResult result = runQuery(statement);

        JsonObject response = new JsonObject();
        while (result.hasNext()) {
            response.addProperty("videoID", result.next().get("uid").asInt());
        }

        return response;
    }

    public JsonArray getRelatedUsers(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FOLLOWS]-(users:User) " +
                "RETURN users",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        JsonArray users = new JsonArray();

        while (result.hasNext()) {
            users.add(result.next().get("users").get("userID").asString());
        }

        return users;
    }

    public JsonObject follow(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "CREATE UNIQUE (a)-[:FOLLOWS]->(b)",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject unfollow(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "MATCH (a)-[r:FOLLOWS]->(b) " +
                "DELETE r",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject createEvent(String title, String location, String time, String creatorID) {
        Statement statement = new Statement(
                // Get UUID
                "MERGE (id:UniqueId{name:'Event'}) " +
                "ON CREATE SET id.count = 1 " +
                "ON MATCH SET id.count = id.count + 1 " +
                "WITH id.count AS uid " +

                // Create event
                "MATCH (u:User {userID: {creatorID}}) " +
                "CREATE (e:Event {title: {title}, eventID: uid, location: {location}, time: {time}}) " +
                "CREATE UNIQUE (u)-[:CREATED]->(e), " +
                "(u)-[:ATTENDED]->(e) " +
                "RETURN uid",
                Values.parameters("title", title,
                        "creatorID", creatorID,
                        "location", location,
                        "time", time));
        StatementResult result = runQuery(statement);

        JsonObject response = new JsonObject();
        while (result.hasNext()) {
            response.addProperty("eventID", result.next().get("uid").asInt());
        }

        return response;
    }

    public JsonObject addUserToEvent(int eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE UNIQUE (u)-[:ATTENDED]->(e)",
                Values.parameters("eventID", eventID, "userID", userID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject addUsersToEvent(int eventID, List<String> members) {
        Statement statement = new Statement(
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u:User) " +
                "WHERE u.userID IN {members} " +
                "CREATE UNIQUE (u)-[:ATTENDED]->(e)",
                Values.parameters("eventID", eventID, "members", members));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject removeUserFromEvent(int eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u)-[r:ATTENDED]->(e) " +
                "DELETE r",
                Values.parameters("eventID", eventID, "userID", userID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject getEvents(String userID) {
        Statement statement = new Statement(
                "MATCH (caller:User {userID: {userID}})-[*1..2]-(events:Event) " +
                "MATCH (events)-[:ATTENDED]-(atEvent:User) " +
                "OPTIONAL MATCH (events)-[:VIDEO]-(eventVideos:Video) " +
                "OPTIONAL MATCH (atEvent)-[l:LIKES]-(events) " +
                "RETURN events, collect(distinct {member: atEvent, likes: l}) AS members, collect(distinct eventVideos) AS videos",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getEventsFromResults(result, userID);
    }

    public JsonObject likeEvent(String userID, int eventID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE UNIQUE (u)-[:LIKES]->(e) ",
                Values.parameters("eventID", eventID, "userID", userID));

        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JsonObject unlikeEvent(String userID, int eventID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u)-[r:LIKES]->(e) " +
                "DELETE r",
                Values.parameters("eventID", eventID, "userID", userID));

        StatementResult result = runQuery(statement);

        return successJson();
    }


    public StatementResult runQuery(Statement query) {
        Session session = getSession();
        StatementResult result = session.run(query);
        session.close();
        return result;
    }

    private JsonObject getEventsFromResults(StatementResult result, String callerID) {
        JsonObject response = successJson();
        JsonArray events = new JsonArray();

        while (result.hasNext()) {
            Record record = result.next();
            JsonObject event = new JsonObject();
            event.addProperty("eventID", record.get("events").get("eventID").asInt());
            event.addProperty("title", record.get("events").get("title").asString());
            event.addProperty("location", record.get("events").get("location").asString());
            event.addProperty("time", record.get("events").get("time").asString());

            // Videos
            JsonArray videosJson = new JsonArray();
            Value videos  = record.get("videos");

            for (int i = 0; i < videos.size(); i++) {
                Value video = videos.get(i);
                videosJson.add(getJsonFromVideo(video));
            }

            event.add("videos", videosJson);
            event.addProperty("likes", Boolean.FALSE);


            // Members
            JsonArray membersJson = new JsonArray();
            Value members  = record.get("members");
            int likesCount = 0;

            for (int i = 0; i < members.size(); i++) {
                Value member = members.get(i);
                JsonObject users = new JsonObject();

                users.addProperty("userID", member.get("member").get("userID").asString());
                users.addProperty("name", member.get("member").get("name").asString());
                users.addProperty("email", member.get("member").get("email").asString());
                users.addProperty("profileID", member.get("member").get("profileThumbnailS3Path", ""));

                if (relationshipExists(member.get("likes"))) {
                    likesCount++;
                    if (member.get("member").get("userID").toString().equals(callerID)) {
                        event.addProperty("likes", Boolean.TRUE);
                    }
                }

                membersJson.add(users);
            }

            event.addProperty("likesCount", likesCount);
            event.add("members", membersJson);

            events.add(event);
        }

        response.add("events", events);

        return response;
    }

    private JsonObject getUsersFromResult(StatementResult result) {
        JsonObject response = successJson();
        JsonArray users = new JsonArray();

        while (result.hasNext()) {
            Record record = result.next();
            Value userValue = record.get("users");
            JsonObject user = new JsonObject();

            user.addProperty("userID", userValue.get("userID").asString());
            user.addProperty("name", userValue.get("name").asString());
            user.addProperty("email", userValue.get("email").asString());
            user.addProperty("profileID", userValue.get("profileThumbnailS3Path", ""));

            users.add(user);
        }

        response.add("users", users);

        return response;
    }

    private JsonObject getJsonFromVideo(Value value) {
        JsonObject video = new JsonObject();

        video.addProperty("videoID", value.get("videoID").asInt());
        video.addProperty("videoPath", value.get("S3ID").asString());
        video.addProperty("thumbnailPath", value.get("thumbnailS3Path", ""));

        return video;
    }

    public JsonObject getFollowing(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FOLLOWS]->(users) " +
                "RETURN users",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getUsersFromResult(result);
    }

    public JsonObject getFollowers(String userID) {
        Statement statement = new Statement(
                "MATCH (users)-[:FOLLOWS]->(u:User {userID: {userID}}) " +
                "RETURN users",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getUsersFromResult(result);
    }

    private Boolean relationshipExists(Value value) {
        return value instanceof RelationshipValue;
    }

    private JsonObject successJson() {
        return new JsonObject();
    }

    public JsonObject getInfo(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "RETURN u",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        JsonObject user = new JsonObject();

        while (result.hasNext()) {
            Record record = result.next();
            Value userValue = record.get("u");

            user.addProperty("userID", userValue.get("userID").asString());
            user.addProperty("name", userValue.get("name").asString());
            user.addProperty("email", userValue.get("email").asString());
            user.addProperty("profileID", userValue.get("profileThumbnailS3Path", ""));

        }

        return user;
    }
}
