package com.selfdiscipline.service;

import com.selfdiscipline.dto.WordRequest;
import com.selfdiscipline.dto.WordImportRequest;
import com.selfdiscipline.dto.WordStatusRequest;
import com.selfdiscipline.model.User;
import com.selfdiscipline.model.Word;
import com.selfdiscipline.repository.UserRepository;
import com.selfdiscipline.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import java.time.Duration;

@Service
public class WordService {

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Word> getWords(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return wordRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
    }

    public Word addWord(String username, WordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Word word = new Word();
        word.setUserId(user.getId());
        word.setWord(request.getWord());
        word.setTranslation(request.getTranslation());
        word.setExample(request.getExample());
        return wordRepository.save(word);
    }

    public void deleteWord(String username, @NonNull String wordId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));

        String wordUserId = word.getUserId();
        String userId = user.getId();
        if (wordUserId == null || userId == null || !wordUserId.equals(userId)) {
            throw new RuntimeException("无权删除此单词");
        }

        wordRepository.delete(word);
    }

    public Word reviewWord(String username, @NonNull String wordId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));

        String wordUserId = word.getUserId();
        String userId = user.getId();
        if (wordUserId == null || userId == null || !wordUserId.equals(userId)) {
            throw new RuntimeException("无权复习此单词");
        }

        // 成功复习一次，进入下一阶段并根据艾宾浩斯间隔设置下次 dueDate
        applySuccessfulReviewSchedule(word);
        return wordRepository.save(word);
    }

    public int getWordCount(String userId) {
        // 仅统计已掌握（done）的单词数，避免把刚导入的待学单词计入
        return (int) wordRepository.countByUserIdAndStatus(userId, "done");
    }

    public List<Word> getTodayWords(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);
        try {
            return wordRepository.findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
                    user.getId(), "done", endOfToday
            );
        } catch (Exception e) {
            // 兜底：如果派生查询报错，则退化为全量加载后在内存中过滤
            List<Word> all = wordRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
            return all.stream()
                    .filter(w -> w != null)
                    .filter(w -> !"done".equalsIgnoreCase(w.getStatus()))
                    .filter(w -> w.getDueDate() != null && !w.getDueDate().isAfter(endOfToday))
                    .sorted((a, b) -> {
                        LocalDateTime da = a.getDueDate();
                        LocalDateTime db = b.getDueDate();
                        if (da == null && db == null) return 0;
                        if (da == null) return 1;
                        if (db == null) return -1;
                        return da.compareTo(db);
                    })
                    .toList();
        }
    }

    public Map<String, Object> importWords(String username, WordImportRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (req.getSectionSize() == null || req.getSectionSize() <= 0) {
            throw new RuntimeException("分区大小需要为正整数");
        }
        String bookName = (req.getBookName() == null || req.getBookName().isBlank())
                ? "默认词库" : req.getBookName().trim();
        LocalDate startDate;
        try {
            startDate = (req.getStartDate() == null || req.getStartDate().isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(req.getStartDate());
        } catch (Exception e) {
            throw new RuntimeException("开始日期格式应为yyyy-MM-dd");
        }

        String content = fetchText(req.getSourceUrl());
        if (content == null || content.isBlank()) {
            throw new RuntimeException("未获取到词库内容");
        }
        // 解析行：支持 "word|translation" / "word,translation" / "word\ttranslation" / "word"
        String[] lines = content.split("\\r?\\n");
        List<Word> toSave = new ArrayList<>();
        int sectionSize = req.getSectionSize();
        int index = 0;
        int sectionIndex = 1;

        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String wordText = line;
            String translation = "";
            String[] parts;
            if (line.contains("|")) {
                parts = line.split("\\|", 2);
                wordText = parts[0].trim();
                translation = parts[1].trim();
            } else if (line.contains("\t")) {
                parts = line.split("\\t", 2);
                wordText = parts[0].trim();
                translation = parts[1].trim();
            } else if (line.contains(",")) {
                parts = line.split(",", 2);
                wordText = parts[0].trim();
                translation = parts[1].trim();
            }

            if (wordText.isEmpty()) continue;

            // 分区计算
            sectionIndex = (index / sectionSize) + 1;
            LocalDate dueDay = startDate.plusDays(sectionIndex - 1L);
            LocalDateTime dueDate = dueDay.atStartOfDay();

            Word w = new Word();
            w.setUserId(user.getId());
            w.setWord(wordText);
            w.setTranslation(translation);
            w.setBook(bookName);
            w.setSectionIndex(sectionIndex);
            w.setStatus("todo");
            w.setDueDate(dueDate);
            toSave.add(w);

            index++;
        }

        if (toSave.isEmpty()) {
            throw new RuntimeException("有效单词为空，导入取消");
        }
        wordRepository.saveAll(toSave);

        Map<String, Object> resp = new HashMap<>();
        resp.put("book", bookName);
        resp.put("imported", toSave.size());
        resp.put("sections", ((toSave.size() - 1) / sectionSize) + 1);
        resp.put("startDate", startDate.toString());
        return resp;
    }

    // 艾宾浩斯间隔（分钟）：5m, 30m, 12h, 1d, 2d, 4d, 7d, 15d, 30d
    private static final long[] EBBINGHAUS_INTERVALS_MINUTES = new long[] {
            5, 30, 12 * 60L, 24 * 60L, 2 * 24 * 60L, 4 * 24 * 60L, 7 * 24 * 60L, 15 * 24 * 60L, 30 * 24 * 60L
    };

    private void applySuccessfulReviewSchedule(Word word) {
        int currentCount = word.getReviewCount() == null ? 0 : word.getReviewCount();
        int nextIndex = currentCount; // 第一次成功（current=0）使用 intervals[0]
        LocalDateTime now = LocalDateTime.now();
        word.setLastReviewTime(now);

        if (nextIndex >= EBBINGHAUS_INTERVALS_MINUTES.length) {
            // 已超过最后阶段：直接视为掌握
            word.setStatus("done");
            word.setReviewCount(currentCount + 1);
            word.setDueDate(null);
            return;
        }

        long minutes = EBBINGHAUS_INTERVALS_MINUTES[nextIndex];
        LocalDateTime nextDue = now.plus(Duration.ofMinutes(minutes));
        word.setDueDate(nextDue);
        word.setReviewCount(currentCount + 1);
        // 保持为 todo，使其在达到下个计划时继续出现（最终阶段后才置为 done）
        word.setStatus("todo");
    }

    private void applyFailedReviewReset(Word word) {
        // 遗忘或失败：回退到第一阶段，5分钟后再次安排
        LocalDateTime now = LocalDateTime.now();
        word.setLastReviewTime(now);
        word.setReviewCount(0);
        word.setDueDate(now.plus(Duration.ofMinutes(EBBINGHAUS_INTERVALS_MINUTES[0])));
        word.setStatus("todo");
    }

    private String fetchText(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new RuntimeException("下载失败，HTTP " + response.statusCode());
        } catch (Exception e) {
            throw new RuntimeException("下载词库失败: " + e.getMessage());
        }
    }

    public Map<String, Object> importDefaultWords(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        try {
            ClassPathResource resource = new ClassPathResource("wordlists/default.txt");
            if (!resource.exists()) {
                throw new RuntimeException("默认词库资源不存在");
            }
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                List<String> lines = reader.lines().toList();
                if (lines.isEmpty()) {
                    throw new RuntimeException("默认词库为空");
                }
                // 采用每日一分区，默认每区50词，从今天开始
                int sectionSize = 50;
                String bookName = "默认词库";
                LocalDate startDate = LocalDate.now();
                List<Word> toSave = new ArrayList<>();
                int index = 0;
                for (String raw : lines) {
                    if (raw == null) continue;
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String wordText = line;
                    String translation = "";
                    String[] parts;
                    if (line.contains("|")) {
                        parts = line.split("\\|", 2);
                        wordText = parts[0].trim();
                        translation = parts[1].trim();
                    } else if (line.contains("\t")) {
                        parts = line.split("\\t", 2);
                        wordText = parts[0].trim();
                        translation = parts[1].trim();
                    } else if (line.contains(",")) {
                        parts = line.split(",", 2);
                        wordText = parts[0].trim();
                        translation = parts[1].trim();
                    }
                    if (wordText.isEmpty()) continue;
                    int sectionIndex = (index / sectionSize) + 1;
                    LocalDate dueDay = startDate.plusDays(sectionIndex - 1L);
                    LocalDateTime dueDate = dueDay.atStartOfDay();
                    Word w = new Word();
                    w.setUserId(user.getId());
                    w.setWord(wordText);
                    w.setTranslation(translation);
                    w.setBook(bookName);
                    w.setSectionIndex(sectionIndex);
                    w.setStatus("todo");
                    w.setDueDate(dueDate);
                    toSave.add(w);
                    index++;
                }
                if (toSave.isEmpty()) {
                    throw new RuntimeException("默认词库无有效词条");
                }
                wordRepository.saveAll(toSave);
                Map<String, Object> resp = new HashMap<>();
                resp.put("book", bookName);
                resp.put("imported", toSave.size());
                resp.put("sections", ((toSave.size() - 1) / sectionSize) + 1);
                resp.put("startDate", startDate.toString());
                return resp;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("导入默认词库失败: " + e.getMessage());
        }
    }

    public Word updateWordStatus(String username, @NonNull String wordId, WordStatusRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        if (!user.getId().equals(word.getUserId())) {
            throw new RuntimeException("无权操作此单词");
        }
        String status = (req.getStatus() == null ? "todo" : req.getStatus().trim().toLowerCase());
        if (!status.equals("todo") && !status.equals("done")) {
            throw new RuntimeException("状态只能为 todo 或 done");
        }
        // 语义调整：
        // - 'done' 视为一次成功复习，按艾宾浩斯曲线安排下一次 dueDate（最终阶段则永久 done）
        // - 'todo' 可用于表示记不住/撤销，将计划重置到首阶段
        if (status.equals("done")) {
            applySuccessfulReviewSchedule(word);
        } else {
            applyFailedReviewReset(word);
        }
        return wordRepository.save(word);
    }

    public List<Map<String, Object>> getBooks(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        List<Word> all = wordRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
        Map<String, List<Word>> grouped = all.stream()
                .collect(Collectors.groupingBy(w -> w.getBook() == null ? "未命名" : w.getBook()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Word>> e : grouped.entrySet()) {
            List<Word> list = e.getValue();
            long total = list.size();
            long done = list.stream().filter(w -> "done".equalsIgnoreCase(w.getStatus())).count();
            long todo = total - done;
            LocalDateTime nextDue = list.stream()
                    .filter(w -> !"done".equalsIgnoreCase(w.getStatus()) && w.getDueDate() != null)
                    .map(Word::getDueDate)
                    .sorted()
                    .findFirst()
                    .orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("book", e.getKey());
            m.put("total", total);
            m.put("done", done);
            m.put("todo", todo);
            m.put("nextDueDate", nextDue);
            result.add(m);
        }
        return result;
    }
}
