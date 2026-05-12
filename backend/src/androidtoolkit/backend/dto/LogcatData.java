package androidtoolkit.backend.dto;

public class LogcatData {

    private String serial;
    private String environment;
    private String clientVersion;
    private String serverProductVersion;
    private String serverProjectVersion;
    private String accessToken;
    private String tokenType;

    public LogcatData(String serial) {
        this.serial = serial;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getServerProductVersion() {
        return serverProductVersion;
    }

    public void setServerProductVersion(String serverProductVersion) {
        this.serverProductVersion = serverProductVersion;
    }

    public String getServerProjectVersion() {
        return serverProjectVersion;
    }

    public void setServerProjectVersion(String serverProjectVersion) {
        this.serverProjectVersion = serverProjectVersion;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
