package io.github.jklingsporn.vertx.jooq.async.generate.classic;

import generated.classic.async.vertx.Tables;
import generated.classic.async.vertx.tables.pojos.Something;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jooq.impl.DSL;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Created by jensklingsporn on 02.11.16.
 */
public class VertxSomethingDaoTest extends VertxDaoTestBase {


    @Test
    public void asyncCRUDExecShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        dao.insertExecAsync(createSomething(), consumeOrFailHandler(x -> {
            Assert.assertEquals(1L,x.longValue());
            dao.client().fetchOne(
                    DSL.using(dao.configuration()).selectFrom(Tables.SOMETHING).orderBy(Tables.SOMETHING.SOMEID.desc()).limit(1),
                    dao.jsonMapper(),
                    consumeOrFailHandler(p->{
                        Assert.assertNotNull(p);
                        dao.fetchOneBySomeidAsync(p.getSomeid(), consumeOrFailHandler(something -> {
                            dao.updateExecAsync(createSomething().setSomeid(p.getSomeid()),
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
        }));
        await(latch);
    }

//    @Test
//    public void insertExecShouldSucceed() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//        dao.insertExecAsync(createSomething(), consumeOrFailHandler(insertedRows -> {
//            Assert.assertEquals(1l, insertedRows.longValue());
//            dao.executeAsync(dslContext -> dslContext.
//                            selectFrom(Tables.SOMETHING).
//                            orderBy(Tables.SOMETHING.SOMEID.desc()).
//                            limit(1).
//                            fetchOne(),
//                    consumeOrFailHandler(something -> {
//                    dao.deleteExecAsync(something.getSomeid(), deletedRows -> {
//                        if (deletedRows.failed()) {
//                            Assert.fail(deletedRows.cause().getMessage());
//                        } else {
//                            Assert.assertEquals(1l, deletedRows.result().longValue());
//                        }
//                        latch.countDown();
//                    });
//                })
//            );
//        }));
//        await(latch);
//    }
//
//    @Test
//    public void insertReturningShouldFailOnDuplicateKey() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//        Something something = createSomething();
//        dao.insertReturningPrimaryAsync(something,consumeOrFailHandler(c->{
//            dao.insertReturningPrimaryAsync(something.setSomeid(c), h -> {
//                Assert.assertTrue(h.failed());
//                Assert.assertEquals(DataAccessException.class,h.cause().getClass());
//                latch.countDown();
//            });
//        }));
//        await(latch);
//    }
//
//    @Test
//    public void asyncCRUDConditionShouldSucceed() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//        dao.insertReturningPrimaryAsync(createSomething(),consumeOrFailHandler(pk->{
//            dao.fetchOneAsync(Tables.SOMETHING.SOMEID.eq(pk),consumeOrFailHandler(val->{
//                Assert.assertNotNull(val);
//                dao.deleteExecAsync(Tables.SOMETHING.SOMEID.eq(pk),countdownLatchHandler(latch));
//            }));
//        }));
//        await(latch);
//    }
//
//    @Test
//    public void fetchOneByConditionWithMultipleMatchesShouldFail() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//        Future<Integer> insertFuture1 = Future.future();
//        Future<Integer> insertFuture2 = Future.future();
//        dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L),insertFuture1);
//        dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L),insertFuture2);
//        CompositeFuture.all(insertFuture1,insertFuture2).
//                setHandler(consumeOrFailHandler(v->{
//                    dao.fetchOneAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L),h->{
//                        Assert.assertNotNull(h.cause());
//                        //cursor fetched more than one row
//                        Assert.assertEquals(TooManyRowsException.class, h.cause().getClass());
//                        dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L),countdownLatchHandler(latch));
//                    });
//                }));
//        await(latch);
//    }
//
//    @Test
//    public void fetchByConditionWithMultipleMatchesShouldSucceed() throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(1);
//        Future<Integer> insertFuture1 = Future.future();
//        Future<Integer> insertFuture2 = Future.future();
//        dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L),insertFuture1);
//        dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L),insertFuture2);
//        CompositeFuture.all(insertFuture1, insertFuture2).
//                setHandler(consumeOrFailHandler(v -> {
//                    dao.fetchAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L), h -> {
//                        Assert.assertNotNull(h.result());
//                        //cursor fetched more than one row
//                        Assert.assertEquals(2, h.result().size());
//                        dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L), countdownLatchHandler(latch));
//                    });
//                }));
//        await(latch);
//    }

    private Something createSomething(){
        Random random = new Random();
        Something something = new Something();
        something.setSomedouble(random.nextDouble());
        something.setSomehugenumber(random.nextLong());
        something.setSomejsonarray(new JsonArray().add(1).add(2).add(3));
        something.setSomejsonobject(new JsonObject().put("key", "value"));
        something.setSomesmallnumber((short) random.nextInt(Short.MAX_VALUE));
        return something;
    }


}
