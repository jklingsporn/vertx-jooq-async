package io.github.jklingsporn.vertx.jooq.async.generate.rx;

import generated.rx.async.vertx.tables.daos.SomethingDao;
import generated.rx.async.vertx.tables.daos.SomethingcompositeDao;
import io.github.jklingsporn.vertx.jooq.async.generate.TestTool;
import io.github.jklingsporn.vertx.jooq.async.rx.AsyncJooqSQLClient;
import io.reactivex.CompletableObserver;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.asyncsql.MySQLClient;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 * @author <a href="https://jensonjava.wordpress.com">Jens Klingsporn</a>
 */
public class RXVertxDaoTestBase {

    protected static SomethingDao dao;
    protected static SomethingcompositeDao compositeDao;

    @BeforeClass
    public static void beforeClass() throws SQLException {
        TestTool.setupDB();
        Configuration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.MYSQL);

        JsonObject config = new JsonObject().put("host", "127.0.0.1").put("username", "vertx").putNull("password").put("database","vertx");
        dao = new SomethingDao(configuration);
        Vertx vertx = Vertx.vertx();
        dao.setClient(AsyncJooqSQLClient.create(vertx, MySQLClient.createNonShared(vertx, config)));

        compositeDao = new SomethingcompositeDao(configuration);
        compositeDao.setClient(AsyncJooqSQLClient.create(vertx,MySQLClient.createNonShared(vertx, config)));
    }

    protected void await(CountDownLatch latch) throws InterruptedException {
        if(!latch.await(3, TimeUnit.SECONDS)){
            Assert.fail("latch not triggered");
        }
    }

    protected <T> Observer<T> failOrCountDownPlainObserver(CountDownLatch latch) {
        return new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(T t) {

            }

            @Override
            public void onError(Throwable e) {
                fail(e);
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

        };
    }

    protected <T> SingleObserver<T> failOrCountDownSingleObserver(CountDownLatch latch) {
        return new SingleObserver<T>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(T t) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                fail(e);
            }

        };
    }

    protected CompletableObserver failOrCountDownCompletableObserver(CountDownLatch latch) {
        return new CompletableObserver() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                fail(e);
            }

        };
    }

    protected static void fail(Throwable t) {
        throw new RuntimeException(t);
    }

}
