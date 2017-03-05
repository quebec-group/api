package uk.ac.cam.cl.quebec.api;

import org.json.simple.JSONArray;
import org.neo4j.driver.v1.*;
import uk.ac.cam.cl.quebec.api.face.SNSUser;
import uk.ac.cam.cl.quebec.api.json.JSON;
import uk.ac.cam.cl.quebec.api.json.ResultFormatter;

import java.util.ArrayList;
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

    public JSON createUser(String userID, String name, String email, String arn) {
        Statement statement = new Statement(
                "MERGE (u:User {userID: {ID}}) " +
                "ON CREATE SET u.name = {name}, u.email = {email}, u.arn = {arn} " +
                "ON MATCH SET u.arn = {arn} ",
                Values.parameters("name", name, "email", email, "ID", userID, "arn", arn));
        runQuery(statement);

        return ResultFormatter.successJson();
    }

    public JSON hasCompletedSignUp(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "OPTIONAL MATCH (u)-[:TRAINING_VIDEO]->(v:Video) " +
                "RETURN u, COUNT(v) AS trainingVideoCount",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        JSON json = new JSON();
        while (result.hasNext()) {
            Record record = result.next();
            boolean setupComplete = record.get("trainingVideoCount").asInt() > 0
                    && record.get("u").containsKey("profileThumbnailS3Path");
            json.put("hasCompletedSignUp", setupComplete);
        }

        return json;
    }

    public JSON setTrainingVideo(String userID, String S3Path) {
        Statement statement = new Statement(
                // Get UUID
                "MERGE (id:UniqueId {name:'Video'}) " +
                "ON CREATE SET id.count = 1 " +
                "ON MATCH SET id.count = id.count + 1 " +
                "WITH id.count AS videoID " +

                // Create video
                "MATCH (u:User {userID: {userID}}) " +
                "MERGE (v:Video {S3ID: {S3ID}, videoID: videoID}) " +
                "CREATE UNIQUE (u)-[:TRAINING_VIDEO]->(v) " +
                "RETURN videoID",
                Values.parameters("userID", userID, "S3ID", S3Path));

        StatementResult result = runQuery(statement);

        return ResultFormatter.getReturnValue(result, "videoID");
    }

    public JSON setProfilePicture(String userID, String profileThumbnailS3Path) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "SET u.profileThumbnailS3Path = {profileThumbnailS3Path} ",
                Values.parameters("userID", userID, "profileThumbnailS3Path", profileThumbnailS3Path));
        runQuery(statement);

        return ResultFormatter.successJson();
    }

    public JSON setVideoThumbnail(int videoID, String S3Path) {
        Statement statement = new Statement(
                "MATCH (v:Video {videoID: {videoID}}) " +
                "SET v.thumbnailS3Path = {S3Path} ",
                Values.parameters("videoID", videoID, "S3Path", S3Path));
        runQuery(statement);

        return ResultFormatter.successJson();
    }

    public JSON addVideoToEvent(int eventID, String S3ID) {
        Statement statement = new Statement(
                // Get UUID
                "MERGE (id:UniqueId {name:'Video'}) " +
                "ON CREATE SET id.count = 1 " +
                "ON MATCH SET id.count = id.count + 1 " +
                "WITH id.count AS videoID " +

                // Create video
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MERGE (v:Video {S3ID: {S3ID}, videoID: videoID}) " +
                "CREATE UNIQUE (e)-[:VIDEO]->(v) " +
                "RETURN videoID",
                Values.parameters("eventID", eventID, "S3ID", S3ID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getReturnValue(result, "videoID");
    }

    public JSONArray getRelatedUsers(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[*0..3]-(related:User) " +
                "RETURN collect(DISTINCT related.userID) AS users",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getUsersIDs(result);
    }

    public JSON follow(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "CREATE UNIQUE (a)-[:FOLLOWS]->(b) " +
                "RETURN b.arn AS arn",
                Values.parameters("userAID", userAID, "userBID", userBID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getReturnValue(result, "arn");
    }

    public JSON unfollow(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID: {userAID}}) " +
                "MATCH (b:User {userID: {userBID}}) " +
                "MATCH (a)-[r:FOLLOWS]->(b) " +
                "DELETE r",
                Values.parameters("userAID", userAID, "userBID", userBID));
        runQuery(statement);

        return ResultFormatter.successJson();
    }

    public JSON createEvent(String title, String location, String time, String creatorID) {
        Statement statement = new Statement(
                // Get UUID
                "MERGE (id:UniqueId{name:'Event'}) " +
                "ON CREATE SET id.count = 1 " +
                "ON MATCH SET id.count = id.count + 1 " +
                "WITH id.count AS eventID " +

                // Create event
                "MATCH (u:User {userID: {creatorID}}) " +
                "CREATE (e:Event {title: {title}, eventID: eventID, location: {location}, time: {time}}) " +
                "CREATE UNIQUE (u)-[:CREATED]->(e), " +
                "(u)-[:ATTENDED]->(e) " +
                "RETURN eventID",
                Values.parameters("title", title,
                        "creatorID", creatorID,
                        "location", location,
                        "time", time));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getReturnValue(result, "eventID");
    }

    public JSON addUserToEvent(int eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE UNIQUE (u)-[:ATTENDED]->(e)",
                Values.parameters("eventID", eventID, "userID", userID));
        runQuery(statement);

        return ResultFormatter.successJson();
    }

    public List<SNSUser> addUsersToEventAndGetArns(int eventID, List<String> members) {
        Statement statement = new Statement(
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u:User) " +
                "WHERE u.userID IN {members} " +
                "CREATE UNIQUE (u)-[:ATTENDED]->(e) " +
                "RETURN u.arn AS arn, u.name AS name",
                Values.parameters("eventID", eventID, "members", members));
        StatementResult result = runQuery(statement);

        ArrayList<SNSUser> users = new ArrayList<>();
        while (result.hasNext()) {
            Record record = result.next();
            String arn = record.get("arn").asString();
            String name = record.get("name").asString();
            users.add(new SNSUser(arn, name));
        }

        return users;
    }

    public JSON removeUserFromEvent(int eventID, String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u)-[r:ATTENDED]->(e) " +
                "DELETE r",
                Values.parameters("eventID", eventID, "userID", userID));
        runQuery(statement);

        return ResultFormatter.successJson();
    }

    public JSON getAttendedEvents(String userID) {
        Statement statement = new Statement(
                "MATCH (caller:User {userID: {userID}})-[:ATTENDED]->(events:Event) " +
                "MATCH (events)-[:ATTENDED]-(atEvent:User) " +
                "OPTIONAL MATCH (events)-[:VIDEO]-(eventVideos:Video) " +
                "OPTIONAL MATCH (atEvent)-[l:LIKES]-(events) " +
                "OPTIONAL MATCH (atEvent)-[c:CREATED]-(events) " +
                "RETURN events, collect(distinct {member: atEvent, likes: l, created: c}) AS members, " +
                "collect(distinct eventVideos) AS videos",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getEventsFromResults(result, userID);
    }

    public JSON getEvents(String userID) {
        Statement statement = new Statement(
                "MATCH (caller:User {userID: {userID}})-[*1..2]-(events:Event) " +
                "MATCH (events)-[:ATTENDED|CREATED]-(atEvent:User) " +
                "OPTIONAL MATCH (events)-[:VIDEO]-(eventVideos:Video) " +
                "OPTIONAL MATCH (atEvent)-[l:LIKES]-(events) " +
                "OPTIONAL MATCH (atEvent)-[c:CREATED]-(events) " +
                "RETURN events, collect(distinct {member: atEvent, likes: l, created: c}) AS members, " +
                    "collect(distinct eventVideos) AS videos",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getEventsFromResults(result, userID);
    }

    public JSON likeEvent(String userID, int eventID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "CREATE UNIQUE (u)-[:LIKES]->(e) ",
                Values.parameters("eventID", eventID, "userID", userID));

        runQuery(statement);

        return ResultFormatter.successJson();
    }

    public JSON unlikeEvent(String userID, int eventID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u)-[r:LIKES]->(e) " +
                "DELETE r",
                Values.parameters("eventID", eventID, "userID", userID));

        runQuery(statement);

        return ResultFormatter.successJson();
    }

    public JSON getFollowing(String userID) {
        Statement statement = new Statement(
                "MATCH (me:User {userID: {userID}}) " +
                "MATCH (me)-[followsRelation:FOLLOWS]->(users) " +
                "RETURN users, followsRelation",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getUsersFromResultWithFollowingInfo(result);
    }

    public JSON getFollowers(String userID) {
        Statement statement = new Statement(
                "MATCH (me:User {userID: {userID}})" +
                "MATCH (users)-[:FOLLOWS]->(me) " +
                "OPTIONAL MATCH (me)-[followsRelation:FOLLOWS]->(users)" +
                "RETURN users, followsRelation",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getUsersFromResultWithFollowingInfo(result);
    }

    public JSON getFollowingCount(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}})-[:FOLLOWS]->(users) " +
                "RETURN COUNT(users) AS count",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getReturnValue(result, "count");
    }

    public JSON getFollowersCount(String userID) {
        Statement statement = new Statement(
                "MATCH (users)-[:FOLLOWS]->(u:User {userID: {userID}}) " +
                "RETURN COUNT(users) AS count",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getReturnValue(result, "count");
    }

    public JSON getInfo(String userID) {
        Statement statement = new Statement(
                "MATCH (user:User {userID: {userID}}) " +
                "RETURN user",
                Values.parameters("userID", userID));
        StatementResult result = runQuery(statement);

        return ResultFormatter.getUser(result);
    }

    public JSON findByName(String currentID, String name) {
        Statement statement = new Statement(
                "MATCH (users:User) " +
                "WHERE users.name =~ {name} AND users.userID <> {currentID} " +
                "MATCH (me:User {userID:{currentID}}) " +
                "OPTIONAL MATCH (me)-[followsRelation:FOLLOWS]->(users)" +
                "RETURN users, followsRelation",
                Values.parameters("name", "(?i).*" + name + ".*", "currentID", currentID));

        StatementResult result = runQuery(statement);

        return ResultFormatter.getUsersFromResultWithFollowingInfo(result);
    }

    public JSON findByEmail(String currentID, String email) {
        Statement statement = new Statement(
                "MATCH (users:User) " +
                "WHERE users.email = {email} AND users.userID <> {currentID} " +
                "MATCH (me:User {userID:{currentID}}) " +
                "OPTIONAL MATCH (me)-[followsRelation:FOLLOWS]->(users)" +
                "RETURN users, followsRelation",
                Values.parameters("email", email, "currentID", currentID));

        StatementResult result = runQuery(statement);

        return ResultFormatter.getUsersFromResultWithFollowingInfo(result);
    }

    public JSON doesAfollowB(String userAID, String userBID) {
        Statement statement = new Statement(
                "MATCH (a:User {userID:{userAID}}) " +
                "MATCH (b:User {userID:{userBID}}) " +
                "OPTIONAL MATCH (a)-[r:FOLLOWS]->(b) " +
                "RETURN r",
                Values.parameters("userAID", userAID, "userBID", userBID));

        StatementResult result = runQuery(statement);

        return ResultFormatter.doesRelationExist(result);
    }

    public SNSUser getCreator(int eventID) {
        Statement statement = new Statement(
                "MATCH (e:Event {eventID: {eventID}}) " +
                "MATCH (u:User)-[:CREATED]->(e) " +
                "RETURN u.arn AS arn, u.name AS name",
                Values.parameters("eventID", eventID));

        StatementResult result = runQuery(statement);

        Record record = result.next();

        return new SNSUser(record.get("arn").asString(), record.get("name").asString());
    }

    public SNSUser getUser(String userID) {
        Statement statement = new Statement(
                "MATCH (u:User {userID: {userID}}) " +
                "RETURN u.arn AS arn, u.name AS name",
                Values.parameters("userID", userID));

        StatementResult result = runQuery(statement);

        Record record = result.next();

        return new SNSUser(record.get("arn").asString(), record.get("name").asString());
    }

    public StatementResult runQuery(Statement query) {
        Session session = getSession();
        StatementResult result = session.run(query);
        session.close();
        return result;
    }
}
