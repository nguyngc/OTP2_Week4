# Shopping Cart Localization App

JavaFX shopping cart application with MariaDB-backed localization and cart record storage.

## Requirements

- Java 21
- Maven 3.9+
- MariaDB 11+ for local development
- Docker and Docker Compose for containerized setup

## Environment Variables

The application reads database settings from a `.env` file in the project root.

Example:

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=shopping_cart_localization
DB_USERNAME=root
DB_PASSWORD=password
```

You can create it from the sample file:

```bash
cp .env.example .env
```

Then update the values to match your MariaDB setup.

## Local Setup

1. Start MariaDB on your machine.
2. Create the database schema and seed data:

```bash
mysql -u root -p < init.sql
```

3. Verify your `.env` file points to the local database.
4. Start the application:

```bash
mvn javafx:run
```

## Build and Test

Compile the project:

```bash
mvn clean compile
```

Run tests:

```bash
mvn test
```

Build the runnable JAR:

```bash
mvn clean package
```

Run the packaged application:

```bash
java -jar target/app.ShoppingCartApp.jar
```

## Docker Setup

Build the application image:

```bash
docker build -t shopping-cart-app .
```

Start the application container:

```bash
docker run --rm -it \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=shopping_cart_localization \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=password \
  -e DISPLAY=host.docker.internal:0 \
  shopping-cart-app:latest
```

## Notes

- Make sure Docker Desktop or your Docker engine is installed and running before building or starting the app container.
- For local runs, use `DB_HOST=localhost`.
- The MariaDB container initializes the schema automatically from [init.sql](init.sql).
- Because the application uses JavaFX, Docker also needs access to a GUI display server from your host machine.
- On macOS, install and start XQuartz, then allow network client connections before running the container.
- On Windows, run an X server such as VcXsrv or Xming before starting the container.
