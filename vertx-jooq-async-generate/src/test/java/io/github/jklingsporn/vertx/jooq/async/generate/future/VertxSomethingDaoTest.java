package io.github.jklingsporn.vertx.jooq.async.generate.future;

import com.github.mauricio.async.db.mysql.exceptions.MySQLException;
import generated.future.async.vertx.Tables;
import generated.future.async.vertx.tables.pojos.Something;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jooq.exception.TooManyRowsException;
import org.jooq.impl.DSL;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * Created by jensklingsporn on 13.06.17.
 */
public class VertxSomethingDaoTest extends VertxDaoTestBase {


    @Test
    public void asyncCRUDExecShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Something somethingCreated = createSomething();
        dao.insertExecAsync(somethingCreated).
                thenAccept(created -> Assert.assertEquals(1L, created.longValue())).
                thenCompose(v -> dao.fetchOneAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(somethingCreated.getSomehugenumber()))).
                thenCompose(fetch -> dao.updateExecAsync(somethingCreated.setSomestring("modified").setSomeid(fetch.getSomeid()))).
                thenAccept(updated -> Assert.assertEquals(1L, updated.longValue())).
                thenCompose(v -> dao.deleteExecAsync(somethingCreated.getSomeid())).
                thenAccept(deleted -> Assert.assertEquals(1L, deleted.longValue())).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }

    @Test
    public void insertExecShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Something somethingCreated = createSomething();
        dao.insertExecAsync(somethingCreated).
                thenAccept(insertedRows -> Assert.assertEquals(1L,insertedRows.longValue())).
                thenCompose(v-> dao.fetchOneAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(somethingCreated.getSomehugenumber()))).
                thenCompose(id -> {
                    Assert.assertNotNull(id);
                    return dao.deleteExecAsync(id.getSomeid());

                }).
                thenAccept(deletedRows -> Assert.assertEquals(1L,deletedRows.longValue())).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }


    @Test
    public void insertReturningShouldFailOnDuplicateKey() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Something something = createSomething();
        dao.insertReturningPrimaryAsync(something).
                thenCompose(id -> dao.insertReturningPrimaryAsync(something.setSomeid(id))).
                exceptionally(x -> {
                    Assert.assertNotNull(x);
                    Assert.assertEquals(MySQLException.class, x.getCause().getClass());
                    return null;
                }).
                thenCompose(v -> dao.deleteExecAsync(DSL.trueCondition())).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }

    @Test
    public void asyncCRUDConditionShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Integer> insertFuture = dao.insertReturningPrimaryAsync(createSomething());
        insertFuture.
                thenCompose(v -> dao.fetchOneAsync(Tables.SOMETHING.SOMEID,insertFuture.join())).
                thenAccept(Assert::assertNotNull).
                thenCompose(v -> dao.deleteExecAsync(Tables.SOMETHING.SOMEID.eq(insertFuture.join()))).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }

    @Test
    public void fetchOneByConditionWithMultipleMatchesShouldFail() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Integer> insertFuture1 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));
        CompletableFuture<Integer> insertFuture2 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));
        CompletableFuture.allOf(insertFuture1, insertFuture2).
                thenCompose(v->dao.fetchOneAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L))).
                exceptionally((x) -> {
                    Assert.assertNotNull(x);
                    //cursor fetched more than one row
                    Assert.assertEquals(TooManyRowsException.class, x.getCause().getClass());
                    return null;}).
                thenCompose(v -> dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L))).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }

    @Test
    public void fetchByConditionWithMultipleMatchesShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Integer> insertFuture1 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));
        CompletableFuture<Integer> insertFuture2 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));
        CompletableFuture.allOf(insertFuture1, insertFuture2).
                thenCompose(v->dao.fetchAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L))).
                thenAccept(values->Assert.assertEquals(2,values.size())).
                thenCompose(v -> dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L))).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }

    @Test
    public void existingValueShouldExist() throws InterruptedException {
        Something something = createSomething();
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Integer> primaryAsync = dao.insertReturningPrimaryAsync(something);
        primaryAsync
                .thenAccept(pk -> {
                    something.setSomeid(pk);
                    CompletableFuture<Boolean> existsByIdFuture = dao.existsByIdAsync(pk);
                    existsByIdFuture
                            .thenAccept(Assert::assertTrue)
                            .whenComplete(failOrCountDown(latch));
                })
                .whenComplete((r, x) -> dao.deleteExecAsync(primaryAsync.join()))
                .whenComplete(failOnException());
        await(latch);
    }

    @Test
    public void nonExistingValueShouldNotExist() throws InterruptedException {
        CompletableFuture<Boolean> existsByIdFuture = dao.existsByIdAsync(-1);
        CountDownLatch latch = new CountDownLatch(1);
        existsByIdFuture
                .thenAccept(Assert::assertFalse)
                .whenComplete(failOrCountDown(latch));
        await(latch);
    }

    @Test
    public void countShouldReturnNumberOfEntries() throws InterruptedException{
        CompletableFuture<Long> countZeroFuture = dao.countAsync();
        CountDownLatch latch = new CountDownLatch(1);
        countZeroFuture.thenAccept(zero->
                Assert.assertEquals(0L,zero.longValue())).
                thenCompose(v->dao.insertReturningPrimaryAsync(createSomething())).
                thenCompose(pk->dao.countAsync()).
                thenAccept(one -> Assert.assertEquals(1L, one.longValue())).
                thenCompose(v->dao.deleteExecAsync(DSL.trueCondition())).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }

    @Test
    public void fetchOptionalShouldNotBePresentOnNoResult() throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(1);
        dao.fetchOptionalAsync(Tables.SOMETHING.SOMEID,-1).
                thenAccept(opt -> Assert.assertFalse(opt.isPresent())).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }

    @Test
    public void fetchOptionalShouldReturnResultWhenPresent() throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(1);
        dao.insertReturningPrimaryAsync(createSomething()).thenCompose(pk ->
                dao.fetchOptionalAsync(Tables.SOMETHING.SOMEID, pk).thenAccept(opt -> {
                    Assert.assertTrue(opt.isPresent());
                    Assert.assertEquals(pk.longValue(), opt.get().getSomeid().longValue());
                    dao.deleteExecAsync(pk).whenComplete(failOrCountDown(latch));
                })).whenComplete(failOnException());
        await(latch);
    }

    @Test
    public void fetchAllShouldReturnValues() throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Integer> insertFuture1 = dao.insertReturningPrimaryAsync(createSomething());
        CompletableFuture<Integer> insertFuture2 = dao.insertReturningPrimaryAsync(createSomething());
        CompletableFuture.allOf(insertFuture1, insertFuture2).
                thenCompose(v->dao.findAllAsync()).
                thenAccept(list -> {
                    Assert.assertNotNull(list);
                    Assert.assertEquals(2, list.size());
                }).
                thenCompose(v -> dao.deleteExecAsync(DSL.trueCondition())).
                whenComplete(failOrCountDown(latch));
        await(latch);
    }


    private Something createSomething(){
        Random random = new Random();
        Something something = new Something();
        something.setSomestring("test");
        something.setSomedouble(random.nextDouble());
        something.setSomehugenumber(random.nextLong());
        something.setSomeregularnumber(random.nextInt());
        something.setSomejsonarray(new JsonArray().add(1).add(2).add(3));
        something.setSomejsonobject(new JsonObject().put("key", "value"));
        something.setSomesmallnumber((short) random.nextInt(Short.MAX_VALUE));
        return something;
    }

}
