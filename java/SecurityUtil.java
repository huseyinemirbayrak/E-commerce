public class SecurityUtil {
    
    public static void requireRole(User user, String... allowedRoles) 
            throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Not logged in");
        }
        
        for (String role : allowedRoles) {
            if (role.equals(user.getRole())) {
                return;  // Authorized
            }
        }
        
        throw new UnauthorizedException(
            "Access denied. Required role: " + String.join(" or ", allowedRoles));
    }
    
    public static void requireCustomer(User user) throws UnauthorizedException {
        requireRole(user, "Customer");
    }
    
    public static void requireSeller(User user) throws UnauthorizedException {
        requireRole(user, "Seller");
    }
    
    public static void requireAdmin(User user) throws UnauthorizedException {
        requireRole(user, "Administrator");
    }
}

class UnauthorizedException extends Exception {
    public UnauthorizedException(String message) {
        super(message);
    }
}

