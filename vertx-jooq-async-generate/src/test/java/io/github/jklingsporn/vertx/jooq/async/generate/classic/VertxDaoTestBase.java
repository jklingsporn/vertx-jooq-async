package io.github.jklingsporn.vertx.jooq.async.generate.classic;

import generated.classic.async.vertx.tables.daos.SomethingDao;
import generated.classic.async.vertx.tables.daos.SomethingcompositeDao;
import io.github.jklingsporn.vertx.jooq.async.classic.AsyncJooqSQLClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.MySQLClient;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by jensklingsporn on 07.11.16.
 */
public class VertxDaoTestBase {

    protected static SomethingDao dao;
    protected static SomethingcompositeDao compositeDao;

    @BeforeClass
    public static void beforeClass() throws SQLException {
//        TestTool.setupMysqlDB();
        Configuration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.MYSQL);
        configuration.set(DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/vertx", "vertx", ""));

        JsonObject config = new JsonObject().put("host", "127.0.0.1").put("username", "vertx").putNull("password").put("database","vertx");
        dao = new SomethingDao(configuration);
        Vertx vertx = Vertx.vertx();
        dao.setClient(AsyncJooqSQLClient.create(vertx, MySQLClient.createNonShared(vertx, config)));

        compositeDao = new SomethingcompositeDao(configuration);
        compositeDao.setClient(AsyncJooqSQLClient.create(vertx, MySQLClient.createNonShared(vertx, config)));
    }

    protected void await(CountDownLatch latch) throws InterruptedException {
        if(!latch.await(3, TimeUnit.SECONDS)){
            Assert.fail("latch not triggered");
        }
    }


    protected <T> Handler<AsyncResult<T>> countdownLatchHandler(final CountDownLatch latch){
        return h->{
            if(h.failed()){
                Assert.fail(h.cause().getMessage());
            }
            latch.countDown();
        };
    }

    protected <T> Handler<AsyncResult<T>> consumeOrFailHandler(Consumer<T> consumer){
        return h->{
            if(h.succeeded()){
                consumer.accept(h.result());
            }else{
                Assert.fail(h.cause().getMessage());
            }
        };

    }

}
