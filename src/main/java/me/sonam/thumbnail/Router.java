package me.sonam.thumbnail;

import me.sonam.thumbnail.handler.S3Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

/**
 * Set Email route
 */
@Configuration
public class Router {
    private static final Logger LOG = LoggerFactory.getLogger(Router.class);

    @Bean
    public RouterFunction<ServerResponse> route(S3Handler handler) {
        LOG.info("building router function {}", handler);
        return RouterFunctions.route(POST("/videothumbnail").and(accept(MediaType.APPLICATION_JSON)),
                        handler::createVideoThumbnail);
    }
}
