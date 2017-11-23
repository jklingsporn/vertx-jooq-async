package io.github.jklingsporn.vertx.jooq.async.rx.util;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;

import java.util.List;
import java.util.function.Function;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class RXTool {
    private RXTool() {
    }


    public static <T> Single<T> executeBlocking(Handler<Future<T>> blockingCodeHandler, Vertx
        vertx) {
        return vertx.rxExecuteBlocking(blockingCodeHandler);
    }

    public static <T> Observable<T> executeBlockingObservable(Handler<Future<List<T>>> blockingCodeHandler, Vertx
        vertx) {
        return executeBlocking(blockingCodeHandler,vertx)
            .flatMapObservable(Observable::fromIterable);
    }



    public static <T> Single<T> failure(Throwable e) {
        return Single.error(e);
    }

    public static <T,R> io.reactivex.functions.Function<T,R> toFunction(Function<T,R> f){
        return f::apply;
    }

}
