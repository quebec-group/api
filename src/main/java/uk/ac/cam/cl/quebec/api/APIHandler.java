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
    private DBManager db = new DBManager();
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

    private String getString(JSONObject params, String key) throws APIException {
        String param = (String) params.get(key);

        if (param == null) {
            throw new APIException("Parameter '" + key + "' not found");
        }

        return param;
    }

    private Integer getInteger(JSONObject params, String key) throws APIException {
        if (params.containsKey(key) && params.get(key) instanceof Number) {
            return getInt(params.get(key));
        }

        throw new APIException("Parameter '" + key + "' not found");
    }

    private int getInt(Object object) {
        return ((Number) object).intValue();
    }

    private JSONObject getResultForQuery(JSONObject input) throws ParseException, APIException, ClientException {
        JSONObject params = getParams(input);
        String request = getRequest(input);

        switch (request) {
            case "createUser":
                return db.createUser(getUserID(input),
                        getString(params, "name"),
                        getString(params, "email"),
                        getString(params, "arn"));
            case "follow":
                return db.follow(getUserID(input),
                        getString(params, "userID"));
            case "hasCompletedSignUp":
                return db.hasCompletedSignUp(getUserID(input));
            case "unfollow":
                return db.unfollow(getUserID(input),
                        getString(params, "userID"));
            case "following":
                return db.getFollowing(getString(params, "userID"));
            case "followers":
                return db.getFollowers(getString(params, "userID"));
            case "setTrainingVideo": {
                String S3ID = getString(params, "S3ID");
                String userID = getUserID(input);

                JSONObject response = db.setTrainingVideo(userID, S3ID);

                sqs.sendTrainingVideo(S3ID, userID, (Integer) response.get("videoID"));

                return response;
            }
            case "setProfilePicture":
                return db.setProfilePicture(getUserID(input), getString(params, "S3ID"));
            case "addVideoToEvent":
                return addVideoToEvent(getUserID(input),
                    getInteger(params, "eventID"),
                    getString(params, "S3ID"));
            case "createEvent": {
                String userID = getUserID(input);
                JSONObject eventResponse = db.createEvent(getString(params, "title"),
                        getString(params, "location"),
                        getString(params, "time"),
                        userID);

                String S3ID = getString(params, "videoPath");
                int eventID = getInt(eventResponse.get("eventID"));

                return addVideoToEvent(userID, eventID, S3ID);
            }
            case "addUserToEvent":
                return db.addUserToEvent(getInteger(params, "eventID"),
                        getString(params, "userID"));
            case "removeFromEvent":
                return db.removeUserFromEvent(getInteger(params, "eventID"),
                        getUserID(input));
            case "getEvents":
                return db.getEvents(getUserID(input));
            case "likeEvent":
                return db.likeEvent(getUserID(input), getInteger(params, "eventID"));
            case "unlikeEvent":
                return db.unlikeEvent(getUserID(input), getInteger(params, "eventID"));
            case "getInfo":
                return db.getInfo(getUserID(input));
            case "isFollowing":
                return db.isFollowing(getUserID(input), getString(params, "userID"));
            case "find": {
                String searchString = getString(params, "searchString");
                if (searchString.contains("@")) {
                    return db.findByEmail(searchString);
                } else {
                    return db.findByName(searchString);
                }
            }
            default:
                throw new APIException("API '" + request + "' not supported");
        }
    }

    private JSONObject addVideoToEvent(String userID, int eventID, String S3ID) {
        JSONObject response = db.addVideoToEvent(eventID, S3ID);


        sqs.sendEventVideo(S3ID, eventID, getInt(response.get("videoID")),
                db.getRelatedUsers(userID));

        return response;
    }

    private JSONObject errorBody(String message) {
        JSONObject error = new JSONObject();
        error.put("errorMessage", message);
        return error;
    }
}
