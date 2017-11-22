/*
 * This file is generated by jOOQ.
*/
package generated.rx.async.vertx;


import generated.rx.async.vertx.tables.Something;
import generated.rx.async.vertx.tables.Somethingcomposite;
import generated.rx.async.vertx.tables.Somethingwithoutjson;

import javax.annotation.Generated;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.AbstractKeys;


/**
 * A class modelling indexes of tables of the <code></code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index SOMETHING_PRIMARY = Indexes0.SOMETHING_PRIMARY;
    public static final Index SOMETHINGCOMPOSITE_PRIMARY = Indexes0.SOMETHINGCOMPOSITE_PRIMARY;
    public static final Index SOMETHINGWITHOUTJSON_PRIMARY = Indexes0.SOMETHINGWITHOUTJSON_PRIMARY;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Indexes0 extends AbstractKeys {
        public static Index SOMETHING_PRIMARY = createIndex("PRIMARY", Something.SOMETHING, new OrderField[] { Something.SOMETHING.SOMEID }, true);
        public static Index SOMETHINGCOMPOSITE_PRIMARY = createIndex("PRIMARY", Somethingcomposite.SOMETHINGCOMPOSITE, new OrderField[] { Somethingcomposite.SOMETHINGCOMPOSITE.SOMEID, Somethingcomposite.SOMETHINGCOMPOSITE.SOMESECONDID }, true);
        public static Index SOMETHINGWITHOUTJSON_PRIMARY = createIndex("PRIMARY", Somethingwithoutjson.SOMETHINGWITHOUTJSON, new OrderField[] { Somethingwithoutjson.SOMETHINGWITHOUTJSON.SOMEID }, true);
    }
}
