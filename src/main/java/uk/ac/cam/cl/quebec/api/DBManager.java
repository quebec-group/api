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
                "CREATE (u:User {name: {name}, email: {email}, userID: {ID}}) ",
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
                "CREATE (v:Video {S3ID: {S3ID}})" +
                "CREATE (u)-[:VIDEO]->(v)",
                Values.parameters("eventID", Integer.parseInt(eventID), "S3ID", S3ID));

        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject getFriends(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FRIENDS]-(friends:User) " +
                "RETURN friends",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getFriendsFromResult(result);
    }

    public JSONObject addFriend(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "CREATE (a)-[:FRIENDS]->(b)",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject removeFriend(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "MATCH (a)-[r:FRIENDS]-(b) " +
                "DELETE r",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject addFriendRequest(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "CREATE (a)-[:REQUEST]->(b)",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return successJson();
    }

    public JSONObject getPendingFriendRequests(String userID) {
        Statement statement = new Statement(
                "MATCH (friends) -[:REQUEST]->(u:User {userID: {userID}}) " +
                "RETURN friends",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getFriendsFromResult(result);
    }

    public JSONObject getSentFriendRequests(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:REQUEST]->(friends) " +
                "RETURN friends",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return getFriendsFromResult(result);
    }

    public JSONObject removeFriendRequest(String userAID, String userBID) {
        Statement statement = new Statement("MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "MATCH (a)-[r:REQUEST]->(b)" +
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
                "CREATE (u)-[:ATTENDED]->(e)",
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
                "MATCH (events)-[:VIDEO]-(eventVideos:Video) " +
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
                "CREATE (u)-[:LIKES]->(e) ",
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

            // Friends
            JSONArray membersJson = new JSONArray();
            Value members  = record.get("members");

            for (int i = 0; i < members.size(); i++) {
                Value member = members.get(i);
                JSONObject friend = new JSONObject();

                friend.put("userID", member.get("member").get("userID"));
                friend.put("name", member.get("member").get("name"));
                friend.put("email", member.get("member").get("email"));
                friend.put("profileID", member.get("member").get("profilePicID", ""));
                friend.put("likesEvent", relationshipExists(member.get("likes")));

                membersJson.add(friend);
            }

            event.put("members", membersJson);

            events.add(event);
        }

        response.put("events", events);

        return response;
    }

    private JSONObject getFriendsFromResult(StatementResult result) {
        JSONObject response = successJson();
        JSONArray friends = new JSONArray();

        while (result.hasNext()) {
            Record record = result.next();
            Value friendValue = record.get("friends");
            JSONObject friend = new JSONObject();

            friend.put("userID", friendValue.get("userID"));
            friend.put("name", friendValue.get("name"));
            friend.put("email", friendValue.get("email"));
            friend.put("profileID", friendValue.get("profilePicID", ""));

            friends.add(friend);
        }

        response.put("friends", friends);

        return response;
    }

    private JSONObject getJsonFromVideo(Value value) {
        JSONObject video = new JSONObject();

        video.put("videoID", value.get("S3ID"));

        return video;
    }

    private Boolean relationshipExists(Value value) {
        return value instanceof RelationshipValue;
    }

    private JSONObject successJson() {
        return new JSONObject();
    }
}
