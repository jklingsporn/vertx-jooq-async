package io.github.jklingsporn.vertx.jooq.async.generate.classic;

import generated.classic.async.vertx.tables.pojos.Somethingcomposite;
import generated.classic.async.vertx.tables.records.SomethingcompositeRecord;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * Created by jensklingsporn on 02.11.16.
 */
public class VertxSomethingCompositeDaoTest extends VertxDaoTestBase {


    @Test
    public void asyncCRUDExecShouldSucceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Somethingcomposite something = createSomething(1, 1);
        compositeDao.insertExecAsync(something, consumeOrFailHandler(key->{
                Assert.assertEquals(1l,key.longValue());
                something.getSomejsonobject().put("foo","bar");
                compositeDao.updateExecAsync(something,
                        consumeOrFailHandler(updatedRows -> {
                            Assert.assertEquals(1l, updatedRows.longValue());
                            SomethingcompositeRecord somethingcompositeRecord = new SomethingcompositeRecord();
                            somethingcompositeRecord.from(something);
                            compositeDao.deleteExecAsync(somethingcompositeRecord.key(), deletedRows -> {
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
        await(latch);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void insertReturningShouldFailForCompositePK(){
        compositeDao.insertReturningPrimaryAsync(createSomething(0,0), f->{});
    }


    private Somethingcomposite createSomething(int someId, int someSecondId){
        Somethingcomposite something = new Somethingcomposite();
        something.setSomeid(someId);
        something.setSomesecondid(someSecondId);
        something.setSomejsonobject(new JsonObject().put("key", "value"));
        return something;
    }


}
