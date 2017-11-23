package io.github.jklingsporn.vertx.jooq.async.generate.classic;

import com.github.mauricio.async.db.mysql.exceptions.MySQLException;
import generated.classic.async.vertx.Tables;
import generated.classic.async.vertx.tables.pojos.Something;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.jooq.exception.TooManyRowsException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * Created by jensklingsporn on 02.11.16.
 */
public class VertxSomethingDaoTest extends VertxDaoTestBase {


    @Test
    public void asyncCRUDExecShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Something someNewObject = createSomething();
        dao.insertExecAsync(someNewObject, consumeOrFailHandler(x -> {
            Assert.assertEquals(1L,x.longValue());
            dao.fetchOneAsync(
                    Tables.SOMETHING.SOMEHUGENUMBER.eq(someNewObject.getSomehugenumber()),
                    consumeOrFailHandler(p -> {
                        Assert.assertNotNull(p);
                        dao.updateExecAsync(p.setSomestring("modified"),
                                consumeOrFailHandler(updatedRows -> {
                                    Assert.assertEquals(1l, updatedRows.longValue());
                                    dao.deleteExecAsync(p.getSomeid(), deletedRows -> {
                                        if (deletedRows.failed()) {
                                            Assert.fail(deletedRows.cause().getMessage());
                                        } else {
                                            Assert.assertEquals(1l, deletedRows.result().longValue());
                                        }
                                        latch.countDown();
                                    });
                                })
                        );

                    }));
        }));
        await(latch);
    }

    @Test
    public void insertExecShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Something someNewObject = createSomething();
        dao.insertExecAsync(someNewObject, consumeOrFailHandler(insertedRows -> {
            Assert.assertEquals(1l, insertedRows.longValue());
            dao.fetchOneAsync(
                    Tables.SOMETHING.SOMEHUGENUMBER.eq(someNewObject.getSomehugenumber()),
                    consumeOrFailHandler(something -> dao.deleteExecAsync(something.getSomeid(), deletedRows -> {
                        if (deletedRows.failed()) {
                            Assert.fail(deletedRows.cause().getMessage());
                        } else {
                            Assert.assertEquals(1l, deletedRows.result().longValue());
                        }
                        latch.countDown();
                    }))
            );
        }));
        await(latch);
    }

    @Test
    public void insertReturningShouldFailOnDuplicateKey() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Something something = createSomething();
        dao.insertReturningPrimaryAsync(something,consumeOrFailHandler(c->{
            dao.insertReturningPrimaryAsync(something.setSomeid(c.intValue()), h -> {
                Assert.assertTrue(h.failed());
                Assert.assertEquals(MySQLException.class,h.cause().getClass());
                Assert.assertTrue(h.cause().getMessage().contains("1062")); //duplicate entry
                latch.countDown();
            });
        }));
        await(latch);
    }

    @Test
    public void asyncCRUDConditionShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        dao.insertReturningPrimaryAsync(createSomething(),consumeOrFailHandler(pk->{
            dao.fetchOneAsync(Tables.SOMETHING.SOMEID.eq(pk.intValue()),consumeOrFailHandler(val->{
                Assert.assertNotNull(val);
                dao.deleteExecAsync(Tables.SOMETHING.SOMEID.eq(pk.intValue()), countdownLatchHandler(latch));
            }));
        }));
        await(latch);
    }

    @Test
    public void fetchOneByConditionWithMultipleMatchesShouldFail() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Future<Integer> insertFuture1 = Future.future();
        Future<Integer> insertFuture2 = Future.future();

        Something someNewObject = createSomething();
        dao.insertReturningPrimaryAsync(someNewObject,insertFuture1);
        dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(someNewObject.getSomehugenumber()),insertFuture2);
        CompositeFuture.all(insertFuture1,insertFuture2).
                setHandler(consumeOrFailHandler(v->{
                    dao.fetchOneAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(someNewObject.getSomehugenumber()),h->{
                        Assert.assertNotNull(h.cause());
                        //cursor fetched more than one row
                        Assert.assertEquals(TooManyRowsException.class, h.cause().getClass());
                        dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(someNewObject.getSomehugenumber()),countdownLatchHandler(latch));
                    });
                }));
        await(latch);
    }

    @Test
    public void fetchByConditionWithMultipleMatchesShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Future<Integer> insertFuture1 = Future.future();
        Future<Integer> insertFuture2 = Future.future();

        Something someNewObject = createSomething();
        dao.insertReturningPrimaryAsync(someNewObject,insertFuture1);
        dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(someNewObject.getSomehugenumber()),insertFuture2);
        CompositeFuture.all(insertFuture1, insertFuture2).
                setHandler(consumeOrFailHandler(v -> {
                    dao.fetchAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(someNewObject.getSomehugenumber()), h -> {
                        Assert.assertNotNull(h.result());
                        //cursor fetched more than one row
                        Assert.assertEquals(2, h.result().size());
                        dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(someNewObject.getSomehugenumber()), countdownLatchHandler(latch));
                    });
                }));
        await(latch);
    }


}
