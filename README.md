# SENG302 LENSFOLIO (Skillhub) PROJECT

Basic project template using `gradle`, `Spring Boot`, `Thymeleaf` and `Gitlab CI`.

## Basic Project Structure

- `systemd/` - This folder includes the systemd service files that will be present on the VM, these can be safely ignored.
- `runner/` - These are the bash scripts used by the VM to execute the application.
- `shared/` - Contains (initially) some `.proto` contracts that are used to generate Java classes and stubs that the following modules will import and build on.
- `identityprovider/` - The Identity Provider (IdP) is built with Spring Boot, and uses gRPC to communicate with other modules. The IdP is where we will store user information (such as usernames, passwords, names, ids, etc.).
- `portfolio/` - The Portfolio module is another fully fledged Java application running Spring Boot. It also uses gRPC to communicate with other modules.


### 1 - Create a .env file for IDP
Create a file named '.env' under 'identityprovider->src->main->resources'

Persistence options: create, create-drop, update

Fill it with these details:
```
DATABASE=<url>;NON_KEYWORDS=USER
USER_NAME=<username>
PASSWORD=<password>
DRIVER=<driver>
DIALECT=<hibernate_dialect>
PERSISTENCE=<persistence>

```
For an H2 configuration (recommended for local development) it would look like this:
```
DATABASE=jdbc:h2:file:./dev-idp;NON_KEYWORDS=USER
USER_NAME=sa
PASSWORD=
DRIVER=org.h2.Driver
DIALECT=org.hibernate.dialect.H2Dialect
PERSISTENCE=create-drop
```

### 2 - Create a .env file for Portfolio
Create a file named '.env' under 'identityprovider->src->main->resources'

Persistence options: create, create-drop, update

Fill it with these details:
```
DATABASE=<url>
USER_NAME=<username>
PASSWORD=<password>
DRIVER=<driver>
DIALECT=<hibernate_dialect>
PERSISTENCE=<persistence>

```
For an H2 configuration (recommended for local development) it would look like this:
```
DATABASE=jdbc:h2:file:./dev-portfolio
USER_NAME=sa
PASSWORD=
DRIVER=org.h2.Driver
DIALECT=org.hibernate.dialect.H2Dialect
PERSISTENCE=create-drop
```

## Building and running the project with Gradle

## How to run

### 1 - Generating Java dependencies from the `shared` class library
The `shared` class library is a dependency of the two main applications, so before you will be able to build either `portfolio` or `identityprovider`, you must make sure the shared library files are available via the local maven repository.

Assuming we start in the project root, the steps are as follows...

On Linux: 
```
cd shared
./gradlew clean
./gradlew publishToMavenLocal
```

On Windows:
```
cd shared
gradlew clean
gradlew publishToMavenLocal
```

*Note: The `gradle clean` step is usually only necessary if there have been changes since the last publishToMavenLocal.*

### 2 - Identity Provider (IdP) Module
Assuming we are starting in the root directory...

On Linux:
```
cd identityprovider
./gradlew bootRun
```

On Windows:
```
cd identityprovider
gradlew bootRun
```

By default, the IdP will run on local port 9002 (`http://localhost:9002`).

### 3 - Portfolio Module
Now that the IdP is up and running, we will be able to use the Portfolio module (note: it is entirely possible to start it up without the IdP running, you just won't be able to get very far).

From the root directory (and likely in a second terminal tab / window)...
On Linux:
```
cd portfolio
./gradlew bootRun
```

On Windows:
```
cd portfolio
gradlew bootRun
```

By default, the Portfolio will run on local port 9000 (`http://localhost:9000`)


### 4 - Connect to the Portfolio UI through your web browser
Everything should now be up and running, so you can load up your preferred web browser and connect to the Portfolio UI by going to http://localhost:9000 - though you will probably want to start at http://localhost:9000/login until you set up an automatic redirect, or a home page of sorts.


## How to run Virtual Machine 
To run the VM, click on https://csse-s302g2.canterbury.ac.nz/prod/portfolio/login for testing purpose

### Default Users (use for testing)
| Roles        | Username | Password  | UserId |
|--------------|----------|-----------|--------|
| Student      | student200  | wkhMNHn6HROm8Lx19G3T | 4 |
| Teacher      |  teacher200 | j8vgxsVUddfHk4g0skSc | 5 |
| Course Admin |   admin200  | OEZZsr64wvYF7kFeV3dC | 6 |


## Contributors

1. SENG302 teaching team
2. Team 200 Developers:
+ Matthew Garrett 
+ Euan Morgan
+ Kvie Nguyen
+ Moses Wescombe
+ Dianame Altalim
+ Sami Elmadani
+ James Hazlehurst

## References

- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring JPA docs](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Thymeleaf Docs](https://www.thymeleaf.org/documentation.html)
- [Team Wiki](https://eng-git.canterbury.ac.nz/seng302-2022/team-200/-/wikis/home)
- [User Instruction](https://docs.google.com/document/d/19RMYTqiCmVQGrUUhNkaTFz5JE3CNyLtR94dgjnmGbPg/edit#)
