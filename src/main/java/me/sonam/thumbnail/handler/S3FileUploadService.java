package me.sonam.thumbnail.handler;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import me.sonam.thumbnail.config.S3ClientConfigurationProperties;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handler
 */
@Component
public class S3FileUploadService implements S3Service {
    private static final Logger LOG = LoggerFactory.getLogger(S3FileUploadService.class);

    @Autowired
    private S3AsyncClient s3client;

    @Autowired
    private S3ClientConfigurationProperties s3config;

    @Override
    public Mono<String> createVideoThumbnail(Mono<String> presignUrl) {
        LOG.info("Create thumbnail for presignUrl: {}", presignUrl);
        LocalDateTime localDateTime = LocalDateTime.now();


        Mono<ByteArrayOutputStream> byteArrayOutputStreamMono = createThumbnail(presignUrl, "png");

       return byteArrayOutputStreamMono.flatMap(byteArrayOutputStream -> {
           LOG.info("create ByteBuffer");
           byte[] bytes = byteArrayOutputStream.toByteArray();

           ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

           String thumbKey = s3config.getVideoPath() + "thumbnail/" + localDateTime + "." + "png";
           Map<String, String> metadata2 = new HashMap<>();
           metadata2.put("Content-Length", "" + bytes.length);
           metadata2.put("Content-Type", "image/png");
           metadata2.put("x-amz-acl", "public-read");

           LOG.info("saving thumbnail with key: {}", thumbKey);
           CompletableFuture future = s3client
                   .putObject(PutObjectRequest.builder()
                                   .bucket(s3config.getBucket())
                                   .contentLength((long) bytes.length)
                                   .key(thumbKey)
                                   .contentType("image/png")
                                   .metadata(metadata2)
                                   .acl(ObjectCannedACL.PUBLIC_READ)
                                   .build(),
                           AsyncRequestBody.fromPublisher(Flux.just(byteBuffer)));

            return Mono.fromFuture(future).map(response -> {
                checkResult(response);

                LOG.info("checked thumbnail response and returning presignUrl: {}", response.toString());

                return thumbKey;
            });
        });
    }

    @Override
    public Mono<String> createGif(String presignUrl) {
        try {
            InputStream inputStream = new URL(s3config.getSubdomain() + presignUrl).openStream();
            File tempFile = File.createTempFile(UUID.randomUUID().toString(), ".gif");
            getGifBytes(inputStream, 0, 2, 10, 2, new FileOutputStream(tempFile));

            LocalDateTime localDateTime = LocalDateTime.now();

            String gifKey = s3config.getVideoPath() + "gif/" + localDateTime + "." + "gif";
            Map<String, String> metadata2 = new HashMap<>();
            metadata2.put("Content-Length", "" + tempFile.length());
            metadata2.put("Content-Type", "image/gif");
            metadata2.put("x-amz-acl", "public-read");

            LOG.info("saving thumbnail with key: {}", gifKey);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);


            CompletableFuture future = s3client
                    .putObject(PutObjectRequest.builder()
                                    .bucket(s3config.getBucket())
                                    .contentLength(tempFile.length())
                                    .key(gifKey)
                                    .contentType("image/gif")
                                    .metadata(metadata2)
                                    .acl(ObjectCannedACL.PUBLIC_READ)
                                    .build(),
                           tempFile.toPath());
            return Mono.fromFuture(future).map(response -> {
                checkResult(response);

                LOG.info("checked gifKey response and returning gifKey: {}", response.toString());

                return gifKey;
            });

        }
        catch (Exception e) {
            LOG.error("exception occured", e);
            return Mono.just(e.getLocalizedMessage());
        }
    }


    private PutObjectResponse checkResult(Object result1) {
        PutObjectResponse result = (PutObjectResponse) result1;
        LOG.info("response.sdkHttpResponse: {}", result.sdkHttpResponse().isSuccessful());

        if (result.sdkHttpResponse() == null || !result.sdkHttpResponse().isSuccessful()) {
            LOG.error("response is un successful");
            throw new RuntimeException("sdkHttpResponse fail");
        }
        return result;
    }

    public Mono<ByteArrayOutputStream> createThumbnail(Mono<String> presignUrlMono, String imageFormat)  {

        return presignUrlMono.map(presignUrl -> {
            try {
                InputStream inputStream = new URL(presignUrl).openStream();
                return getThumbnailBytes(inputStream, imageFormat);
            } catch (Exception e) {
                LOG.error("exception occured", e);
                return null;
            }
        });
    }

    private ByteArrayOutputStream getThumbnailBytes(InputStream inputStream, String imageFormat) {
        try {
            LOG.info("getting ByteArrayOutputStream for inpustreamm imageFormat: {}", imageFormat);

            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(inputStream);
            frameGrabber.start();
            Java2DFrameConverter fc = new Java2DFrameConverter();
            Frame frame = frameGrabber.grabKeyFrame();
            LOG.info("frame: {}", frame);

            BufferedImage bufferedImage = fc.convert(frame);
            LOG.info("bufferedImage: {}", bufferedImage);

            int i = 0;
            if (bufferedImage != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, imageFormat, baos);

                frame = frameGrabber.grabKeyFrame();
                bufferedImage = fc.convert(frame);

                frameGrabber.stop();
                frameGrabber.close();

                LOG.info("i: {}, bytearray.length: {}", i++, baos.toByteArray().length);
                return baos;

            }
            else {
                frameGrabber.stop();
                frameGrabber.close();
            }

            LOG.info("thumbnail done");
        } catch (Exception e) {
            LOG.error("failed to create thumbnail for video", e);
        }

        return null;
    }


    private void getGifBytes(InputStream inputStream, int startFrame, int frameCount, Integer frameRate, Integer margin, OutputStream outputStream) {
        try {
            LOG.info("create gif");

            FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(inputStream);
            frameGrabber.start();
            Java2DFrameConverter fc = new Java2DFrameConverter();

            Integer videoLength = frameGrabber.getLengthInFrames();
            // If the user uploads the video to be extremely short and does not meet the value interval defined by the user, the acquisition starts at 1/5 and ends at 1/2
            if (startFrame > videoLength || (startFrame + frameCount) > videoLength) {
                startFrame = videoLength / 5;
                frameCount = videoLength / 2;
            }
            LOG.info("startFrame: {}, frameRate: {}", startFrame, frameRate);

            frameGrabber.setFrameNumber(startFrame);
            AnimatedGifEncoder en = new AnimatedGifEncoder();
            en.setFrameRate(frameRate);
            en.start(outputStream);

           for (int i = 0; i < frameCount; i++) {
                Frame frame = frameGrabber.grabFrame(false, true, true, false);
                LOG.info("frame: {}", frame);

                BufferedImage bufferedImage = fc.convert(frame);
                LOG.info("bufferedImage: {}", bufferedImage);

                if (bufferedImage != null) {
                    en.addFrame(bufferedImage);
                    frameGrabber.setFrameNumber(frameGrabber.getFrameNumber() + margin);

                }
            }
            en.finish();

            frameGrabber.stop();
            frameGrabber.close();

        } catch (Exception e) {
            LOG.error("failed to create gif for video", e);
        }
    }


}
