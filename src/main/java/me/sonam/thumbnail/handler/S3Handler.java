package me.sonam.thumbnail.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
public class S3Handler {
    private static final Logger LOG = LoggerFactory.getLogger(S3Handler.class);

    @Autowired
    private S3Service s3Service;

    /**
     * this method will upload the video as s3 object.
     * Then it will create a thumbnail for that video using the key from uploaded video.
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> createVideoThumbnail(ServerRequest serverRequest) {
        LOG.info("upload video and create thumbnail");

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(s3Service.createVideoThumbnail(serverRequest.bodyToMono(String.class)), String.class)
                .onErrorResume(e -> ServerResponse.badRequest().body(BodyInserters
                        .fromValue(e.getMessage())));
    }

}
