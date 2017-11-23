package io.github.jklingsporn.vertx.jooq.async.classic.impl;

import io.github.jklingsporn.vertx.jooq.async.classic.AsyncJooqSQLClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.jooq.Param;
import org.jooq.Query;
import org.jooq.conf.ParamType;
import org.jooq.exception.TooManyRowsException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by jensklingsporn on 13.06.17.
 */
public class AsyncJooqSQLClientImpl implements AsyncJooqSQLClient {

    private static final Logger logger = LoggerFactory.getLogger(AsyncJooqSQLClientImpl.class);

    private final Vertx vertx;
    private final AsyncSQLClient delegate;

    public AsyncJooqSQLClientImpl(Vertx vertx, AsyncSQLClient delegate) {
        this.vertx = vertx;
        this.delegate = delegate;
    }

    @Override
    public <P> void fetch(Query query, Function<JsonObject, P> mapper, Handler<AsyncResult<List<P>>> resultHandler) {
        getConnection().setHandler(sqlConnectionResult->{
            if(sqlConnectionResult.succeeded()){
                log("Fetch", ()-> query.getSQL(ParamType.INLINED));
                sqlConnectionResult.result().queryWithParams(
                        query.getSQL(),
                        getBindValues(query),
                        executeAndClose(rs -> rs.getRows().stream().map(mapper).collect(Collectors.toList()), sqlConnectionResult.result(), resultHandler)
                );
            }else{
                resultHandler.handle(Future.failedFuture(sqlConnectionResult.cause()));
            }
        });
    }

    @Override
    public <P> void fetchOne(Query query, Function<JsonObject, P> mapper, Handler<AsyncResult<P>> resultHandler) {
        getConnection().setHandler(sqlConnectionResult->{
            if(sqlConnectionResult.succeeded()){
                log("Fetch one", ()-> query.getSQL(ParamType.INLINED));
                sqlConnectionResult.result().queryWithParams(
                        query.getSQL(),
                        getBindValues(query),
                        executeAndClose(rs -> {
                                    if(rs.getRows().size() > 1){
                                        throw new TooManyRowsException(String.format("Got more than one row: %d",rs.getRows().size()));
                                    }
                                    Optional<P> optional = rs.getRows().stream().findFirst().map(mapper);
                                    return (optional.orElseGet(() -> null));
                                },
                                sqlConnectionResult.result(),
                                resultHandler)
                );
            }else{
                resultHandler.handle(Future.failedFuture(sqlConnectionResult.cause()));
            }
        });
    }

    @Override
    public void execute(Query query, Handler<AsyncResult<Integer>> resultHandler) {
        getConnection().setHandler(sqlConnectionResult->{
            if(sqlConnectionResult.succeeded()){
                log("Execute", ()-> query.getSQL(ParamType.INLINED));
                sqlConnectionResult.result().updateWithParams(
                        query.getSQL(),
                        getBindValues(query),
                        executeAndClose(UpdateResult::getUpdated,
                                sqlConnectionResult.result(),
                                resultHandler)
                );
            }else{
                resultHandler.handle(Future.failedFuture(sqlConnectionResult.cause()));
            }
        });
    }

    @Override
    public void insertReturning(Query query, Handler<AsyncResult<Long>> resultHandler) {
        getConnection().setHandler(sqlConnectionResult->{
            if(sqlConnectionResult.succeeded()){
                log("Insert Returning", ()-> query.getSQL(ParamType.INLINED));
                sqlConnectionResult.result().update(
                        query.getSQL(ParamType.INLINED),
                        executeAndClose(res -> res.getKeys().getLong(0),
                                sqlConnectionResult.result(),
                                resultHandler)
                );
            }else{
                resultHandler.handle(Future.failedFuture(sqlConnectionResult.cause()));
            }
        });
    }

    private void log(String type, Supplier<String> messageSupplier){
        if(logger.isDebugEnabled()){
            logger.debug("{}: {}",type, messageSupplier.get());
        }
    }

    private <P,U> Handler<AsyncResult<U>> executeAndClose(Function<U, P> func, SQLConnection sqlConnection, Handler<AsyncResult<P>> resultHandler) {
        return rs -> {
            try{
                if (rs.succeeded()) {
                    resultHandler.handle(Future.succeededFuture(func.apply(rs.result())));
                } else {
                    resultHandler.handle(Future.failedFuture(rs.cause()));
                }
            }catch(Throwable e) {
                resultHandler.handle(Future.failedFuture(e));
            }finally {
                sqlConnection.close();
            }
        };
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
     * @return a Future that returns a SQLConnection or an Exception.
     */
    private Future<SQLConnection> getConnection(){
        Future<SQLConnection> future = Future.future();
        delegate.getConnection(future);
        return future;
    }

    @Override
    public AsyncSQLClient delegate() {
        return delegate;
    }
}
