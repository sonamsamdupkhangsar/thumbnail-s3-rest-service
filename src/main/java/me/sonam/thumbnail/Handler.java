package me.sonam.thumbnail;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler
 */
@Component
public class Handler {

    /**
     * outline only
     */
    public Mono<ServerResponse> handle(ServerRequest serverRequest) {
        return ServerResponse.ok().build();
    }
}
