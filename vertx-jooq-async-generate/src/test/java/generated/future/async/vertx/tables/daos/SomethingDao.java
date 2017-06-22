/*
 * This file is generated by jOOQ.
*/
package generated.future.async.vertx.tables.daos;


import generated.future.async.vertx.tables.Something;
import generated.future.async.vertx.tables.records.SomethingRecord;

import io.github.jklingsporn.vertx.jooq.async.future.VertxDAO;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import javax.annotation.Generated;

import org.jooq.Configuration;
import org.jooq.impl.DAOImpl;


import java.util.concurrent.CompletableFuture;
import io.github.jklingsporn.vertx.jooq.async.future.AsyncJooqSQLClient;
/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.2"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SomethingDao extends DAOImpl<SomethingRecord, generated.future.async.vertx.tables.pojos.Something, Integer> implements VertxDAO<generated.future.async.vertx.tables.records.SomethingRecord,generated.future.async.vertx.tables.pojos.Something,java.lang.Integer> {

    /**
     * Create a new SomethingDao without any configuration
     */
    public SomethingDao() {
        super(Something.SOMETHING, generated.future.async.vertx.tables.pojos.Something.class);
    }

    /**
     * Create a new SomethingDao with an attached configuration
     */
    public SomethingDao(Configuration configuration) {
        super(Something.SOMETHING, generated.future.async.vertx.tables.pojos.Something.class, configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Integer getId(generated.future.async.vertx.tables.pojos.Something object) {
        return object.getSomeid();
    }

    /**
     * Fetch records that have <code>someId IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomeid(Integer... values) {
        return fetch(Something.SOMETHING.SOMEID, values);
    }

    /**
     * Fetch a unique record that has <code>someId = value</code>
     */
    public generated.future.async.vertx.tables.pojos.Something fetchOneBySomeid(Integer value) {
        return fetchOne(Something.SOMETHING.SOMEID, value);
    }

    /**
     * Fetch records that have <code>someString IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomestring(String... values) {
        return fetch(Something.SOMETHING.SOMESTRING, values);
    }

    /**
     * Fetch records that have <code>someHugeNumber IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomehugenumber(Long... values) {
        return fetch(Something.SOMETHING.SOMEHUGENUMBER, values);
    }

    /**
     * Fetch records that have <code>someSmallNumber IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomesmallnumber(Short... values) {
        return fetch(Something.SOMETHING.SOMESMALLNUMBER, values);
    }

    /**
     * Fetch records that have <code>someRegularNumber IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomeregularnumber(Integer... values) {
        return fetch(Something.SOMETHING.SOMEREGULARNUMBER, values);
    }

    /**
     * Fetch records that have <code>someDouble IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomedouble(Double... values) {
        return fetch(Something.SOMETHING.SOMEDOUBLE, values);
    }

    /**
     * Fetch records that have <code>someEnum IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomeenum(String... values) {
        return fetch(Something.SOMETHING.SOMEENUM, values);
    }

    /**
     * Fetch records that have <code>someJsonObject IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomejsonobject(JsonObject... values) {
        return fetch(Something.SOMETHING.SOMEJSONOBJECT, values);
    }

    /**
     * Fetch records that have <code>someJsonArray IN (values)</code>
     */
    public List<generated.future.async.vertx.tables.pojos.Something> fetchBySomejsonarray(JsonArray... values) {
        return fetch(Something.SOMETHING.SOMEJSONARRAY, values);
    }

    /**
     * Fetch records that have <code>someId IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomeidAsync(List<Integer> values) {
        return fetchAsync(Something.SOMETHING.SOMEID,values);
    }

    /**
     * Fetch a unique record that has <code>someId = value</code> asynchronously
     */
    public CompletableFuture<generated.future.async.vertx.tables.pojos.Something> fetchOneBySomeidAsync(Integer value) {
        return fetchOneAsync(Something.SOMETHING.SOMEID,value);
    }

    /**
     * Fetch records that have <code>someString IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomestringAsync(List<String> values) {
        return fetchAsync(Something.SOMETHING.SOMESTRING,values);
    }

    /**
     * Fetch records that have <code>someHugeNumber IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomehugenumberAsync(List<Long> values) {
        return fetchAsync(Something.SOMETHING.SOMEHUGENUMBER,values);
    }

    /**
     * Fetch records that have <code>someSmallNumber IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomesmallnumberAsync(List<Short> values) {
        return fetchAsync(Something.SOMETHING.SOMESMALLNUMBER,values);
    }

    /**
     * Fetch records that have <code>someRegularNumber IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomeregularnumberAsync(List<Integer> values) {
        return fetchAsync(Something.SOMETHING.SOMEREGULARNUMBER,values);
    }

    /**
     * Fetch records that have <code>someDouble IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomedoubleAsync(List<Double> values) {
        return fetchAsync(Something.SOMETHING.SOMEDOUBLE,values);
    }

    /**
     * Fetch records that have <code>someEnum IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomeenumAsync(List<String> values) {
        return fetchAsync(Something.SOMETHING.SOMEENUM,values);
    }

    /**
     * Fetch records that have <code>someJsonObject IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomejsonobjectAsync(List<JsonObject> values) {
        return fetchAsync(Something.SOMETHING.SOMEJSONOBJECT,values);
    }

    /**
     * Fetch records that have <code>someJsonArray IN (values)</code> asynchronously
     */
    public CompletableFuture<List<generated.future.async.vertx.tables.pojos.Something>> fetchBySomejsonarrayAsync(List<JsonArray> values) {
        return fetchAsync(Something.SOMETHING.SOMEJSONARRAY,values);
    }

    private AsyncJooqSQLClient client;

    @Override
    public void setClient(AsyncJooqSQLClient client) {
        this.client = client;
    }

    @Override
    public AsyncJooqSQLClient client() {
        return this.client;
    }

    @Override
    public java.util.function.Function<JsonObject, generated.future.async.vertx.tables.pojos.Something> jsonMapper() {
        return generated.future.async.vertx.tables.pojos.Something::new;
    }

}
