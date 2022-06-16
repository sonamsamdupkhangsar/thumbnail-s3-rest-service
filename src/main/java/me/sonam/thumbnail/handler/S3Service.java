package me.sonam.thumbnail.handler;

import reactor.core.publisher.Mono;

public interface S3Service {
    Mono<String> createVideoThumbnail(Mono<String> fileKey);
    //this does not work yet, creates the file on s3 but nothing is in it
    Mono<String> createGif(String fileKey);
}
