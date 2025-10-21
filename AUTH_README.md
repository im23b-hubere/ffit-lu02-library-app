# JWT Authentication Implementation - LU06a

This implementation adds JWT (JSON Web Token) authentication to the Library App REST API.

## Features Implemented

### 1. JWT Authentication System
- **JWT Token Generation**: Creates signed JWT tokens with user information
- **Password Hashing**: Uses PBKDF2 with SHA-256 for secure password storage
- **Token Validation**: Validates JWT tokens for protected endpoints

### 2. API Endpoints

#### POST /auth/login
Authenticates a user and returns a JWT token.

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "userpassword"
}
```

**Success Response (200):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Error Response (401):**
```json
{
    "error": "Invalid email or password"
}
```

#### PUT /auth/change-password
Changes a user's password (requires JWT authentication).

**Headers:**
```
Authorization: Bearer <your_jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
    "oldPassword": "currentpassword",
    "newPassword": "newpassword"
}
```

**Success Response (200):**
```json
{
    "message": "Password changed successfully"
}
```

**Error Responses:**
- 401: Invalid or expired token / Invalid old password
- 404: User not found

## Test Users

The system automatically creates test users on startup:

| Email | Password |
|-------|----------|
| test@example.com | password123 |
| admin@library.com | admin123 |
| librarian@library.com | librarian123 |
| user@library.com | user123 |

## Testing the API

### 1. Using HTTP Client Files
Use the `test-auth-api.http` file with REST Client extensions in VS Code or similar tools.

### 2. Using Java Test Class
Run the `test-auth.java` file after starting the server:
```bash
java test-auth.java
```

### 3. Using Postman
1. **Login Request:**
   - Method: POST
   - URL: `http://localhost:7070/auth/login`
   - Headers: `Content-Type: application/json`
   - Body: `{"email": "test@example.com", "password": "password123"}`

2. **Change Password Request:**
   - Method: PUT
   - URL: `http://localhost:7070/auth/change-password`
   - Headers: 
     - `Content-Type: application/json`
     - `Authorization: Bearer <token_from_login>`
   - Body: `{"oldPassword": "password123", "newPassword": "newPassword456"}`

## JWT Token Analysis

You can analyze the returned JWT tokens at [https://jwt.io/](https://jwt.io/)

The token contains:
- **Header**: Algorithm and token type
- **Payload**: User email, user ID, issued at, expiration time
- **Signature**: Verification signature

## Database Schema

The system creates a `users` table with the following structure:
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Security Features

1. **Password Hashing**: Uses PBKDF2 with 10,000 iterations
2. **Salt**: Each password uses a unique random salt
3. **JWT Signing**: Tokens are signed with a secret key
4. **Token Expiration**: Tokens expire after 24 hours
5. **Consistent Error Messages**: Same error message for invalid email or password

## Files Added/Modified

### New Files:
- `src/main/java/ch/bzz/model/User.java` - User entity model
- `src/main/java/ch/bzz/util/PasswordHandler.java` - Password hashing utilities
- `src/main/java/ch/bzz/util/JwtHandler.java` - JWT token utilities
- `src/main/java/ch/bzz/persistence/UserPersistor.java` - User database operations
- `src/main/java/ch/bzz/util/TestDataUtil.java` - Test data creation
- `test-auth-api.http` - HTTP client test file
- `test-auth.java` - Java test class

### Modified Files:
- `build.gradle` - Added JWT dependencies
- `src/main/java/ch/bzz/JavalinMain.java` - Added authentication endpoints

## Running the Application

1. **Start the server:**
   ```bash
   ./gradlew run
   ```

2. **Test the endpoints:**
   - Use the provided test files
   - Or use Postman/curl with the examples above

3. **Check the console output** for test user creation and server startup messages.

The server will run on `http://localhost:7070` with the following endpoints:
- `GET /books` - List books (existing endpoint)
- `POST /auth/login` - User login
- `PUT /auth/change-password` - Change password
