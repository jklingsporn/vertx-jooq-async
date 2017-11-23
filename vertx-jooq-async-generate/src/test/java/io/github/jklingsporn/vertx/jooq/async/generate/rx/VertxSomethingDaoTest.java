package io.github.jklingsporn.vertx.jooq.async.generate.rx;

import com.github.mauricio.async.db.mysql.exceptions.MySQLException;
import generated.future.async.vertx.Tables;
import generated.rx.async.vertx.tables.pojos.Something;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.hamcrest.core.Is;
import org.jooq.exception.TooManyRowsException;
import org.jooq.impl.DSL;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 * @author <a href="https://jensonjava.wordpress.com">Jens Klingsporn</a>
 */
public class VertxSomethingDaoTest extends RXVertxDaoTestBase {

    private Random random;


    @Test
    public void asyncCRUDExecShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Something somethingCreated = createSomething();
        dao.insertExecAsync(somethingCreated).
                doOnSuccess(created -> Assert.assertEquals(1L, created.longValue())).
                flatMap(v -> dao.fetchOneAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(somethingCreated.getSomehugenumber()))).
                flatMap(fetch -> dao.updateExecAsync(somethingCreated.setSomestring("modified").setSomeid(fetch.getSomeid()))).
                doOnSuccess(updated -> Assert.assertEquals(1L, updated.longValue())).
                flatMap(v -> dao.deleteExecAsync(somethingCreated.getSomeid())).
                doOnSuccess(deleted -> Assert.assertEquals(1L, deleted.longValue())).
                subscribe(failOrCountDownSingleObserver(latch));
        await(latch);
    }

    @Test
    public void insertExecShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        dao.insertExecAsync(createSomething()).
                doOnSuccess(insertedRows -> Assert.assertEquals(1L,insertedRows.longValue())).
                flatMap(v-> dao.client().fetchOne(DSL.using(dao.configuration()).selectFrom(Tables.SOMETHING).orderBy(Tables.SOMETHING.SOMEID.desc()).limit(1),dao.jsonMapper())).
                flatMap(id -> {
                    Assert.assertNotNull(id);
                    return dao.deleteExecAsync(id.getSomeid());

                }).
                doOnSuccess(deletedRows -> Assert.assertEquals(1L, deletedRows.longValue())).
                subscribe(failOrCountDownSingleObserver(latch));
        await(latch);
    }

    @Test
    public void insertReturningShouldFailOnDuplicateKey() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Something something = createSomething();
        dao.insertReturningPrimaryAsync(something)
            .flatMap(id -> dao.insertReturningPrimaryAsync(something.setSomeid(id)))
            .subscribe(
                i -> Assert.fail("Should not happen"),
                err -> {
                    Assert.assertEquals(MySQLException.class, err.getClass());
                    latch.countDown();
                }
            );
        await(latch);
    }

    @Test
    public void asyncCRUDConditionShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        dao.insertReturningPrimaryAsync(createSomething())
            .flatMap(i ->
                dao.fetchOneAsync(Tables.SOMETHING.SOMEID.eq(i))
                    .doOnSuccess(Assert::assertNotNull)
                    .flatMap(s -> dao.deleteExecAsync(Tables.SOMETHING.SOMEID.eq(i)))
            )
            .subscribe(failOrCountDownSingleObserver(latch));
        await(latch);
    }

    @Test
    public void fetchOneByConditionWithMultipleMatchesShouldFail() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Single<Integer> insert1 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));
        Single<Integer> insert2 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));


        Single.zip(insert1, insert2, (i1, i2) -> i1)
            .flatMap(i -> dao.fetchOneAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L)))
            .onErrorReturn(x -> {
                Assert.assertNotNull(x);
                Assert.assertEquals(TooManyRowsException.class, x.getClass());
                return null;
            })
            .flatMap(n -> dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L)))
            .subscribe(failOrCountDownSingleObserver(latch));

        await(latch);
    }

    @Test
    public void fetchByConditionWithMultipleMatchesShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Single<Integer> insert1 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));
        Single<Integer> insert2 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));

        Single.zip(insert1, insert2, (i1, i2) -> i1)
            .flatMap(i -> dao.fetchAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L)))
            .doOnSuccess(values -> Assert.assertEquals(2, values.size()))
            .flatMap(list -> dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L)))
            .subscribe(failOrCountDownSingleObserver(latch));

        await(latch);
    }

    @Test
    public void fetchByConditionWithMultipleMatchesWithObservableShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Single<Integer> insert1 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));
        Single<Integer> insert2 = dao.insertReturningPrimaryAsync(createSomething().setSomehugenumber(1L));

        AtomicInteger count = new AtomicInteger();
        Single.zip(insert1, insert2, (i1, i2) -> i1)
            .flatMapObservable(i -> dao.fetchObservable(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L)))
            .doOnNext(s -> count.getAndIncrement())
            .doOnComplete(() -> dao.deleteExecAsync(Tables.SOMETHING.SOMEHUGENUMBER.eq(1L)))
            .subscribe(failOrCountDownPlainObserver(latch));
        await(latch);
        Assert.assertThat(count.get(), Is.is(2));
    }

    private Something createSomething() {
        random = new Random();
        Something something = new Something();
        something.setSomedouble(random.nextDouble());
        something.setSomehugenumber(random.nextLong());
        something.setSomejsonarray(new JsonArray().add(1).add(2).add(3));
        something.setSomejsonobject(new JsonObject().put("key", "value"));
        something.setSomesmallnumber((short) random.nextInt(Short.MAX_VALUE));
        return something;
    }


}
