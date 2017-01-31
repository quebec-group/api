package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neo4j.driver.v1.*;

import java.io.*;

// uk.ac.cam.cl.quebec.api.TestDB::handleRequest
public class TestDB implements RequestStreamHandler {

    private static String DB_PASS = System.getenv("grapheneDBPass");

    JSONParser parser = new JSONParser();

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("Loading Java Lambda handler of ProxyWithStream");


        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        JSONObject responseJson = new JSONObject();
        String responseCode = "200";

        try {
            JSONObject event = (JSONObject) parser.parse(reader);
            JSONObject responseBody = new JSONObject();
            responseBody.put("message", getDatabaseQueryResult());
            responseBody.put("input", event.toJSONString());

            responseJson.put("statusCode", responseCode);
            responseJson.put("headers", new JSONObject());
            responseJson.put("body", responseBody.toString());

        } catch (ParseException pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }

        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
        writer.write(responseJson.toJSONString());
        writer.close();
    }


    public String getDatabaseQueryResult() {
        StringBuilder builder = new StringBuilder();

        //Test database connection
        Driver driver = GraphDatabase.driver("bolt://hobby-dckpkbcijildgbkelginfool.dbs.graphenedb.com:24786",
                AuthTokens.basic("quebec", DB_PASS));

        Session session = driver.session();

        StatementResult result = session.run("MATCH (you {name:\"Callum\"})-[:FRIENDS_WITH]->(yourFriends) RETURN yourFriends");

        while (result.hasNext()) {
            Record record = result.next();
            builder.append("Friend: ")
                    .append(record.get("yourFriends").get("name").asString())
                    .append(", ")
                    .append(record.get("yourFriends").get("crsid").asString())
                    .append(" \n");
        }

        session.close();
        driver.close();

        return String.valueOf( builder.toString());

    }
}