package io.github.jklingsporn.vertx.jooq.async.shared.internal;

import io.github.jklingsporn.vertx.jooq.async.shared.VertxPojo;
import io.vertx.core.impl.Arguments;
import io.vertx.core.json.JsonObject;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.jooq.impl.DSL.row;

/**
 * Created by jensklingsporn on 23.11.17.
 * Utility class to reduce duplicate code in the different VertxDAO implementations.
 * Only meant to be used by vertx-jooq-async.
 */
public class VertxDAOHelper {

    private VertxDAOHelper() {
    }

    static EnumSet<SQLDialect> INSERT_RETURNING_SUPPORT = EnumSet.of(SQLDialect.MYSQL,SQLDialect.MYSQL_5_7,SQLDialect.MYSQL_8_0);


    @SuppressWarnings("unchecked")
    public static <R extends UpdatableRecord<R>,T,F> F applyConditionally(T id, Table<R> table, Function<Condition, F> function){
        UniqueKey<?> uk = table.getPrimaryKey();
        Objects.requireNonNull(uk,()->"No primary key");
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
        return function.apply(condition);
    }


    @SuppressWarnings("unchecked")
    public static <P extends VertxPojo, R extends UpdatableRecord<R>,T,F> F updateExecAsync(P object, DAO<R,P,T> dao, Function<Query,F> function){
        DSLContext dslContext = DSL.using(dao.configuration());
        UniqueKey<R> pk = dao.getTable().getPrimaryKey();
        R record = dslContext.newRecord(dao.getTable(), object);
        Condition where = DSL.trueCondition();
        for (TableField<R,?> tableField : pk.getFields()) {
            //exclude primary keys from update
            record.changed(tableField,false);
            where = where.and(((TableField<R,Object>)tableField).eq(record.get(tableField)));
        }
        Map<String, Object> valuesToUpdate =
                Arrays.stream(record.fields())
                        .collect(HashMap::new, (m, f) -> m.put(f.getName(), f.getValue(record)), HashMap::putAll);

        return function.apply(dslContext.update(dao.getTable()).set(valuesToUpdate).where(where));
    }

    public static <P extends VertxPojo, R extends UpdatableRecord<R>,T,F> F insertExecAsync(P object, DAO<R,P,T> dao,Function<Query,F> function){
        return function.apply(DSL.using(dao.configuration()).insertInto(dao.getTable()).values(object.toJson().getMap().values()));
    }

    public static <P extends VertxPojo, R extends UpdatableRecord<R>,T,F> F countAsync(DAO<R,P,T> dao,BiFunction<Query, Function<JsonObject, Optional<Object>>, F> function){
        return function.apply(DSL.using(dao.configuration()).selectCount().from(dao.getTable()), json -> json.getMap().values().stream().findFirst());
    }

    @SuppressWarnings("unchecked")
    public static <P extends VertxPojo, R extends UpdatableRecord<R>,T,F> F insertReturningPrimaryAsync(P object, DAO<R,P,T> dao,BiFunction<Query,Function<Long,T>,F> function){
        Arguments.require(INSERT_RETURNING_SUPPORT.contains(dao.configuration().dialect()), "Only MySQL supported");
        UniqueKey<?> key = dao.getTable().getPrimaryKey();
        TableField<? extends Record, ?> tableField = key.getFieldsArray()[0];
        Function<Long,T> keyConverter = lastId -> {
            T checkedResult;
            if(tableField.getType().equals(Integer.class)){
                checkedResult = (T) Integer.valueOf(lastId.intValue());
            }else{
                checkedResult = (T) lastId;
            }
            return checkedResult;
        };
        DSLContext dslContext = DSL.using(dao.configuration());
        return function.apply(dslContext.insertInto(dao.getTable()).set(dslContext.newRecord(dao.getTable(), object)).returning(key.getFields()), keyConverter);
    }
}
