package com.codegym.customermanagementjpa.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class BadWordFilter {

    /**
     * Danh sách các từ ngữ xấu / không phù hợp được lọc
     */
    private static final List<String> BAD_WORDS = Collections.unmodifiableList(Arrays.asList(
            "badword",
            "xấu",
            "dở",
            "tệ",
            "chửi",
            "tục",
            "bậy",
            "spam",
            "lừa đảo",
            "lua dao",
            "scam",
            "cheat",
            "fuck",
            "shit",
            "dm",
            "vcl",
            "địt",
            "đụ",
            "ngu",
            "hate",
            "kill"
    ));

    public List<String> getBadWords() {
        return BAD_WORDS;
    }

    /**
     * Tìm từ xấu đầu tiên xuất hiện trong văn bản.
     * @param text Đoạn văn bản cần kiểm tra
     * @return Từ xấu được tìm thấy hoặc null nếu không có
     */
    public String findFirstBadWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);

        for (String badWord : BAD_WORDS) {
            String lowerBadWord = badWord.toLowerCase(Locale.ROOT);

            // Với từ ngắn (<= 3 ký tự như: dm, vcl, ngu, tệ), kiểm tra theo ranh giới từ để tránh nhầm lẫn từ bình thường
            if (lowerBadWord.length() <= 3) {
                // Regex kiểm tra ranh giới từ hoặc ký tự phân tách khoảng trắng / dấu câu
                String regex = "(?i)(^|[\\s\\p{Punct}])" + Pattern.quote(lowerBadWord) + "($|[\\s\\p{Punct}])";
                if (Pattern.compile(regex).matcher(lowerText).find()) {
                    return badWord;
                }
            } else {
                // Với các từ dài hơn, kiểm tra xuất hiện trong chuỗi
                if (lowerText.contains(lowerBadWord)) {
                    return badWord;
                }
            }
        }
        return null;
    }

    /**
     * Kiểm tra chuỗi có chứa từ xấu hay không
     */
    public boolean containsBadWord(String text) {
        return findFirstBadWord(text) != null;
    }
}
