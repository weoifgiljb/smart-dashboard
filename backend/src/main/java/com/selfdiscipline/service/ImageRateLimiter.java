package com.selfdiscipline.service;

import com.selfdiscipline.exception.ImageGenerationException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImageRateLimiter {

	private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();
	private final long windowMillis;
	private final int limit;

	public ImageRateLimiter() {
		this(60_000L, 5);
	}

	public ImageRateLimiter(long windowMillis, int limit) {
		this.windowMillis = windowMillis;
		this.limit = limit;
	}

	public boolean allow(String key) {
		long now = System.currentTimeMillis();
		Deque<Long> q = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (q) {
			// 清理过期窗口
			while (!q.isEmpty() && now - q.peekFirst() >= windowMillis) {
				q.pollFirst();
			}
			if (q.size() >= limit) {
				return false;
			}
			q.addLast(now);
			return true;
		}
	}

	public void consumeOrThrow(String key) {
		if (!allow(key)) {
			throw new ImageGenerationException(ImageGenerationException.ErrorType.RATE_LIMITED, "生成频率过高，请稍后再试");
		}
	}
}


