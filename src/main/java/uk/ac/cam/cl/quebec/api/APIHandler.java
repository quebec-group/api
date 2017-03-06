package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.neo4j.driver.v1.exceptions.ClientException;
import uk.ac.cam.cl.quebec.api.json.JSON;

import java.util.LinkedHashMap;

// uk.ac.cam.cl.quebec.api.APIHandler::handleRequest
public class APIHandler implements RequestHandler<JSON, JSON> {

    private JSONParser parser = new JSONParser();
    private DBManager db = new DBManager();
    private SQSWrapper sqs = new SQSWrapper();
    private SNSWrapper sns = new SNSWrapper();

    @Override
    public JSON handleRequest(JSON input, Context context) {
        JSON responseJson;

        try {
            responseJson = new JSON();
            responseJson.put("statusCode", "200");
            responseJson.put("headers", new JSON());
            responseJson.put("body", getResultForQuery(input, context).toString());
        } catch (ParseException|APIException|ClientException e) {
            if (context != null) {
                context.getLogger().log(e.toString());
            }

            responseJson = new JSON();
            responseJson.put("statusCode", "400");
            responseJson.put("headers", new JSON());
            responseJson.put("body", errorBody(e.getMessage()).toString());
        }


        return responseJson;
    }

    // Get the userID in requestContext -> identity -> cognitoIdentityId, set by AWS
    private String getUserID(JSON input) {
        LinkedHashMap requestContext = (LinkedHashMap) input.get("requestContext");
        LinkedHashMap identity = (LinkedHashMap) requestContext.get("identity");
        return (String) identity.get("cognitoIdentityId");
    }

    // Extract the parameters from the raw input JSON
    private JSON getParams(JSON input) throws ParseException {
        JSONObject json = (JSONObject) parser.parse((String) input.get("body"));
        return new JSON(json);
    }

    // Gets the request by finding the path element after /api/
    // E.g. /api/cat -> cat
    private String getRequest(JSON input, Context context) throws APIException {
        String path = input.getString("path");
        String request = path.replace("/api/", "");

        if (context != null) {
            context.getLogger().log("Doing: " + request);
        }

        return request;
    }

    private JSON getResultForQuery(JSON input, Context context) throws ParseException, APIException, ClientException {
        JSON params = getParams(input);
        String request = getRequest(input, context);

        switch (request) {
            case "createUser":
                return db.createUser(getUserID(input),
                        params.getString("name"),
                        params.getString("email"),
                        params.getString("arn"));
            case "follow": {
                String myID = getUserID(input);
                JSON result =  db.follow(myID, params.getString("userID"));
                sns.notifyFollowed(result.getString("arn"), myID);

                return result;
            }
            case "hasCompletedSignUp":
                return db.hasCompletedSignUp(getUserID(input));
            case "unfollow":
                return db.unfollow(getUserID(input),
                        params.getString("userID"));
            case "following":
                return db.getFollowing(params.getString("userID"));
            case "followers":
                return db.getFollowers(params.getString("userID"));
            case "followingCount":
                return db.getFollowingCount(params.getString("userID"));
            case "followersCount":
                return db.getFollowersCount(params.getString("userID"));
            case "setTrainingVideo": {
                String S3ID = params.getString("S3ID");
                String userID = getUserID(input);

                JSON response = db.setTrainingVideo(userID, S3ID);

                sqs.sendTrainingVideo(S3ID, userID, response.getInteger("videoID"), context);

                return response;
            }
            case "setProfilePicture":
                return db.setProfilePicture(getUserID(input), params.getString("S3ID"));
            case "addVideoToEvent":
                return addVideoToEvent(getUserID(input),
                        params.getInteger("eventID"), params.getString("S3ID"), context);
            case "createEvent": {
                String userID = getUserID(input);
                JSON eventResponse = db.createEvent(params.getString("title"),
                        params.getString("location"),
                        params.getString("time"),
                        userID);

                String S3ID = params.getString("videoPath");
                int eventID = eventResponse.getInteger("eventID");

                return addVideoToEvent(userID, eventID, S3ID, context);
            }
            case "addUserToEvent":
                return db.addUserToEvent(params.getInteger("eventID"),
                        params.getString("userID"));
            case "removeFromEvent":
                return db.removeUserFromEvent(params.getInteger("eventID"),
                        getUserID(input));
            case "getEvents":
                return db.getEvents(getUserID(input));
            case "getAttendedEvents":
                return db.getAttendedEvents(params.getString("userID"));
            case "likeEvent":
                return db.likeEvent(getUserID(input), params.getInteger("eventID"));
            case "unlikeEvent":
                return db.unlikeEvent(getUserID(input), params.getInteger("eventID"));
            case "getInfo":
                return db.getInfo(getUserID(input));
            case "followsMe":
                return db.doesAfollowB(params.getString("userID"), getUserID(input));
            case "iFollow":
                return db.doesAfollowB(getUserID(input), params.getString("userID"));
            case "find": {
                String searchString = params.getString("searchString");
                if (searchString.contains("@")) {
                    return db.findByEmail(getUserID(input), searchString);
                } else {
                    return db.findByName(getUserID(input), searchString);
                }
            }
            default:
                throw new APIException("API '" + request + "' not supported");
        }
    }

    private JSON addVideoToEvent(String userID, int eventID, String S3ID, Context context) throws APIException {
        JSON response = db.addVideoToEvent(eventID, S3ID);

        sqs.sendEventVideo(S3ID, eventID, response.getInteger("videoID"),
                db.getRelatedUsers(userID), context);

        return response;
    }

    private JSON errorBody(String message) {
        JSON error = new JSON();
        error.put("errorMessage", message);
        return error;
    }
}
