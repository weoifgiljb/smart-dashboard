package com.selfdiscipline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfdiscipline.config.OpenAIConfig;
import com.selfdiscipline.exception.ImageGenerationException;
import com.selfdiscipline.model.Book;
import com.selfdiscipline.model.User;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.BookRepository;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private OpenAIConfig openAIConfig;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Qualifier("openAIWebClient")
    private WebClient webClient;

    @Autowired
    private ImageRateLimiter imageRateLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Book generateBookCover(String bookId) {
        return generateBookCover(bookId, false);
    }

    public Book generateBookCover(String bookId, boolean force) {
        imageRateLimiter.consumeOrThrow("book:" + bookId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("书籍不存在"));
        if (!force && book.getCover() != null && !book.getCover().isBlank()) {
            return book;
        }
        String prompt = PromptTemplates.buildBookCoverPrompt(book);
        long start = System.currentTimeMillis();
        String image = generateImage(prompt);
        long cost = System.currentTimeMillis() - start;
        try {
            String promptHash = Integer.toHexString(prompt.hashCode());
            String model = (openAIConfig.getImageModel() == null || openAIConfig.getImageModel().isBlank()) ? "dall-e-3" : openAIConfig.getImageModel();
            String size = (openAIConfig.getImageSize() == null || openAIConfig.getImageSize().isBlank()) ? "512x512" : openAIConfig.getImageSize();
            log.info("bookCover generated bookId={}, promptHash={}, model={}, size={}, costMs={}, resultType={}",
                    bookId, promptHash, model, size, cost, (image != null && image.startsWith("http")) ? "url" : "base64");
        } catch (Exception ignore) {}
        if (image == null || image.isBlank()) {
            throw new ImageGenerationException(ImageGenerationException.ErrorType.UPSTREAM_5XX, "图像生成失败");
        }
        book.setCover(image);
        return bookRepository.save(book);
    }

    public Word generateWordImage(String username, String wordId) {
        return generateWordImage(username, wordId, false);
    }

    public Word generateWordImage(String username, String wordId, boolean force) {
        imageRateLimiter.consumeOrThrow("user:" + username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        String currentUserId = user.getId();
        String ownerId = word.getUserId();
        if (ownerId == null || ownerId.isBlank()) {
            // 兼容旧数据：无 owner 时认领到当前用户
            word.setUserId(currentUserId);
            wordRepository.save(word);
        } else if (!currentUserId.equals(ownerId)) {
            throw new RuntimeException("无权为该单词生成配图");
        }
        if (!force && word.getImage() != null && !word.getImage().isBlank()) {
            return word;
        }
        String prompt = PromptTemplates.buildWordImagePrompt(word);
        long start = System.currentTimeMillis();
        String image = generateImage(prompt);
        long cost = System.currentTimeMillis() - start;
        try {
            String promptHash = Integer.toHexString(prompt.hashCode());
            String model = (openAIConfig.getImageModel() == null || openAIConfig.getImageModel().isBlank()) ? "dall-e-3" : openAIConfig.getImageModel();
            String size = (openAIConfig.getImageSize() == null || openAIConfig.getImageSize().isBlank()) ? "512x512" : openAIConfig.getImageSize();
            log.info("wordImage generated username={}, wordId={}, promptHash={}, model={}, size={}, costMs={}, resultType={}",
                    username, wordId, promptHash, model, size, cost, (image != null && image.startsWith("http")) ? "url" : "base64");
        } catch (Exception ignore) {}
        if (image == null || image.isBlank()) {
            throw new ImageGenerationException(ImageGenerationException.ErrorType.UPSTREAM_5XX, "图像生成失败");
        }
        word.setImage(image);
        return wordRepository.save(word);
    }

    private String generateImage(String prompt) {
        try {
            String key = openAIConfig.getApiKey();
            String model = openAIConfig.getImageModel();
            String size = openAIConfig.getImageSize();
            if (key == null || key.isBlank()) {
                throw new ImageGenerationException(ImageGenerationException.ErrorType.CONFIG_MISSING,
                        "OPENAI_API_KEY 未配置，请在 backend\\env.bat 中设置并通过 start-backend.bat 启动");
            }
            Map<String, Object> body = new HashMap<>();
            body.put("model", (model == null || model.isBlank()) ? "dall-e-3" : model);
            body.put("prompt", prompt);
            body.put("size", (size == null || size.isBlank()) ? "512x512" : size);
            body.put("n", 1);
            // 强制返回 base64，避免外链图片在某些网络环境下无法加载
            body.put("response_format", "b64_json");

            String response = webClient.post()
                    .uri("/images/generations")
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 401, resp ->
                            resp.bodyToMono(String.class).map(err ->
                                    new ImageGenerationException(ImageGenerationException.ErrorType.UNAUTHORIZED, "上游未授权(401): " + err, 401)))
                    .onStatus(status -> status.value() == 403, resp ->
                            resp.bodyToMono(String.class).map(err ->
                                    new ImageGenerationException(ImageGenerationException.ErrorType.FORBIDDEN, "上游拒绝访问(403): " + err, 403)))
                    .onStatus(status -> status.value() == 429, resp ->
                            resp.bodyToMono(String.class).map(err ->
                                    new ImageGenerationException(ImageGenerationException.ErrorType.RATE_LIMITED, "生成频率过高，请稍后再试", 429)))
                    .onStatus(status -> status.is4xxClientError(), resp ->
                            resp.bodyToMono(String.class).map(err ->
                                    new ImageGenerationException(ImageGenerationException.ErrorType.UPSTREAM_4XX, "图像生成请求不被接受: " + err, resp.statusCode().value())))
                    .onStatus(status -> status.is5xxServerError(), resp ->
                            resp.bodyToMono(String.class).map(err ->
                                    new ImageGenerationException(ImageGenerationException.ErrorType.UPSTREAM_5XX, "图像生成服务异常: " + err, resp.statusCode().value())))
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            if (data.isArray() && data.size() > 0) {
                JsonNode first = data.get(0);
                String url = first.path("url").asText(null);
                if (url != null && !url.isBlank()) {
                    return url;
                }
                String b64 = first.path("b64_json").asText(null);
                if (b64 != null && !b64.isBlank()) {
                    return "data:image/png;base64," + b64;
                }
            }
            return null;
        } catch (WebClientResponseException e) {
            int sc = e.getStatusCode().value();
            if (sc == 429) {
                throw new ImageGenerationException(ImageGenerationException.ErrorType.RATE_LIMITED, "生成频率过高，请稍后再试", sc);
            } else if (sc >= 500) {
                throw new ImageGenerationException(ImageGenerationException.ErrorType.UPSTREAM_5XX, "图像生成服务异常: " + e.getResponseBodyAsString(), sc);
            } else if (sc >= 400) {
                throw new ImageGenerationException(ImageGenerationException.ErrorType.UPSTREAM_4XX, "图像生成请求不被接受: " + e.getResponseBodyAsString(), sc);
            } else {
                throw new ImageGenerationException(ImageGenerationException.ErrorType.UPSTREAM_5XX, "图像生成失败（上游返回 " + sc + "）: " + e.getResponseBodyAsString(), sc);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.toLowerCase().contains("timeout")) {
                throw new ImageGenerationException(ImageGenerationException.ErrorType.TIMEOUT, "图像生成超时，请稍后重试");
            }
            throw new ImageGenerationException(ImageGenerationException.ErrorType.PARSING, "图像生成失败：" + msg);
        }
    }

}


