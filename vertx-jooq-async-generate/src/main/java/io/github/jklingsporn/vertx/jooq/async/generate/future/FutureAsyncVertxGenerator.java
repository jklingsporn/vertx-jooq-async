package io.github.jklingsporn.vertx.jooq.async.generate.future;

import io.github.jklingsporn.vertx.jooq.async.generate.AbstractVertxGenerator;
import org.jooq.util.JavaWriter;
import org.jooq.util.TableDefinition;

import java.util.List;

/**
 * Created by jensklingsporn on 19.04.17.
 */
public class FutureAsyncVertxGenerator extends AbstractVertxGenerator {

    public static final String VERTX_DAO_NAME = "io.github.jklingsporn.vertx.jooq.async.future.VertxDAO";

    @Override
    protected void generateDAOImports(JavaWriter out) {
        out.println("import java.util.concurrent.CompletableFuture;");
        out.println("import io.github.jklingsporn.vertx.jooq.async.future.AsyncJooqSQLClient;");
    }

    @Override
    protected void generateFetchOneByMethods(JavaWriter out, String pType, String colName, String colClass, String colType, String colIdentifier) {
        out.tab(1).javadoc("Fetch a unique record that has <code>%s = value</code> asynchronously", colName);

        out.tab(1).println("public CompletableFuture<%s> fetchOneBy%sAsync(%s value) {", pType,colClass, colType);
        out.tab(2).println("return fetchOneAsync(%s,value);", colIdentifier);
        out.tab(1).println("}");
    }

    @Override
    protected void generateFetchByMethods(JavaWriter out, String pType, String colName, String colClass, String colType, String colIdentifier) {
        out.tab(1).javadoc("Fetch records that have <code>%s IN (values)</code> asynchronously", colName);
        out.tab(1).println("public CompletableFuture<List<%s>> fetchBy%sAsync(%s<%s> values) {", pType, colClass, List.class, colType);
        //out.tab(2).println("return fetch(%s, values);", colIdentifier);
        out.tab(2).println("return fetchAsync(%s,values);", colIdentifier);
        out.tab(1).println("}");
    }

    @Override
    protected void generateVertxGetterAndSetterConfigurationMethod(JavaWriter out) {
        //noop
    }

    @Override
    protected void renderInsertReturningOverwrite(TableDefinition table, JavaWriter out, String reason) {

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
