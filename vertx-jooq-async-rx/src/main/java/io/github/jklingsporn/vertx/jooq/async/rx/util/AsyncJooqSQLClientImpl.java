package io.github.jklingsporn.vertx.jooq.async.rx.util;

import io.github.jklingsporn.vertx.jooq.async.rx.AsyncJooqSQLClient;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.asyncsql.AsyncSQLClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import org.jooq.Param;
import org.jooq.Query;

import java.util.List;
import java.util.Optional;
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
    public <P> Single<List<P>> fetch(Query query, java.util.function.Function<JsonObject, P> mapper){
        return getConnection().flatMap(executeAndClose(sqlConnection ->
                sqlConnection.rxQueryWithParams(query.getSQL(), getBindValues(query)).map(rs ->
                                rs.getRows().stream().map(mapper).collect(Collectors.toList())
                )));
    }

    @Override
    public <P> Single<P> fetchOne(Query query, Function<JsonObject, P> mapper){
        return getConnection().flatMap(executeAndClose(sqlConnection ->
            sqlConnection.rxQueryWithParams(query.getSQL(), getBindValues(query)).map(rs -> {
                Optional<P> optional = rs.getRows().stream().findFirst().map(mapper);
                return optional.orElseGet(() -> null);
            })));
    }

    @Override
    public Single<Integer> execute(Query query){
        return getConnection()
                .flatMap(executeAndClose(sqlConnection ->
                        sqlConnection
                                .rxUpdateWithParams(query.getSQL(), getBindValues(query))
                                .map(UpdateResult::getUpdated))
                );
    }

    @Override
    public Single<Long> insertReturning(Query query) {
        return getConnection()
                .flatMap(executeAndClose(sqlConnection ->
                                sqlConnection
                                        .rxUpdateWithParams(query.getSQL(), getBindValues(query))
                                        .map(updateResult -> updateResult.getKeys().getLong(0)))
                );
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
    private Single<SQLConnection> getConnection(){
        return delegate().rxGetConnection();
    }

    private <R> io.reactivex.functions.Function<SQLConnection, Single<? extends  R>> executeAndClose(Function<SQLConnection, Single<? extends R>> func) {
        return sqlConnection -> func.apply(sqlConnection).doAfterTerminate(sqlConnection::close);
    }

    @Override
    public AsyncSQLClient delegate() {
        return delegate;
    }
}
