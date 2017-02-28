package uk.ac.cam.cl.quebec.api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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

    public JSONObject createUser(String userID, String name, String email, String arn) {
        Statement statement = new Statement(
                "MERGE (u:User {name: {name}, email: {email}, userID: {ID}, arn: {arn}}) ",
                Values.parameters("name", name, "email", email, "ID", userID, "arn", arn));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject setProfileVideo(String userID, String S3Path) {
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

        JSONObject response = new JSONObject();
        while (result.hasNext()) {
            response.put("videoID", result.next().get("uid").asInt());
        }

        return response;

    }

    public JSONObject setProfileThumbnail(String userID, String profileThumbnailS3Path) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "SET u.profileThumbnailS3Path = {profileThumbnailS3Path} ",
                Values.parameters("userID", userID, "profileThumbnailS3Path", profileThumbnailS3Path));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject setVideoThumbnail(int videoID, String S3Path) {
        Statement statement = new Statement(
                "MATCH (v:Video {videoID: {videoID}}) " +
                "SET v.thumbnailS3Path = {S3Path} ",
                Values.parameters("videoID", videoID, "S3Path", S3Path));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject addVideoToEvent(int eventID, String S3ID) {
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

        JSONObject response = new JSONObject();
        while (result.hasNext()) {
            response.put("videoID", result.next().get("uid").asInt());
        }

        return response;
    }

    public JSONArray getRelatedUsers(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FOLLOWS]-(users:User) " +
                "RETURN users",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        JSONArray users = new JSONArray();

        while (result.hasNext()) {
            users.add(result.next().get("users").get("userID").asString());
        }

        return users;
    }

    public JSONObject follow(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "CREATE UNIQUE (a)-[:FOLLOWS]->(b)",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject unfollow(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "MATCH (a)-[r:FOLLOWS]->(b) " +
                "DELETE r",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject createEvent(String title, String location, String time, String creatorID) {
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
                "(u)-[:ATTENDED]->(e)",
                Values.parameters("title", title,
                        "creatorID", creatorID,
                        "location", location,
                        "time", time));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject addUserToEvent(int eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE UNIQUE (u)-[:ATTENDED]->(e)",
                Values.parameters("eventID", eventID, "userID", userID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject addUsersToEvent(int eventID, List<String> members) {
        Statement statement = new Statement(
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u:User) " +
                "WHERE u.userID IN {members} " +
                "CREATE UNIQUE (u)-[:ATTENDED]->(e)",
                Values.parameters("eventID", eventID, "members", members));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject removeUserFromEvent(int eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u)-[r:ATTENDED]->(e) " +
                "DELETE r",
                Values.parameters("eventID", eventID, "userID", userID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject getEvents(String userID) {
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

    public JSONObject likeEvent(String userID, int eventID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE UNIQUE (u)-[:LIKES]->(e) ",
                Values.parameters("eventID", eventID, "userID", userID));

        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject unlikeEvent(String userID, int eventID) {
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

    private JSONObject getEventsFromResults(StatementResult result, String callerID) {
        JSONObject response = successJson();
        JSONArray events = new JSONArray();

        while (result.hasNext()) {
            Record record = result.next();
            JSONObject event = new JSONObject();
            event.put("eventID", record.get("events").get("eventID").asInt());
            event.put("title", record.get("events").get("title").asString());
            event.put("location", record.get("events").get("location").asString());
            event.put("time", record.get("events").get("time").asString());

            // Videos
            JSONArray videosJson = new JSONArray();
            Value videos  = record.get("videos");

            for (int i = 0; i < videos.size(); i++) {
                Value video = videos.get(i);
                videosJson.add(getJsonFromVideo(video));
            }

            event.put("videos", videosJson);
            event.put("likes", Boolean.FALSE);


            // Members
            JSONArray membersJson = new JSONArray();
            Value members  = record.get("members");
            int likesCount = 0;

            for (int i = 0; i < members.size(); i++) {
                Value member = members.get(i);
                JSONObject users = new JSONObject();

                users.put("userID", member.get("member").get("userID").asString());
                users.put("name", member.get("member").get("name").asString());
                users.put("email", member.get("member").get("email").asString());
                users.put("profileID", member.get("member").get("profileThumbnailS3Path", ""));

                if (relationshipExists(member.get("likes"))) {
                    likesCount++;
                    if (member.get("member").get("userID").toString().equals(callerID)) {
                        event.put("likes", Boolean.TRUE);
                    }
                }

                membersJson.add(users);
            }

            event.put("likesCount", likesCount);
            event.put("members", membersJson);

            events.add(event);
        }

        response.put("events", events);

        return response;
    }

    private JSONObject getUsersFromResult(StatementResult result) {
        JSONObject response = successJson();
        JSONArray users = new JSONArray();

        while (result.hasNext()) {
            Record record = result.next();
            Value userValue = record.get("users");
            JSONObject user = new JSONObject();

            user.put("userID", userValue.get("userID").asString());
            user.put("name", userValue.get("name").asString());
            user.put("email", userValue.get("email").asString());
            user.put("profileID", userValue.get("profileThumbnailS3Path", ""));

            users.add(user);
        }

        response.put("users", users);

        return response;
    }

    private JSONObject getJsonFromVideo(Value value) {
        JSONObject video = new JSONObject();

        video.put("videoID", value.get("videoID").asInt());
        video.put("videoPath", value.get("S3ID").asString());
        video.put("thumbnailPath", value.get("thumbnailS3Path", ""));

        return video;
    }

    public JSONObject getFollowing(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FOLLOWS]->(users) " +
                "RETURN users",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getUsersFromResult(result);
    }

    public JSONObject getFollowers(String userID) {
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

    private JSONObject successJson() {
        return new JSONObject();
    }

    public JSONObject getInfo(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "RETURN u",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        JSONObject user = new JSONObject();

        while (result.hasNext()) {
            Record record = result.next();
            Value userValue = record.get("u");

            user.put("userID", userValue.get("userID").asString());
            user.put("name", userValue.get("name").asString());
            user.put("email", userValue.get("email").asString());
            user.put("profileID", userValue.get("profileThumbnailS3Path", ""));

        }

        return user;
    }
}
