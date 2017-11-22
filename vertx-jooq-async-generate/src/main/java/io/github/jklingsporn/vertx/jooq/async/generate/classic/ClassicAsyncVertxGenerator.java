package io.github.jklingsporn.vertx.jooq.async.generate.classic;

import io.github.jklingsporn.vertx.jooq.async.generate.AbstractVertxGenerator;
import org.jooq.util.JavaWriter;
import org.jooq.util.TableDefinition;

import java.util.List;

/**
 * Created by jensklingsporn on 19.04.17.
 */
public class ClassicAsyncVertxGenerator extends AbstractVertxGenerator {

    public static final String VERTX_DAO_NAME = "io.github.jklingsporn.vertx.jooq.async.classic.VertxDAO";

    @Override
    protected void generateDAOImports(JavaWriter out) {
        out.println("import io.vertx.core.Handler;");
        out.println("import io.vertx.core.AsyncResult;");
        out.println("import io.github.jklingsporn.vertx.jooq.async.classic.AsyncJooqSQLClient;");
    }

    @Override
    protected void generateFetchOneByMethods(JavaWriter out, String pType, String colName, String colClass, String colType, String colIdentifier) {
        out.tab(1).javadoc("Fetch a unique record that has <code>%s = value</code> asynchronously", colName);

        out.tab(1).println("public void fetchOneBy%sAsync(%s value,Handler<AsyncResult<%s>> resultHandler) {", colClass, colType,pType);
        out.tab(2).println("fetchOneAsync(%s,value,resultHandler);", colIdentifier);
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

    @Override
    protected void generateVertxGetterAndSetterConfigurationMethod(JavaWriter out) {
        //nothing
    }

    @Override
    protected void generateDaoClassFooter(TableDefinition table, JavaWriter out) {
        super.generateDaoClassFooter(table, out);
        generateClientGetterAndSetter(out);
        generateJsonMapper(table,out);
    }

    protected void generateClientGetterAndSetter(JavaWriter out) {
        out.println();
        out.tab(1).println("private AsyncJooqSQLClient client;");
        out.println();
        generateSetVertxAnnotation(out);
        out.tab(1).println("@Override");
        out.tab(1).println("public void setClient(AsyncJooqSQLClient client) {");
        out.tab(2).println("this.client = client;");
        out.tab(1).println("}");
        out.println();
        out.tab(1).println("@Override");
        out.tab(1).println("public AsyncJooqSQLClient client() {");
        out.tab(2).println("return this.client;");
        out.tab(1).println("}");
        out.println();
    }

}
