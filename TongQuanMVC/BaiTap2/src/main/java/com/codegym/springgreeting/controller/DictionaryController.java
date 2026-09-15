package com.codegym.springgreeting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/")
public class DictionaryController {

    // Danh sách từ và nghĩa được khởi tạo từ mảng 2 chiều
    private static final Map<String, String> DICTIONARY = new LinkedHashMap<>();

    static {
        String[][] wordPairs = {
            {"hello", "Xin chào"},
            {"world", "Thế giới"},
            {"book", "Quyển sách"},
            {"computer", "Máy vi tính"},
            {"developer", "Lập trình viên"},
            {"software", "Phần mềm"},
            {"teacher", "Giáo viên / Giảng viên"},
            {"student", "Học sinh / Sinh viên"},
            {"library", "Thư viện"},
            {"school", "Trường học"},
            {"apple", "Quả táo"},
            {"banana", "Quả chuối"},
            {"coffee", "Cà phê"},
            {"car", "Xe ô tô"},
            {"flower", "Bông hoa"},
            {"sky", "Bầu trời"},
            {"ocean", "Đại dương"},
            {"spring", "Mùa xuân / Spring MVC Framework"},
            {"keyboard", "Bàn phím"},
            {"mouse", "Con chuột"}
        };

        for (String[] pair : wordPairs) {
            DICTIONARY.put(pair[0].toLowerCase(), pair[1]);
        }
    }

    /**
     * Hiển thị trang chủ tra cứu từ điển
     */
    @GetMapping({"", "/"})
    public String index(Model model) {
        model.addAttribute("sampleWords", DICTIONARY.keySet());
        return "index";
    }

    /**
     * Xử lý tra cứu từ Anh - Việt
     */
    @RequestMapping(value = "/lookup")
    public String lookup(@RequestParam(name = "word", required = false) String word, Model model) {
        model.addAttribute("sampleWords", DICTIONARY.keySet());

        if (word == null || word.trim().isEmpty()) {
            model.addAttribute("hasSearched", true);
            model.addAttribute("found", false);
            model.addAttribute("word", "");
            model.addAttribute("message", "Vui lòng nhập từ tiếng Anh cần tra cứu.");
            return "index";
        }

        String searchKey = word.trim().toLowerCase();
        model.addAttribute("word", word.trim());
        model.addAttribute("hasSearched", true);

        if (DICTIONARY.containsKey(searchKey)) {
            model.addAttribute("found", true);
            model.addAttribute("meaning", DICTIONARY.get(searchKey));
        } else {
            model.addAttribute("found", false);
            model.addAttribute("message", "Không tìm thấy từ \"" + word.trim() + "\" trong từ điển.");
        }

        return "index";
    }
}
