## example
```
//Setup your jOOQ configuration
Configuration configuration = ...

//setup Vertx
Vertx vertx = Vertx.vertx();

//setup the non-blocking SQLClient
AsyncJooqSQLClient client = AsyncJooqSQLClient.create(vertx,MySQLClient.createNonShared(vertx, new JsonObject().put("host", "127.0.0.1").put("username", "vertx").putNull("password").put("database","vertx")));

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
    CompletableFuture<Something> selectFuture = somethingDao.client().fetchOne(
			DSL.using(dao.configuration()).
			selectFrom(Tables.SOMETHING).
			orderBy(Tables.SOMETHING.SOMEID.desc()).
			limit(1),
		dao.jsonMapper());
    //check for completion
    selectFuture.whenComplete((something,ex)->{
        if(ex==null){
            System.out.println("Received: "+something);
        }else{
            System.err.println("Something failed badly: "+ex.getMessage());
        }
    });
});
```
# maven
```
<dependency>
  <groupId>io.github.jklingsporn</groupId>
  <artifactId>vertx-jooq-async-future</artifactId>
  <version>0.1</version>
</dependency>
```
# maven code generator configuration example for mysql
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
                  <artifactId>vertx-jooq-generate</artifactId>
                  <version>0.1</version>
              </dependency>
          </dependencies>

          <!-- Specify the plugin configuration.
               The configuration format is the same as for the standalone code generator -->
          <configuration>
              <!-- JDBC connection parameters (USED ONLY FOR CODE-GENERATION) -->
              <jdbc>
                  <driver>com.mysql.jdbc.Driver</driver>
                  <url>YOUR_JDBC_URL_HERE</url>
                  <user>YOUR_DB_USER_HERE</user>
                  <password>YOUR_DB_PASSWORD_HERE</password>
              </jdbc>

              <!-- Generator parameters -->
              <generator>
                  <name>io.github.jklingsporn.vertx.jooq.generate.future.async.FutureAsyncVertxGenerator</name>
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
                              <converter>io.github.jklingsporn.vertx.jooq.shared.JsonObjectConverter</converter>
                              <expression>someJsonObject</expression>
                              <types>.*</types>
                          </forcedType>
                          <!-- Convert varchar column with name 'someJsonArray' to a io.vertx.core.json.JsonArray-->
                          <forcedType>
                              <userType>io.vertx.core.json.JsonArray</userType>
                              <converter>io.github.jklingsporn.vertx.jooq.shared.JsonArrayConverter</converter>
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
                      <name>io.github.jklingsporn.vertx.jooq.generate.future.async.FutureAsyncGeneratorStrategy</name>
                  </strategy>
              </generator>

          </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```
# known issues

- The insertReturningPrimary-method in `io.github.jklingsporn.vertx.jooq.future.async.VertxDao` is not implemented yet.
- Not all data types that are known by the database can be actually loaded, see this list for [Postgres](https://github.com/mauricio/postgresql-async/tree/master/postgresql-async#what-is-missing) and this list for
 [MySQL](https://github.com/mauricio/postgresql-async/tree/master/mysql-async#gotchas).
- As a rule of thumb, all table fields must be valid JSON-Types, see `io.vertx.json.JSON#checkAndCopy`-method.