package io.github.jklingsporn.vertx.jooq.async.generate.rx;

import io.github.jklingsporn.vertx.jooq.async.generate.VertxGeneratorStrategy;

public class RXAsyncGeneratorStrategy extends VertxGeneratorStrategy {

    public RXAsyncGeneratorStrategy() {
        super(RXAsyncVertxGenerator.VERTX_DAO_NAME);
    }
}
