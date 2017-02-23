package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neo4j.driver.v1.exceptions.ClientException;

import java.util.LinkedHashMap;

// uk.ac.cam.cl.quebec.api.APIHandler::handleRequest
public class APIHandler implements RequestHandler<JSONObject, JSONObject> {

    private JSONParser parser = new JSONParser();
    protected DBManager db = new DBManager();
    private SQSWrapper sqs = new SQSWrapper();

    @Override
    public JSONObject handleRequest(JSONObject input, Context context) {

        JSONObject responseJson = null;

        try {
            responseJson = new JSONObject();
            responseJson.put("statusCode", "200");
            responseJson.put("headers", new JSONObject());
            responseJson.put("body", getResultForQuery(input).toString());

            if (context != null) {
                context.getLogger().log(responseJson.toString());
            }
        } catch (ParseException|APIException|ClientException e) {
            if (context != null) {
                context.getLogger().log(e.toString());
            }

            responseJson = new JSONObject();
            responseJson.put("statusCode", "400");
            responseJson.put("headers", new JSONObject());
            responseJson.put("body", errorBody(e.getMessage()).toString());
        }


        return responseJson;
    }

    private String getUserID(JSONObject input) {
        LinkedHashMap requestContext = (LinkedHashMap) input.get("requestContext");
        LinkedHashMap identity = (LinkedHashMap) requestContext.get("identity");
        return (String) identity.get("cognitoIdentityId");
    }

    private JSONObject getParams(JSONObject input) throws ParseException {
        return (JSONObject) parser.parse((String) input.get("body"));
    }

    private String getRequest(JSONObject input) {
        String path = (String) input.get("path");
        return path.replace("/api/", "");
    }

    private String getParam(JSONObject params, String key) throws APIException {
        String param = (String) params.get(key);

        if (param == null) {
            throw new APIException("Parameter '" + key + "' not found");
        }

        return param;
    }


    private JSONObject getResultForQuery(JSONObject input) throws ParseException, APIException, ClientException {
        JSONObject params = getParams(input);
        String request = getRequest(input);

        switch (request) {
            case "createUser":
                return db.createUser(getUserID(input),
                        getParam(params, "name"),
                        getParam(params, "email"));
            case "getFriends":
                return db.getFriendsForApp(getUserID(input));
            case "setProfilePicture": {
                String S3ID = getParam(params, "S3ID");
                String userID = getUserID(input);

                sqs.sendTrainingVideo(userID, S3ID);
                return db.setProfilePicture(userID, S3ID);
            }
            case "addVideoToEvent": {
                String S3ID = getParam(params, "S3ID");
                String eventID = getParam(params, "eventID");
                String userID = getUserID(input);

                sqs.sendEventVideo(S3ID, eventID, db.getFriendsIDList(userID));
                return db.addVideoToEvent(getParam(params, "eventID"),
                        getParam(params, "S3ID"));
            }
            case "addFriend":
                return db.addFriend(getUserID(input),
                        getParam(params, "friendID"));
            case "removeFriend":
                return db.removeFriend(getUserID(input),
                        getParam(params, "friendID"));
            case "addFriendRequest":
                return db.addFriendRequest(getUserID(input),
                        getParam(params, "friendID"));
            case "getPendingFriendRequests":
                return db.getPendingFriendRequests(getUserID(input));
            case "getSentFriendRequests":
                return db.getSentFriendRequests(getUserID(input));
            case "createEvent":
                return db.createEvent(getParam(params, "title"),
                        getParam(params, "location"),
                        getParam(params, "time"),
                        getUserID(input));
            case "addUserToEvent":
                return db.addUserToEvent(getParam(params, "eventID"),
                        getParam(params, "friendID"));
            case "removeUserFromEvent":
                return db.removeUserFromEvent(getParam(params, "eventID"),
                        getUserID(input));
            case "getEvents":
                return db.getEvents(getUserID(input));
            case "likeEvent":
                return db.likeEvent(getUserID(input), getParam(params, "eventID"));
            case "unlikeEvent":
                return db.likeEvent(getUserID(input), getParam(params, "eventID"));
            default:
                throw new APIException("API '" + request + "' not supported");
        }
    }

    private JSONObject errorBody(String message) {
        JSONObject error = new JSONObject();
        error.put("errorMessage", message);
        return error;
    }
}
