package uk.ac.cam.cl.quebec.api.face;

public class ProfileProcessedLambdaInput {
    private String userID;
    private int videoID;
    private String S3ID;

    public String getUserID() {
        return userID;
    }

    public void setVideoID(int videoID) {
        this.videoID = videoID;
    }

    public int getVideoID() {
        return videoID;
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
        return "ProfileProcessedLambdaInput userID: " + userID +
                ", S3ID: " + S3ID +
                ", videoID: " + videoID;
    }
}
