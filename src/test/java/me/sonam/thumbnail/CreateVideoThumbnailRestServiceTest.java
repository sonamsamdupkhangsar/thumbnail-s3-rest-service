package me.sonam.thumbnail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * this test will upload file to s3 bucket using the router
 */
/*@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest( webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)*/
public class CreateVideoThumbnailRestServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(CreateVideoThumbnailRestServiceTest.class);

    @Autowired
    private WebTestClient client;

    @Value("classpath:mydogsleeping.mp4")
    private Resource video;

    @Value("${s3-rest-service-endpoint}")
    private String s3RestEndpoint;

  @Test
    public void hello() {
        assertThat(true).isTrue();
        LOG.info("hello");
    }

    public void uploadVideoFile() throws IOException, InterruptedException {
        LOG.info("s3RestEndpoint: {}", s3RestEndpoint);
        assertThat(video).isNotNull();

        client = client.mutate().responseTimeout(Duration.ofSeconds(80)).build();
        /*WebClient.ResponseSpec responseSpec = */
        EntityExchangeResult<String> result = client.post().uri(s3RestEndpoint)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(""))
                .header("filename", video.getFilename())
                .header("format", "video/mp4")
                .header(HttpHeaders.CONTENT_LENGTH, ""+video.contentLength())
                .bodyValue(video).exchange().expectBody(String.class).returnResult();

        LOG.info("reult: {}", result.getResponseBody());
        final String fileKey = result.getResponseBody();

        final String presignUrlEndpoint = "https://s3-rest-service.sonam.cloud/presignurl";
        LOG.info("create presign url");
        result = client.post().uri(presignUrlEndpoint)
        . headers(httpHeaders -> httpHeaders.setBearerAuth(""))
                .bodyValue(fileKey)
                    .exchange().expectStatus().isOk()
                    .expectBody(String.class).returnResult();

        final String presignUrl = result.getResponseBody();
        LOG.info("presignUrl: {}", presignUrl);
    }

    //@Test
    public void uploadVideoAndThumbnail() throws IOException, InterruptedException {
        final String presignUrl = "";

        LOG.info("presignUrl: {}", presignUrl);

        client = client.mutate().responseTimeout(Duration.ofSeconds(30)).build();

        client.post().uri("/videothumbnail")
                .bodyValue(presignUrl)
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
    }

    public void uploadVideoAndCreateGif() throws IOException, InterruptedException {
        LOG.info("video: {}", video);
        assertThat(video).isNotNull();
        LOG.info("video contentLength: {}, video: {}", video.contentLength(), video);
        assertThat(video.getFile().exists()).isTrue();

        client = client.mutate().responseTimeout(Duration.ofSeconds(80)).build();

        client.post().uri("/upload/video/gif")
                .header("filename", video.getFilename())
                .header("format", "video/mp4")
                .header(HttpHeaders.CONTENT_LENGTH, ""+video.contentLength())
                .bodyValue(video)
                .exchange().expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> LOG.info("result: {}", stringEntityExchangeResult.getResponseBody()));
    }

}
