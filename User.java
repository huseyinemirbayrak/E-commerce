public class User {
    private int id;
    private String username;
    private String role;
    private String fullName;

    public User(int id, String username, String role, String fullName) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
}