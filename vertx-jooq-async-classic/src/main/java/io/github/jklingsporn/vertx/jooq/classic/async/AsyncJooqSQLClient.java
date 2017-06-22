package io.github.jklingsporn.vertx.jooq.classic.async;

import io.github.jklingsporn.vertx.jooq.classic.async.impl.AsyncJooqSQLClientImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import org.jooq.Query;

import java.util.List;
import java.util.function.Function;

/**
 * Created by jensklingsporn on 13.06.17.
 */
public interface AsyncJooqSQLClient {

    public static AsyncJooqSQLClient create(Vertx vertx, AsyncSQLClient delegate){
        return new AsyncJooqSQLClientImpl(vertx, delegate);
    }

    /**
     *
     * @param query
     * @param mapper
     * @param resultHandler
     * @param <P>
     */
    <P> void fetch(Query query, Function<JsonObject, P> mapper, Handler<AsyncResult<List<P>>> resultHandler);

    /**
     * @param query a jOOQ-query
     * @param mapper a function to map the result into another object.
     * @param resultHandler
     * @param <P>
     */
    <P> void fetchOne(Query query, Function<JsonObject, P> mapper, Handler<AsyncResult<P>> resultHandler);

    /**
     * @param query a jOOQ-query
     * @param resultHandler A Handler containing the number of affected rows by this query.
     */
    void execute(Query query, Handler<AsyncResult<Integer>> resultHandler);

    /**
     * @return the underlying client
     */
    AsyncSQLClient delegate();
}
