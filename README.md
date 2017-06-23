# vertx-jooq-async
The _real_ async version of [vertx-jooq](https://github.com/jklingsporn/vertx-jooq-async): a [jOOQ](http://www.jooq.org/)-CodeGenerator to create [vertx](http://vertx.io/)-ified DAOs and POJOs!
This time with a [_real_ asynchronous driver](https://github.com/mauricio/postgresql-async)
that is available for Vertx and therefore perfectly qualifies for another API. That's right - no JDBC.

## differences to vertx-jooq
This project uses jOOQ for code-generation and to render queries. The query execution is done by the `AsyncJooqSQLClient`
which wraps a `io.vertx.ext.asyncsql.AsyncSQLClient`.

## example
```
//Setup your jOOQ configuration
Configuration configuration = new DefaultConfiguration();
configuration.set(SQLDialect.MYSQL); //or SQLDialect.POSTGRES
//no other DB-Configuration necessary because we only use jOOQ to render our statements - not for excecution

//setup Vertx
Vertx vertx = Vertx.vertx();
//setup the client
JsonObject config = new JsonObject().put("host", "127.0.0.1").put("username", "vertx").putNull("password").put("database","vertx");
AsyncJooqSQLClient client = AsyncJooqSQLClient.create(vertx,MySQLClient.createNonShared(vertx, config))

//instantiate a DAO (which is generated for you)
SomethingDao somethingDao = new SomethingDao(configuration);
somethingDao.setVertx(vertx);
somethingDao.setClient(client);

//fetch something with ID 123...
CompletableFuture<Void> sendFuture =
    somethingDao.findByIdAsync(123).
    thenAccept(something->
        vertx.eventBus().send("sendSomething",something.toJson())
    );

//maybe consume it in another verticle
vertx.eventBus().<JsonObject>consumer("sendSomething", jsonEvent->{
    JsonObject message = jsonEvent.body();
    //Convert it back into a POJO...
    Something something = new Something(message);
    //... change some values
    something.setSomeregularnumber(456);
    //... and update it into the DB
    CompletableFuture<Void> updatedFuture = somethingDao.updateAsync(something);

    //or do you prefer writing your own typesafe SQL?
    CompletableFuture<Integer> updatedCustomFuture = somethingDao.executeAsync(dslContext ->
            dslContext.update(Tables.SOMETHING).set(Tables.SOMETHING.SOMEREGULARNUMBER,456).where(Tables.SOMETHING.SOMEID.eq(something.getSomeid())).execute()
    );
    //check for completion
    updatedCustomFuture.whenComplete((rows,ex)->{
        if(ex==null){
            System.out.println("Rows updated: "+rows);
        }else{
            System.err.println("Something failed badly: "+ex.getMessage());
        }
    });
});
```

# callback? future? rx?
Again, this library comes in different flavors:
- the classic callback-handler style.
- a API that returns a [vertx-ified implementation](https://github.com/cescoffier/vertx-completable-future)
of `java.util.concurrent.CompletableFuture` for all async DAO operations and thus makes chaining your async operations easier.
It has some limitations which you need to be aware about (see [known issues](https://github.com/jklingsporn/vertx-jooq#known-issues)).
- a RX Java based API

Depending on your needs, you have to include one of the following dependencies into your pom:
- [`vertx-jooq-async-classic`](https://github.com/jklingsporn/vertx-jooq/tree/master/vertx-jooq-classic) is the module containing the callback handler API.
- [`vertx-jooq-async-future`](https://github.com/jklingsporn/vertx-jooq/tree/master/vertx-jooq-future) is the module containing the `CompletableFuture` based API.
- [`vertx-jooq-async-rx`](https://github.com/jklingsporn/vertx-jooq/tree/master/vertx-jooq-rx) is the module containing the RX Java based API


# maven
```
<dependency>
  <groupId>io.github.jklingsporn</groupId>
  <artifactId>vertx-jooq-async-future</artifactId>
  <version>0.1</version>
</dependency>
```
# maven code generator configuration example for mysql
The following code-snippet can be copy-pasted into your pom.xml to generate code from your MySQL database schema.

**Watch out for placeholders beginning with 'YOUR_xyz' though! E.g. you have to define credentials for DB access and specify the target directory where jOOQ
should put the generated code into, otherwise it won't run!**

After you replaced all placeholders with valid values, you should be able to run `mvn generate-sources` which creates all POJOs and DAOs into the target directory you specified.

If you are new to jOOQ, I recommend to read the awesome [jOOQ documentation](http://www.jooq.org/doc/latest/manual/), especially the chapter about
[code generation](http://www.jooq.org/doc/latest/manual/code-generation/).

```
<project>
...your project configuration here...

  <dependencies>
    ...your other dependencies...
    <dependency>
      <groupId>org.jooq</groupId>
      <artifactId>jooq</artifactId>
      <version>3.9.2</version>
    </dependency>
    <dependency>
      <groupId>io.github.jklingsporn</groupId>
      <artifactId>vertx-jooq-async-future</artifactId>
      <version>0.1</version>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
          <!-- Specify the maven code generator plugin -->
          <groupId>org.jooq</groupId>
          <artifactId>jooq-codegen-maven</artifactId>
          <version>3.9.2</version>

          <!-- The plugin should hook into the generate goal -->
          <executions>
              <execution>
                  <goals>
                      <goal>generate</goal>
                  </goals>
              </execution>
          </executions>

          <dependencies>
              <dependency>
                  <groupId>mysql</groupId>
                  <artifactId>mysql-connector-java</artifactId>
                  <version>5.1.37</version>
              </dependency>
              <dependency>
                  <groupId>io.github.jklingsporn</groupId>
                  <artifactId>vertx-jooq-async-generate</artifactId>
                  <version>0.1</version>
              </dependency>
          </dependencies>

          <!-- Specify the plugin configuration.
               The configuration format is the same as for the standalone code generator -->
          <configuration>
              <!-- JDBC connection parameters -->
              <jdbc>
                  <driver>com.mysql.jdbc.Driver</driver>
                  <url>YOUR_JDBC_URL_HERE</url>
                  <user>YOUR_DB_USER_HERE</user>
                  <password>YOUR_DB_PASSWORD_HERE</password>
              </jdbc>

              <!-- Generator parameters -->
              <generator>
                  <name>io.github.jklingsporn.vertx.jooq.async.generate.future.FutureAsyncGeneratorStrategy</name>
                  <database>
                      <name>org.jooq.util.mysql.MySQLDatabase</name>
                      <includes>.*</includes>
                      <inputSchema>YOUR_INPUT_SCHEMA</inputSchema>
                      <outputSchema>YOUR_OUTPUT_SCHEMA</outputSchema>
                      <unsignedTypes>false</unsignedTypes>
                      <forcedTypes>
                          <!-- Convert tinyint to boolean -->
                          <forcedType>
                              <name>BOOLEAN</name>
                              <types>(?i:TINYINT)</types>
                          </forcedType>
                          <!-- Convert varchar column with name 'someJsonObject' to a io.vertx.core.json.JsonObject-->
                          <forcedType>
                              <userType>io.vertx.core.json.JsonObject</userType>
                              <converter>io.github.jklingsporn.vertx.jooq.async.shared.JsonObjectConverter</converter>
                              <expression>someJsonObject</expression>
                              <types>.*</types>
                          </forcedType>
                          <!-- Convert varchar column with name 'someJsonArray' to a io.vertx.core.json.JsonArray-->
                          <forcedType>
                              <userType>io.vertx.core.json.JsonArray</userType>
                              <converter>Jio.github.jklingsporn.vertx.jooq.async.shared.sonArrayConverter</converter>
                              <expression>someJsonArray</expression>
                              <types>.*</types>
                          </forcedType>
                      </forcedTypes>
                  </database>
                  <target>
                      <!-- This is where jOOQ will put your files -->
                      <packageName>YOUR_TARGET_PACKAGE_HERE</packageName>
                      <directory>YOUR_TARGET_DIRECTORY_HERE</directory>
                  </target>
                  <generate>
                      <interfaces>true</interfaces>
                      <daos>true</daos>
                      <fluentSetters>true</fluentSetters>
                  </generate>


                  <strategy>
                      <name>io.github.jklingsporn.vertx.jooq.async.generate.future.FutureAsyncGeneratorStrategy</name>
                  </strategy>
              </generator>

          </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```
# programmatic configuration of the code generator
See the [TestTool](https://github.com/jklingsporn/vertx-jooq-async/blob/master/vertx-jooq-async-generate/src/test/java/io/github/jklingsporn/vertx/jooq/async/generate/TestTool.java)
of how to setup the generator programmatically.

# known issues
- `insertReturningPrimary`-method is not implemented yet.
- only basic CRUD tested.

