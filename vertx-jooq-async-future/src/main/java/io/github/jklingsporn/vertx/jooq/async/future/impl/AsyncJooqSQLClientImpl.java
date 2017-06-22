package io.github.jklingsporn.vertx.jooq.async.future.impl;

import io.github.jklingsporn.vertx.jooq.async.future.AsyncJooqSQLClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.jooq.Param;
import org.jooq.Query;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by jensklingsporn on 13.06.17.
 */
public class AsyncJooqSQLClientImpl implements AsyncJooqSQLClient {

    private final Vertx vertx;
    private final AsyncSQLClient delegate;

    public AsyncJooqSQLClientImpl(Vertx vertx, AsyncSQLClient delegate) {
        this.vertx = vertx;
        this.delegate = delegate;
    }

    @Override
    public <P> CompletableFuture<List<P>> fetch(Query query, Function<JsonObject, P> mapper){
        return getConnection().thenCompose(sqlConnection -> {
            CompletableFuture<List<P>> cf = new VertxCompletableFuture<>(vertx);
            sqlConnection.queryWithParams(
                    query.getSQL(),
                    getBindValues(query),
                    executeAndClose(rs ->
                            rs.getRows().stream().map(mapper).collect(Collectors.toList()),
                            sqlConnection,
                            cf)
            );
            return cf;
        });
    }

    @Override
    public <P> CompletableFuture<P> fetchOne(Query query, Function<JsonObject, P> mapper){
        return getConnection().thenCompose(sqlConnection -> {
            CompletableFuture<P> cf = new VertxCompletableFuture<P>(vertx);
            sqlConnection.queryWithParams(query.getSQL(), getBindValues(query), executeAndClose(rs -> {
                Optional<P> optional = rs.getRows().stream().findFirst().map(mapper);
                return optional.orElseGet(() -> null);
            }, sqlConnection, cf));
            return cf;
        });
    }

    @Override
    public CompletableFuture<Integer> execute(Query query){
        return getConnection().thenCompose(sqlConnection -> {
            CompletableFuture<Integer> cf = new VertxCompletableFuture<>(vertx);
            JsonArray bindValues = getBindValues(query);
            sqlConnection.updateWithParams(query.getSQL(), bindValues, executeAndClose(UpdateResult::getUpdated,sqlConnection,cf));
            return cf;
        });
    }

    private JsonArray getBindValues(Query query) {
        JsonArray bindValues = new JsonArray();
        for (Param<?> param : query.getParams().values()) {
            Object value = convertToDatabaseType(param);
            if(value==null){
                bindValues.addNull();
            }else{
                bindValues.add(value);
            }
        }
        return bindValues;
    }

    static <T> Object convertToDatabaseType(Param<T> param) {
        return param.getBinding().converter().to(param.getValue());
    }

    /**
     * @return a CompletableFuture that returns a SQLConnection or an Exception.
     */
    private CompletableFuture<SQLConnection> getConnection(){
        CompletableFuture<SQLConnection> cf = new VertxCompletableFuture<>(vertx);
        delegate.getConnection(h -> {
            if (h.succeeded()) {
                cf.complete(h.result());
            } else {
                cf.completeExceptionally(h.cause());
            }
        });
        return cf;
    }

    private <P,U> Handler<AsyncResult<U>> executeAndClose(Function<U, P> func, SQLConnection sqlConnection, CompletableFuture<P> cf) {
        return rs -> {
            try{
                if (rs.succeeded()) {
                    cf.complete(func.apply(rs.result()));
                } else {
                    cf.completeExceptionally(rs.cause());
                }
            }finally {
                sqlConnection.close();
            }
        };
    }

    @Override
    public AsyncSQLClient delegate() {
        return delegate;
    }
}
