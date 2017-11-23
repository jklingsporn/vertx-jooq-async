package io.github.jklingsporn.vertx.jooq.async.generate.classic;

import io.github.jklingsporn.vertx.jooq.async.generate.AbstractVertxGuiceGenerator;
import org.jooq.util.GeneratorStrategy;
import org.jooq.util.JavaWriter;
import org.jooq.util.TableDefinition;

import java.util.List;

/**
 * Created by jensklingsporn on 19.04.17.
 */
public class ClassicAsyncVertxGuiceGenerator extends AbstractVertxGuiceGenerator {

    public ClassicAsyncVertxGuiceGenerator() {
        super(ClassicAsyncVertxGenerator.VERTX_DAO_NAME);
    }

    public ClassicAsyncVertxGuiceGenerator(boolean generateJson, boolean generateGuiceModules, boolean generateInjectConfigurationMethod) {
        super(ClassicAsyncVertxGenerator.VERTX_DAO_NAME, generateJson, generateGuiceModules, generateInjectConfigurationMethod);
    }

    @Override
    protected void generateDAOImports(JavaWriter out) {
        out.println("import io.vertx.core.Handler;");
        out.println("import io.vertx.core.AsyncResult;");
    }

    @Override
    protected void renderInsertReturningOverwrite(TableDefinition table, JavaWriter out, String reason) {
        out.println();
        out.tab(1).println("@Override");
        out.tab(1).println("public void insertReturningPrimaryAsync(%s object, Handler<AsyncResult<%s>> resultHandler){",
                out.ref(getStrategy().getFullJavaClassName(table, GeneratorStrategy.Mode.POJO)),
                getKeyType(table.getPrimaryKey()));
        out.tab(2).println("throw new UnsupportedOperationException(\"%s\");",reason);
        out.tab(1).println("}");
        out.println();
    }

    @Override
    protected void generateFetchOneByMethods(JavaWriter out, String pType, String colName, String colClass, String colType, String colIdentifier) {
        out.tab(1).javadoc("Fetch a unique record that has <code>%s = value</code> asynchronously", colName);

        out.tab(1).println("public void fetchOneBy%sAsync(%s value,Handler<AsyncResult<%s>> resultHandler) {", colClass, colType,pType);
        out.tab(2).println("vertx().executeBlocking(h->h.complete(fetchOneBy%s(value)),resultHandler);", colClass);
        out.tab(1).println("}");
    }

    @Override
    protected void generateFetchByMethods(JavaWriter out, String pType, String colName, String colClass, String colType, String colIdentifier) {
        out.tab(1).javadoc("Fetch records that have <code>%s IN (values)</code> asynchronously", colName);
        out.tab(1).println("public void fetchBy%sAsync(%s<%s> values,Handler<AsyncResult<List<%s>>> resultHandler) {", colClass, List.class, colType,pType);
        //out.tab(2).println("return fetch(%s, values);", colIdentifier);
        out.tab(2).println("fetchAsync(%s,values,resultHandler);", colIdentifier);
        out.tab(1).println("}");
    }
}
