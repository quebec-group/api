package uk.ac.cam.cl.quebec.api.face;

public class SNSUser {
    private String arn;
    private String name;

    public SNSUser(String arn, String name) {
        this.arn = arn;
        this.name = name;
    }

    public String getArn() {
        return arn;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SNSUser name: " + name + ", arn: " + arn;
    }
}
