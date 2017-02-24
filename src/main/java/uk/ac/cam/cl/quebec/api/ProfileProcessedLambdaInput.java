package uk.ac.cam.cl.quebec.api;

public class ProfileProcessedLambdaInput {

    private String userID = "";
    private String S3ID = "";

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getS3ID() {
        return S3ID;
    }

    public void setS3ID(String s3ID) {
        S3ID = s3ID;
    }

    @Override
    public String toString() {
        return "ProfileProcessedLambdaInput userID: " + userID + ", S3ID: " + S3ID;
    }
}
