package io.github.jklingsporn.vertx.jooq.async.rx;

import io.github.jklingsporn.vertx.jooq.async.shared.VertxPojo;
import io.vertx.core.json.JsonObject;
import org.jooq.*;
import org.jooq.impl.DSL;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static org.jooq.impl.DSL.row;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public interface VertxDAO<R extends UpdatableRecord<R>, P extends VertxPojo, T> extends DAO<R, P, T> {

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
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #existsById(Object)
     */
    default Single<Boolean> existsByIdAsync(T id) {
        return findByIdAsync(id).map(p->p!=null);
    }

    /**
     * Count all records of the underlying table asynchronously.
     *
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #count()
     */
    default Single<Long> countAsync() {
        return client().fetchOne(DSL.using(configuration()).selectCount().from(getTable()),
                json -> json.getMap().values().stream().findFirst()).
                map(opt -> (Long) opt.get());
    }

    /**
     * Find all records of the underlying table asynchronously.
     *
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #findAll()
     */
    default Single<List<P>> findAllAsync() {
        return fetchAsync(DSL.trueCondition());
    }


    /**
     * Find a record of the underlying table by ID asynchronously.
     *
     * @param id The ID of a record in the underlying table
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #findById(Object)
     */
    default Single<P> findByIdAsync(T id) {
        UniqueKey<?> uk = getTable().getPrimaryKey();
        Objects.requireNonNull(uk, () -> "No primary key");
        /**
         * Copied from jOOQs DAOImpl#equal-method
         */
        TableField<? extends Record, ?>[] pk = uk.getFieldsArray();
        Condition condition;
        if (pk.length == 1) {
            condition = ((Field<Object>) pk[0]).equal(pk[0].getDataType().convert(id));
        }
        else {
            condition = row(pk).equal((Record) id);
        }
        return fetchOneAsync(condition);
    }

    /**
     * Find a unique record by a given field and a value asynchronously.
     *
     * @param field The field to compare value against
     * @param value The accepted value
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #fetchOne(Field, Object)
     */
    default <Z> Single<P> fetchOneAsync(Field<Z> field, Z value) {
        return fetchOneAsync(field.eq(value));
    }

    /**
     * Find a unique record by a given condition asynchronously.
     *
     * @param condition The condition to look for this value
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception,
     * e.g. when more than one result is returned.
     */
    default <Z> Single<P> fetchOneAsync(Condition condition) {
        return client().fetchOne(DSL.using(configuration()).selectFrom(getTable()).where(condition), jsonMapper());
    }

    /**
     * Find a unique record by a given field and a value asynchronously.
     *
     * @param field The field to compare value against
     * @param value The accepted value
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     * @see #fetchOptional(Field, Object)
     */
    default <Z> Single<Optional<P>> fetchOptionalAsync(Field<Z> field, Z value) {
        return fetchOneAsync(field,value).map(Optional::ofNullable);
    }

    /**
     * Find records by a given field and a set of values asynchronously.
     *
     * @param field  The field to compare values against
     * @param values The accepted values
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default <Z> Single<List<P>> fetchAsync(Field<Z> field, Collection<Z> values) {
        return fetchAsync(field.in(values));
    }

    default <Z> Observable<P> fetchObservable(Field<Z> field, Collection<Z> values) {
        return fetchObservable(field.in(values));
    }

    /**
     * Find records by a given condition asynchronously.
     *
     * @param condition the condition to fetch the values
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default Single<List<P>> fetchAsync(Condition condition) {
        return client().fetch(DSL.using(configuration()).selectFrom(getTable()).where(condition), jsonMapper());
    }

    default Observable<P> fetchObservable(Condition condition) {
        return fetchAsync(condition).flatMapObservable(Observable::fromIterable);
    }

    /**
     * Performs an async <code>DELETE</code> statement for a given key and passes the number of affected rows
     * to the returned <code>Single</code>.
     *
     * @param id The key to be deleted
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    @SuppressWarnings("unchecked")
    default Single<Integer> deleteExecAsync(T id) {
        UniqueKey<?> uk = getTable().getPrimaryKey();
        Objects.requireNonNull(uk, () -> "No primary key");
        /*
         * Copied from jOOQs DAOImpl#equal-method
         */
        TableField<? extends Record, ?>[] pk = uk.getFieldsArray();
        Condition condition;
        if (pk.length == 1) {
            condition = ((Field<Object>) pk[0]).equal(pk[0].getDataType().convert(id));
        } else {
            condition = row(pk).equal((Record) id);
        }
        return deleteExecAsync(condition);
    }

    /**
     * Performs an async <code>DELETE</code> statement for a given condition and passes the number of affected rows
     * to the returned <code>Single</code>.
     *
     * @param condition The condition for the delete query
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default Single<Integer> deleteExecAsync(Condition condition) {
        return client().execute(DSL.using(configuration()).deleteFrom(getTable()).where(condition));
    }

    /**
     * Performs an async <code>DELETE</code> statement for a given field and value and passes the number of affected rows
     * to the returned <code>Single</code>.
     *
     * @param field the field
     * @param value the value
     * @param <Z>
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default <Z> Single<Integer> deleteExecAsync(Field<Z> field, Z value) {
        return deleteExecAsync(field.eq(value));
    }

    /**
     * Performs an async <code>UPDATE</code> statement for a given POJO and passes the number of affected rows
     * to the <code>resultHandler</code>.
     *
     * @param object The POJO to be updated
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default Single<Integer> updateExecAsync(P object) {
        DSLContext dslContext = DSL.using(configuration());
        return client().execute(dslContext.update(getTable()).set(dslContext.newRecord(getTable(), object)));
    }

    /**
     * Performs an async <code>INSERT</code> statement for a given POJO and passes the number of affected rows
     * to the <code>resultHandler</code>.
     *
     * @param object The POJO to be inserted
     * @return Single which succeeds when the blocking method of this type succeeds or fails
     * with an <code>DataAccessException</code> if the blocking method of this type throws an exception
     */
    default Single<Integer> insertExecAsync(P object) {
        return client().execute(DSL.using(configuration()).insertInto(getTable()).values(object.toJson().getMap().values()));
    }

    /**
     * Performs an async <code>INSERT</code> statement for a given POJO and passes the primary key
     * to the <code>resultHandler</code>. When the value could not be inserted, the <code>resultHandler</code>
     * will fail.
     *
     * @param object The POJO to be inserted
     * @return the Single
     */
    @SuppressWarnings("unchecked")
    default Single<T> insertReturningPrimaryAsync(P object) {
        throw new UnsupportedOperationException(":(");
//        UniqueKey<?> key = getTable().getPrimaryKey();
//        //usually key shouldn't be null because DAO generation is omitted in such cases
//        Objects.requireNonNull(key, () -> "No primary key");
//        return executeAsync(dslContext -> {
//            R record = dslContext.insertInto(getTable()).set(dslContext.newRecord(getTable(), object)).returning(key.getFields()).fetchOne();
//            Objects.requireNonNull(record, () -> "Failed inserting record or no key");
//            Record key1 = record.key();
//            if (key1.size() == 1) {
//                return ((Record1<T>) key1).value1();
//            }
//            return (T) key1;
//        });
    }

}
