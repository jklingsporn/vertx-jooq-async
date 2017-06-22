package io.github.jklingsporn.vertx.jooq.async.generate.classic;

import io.github.jklingsporn.vertx.jooq.async.generate.VertxGeneratorStrategy;

/**
 * Created by jensklingsporn on 25.10.16.
 *
 * We need this class to let the DAOs implements <code>VertxDAO</code>.
 * Unfortunately we can not get the type easily, that's why we have to
 * set the placeholder.
 */
public class ClassicAsyncGeneratorStrategy extends VertxGeneratorStrategy {

    public ClassicAsyncGeneratorStrategy() {
        super(ClassicAsyncVertxGenerator.VERTX_DAO_NAME);
    }
}
