# TaskFlow

**Collaborative Task & Project Management System**  
Jakarta EE 10 web application with real-time updates, built using JSF, PrimeFaces, JPA, and JMS.

---

## Technologies

- **Jakarta EE 10**
- **JSF + PrimeFaces**
- **EclipseLink JPA**
- **JMS + Message-Driven Beans** (for async activity logging)
- **Payara Server** (Full Profile)
- **MariaDB**

---

## Quick Start for Lecturer / Reviewer

This project is designed to be as simple as possible to run:

### 1. Create the Database
```sql
CREATE DATABASE taskflow_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### 2. Deploy the WAR
Deploy the `taskflow.war` file on **Payara Server**.

```bash
asadmin deploy taskflow.war
```

**That's it.**

All required resources (JDBC DataSource + JMS Queue) are automatically created during deployment using the `glassfish-resources.xml` file included in the project.  
No manual configuration in the Admin Console is required.

---

## Resources Configuration

The project uses **Application-Scoped Resources** defined in:

- `src/main/webapp/WEB-INF/glassfish-resources.xml`

**Created automatically:**
- **JDBC DataSource**: `java:app/jdbc/TaskFlowDS` (MariaDB)
- **JMS Queue**: `java:app/jms/TaskEventsQueue`

This approach ensures the application is self-contained and does not depend on manually created server-level resources.

---

## Database

- **Database**: `taskflow_db`
- **Connection details** are defined inside `glassfish-resources.xml`
- **Schema Generation**: Disabled by default (`jakarta.persistence.schema-generation.database.action = none`)

**For first run**, it is recommended to temporarily change the value in `persistence.xml` to `drop-and-create` so JPA will generate the tables automatically.

Alternatively, you can run your own initialization script.

---

## Default Data

On first startup, the application automatically seeds the database using a `@Startup` bean (`DataInitializer`).

**Default users** (password for all: `123456`):

| Email                    | Role     |
|--------------------------|----------|
| tomer@openu.ac.il        | User     |
| chen@openu.ac.il         | User     |
| test@test.com            | User     |

A default project, board, and sample tasks are also created.

---

## Running Locally (Development)

### Payara Server (Recommended for consistency)
1. Create the database (see above)
2. Build the project:
   ```bash
   mvn clean install
   ```
3. Deploy the WAR to your Payara Server domain. --->

## Deployment

### Using asadmin (Recommended)

```bash
# Deploy the application
asadmin deploy taskflow.war

**To redeploy after making changes:**
asadmin redeploy --name taskflow taskflow.war

---

## Known Issues & Design Decisions

- **JPA Level 2 Cache**: Disabled globally (`<shared-cache-mode>NONE</shared-cache-mode>`) to prevent stale data issues with EclipseLink.
- **JMS Async Logging**: Activity logs are written asynchronously via `TaskEventMDB`. Under very high load, log ordering may slightly deviate from execution order.
- **Hot Reload**: Changes to Java classes require application restart. XHTML/JS/CSS changes are reflected without restart.

---

## Project Structure (Key Files)

```
src/main/webapp/WEB-INF/
├── glassfish-resources.xml     # Auto-creates JDBC + JMS resources
├── web.xml
└── faces-config.xml

src/main/resources/META-INF/
└── persistence.xml

src/main/java/.../util/
└── DataInitializer.java        # Seeds default data on startup
```

---

## Author

**Tomer Shein - 211561980**  
Open University of Israel — Advanced Java Workshop

---