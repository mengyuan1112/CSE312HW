public class User {
    private String username;
    private String password;
    private String salt;
    private String saltedPwd;
    private String cookies;

    public User(String username, String password, String salt, String saltedPwd, String cookies) {
        this.cookies = cookies;
        this.password = password;
        this.salt = salt;
        this.saltedPwd = saltedPwd;
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public String getCookies() {
        return cookies;
    }

    public String getSaltedPwd() {
        return saltedPwd;
    }

    public String getUsername() {
        return username;
    }

    public String getSalt() {
        return salt;
    }
}
