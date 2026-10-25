# CampusShare

A campus marketplace where students list, sell, rent and buy study resources such as notes, books and lab equipment. It is a Spring Boot REST API with JWT authentication, MySQL storage and a single-file web frontend served by the same app.

## Features

- **Members-only access.** Register and log in with email and password. Nothing is visible until you sign in.
- **JWT authentication.** Stateless tokens (24 hours by default), BCrypt-hashed passwords.
- **Listings.** Create, edit and delete resources with a photo. Only the owner can change or delete a listing.
- **Photos.** Uploaded as multipart files, validated by their real file signature (JPEG, PNG or WebP, max 2 MB), stored in MySQL and served from a dedicated endpoint.
- **Search and filters.** By name, category and price range, with sorting.
- **Buy or rent.** A demo checkout. The server looks up the price itself, takes one unit from stock atomically, and records the order. Rentals last 30 days and cost 30% of the price.
- **My purchases dashboard.** Total spent, items owned, active rentals and a full order history.
- **Frontend.** Responsive, supports dark mode, and has loading, empty and error states.

## Tech stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 17+ |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Security) |
| Database | MySQL 8 |
| Auth | JWT (jjwt 0.12.6), BCrypt |
| Mapping / boilerplate | ModelMapper, Lombok |
| Frontend | Plain HTML, CSS and JavaScript in one file, no build step |

## Project structure

```
src/main/java/EdTech/
├── EdTechApplication.java
├── AppConfig.java                  # ModelMapper bean
├── GlobalExceptionHandler.java     # maps exceptions to 400 / 403 / 404 / 409 / 413
├── Controller/
│   ├── AuthController.java         # /api/auth/register, /login
│   └── ProductController.java      # /api/products/**
├── Services/ProductServices.java   # business logic, image validation, checkout
├── Security/
│   ├── SecurityConfig.java         # route rules, stateless sessions
│   ├── JwtAuthFilter.java          # reads the Bearer token on each request
│   └── JwtService.java             # creates and verifies tokens
├── Entity/                         # UserEntity, ProductEntity, TransactionEntity, TransactionType
├── Repository/                     # UserRepository, ProductRepository, TransactionRepository
└── DTO/                            # ProductDTO, PaymentRequestDTO, PurchaseDTO, auth records
src/main/resources/
├── application.properties
└── static/index.html               # the frontend
```

## Getting started

### Prerequisites

- JDK 17 or newer
- MySQL 8 running locally
- Maven (or use the included `mvnw`)

### 1. Create the database

```sql
CREATE DATABASE IF NOT EXISTS shopcart_db;
```

Tables are created automatically on first run (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Set environment variables

The app reads its secrets from the environment. Nothing sensitive belongs in the repository.

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | Your MySQL password |
| `JWT_SECRET` | Base64-encoded signing key, at least 32 bytes |

Generate a secret:

```bash
# macOS / Linux / Git Bash
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | % { Get-Random -Max 256 }))
```

In IntelliJ, open Run → Edit Configurations → Modify options → Environment variables and enter
`DB_PASSWORD=your_password;JWT_SECRET=your_generated_secret`.

From a terminal:

```bash
# Linux / macOS
export DB_PASSWORD=your_password JWT_SECRET=your_generated_secret
# Windows PowerShell
$env:DB_PASSWORD="your_password"; $env:JWT_SECRET="your_generated_secret"
```

### 3. Run

```bash
./mvnw spring-boot:run
```

Open **http://localhost:8080** and create an account.

### Configuration reference (`application.properties`)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/shopcart_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update

app.jwt.secret=${JWT_SECRET}
app.jwt.expiry-ms=86400000

spring.servlet.multipart.max-file-size=2MB
spring.servlet.multipart.max-request-size=3MB
```

## API reference

Every endpoint except `/api/auth/**` requires the header `Authorization: Bearer <token>`.

| Method | Endpoint | Body | Description |
|---|---|---|---|
| POST | `/api/auth/register` | JSON `{name, email, password}` | Create an account. Returns `{token, userId, name}`. Password needs 8+ characters. |
| POST | `/api/auth/login` | JSON `{email, password}` | Returns `{token, userId, name}`. |
| GET | `/api/products/all` | none | All listings. |
| GET | `/api/products/search` | query: `name`, `category`, `minPrice`, `maxPrice` (all optional) | Filtered listings. |
| GET | `/api/products/{id}/image` | none | The photo, with the correct `Content-Type`. |
| POST | `/api/products/add` | form-data: `name`, `category`, `quantity`, `price`, `purpose`, `image` (file, optional) | Create a listing. The owner is taken from the token. |
| PUT | `/api/products/update/{id}` | form-data, any subset of the fields above | Owner only. A new `image` replaces the old one. |
| DELETE | `/api/products/delete/{id}` | none | Owner only. |
| POST | `/api/products/checkout` | JSON `{productId, transactionType, cardNumber, cvv}` | `transactionType` is `PURCHASE` or `RENT`. The server decides the price and takes one unit from stock. |
| GET | `/api/products/my-purchases` | none | The caller's orders, newest first. |

### Status codes

| Code | Meaning |
|---|---|
| 400 | Validation problem (bad input, wrong image type, invalid card, buying your own listing) |
| 401 | Missing, invalid or expired token, or wrong login |
| 403 | You don't own this listing |
| 404 | Listing or image not found |
| 409 | Item just sold out |
| 413 | Image larger than 2 MB |

## Testing with Postman

1. `POST /api/auth/register`, then `POST /api/auth/login`. Save the token in an environment variable by adding this to the login request under Scripts → Post-response:
   ```javascript
   const r = pm.response.json();
   if (r.token) pm.environment.set("token", r.token);
   ```
2. On other requests, set Authorization to Bearer Token with `{{token}}`.
3. For `/add` and `/update`, use Body → form-data, and set the `image` key's type to **File**.
4. Try the negative cases: no token (401), another user's token on update or delete (403), a `.pdf` as the image (400), checkout on a sold-out item (409).

## Design notes

- **Images are loaded with the token.** Because listings and photos need a login, the frontend fetches each image with the `Authorization` header and displays it as a blob. A plain `<img src>` can't send headers.
- **Stock is decremented atomically.** `UPDATE ... SET quantity = quantity - 1 WHERE id = ? AND quantity > 0` means two buyers can't both take the last item. The decrement and the order are in one transaction, so a failure rolls both back.
- **The server owns the price.** The amount sent by the client is ignored. Rentals cost 30% of the listed price (`RENT_RATE` in `ProductServices`).
- **Payments are a demo.** Any card number of 16 or more characters passes. No real payment is made.

## Known limitations and ideas

- Rented items are not returned to stock when the 30 days end. A scheduled job could add that.
- Photos live in MySQL. For many or large images, move them to disk or S3 and store only a key.
- The listing query loads photo bytes even though they are not returned. A separate image table would avoid that.
- No password reset, email verification or roles (admin).
- No refresh tokens. A new login is needed after 24 hours.
- Only one unit can be bought per order.
- Automated tests are minimal.

## Security notes

- Never commit `DB_PASSWORD` or `JWT_SECRET`. Use environment variables.
- Anyone who has your `JWT_SECRET` can forge login tokens, so keep it private and rotate it if it leaks.
- If a password was ever committed to git, changing it in MySQL is required, because deleting the file later does not remove it from history.
- Passwords are stored as BCrypt hashes, and the token's user ID, not a request field, decides who owns a listing or order.
