# Auth Registration API

## Endpoint Details
**Endpoint**: `POST /api/v1/auth/register`
**Purpose**: Allows a new customer to create an account in ShopSphere.

## Request Body
```json
{
  "email": "customer@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Validation Rules
- **email**: Required, must be a valid format, maximum 255 characters.
- **password**: Required, must be between 8 and 255 characters.
- **firstName**: Required, maximum 100 characters.
- **lastName**: Required, maximum 100 characters.

## Success Response
**HTTP Status**: `201 CREATED`
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "customer@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["CUSTOMER"],
  "status": "ACTIVE",
  "createdAt": "2026-08-25T10:00:00.000"
}
```
*Note: The response deliberately excludes the `password` and `passwordHash` to prevent exposing sensitive information.*

## Error Responses
- **Validation Failure (`400 BAD REQUEST`)**: Missing or invalid fields.
- **Duplicate Email (`409 CONFLICT`)**: The email address is already registered.

### Error Structure
```json
{
  "timestamp": "2026-08-25T10:00:00.000",
  "status": 409,
  "error": "CONFLICT",
  "message": "Email is already in use",
  "path": "/api/v1/auth/register"
}
```

## Security Considerations
1. **Password Hashing**: The raw password is NEVER logged or stored. It is immediately hashed via `BCryptPasswordEncoder` before being persisted to the PostgreSQL database. 
2. **Default Role**: All successful registrations are automatically assigned the `CUSTOMER` role on the server side. It is impossible to pass privileged roles like `ADMIN` through the API payload.
3. **No JWT/Login Yet**: Note that login and JWT authentication functionalities are NOT implemented yet. The endpoint remains stateless for the purpose of this initial milestone.

## Registration Flow
```text
Client
  ↓
POST /api/v1/auth/register
  ↓
Controller
  ↓
Validation
  ↓
Registration Service
  ↓
Duplicate Email Check
  ↓
Password Hashing
  ↓
CUSTOMER Role assigned
  ↓
PostgreSQL
  ↓
201 CREATED
```
