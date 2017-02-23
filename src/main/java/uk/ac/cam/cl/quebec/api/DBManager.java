package uk.ac.cam.cl.quebec.api;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.neo4j.driver.internal.value.RelationshipValue;
import org.neo4j.driver.v1.*;

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

    public JSONObject createUser(String ID, String name, String email) {
        Statement statement = new Statement(
                "MERGE (u:User {name: {name}, email: {email}, userID: {ID}}) ",
                Values.parameters("name", name, "email", email, "ID", ID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject setProfilePicture(String userID, String S3ID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "SET u.profilePicID = {S3ID} ",
                Values.parameters("userID", userID, "S3ID", S3ID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject addVideoToEvent(String eventID, String S3ID) {
        Statement statement = new Statement(
                "MATCH (u:Event {eventID: {eventID}}) " +
                "MERGE (v:Video {S3ID: {S3ID}})" +
                "CREATE (u)-[:VIDEO]->(v)",
                Values.parameters("eventID", Integer.parseInt(eventID), "S3ID", S3ID));

        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONArray getRelatedUsers(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FOLLOW]-(users:User) " +
                "RETURN users",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        JSONArray users = new JSONArray();

        while (result.hasNext()) {
            users.add(result.next().get("users").get("userID"));
        }

        return users;
    }

    public JSONObject follow(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "CREATE UNIQUE (a)-[:FOLLOW]->(b)",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject unfollow(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "MATCH (a)-[r:FOLLOW]->(b) " +
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
                "CREATE (u)-[:CREATED]->(e), " +
                "(u)-[:ATTENDED]->(e)",
                Values.parameters("title", title,
                        "creatorID", creatorID,
                        "location", location,
                        "time", time));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject addUserToEvent(String eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE UNIQUE (u)-[:ATTENDED]->(e)",
                Values.parameters("eventID", Integer.parseInt(eventID), "userID", userID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject removeUserFromEvent(String eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u)-[r:ATTENDED]->(e) " +
                "DELETE r",
                Values.parameters("eventID", Integer.parseInt(eventID), "userID", userID));
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

        return getEventsFromResults(result);
    }

    public JSONObject likeEvent(String userID, String eventID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE UNIQUE (u)-[:LIKES]->(e) ",
                Values.parameters("eventID", Integer.parseInt(eventID), "userID", userID));

        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject unlikeEvent(String userID, String eventID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u)-[r:LIKES]->(e) " +
                "DELETE r",
                Values.parameters("eventID", Integer.parseInt(eventID), "userID", userID));

        StatementResult result = runQuery(statement);

        return successJson();
    }


    public StatementResult runQuery(Statement query) {
        Session session = getSession();
        StatementResult result = session.run(query);
        session.close();
        return result;
    }

    private JSONObject getEventsFromResults(StatementResult result) {
        JSONObject response = successJson();
        JSONArray events = new JSONArray();

        while (result.hasNext()) {
            Record record = result.next();
            JSONObject event = new JSONObject();
            event.put("eventID", record.get("events").get("eventID"));
            event.put("title", record.get("events").get("title"));
            event.put("location", record.get("events").get("location"));

            // Videos
            JSONArray videosJson = new JSONArray();
            Value videos  = record.get("videos");

            for (int i = 0; i < videos.size(); i++) {
                Value video = videos.get(i);
                videosJson.add(getJsonFromVideo(video));
            }

            event.put("videos", videosJson);

            // Members
            JSONArray membersJson = new JSONArray();
            Value members  = record.get("members");

            for (int i = 0; i < members.size(); i++) {
                Value member = members.get(i);
                JSONObject users = new JSONObject();

                users.put("userID", member.get("member").get("userID"));
                users.put("name", member.get("member").get("name"));
                users.put("email", member.get("member").get("email"));
                users.put("profileID", member.get("member").get("profilePicID", ""));
                users.put("likesEvent", relationshipExists(member.get("likes")));

                membersJson.add(users);
            }

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

            user.put("userID", userValue.get("userID"));
            user.put("name", userValue.get("name"));
            user.put("email", userValue.get("email"));
            user.put("profileID", userValue.get("profilePicID", ""));

            users.add(user);
        }

        response.put("users", users);

        return response;
    }

    private JSONObject getJsonFromVideo(Value value) {
        JSONObject video = new JSONObject();

        video.put("videoID", value.get("S3ID"));

        return video;
    }

    public JSONObject getFollowing(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FOLLOW]->(users) " +
                "RETURN users",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getUsersFromResult(result);
    }

    public JSONObject getFollowers(String userID) {
        Statement statement = new Statement(
                "MATCH (users)-[:FOLLOW]->(u:User {userID: {userID}}) " +
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
}
