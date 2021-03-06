package io.github.jklingsporn.vertx.jooq.async.classic;

import io.github.jklingsporn.vertx.jooq.async.shared.VertxPojo;
import io.github.jklingsporn.vertx.jooq.async.shared.internal.VertxDAOHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by jensklingsporn on 21.10.16.
 * Vertx-ified version of jOOQs <code>DAO</code>-interface.
 */
public interface VertxDAO<R extends UpdatableRecord<R>, P extends VertxPojo, T> extends DAO<R, P, T> {

    static EnumSet<SQLDialect> INSERT_RETURNING_SUPPORT = EnumSet.of(SQLDialect.MYSQL,SQLDialect.MYSQL_5_7,SQLDialect.MYSQL_8_0);

    AsyncJooqSQLClient client();

    void setClient(AsyncJooqSQLClient client);

    /**
     * @return a function that maps a <code>JsonObject</code> fetched from the vertx-client into a POJO.
     * Because vertx isn't aware of any jOOQ conversions one might have configured for a field,
     * this can differ from the POJOs from/toJson methods.
     */
    Function<JsonObject, P> jsonMapper();

    /**
     * Checks if a given ID exists asynchronously
     *
     * @param id The ID whose existence is checked
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #existsById(Object)
     */
    default void existsByIdAsync(T id, Handler<AsyncResult<Boolean>> resultHandler){
        findByIdAsync(id, h -> {
            if (h.succeeded()) {
                resultHandler.handle(Future.succeededFuture(h.result() != null));
            }else{
                resultHandler.handle(Future.failedFuture(h.cause()));
            }
        });
    }

    /**
     * Count all records of the underlying table asynchronously.
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #count()
     */
    default void countAsync(Handler<AsyncResult<Long>> resultHandler){
        VertxDAOHelper.countAsync(this, (query,mapper)->{
            client().fetchOne(query,mapper, h -> {
                if (h.succeeded()) {
                    resultHandler.handle(Future.succeededFuture((Long) h.result().get()));
                } else {
                    resultHandler.handle(Future.failedFuture(h.cause()));
                }
            });
            return null;
        });
    }

    /**
     * Find all records of the underlying table asynchronously.
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #findAll()
     */
    default void findAllAsync(Handler<AsyncResult<List<P>>> resultHandler){
        fetchAsync(DSL.trueCondition(),resultHandler);
    }

    /**
     * Find a record of the underlying table by ID asynchronously.
     *
     * @param id The ID of a record in the underlying table
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #findById(Object)
     */
    default void findByIdAsync(T id, Handler<AsyncResult<P>> resultHandler){
        VertxDAOHelper.applyConditionally(id, getTable(), condition -> {
            fetchOneAsync(condition, resultHandler);
            return null;
        });
    }

    /**
     * Find a unique record by a given field and a value asynchronously.
     *
     * @param field The field to compare value against
     * @param value The accepted value
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @param <Z> the value type
     * @see #fetchOne(Field, Object)
     */
    default <Z> void fetchOneAsync(Field<Z> field, Z value, Handler<AsyncResult<P>> resultHandler){
        fetchOneAsync(field.eq(value),resultHandler);
    }

    /**
     * Find a unique record by a given condition asynchronously.
     * @param condition the condition to fetch one value
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @param <Z> the value type
     */
    default <Z> void fetchOneAsync(Condition condition, Handler<AsyncResult<P>> resultHandler){
        client().fetchOne(DSL.using(configuration()).selectFrom(getTable()).where(condition), jsonMapper(),resultHandler);
    }


    /**
     * Find a unique record by a given field and a value asynchronously.
     *
     * @param field The field to compare value against
     * @param value The accepted value
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @param <Z> the value type
     * @see #fetchOptional(Field, Object)
     */
    default <Z> void fetchOptionalAsync(Field<Z> field, Z value, Handler<AsyncResult<Optional<P>>> resultHandler){
        fetchOneAsync(field,value,h->{
            if(h.succeeded()){
                resultHandler.handle(Future.succeededFuture(Optional.ofNullable(h.result())));
            }else{
                resultHandler.handle(Future.failedFuture(h.cause()));
            }
        });
    }

    /**
     * Find records by a given field and a set of values asynchronously.
     *
     * @param field The field to compare values against
     * @param values The accepted values
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @param <Z> the value type
     */
    default <Z> void fetchAsync(Field<Z> field, Collection<Z> values, Handler<AsyncResult<List<P>>> resultHandler){
        fetchAsync(field.in(values),resultHandler);
    }

    /**
     * Find records by a given condition asynchronously.
     *
     * @param condition the condition to fetch one value
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default void fetchAsync(Condition condition, Handler<AsyncResult<List<P>>> resultHandler){
        client().fetch(DSL.using(configuration()).selectFrom(getTable()).where(condition), jsonMapper(),resultHandler);
    }

    /**
     * Performs an async <code>DELETE</code> statement for a given key and passes the number of affected rows
     * to the <code>resultHandler</code>.
     * @param id The key to be deleted
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default void deleteExecAsync(T id, Handler<AsyncResult<Integer>> resultHandler){
        VertxDAOHelper.applyConditionally(id, getTable(), condition -> {
            deleteExecAsync(condition, resultHandler);
            return null;
        });
    }

    /**
     * Performs an async <code>DELETE</code> statement for a given condition and passes the number of affected rows
     * to the <code>resultHandler</code>.
     * @param condition The condition for the delete query
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @param <Z> the value type
     */
    default <Z> void deleteExecAsync(Condition condition, Handler<AsyncResult<Integer>> resultHandler ){
        client().execute(DSL.using(configuration()).deleteFrom(getTable()).where(condition),resultHandler);
    }

    /**
     * Performs an async <code>DELETE</code> statement for a given condition and passes the number of affected rows
     * to the <code>resultHandler</code>.
     * @param field the field
     * @param value the value
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @param <Z> the value type
     */
    default <Z> void deleteExecAsync(Field<Z> field, Z value, Handler<AsyncResult<Integer>> resultHandler){
        deleteExecAsync(field.eq(value),resultHandler);
    }

    /**
     * Performs an async <code>UPDATE</code> statement for a given POJO and passes the number of affected rows
     * to the <code>resultHandler</code>.
     * @param object The POJO to be updated
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    @SuppressWarnings("unchecked")
    default void updateExecAsync(P object, Handler<AsyncResult<Integer>> resultHandler){
        VertxDAOHelper.updateExecAsync(object, this, query -> {
            client().execute(query, resultHandler);
            return null;
        });
    }

    /**
     * Performs an async <code>INSERT</code> statement for a given POJO and passes the number of affected rows
     * to the <code>resultHandler</code>.
     * @param object The POJO to be inserted
     * @param resultHandler the resultHandler which succeeds when the blocking method of this type succeeds or fails
     *                      with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default void insertExecAsync(P object, Handler<AsyncResult<Integer>> resultHandler){
        VertxDAOHelper.insertExecAsync(object, this, query -> {
            client().execute(query,resultHandler);
            return null;
        });
    }

    /**
     * Performs an async <code>INSERT</code> statement for a given POJO and passes the primary key
     * to the <code>resultHandler</code>. When the value could not be inserted, the <code>resultHandler</code>
     * will fail.
     * @param object The POJO to be inserted
     * @param resultHandler the resultHandler. In case of Postgres or when PK length is greater 1 or PK is not of
     *                      type int or long, the resultHandler will be in error state.
     */
    default void insertReturningPrimaryAsync(P object, Handler<AsyncResult<T>> resultHandler){
        VertxDAOHelper.insertReturningPrimaryAsync(object,this, (query,fun)->{
            client().insertReturning(query,res -> {
                if (res.failed()) {
                    resultHandler.handle(Future.failedFuture(res.cause()));
                } else {
                    resultHandler.handle(Future.succeededFuture(fun.apply(res.result())));
                }
            });
            return null;
        });
    }

}
