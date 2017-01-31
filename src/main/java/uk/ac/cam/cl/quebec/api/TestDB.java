package uk.ac.cam.cl.quebec.api;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.Base64;
import org.neo4j.driver.v1.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

// uk.ac.cam.cl.quebec.api.TestDB::handleRequest
public class TestDB implements RequestHandler<Object, String> {

    private static String DB_PASS = System.getenv("grapheneDBPass");


    @Override
    public String handleRequest(Object input, Context context) {
        if (context != null) {
            LambdaLogger logger = context.getLogger();
            logger.log("Serving request, input : " + input);
        }

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