package me.sonam.thumbnail;

import me.sonam.thumbnail.handler.S3Handler;
import me.sonam.thumbnail.handler.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.reactive.function.server.support.ServerRequestWrapper;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class CreateVideoThumbnailMockRestServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(CreateVideoThumbnailMockRestServiceTest.class);

    private final ServerRequest mockServerRequest = mock(ServerRequest.class);
    private final ServerRequestWrapper mockServerRequestWrapper = new ServerRequestWrapper(
            mockServerRequest);

    private WebTestClient webTestClient;

    @InjectMocks
    private S3Handler s3Handler;

    @Mock
    private S3Service s3Service;

    @BeforeEach
    public void setUp() {
        LOG.info("setup mock");
        MockitoAnnotations.openMocks(this);

        RouterFunction<ServerResponse> routerFunction = RouterFunctions
                .route(RequestPredicates.POST("/videothumbnail"),
                        s3Handler::createVideoThumbnail);
        webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
    }

    @Test
    public void uploadVideoAndThumbnail() throws IOException, InterruptedException {
        when(s3Service.createVideoThumbnail(Mockito.any())).thenReturn(Mono.just("thumbkey"));

        LOG.info("call thumbnail api");
        webTestClient.post().uri("/videothumbnail")
                .bodyValue("filekey")
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
    }

}
