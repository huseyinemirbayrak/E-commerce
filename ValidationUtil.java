public class ValidationUtil {
    
    public static void validateEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new ValidationException("Invalid email format");
        }
    }
    
    public static void validatePassword(String password) throws ValidationException {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }
    }
    
    public static void validatePrice(double price) throws ValidationException {
        if (price < 0) {
            throw new ValidationException("Price cannot be negative");
        }
        if (price > 1000000) {
            throw new ValidationException("Price exceeds maximum allowed ($1,000,000)");
        }
    }
    
    public static void validateQuantity(int quantity) throws ValidationException {
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be positive");
        }
        if (quantity > 10000) {
            throw new ValidationException("Quantity exceeds maximum allowed (10,000)");
        }
    }
    
    public static void validateName(String name) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }
        if (name.length() > 100) {
            throw new ValidationException("Name cannot exceed 100 characters");
        }
    }
    
    public static String sanitizeString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("[<>\"']", "");
    }
}

class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);
    }
}

