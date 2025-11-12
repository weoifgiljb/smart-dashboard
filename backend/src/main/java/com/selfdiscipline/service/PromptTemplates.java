package com.selfdiscipline.service;

import com.selfdiscipline.model.Book;
import com.selfdiscipline.model.Word;

public class PromptTemplates {

	private PromptTemplates() {}

	public static String buildBookCoverPrompt(Book book) {
		String title = safe(book.getTitle());
		String author = safe(book.getAuthor());
		String desc = safe(book.getDescription());
		String core = String.format(
				"Book cover, high quality, minimalist, centered composition. Title: %s%s%s. Avoid text. Clean, modern, flat style.",
				title,
				author.isBlank() ? "" : ", Author: " + author,
				desc.isBlank() ? "" : ". Description: " + desc
		);
		return clamp(normalize(core), 400);
	}

	public static String buildWordImagePrompt(Word word) {
		String w = safe(word.getWord());
		String t = safe(word.getTranslation());
		String e = safe(word.getExample());
		String core = "An icon-like, simple illustration with white background that visually represents the English word '" +
				w + "' " + (t.isBlank() ? "" : ("(meaning: " + t + ") ")) +
				(e.isBlank() ? "" : ("Example: " + e + " ")) +
				". Avoid text. Clean, modern, flat style.";
		return clamp(normalize(core), 400);
	}

	private static String normalize(String s) {
		String v = safe(s);
		// 压缩多余空白
		v = v.replaceAll("\\s+", " ").trim();
		return v;
	}

	private static String clamp(String s, int maxLen) {
		if (s == null) return "";
		if (s.length() <= maxLen) return s;
		return s.substring(0, maxLen);
	}

	private static String safe(String s) {
		return s == null ? "" : s.trim();
	}
}



