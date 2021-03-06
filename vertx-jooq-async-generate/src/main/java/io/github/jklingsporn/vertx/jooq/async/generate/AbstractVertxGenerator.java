package io.github.jklingsporn.vertx.jooq.async.generate;

import io.github.jklingsporn.vertx.jooq.async.shared.JsonArrayConverter;
import io.github.jklingsporn.vertx.jooq.async.shared.JsonObjectConverter;
import io.vertx.core.json.JsonObject;
import org.jooq.Constants;
import org.jooq.Record;
import org.jooq.tools.JooqLogger;
import org.jooq.util.*;

import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Created by jklingsporn on 17.10.16.
 * Extension of the jOOQ's <code>JavaGenerator</code>.
 * By default, it generates POJO's that have a <code>#fromJson</code> and a <code>#toJson</code>-method which takes/generates a <code>JsonObject</code> out of the generated POJO.
 * When you've enabled Interface-generation, these methods are added to the generated Interface as default-methods.
 * Besides these method there is also a constructor generated which takes a <code>JsonObject</code>.
 * It also generates DAOs which implement
 * <code>VertxDAO</code> and allow you to execute CRUD-operations asynchronously.
 */
public abstract class AbstractVertxGenerator extends JavaGenerator {

    private static final JooqLogger logger = JooqLogger.getLogger(AbstractVertxGenerator.class);

    private final boolean generateJson;

    public AbstractVertxGenerator() {
        this(true);
    }

    public AbstractVertxGenerator(boolean generateJson) {
        this.generateJson = generateJson;
        this.setGeneratePojos(true);
    }

    @Override
    protected void generateDaoClassFooter(TableDefinition table, JavaWriter out) {
        super.generateDaoClassFooter(table, out);
        generateFetchMethods(table,out);
        generateVertxGetterAndSetterConfigurationMethod(out);
        overwriteInsertReturningIfNecessary(table,out);
    }

    @Override
    protected void generatePojoClassFooter(TableDefinition table, JavaWriter out) {
        super.generatePojoClassFooter(table, out);
        if(generateJson){
            generateFromJsonConstructor(table,out);
            if(!generateInterfaces()){
                generateFromJson(table,out);
                generateToJson(table, out);
            }
        }
    }

    @Override
    protected void generateInterfaceClassFooter(TableDefinition table, JavaWriter out) {
        super.generateInterfaceClassFooter(table, out);
        if(generateJson && generateInterfaces()){
            generateFromJson(table, out);
            generateToJson(table, out);
        }
    }

    @Override
    protected void generateDao(TableDefinition table, JavaWriter out) {
        if(table.getPrimaryKey() != null){
            ((VertxJavaWriter)out).setDaoTypeReplacement(getKeyType(table.getPrimaryKey()));
        }
        super.generateDao(table, out);
    }

    @Override
    protected JavaWriter newJavaWriter(File file) {
        return new VertxJavaWriter(file, generateFullyQualifiedTypes(), targetEncoding);
    }


    @Override
    protected void printPackage(JavaWriter out, Definition definition, GeneratorStrategy.Mode mode) {
        super.printPackage(out, definition, mode);
        if(mode.equals(GeneratorStrategy.Mode.DAO)){
            generateDAOImports(out);
        }
    }

    protected abstract void generateDAOImports(JavaWriter out);

    /**
     * You might want to override this class in order to add injection methods.
     * @param out
     */
    protected void generateSetVertxAnnotation(JavaWriter out){};

    protected void generateVertxGetterAndSetterConfigurationMethod(JavaWriter out) {
        out.println();
        out.tab(1).println("private io.vertx.core.Vertx vertx;");
        out.println();
        generateSetVertxAnnotation(out);
        out.tab(1).println("@Override");
        out.tab(1).println("public void setVertx(io.vertx.core.Vertx vertx) {");
        out.tab(2).println("this.vertx = vertx;");
        out.tab(1).println("}");
        out.println();
        out.tab(1).println("@Override");
        out.tab(1).println("public io.vertx.core.Vertx vertx() {");
        out.tab(2).println("return this.vertx;");
        out.tab(1).println("}");
        out.println();
    }

    private void overwriteInsertReturningIfNecessary(TableDefinition table, JavaWriter out){
        Collection<ColumnDefinition> keyColumns = table.getPrimaryKey().getKeyColumns();
        boolean isSupported = keyColumns.size()==1;
        String reason = "More than one PK column";
        if(isSupported){
            isSupported = keyColumns.stream()
                            .map(c -> getJavaType(c.getType()))
                            .allMatch(t -> isType(t, Integer.class) || isType(t, Long.class));
            reason = isSupported ? "":"PK is not of type int or long";
        }
        if(!isSupported){
            logger.info(String.format("insertReturningPrimaryAsync is not supported for %s because '%s'!",table.getName(),reason));
            renderInsertReturningOverwrite(table, out, reason);
        }
    }

    protected abstract void renderInsertReturningOverwrite(TableDefinition table, JavaWriter out, String reason);

    /**
     * Overwrite this method to define the conversion of a column to a JSON name. Defaults to the name of the column.
     * @param columnDefinition
     * @return the name of this column as a JSON name
     */
    protected String getJsonName(ColumnDefinition columnDefinition){
        return columnDefinition.getName();
    }

    private void generateFromJson(TableDefinition table, JavaWriter out){
        out.println();
        String className = getStrategy().getJavaClassName(table, GeneratorStrategy.Mode.INTERFACE);
        out.tab(1).println("@Override");
        out.tab(1).println("default %s fromJson(io.vertx.core.json.JsonObject json) {",className);
        for (ColumnDefinition column : table.getColumns()) {
            String setter = getStrategy().getJavaSetterName(column, GeneratorStrategy.Mode.INTERFACE);
            String columnType = getJavaType(column.getType());
            String jsonName = getJsonName(column);
            if(handleCustomTypeFromJson(column, setter, columnType, jsonName, out)) {
                //handled by user
            }else if(isType(columnType, Integer.class)){
                out.tab(2).println("%s(json.getInteger(\"%s\"));", setter, jsonName);
            }else if(isType(columnType, Short.class)){
                out.tab(2).println("%s(json.getInteger(\"%s\")==null?null:json.getInteger(\"%s\").shortValue());", setter, jsonName, jsonName);
            }else if(isType(columnType, Byte.class)){
                out.tab(2).println("%s(json.getInteger(\"%s\")==null?null:json.getInteger(\"%s\").byteValue());", setter,jsonName, jsonName);
            }else if(isType(columnType, Long.class)){
                out.tab(2).println("%s(json.getLong(\"%s\"));", setter, jsonName);
            }else if(isType(columnType, Float.class)){
                out.tab(2).println("%s(json.getFloat(\"%s\"));", setter, jsonName);
            }else if(isType(columnType, Double.class)){
                out.tab(2).println("%s(json.getDouble(\"%s\"));", setter, jsonName);
            }else if(isType(columnType, Boolean.class)){
                out.tab(2).println("%s(json.getBoolean(\"%s\"));", setter, jsonName);
            }else if(isType(columnType, String.class)){
                out.tab(2).println("%s(json.getString(\"%s\"));", setter, jsonName);
            }else if(columnType.equals(byte.class.getName()+"[]")){
                out.tab(2).println("%s(json.getBinary(\"%s\"));", setter, jsonName);
            }else if(isType(columnType,Instant.class)){
                out.tab(2).println("%s(json.getInstant(\"%s\"));", setter, jsonName);
            }else if(isEnum(table, column)) {
                out.tab(2).println("%s(java.util.Arrays.stream(%s.values()).filter(td -> td.getLiteral().equals(json.getString(\"%s\"))).findFirst().orElse(null));", setter, columnType, jsonName);
            }else if(column.getType().getConverter() != null && isType(column.getType().getConverter(),JsonObjectConverter.class)){
                out.tab(2).println("%s(json.getJsonObject(\"%s\"));", setter, jsonName);
            }else if(column.getType().getConverter() != null && isType(column.getType().getConverter(),JsonArrayConverter.class)){
                out.tab(2).println("%s(json.getJsonArray(\"%s\"));", setter, jsonName);
            }else{
                logger.warn(String.format("Omitting unrecognized type %s for column %s in table %s!",columnType,column.getName(),table.getName()));
                out.tab(2).println(String.format("// Omitting unrecognized type %s for column %s!",columnType,column.getName()));
            }
        }
        out.tab(2).println("return this;");
        out.tab(1).println("}");
        out.println();
    }

    protected void generateJsonMapper(TableDefinition table, JavaWriter out){
//        return json->
//            new generated.classic.async.vertx.tables.pojos.Something()
//                    .setSomeid(json.getInteger("someId"))
//                    .setSomejsonobject(JsonObjectConverter.getInstance().from(json.getString("someJsonObject")))
//                    ;
        out.tab(1).println("@Override");

        out.tab(1).println("public java.util.function.Function<%s, %s> jsonMapper() {", JsonObject.class.getName(),getStrategy().getFullJavaClassName(table, GeneratorStrategy.Mode.POJO));
        out.tab(2).println("return json -> ");
        out.tab(3).println("new %s()",getStrategy().getFullJavaClassName(table, GeneratorStrategy.Mode.POJO));
        for (ColumnDefinition column : table.getColumns()) {
            String setter = getStrategy().getJavaSetterName(column, GeneratorStrategy.Mode.INTERFACE);
            String columnType = getJavaType(column.getType());
            String jsonName = getJsonName(column);
            if(handleCustomTypeJsonMapper(column, setter, columnType, jsonName, out)) {
                //handled by user
            }else if(isType(columnType, Integer.class)){
                out.tab(5).println(".%s(json.getInteger(\"%s\"))", setter, jsonName);
            }else if(isType(columnType, Short.class)){
                out.tab(5).println(".%s(json.getInteger(\"%s\")==null?null:json.getInteger(\"%s\").shortValue())", setter, jsonName, jsonName);
            }else if(isType(columnType, Byte.class)){
                out.tab(5).println(".%s(json.getInteger(\"%s\")==null?null:json.getInteger(\"%s\").byteValue())", setter,jsonName, jsonName);
            }else if(isType(columnType, Long.class)){
                out.tab(5).println(".%s(json.getLong(\"%s\"))", setter, jsonName);
            }else if(isType(columnType, Float.class)){
                out.tab(5).println(".%s(json.getFloat(\"%s\"))", setter, jsonName);
            }else if(isType(columnType, Double.class)){
                out.tab(5).println(".%s(json.getDouble(\"%s\"))", setter, jsonName);
            }else if(isType(columnType, Boolean.class)){
                out.tab(5).println(".%s(json.getBoolean(\"%s\"))", setter, jsonName);
            }else if(isType(columnType, String.class)){
                out.tab(5).println(".%s(json.getString(\"%s\"))", setter, jsonName);
            }else if(columnType.equals(byte.class.getName()+"[]")){
                out.tab(5).println(".%s(json.getBinary(\"%s\"))", setter, jsonName);
            }else if(isType(columnType,Instant.class)){
                out.tab(5).println(".%s(json.getInstant(\"%s\"))", setter, jsonName);
            }else if(isEnum(table, column)) {
                out.tab(5).println(".%s(java.util.Arrays.stream(%s.values()).filter(td -> td.getLiteral().equals(json.getString(\"%s\"))).findFirst().orElse(null))", setter, columnType, jsonName);
            }else if(column.getType().getConverter() != null && isType(column.getType().getConverter(),JsonObjectConverter.class)){
                out.tab(5).println(".%s(%s.getInstance().from(json.getString(\"%s\")))", setter, JsonObjectConverter.class.getName(), jsonName);
            }else if(column.getType().getConverter() != null && isType(column.getType().getConverter(),JsonArrayConverter.class)){
                out.tab(5).println(".%s(%s.getInstance().from(json.getString(\"%s\")))", setter, JsonArrayConverter.class.getName(), jsonName);
            }else{
                logger.warn(String.format("Omitting unrecognized type %s for column %s in table %s!",columnType,column.getName(),table.getName()));
                out.tab(5).println(String.format("// Omitting unrecognized type %s for column %s!",columnType,column.getName()));
            }
        }
        out.tab(5).println(";");
        out.tab(1).println("}");
        out.println();
    }

    private boolean isEnum(TableDefinition table, TypedElementDefinition<?> column) {
        return table.getDatabase().getEnum(table.getSchema(), column.getType().getUserType()) != null;
    }

    private boolean isType(String columnType, Class<?> clazz) {
        return columnType.equals(clazz.getName());
    }

    /**
     * Overwrite this method to handle your custom type. This is needed especially when you have custom converters.
     * @param column the column definition
     * @param setter the setter name
     * @param columnType the type of the column
     * @param javaMemberName the java member name
     * @param out the writer
     * @return <code>true</code> if the column was handled.
     * @see #generateFromJson(TableDefinition, JavaWriter)
     */
    protected boolean handleCustomTypeFromJson(ColumnDefinition column, String setter, String columnType, String javaMemberName, JavaWriter out){
        return false;
    }

    protected boolean handleCustomTypeJsonMapper(ColumnDefinition column, String setter, String columnType, String javaMemberName, JavaWriter out){
        return false;
    }

    private void generateToJson(TableDefinition table, JavaWriter out){
        out.println();
        out.tab(1).println("@Override");
        out.tab(1).println("default io.vertx.core.json.JsonObject toJson() {");
        out.tab(2).println("io.vertx.core.json.JsonObject json = new io.vertx.core.json.JsonObject();");
        for (ColumnDefinition column : table.getColumns()) {
            String getter = getStrategy().getJavaGetterName(column, GeneratorStrategy.Mode.INTERFACE);
            String columnType = getJavaType(column.getType());
            if(handleCustomTypeToJson(column,getter,getJavaType(column.getType()),getStrategy().getJavaMemberName(column, GeneratorStrategy.Mode.POJO), out)) {
                //handled by user
            }else if(isEnum(table, column)){
                out.tab(2).println("json.put(\"%s\",%s()==null?null:%s().getLiteral());", getJsonName(column),getter,getter);
            } else if (isAllowedJsonType(column, columnType)){
                out.tab(2).println("json.put(\"%s\",%s());", getJsonName(column),getter);
            }else{
                logger.warn(String.format("Omitting unrecognized type %s for column %s in table %s!",columnType,column.getName(),table.getName()));
                out.tab(2).println(String.format("// Omitting unrecognized type %s for column %s!",columnType,column.getName()));
            }
        }
        out.tab(2).println("return json;");
        out.tab(1).println("}");
        out.println();
    }

    private boolean isAllowedJsonType(TypedElementDefinition<?> column, String columnType){
        return isType(columnType, Integer.class) || isType(columnType, Short.class) || isType(columnType, Byte.class) ||
                isType(columnType, Long.class) || isType(columnType,Float.class) || isType(columnType, Double.class) ||
                isType(columnType, Boolean.class) || isType(columnType,String.class) || isType(columnType, Instant.class) ||
                columnType.equals(byte.class.getName()+"[]") || (column.getType().getConverter() != null &&
                (isType(column.getType().getConverter(),JsonObjectConverter.class) || isType(column.getType().getConverter(),JsonArrayConverter.class)))
                ;
    }


    /**
     * Overwrite this method to handle your custom type. This is needed especially when you have custom converters.
     * @param column the column definition
     * @param getter the getter name
     * @param columnType the type of the column
     * @param javaMemberName the java member name
     * @param out the writer
     * @return <code>true</code> if the column was handled.
     * @see #generateToJson(TableDefinition, JavaWriter)
     */
    protected boolean handleCustomTypeToJson(ColumnDefinition column, String getter, String columnType, String javaMemberName, JavaWriter out) {
        return false;
    }

    private void generateFromJsonConstructor(TableDefinition table, JavaWriter out){
        final String className = getStrategy().getJavaClassName(table, GeneratorStrategy.Mode.POJO);
        out.println();
        out.tab(1).println("public %s(io.vertx.core.json.JsonObject json) {", className);
        out.tab(2).println("fromJson(json);");
        out.tab(1).println("}");
    }

    /**
     * Copied (more ore less) from JavaGenerator.
     * Generates fetchByCYZAsync- and fetchOneByCYZAsync-methods
     * @param table
     * @param out
     */
    protected void generateFetchMethods(TableDefinition table, JavaWriter out){
        VertxJavaWriter vOut = (VertxJavaWriter) out;
        String pType = vOut.ref(getStrategy().getFullJavaClassName(table, GeneratorStrategy.Mode.POJO));
        for (ColumnDefinition column : table.getColumns()) {
            final String colName = column.getOutputName();
            final String colClass = getStrategy().getJavaClassName(column);
            final String colType = vOut.ref(getJavaType(column.getType()));
            final String colIdentifier = vOut.ref(getStrategy().getFullJavaIdentifier(column), colRefSegments(column));

            // fetchBy[Column]([T]...)
            // -----------------------

            generateFetchByMethods(out, pType, colName, colClass, colType, colIdentifier);

            // fetchOneBy[Column]([T])
            // -----------------------
            ukLoop:
            for (UniqueKeyDefinition uk : column.getUniqueKeys()) {

                // If column is part of a single-column unique key...
                if (uk.getKeyColumns().size() == 1 && uk.getKeyColumns().get(0).equals(column)) {
                    generateFetchOneByMethods(out, pType, colName, colClass, colType, colIdentifier);
                    break ukLoop;
                }
            }
        }
    }

    protected abstract void generateFetchOneByMethods(JavaWriter out, String pType, String colName, String colClass, String colType, String colIdentifier) ;

    protected abstract void generateFetchByMethods(JavaWriter out, String pType, String colName, String colClass, String colType, String colIdentifier) ;

    /**
     * Copied from JavaGenerator
     * @param key
     * @return
     */
    protected String getKeyType(UniqueKeyDefinition key){
        String tType;

        List<ColumnDefinition> keyColumns = key.getKeyColumns();

        if (keyColumns.size() == 1) {
            tType = getJavaType(keyColumns.get(0).getType());
        }
        else if (keyColumns.size() <= Constants.MAX_ROW_DEGREE) {
            String generics = "";
            String separator = "";

            for (ColumnDefinition column : keyColumns) {
                generics += separator + (getJavaType(column.getType()));
                separator = ", ";
            }

            tType = Record.class.getName() + keyColumns.size() + "<" + generics + ">";
        }
        else {
            tType = Record.class.getName();
        }

        return tType;
    }

    /**
     * Copied from JavaGenerator
     * @param column
     * @return
     */
    private int colRefSegments(TypedElementDefinition<?> column) {
        if (column != null && column.getContainer() instanceof UDTDefinition)
            return 2;

        if (!getStrategy().getInstanceFields())
            return 2;

        return 3;
    }


}
