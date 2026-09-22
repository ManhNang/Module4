package com.codegym.controller;

import com.codegym.model.Song;
import com.codegym.model.SongForm;
import com.codegym.service.ISongService;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/songs")
public class SongController {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".mp3", ".wav", ".ogg", ".m4p");

    private final ISongService songService;
    private final ServletContext servletContext;

    @Autowired
    public SongController(ISongService songService, ServletContext servletContext) {
        this.songService = songService;
        this.servletContext = servletContext;
    }

    @GetMapping
    public String listSongs(Model model) {
        model.addAttribute("songs", songService.findAll());
        return "list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("songForm", new SongForm());
        model.addAttribute("availableGenres", songService.getAvailableGenres());
        return "create";
    }

    @PostMapping("/save")
    public String saveSong(@ModelAttribute("songForm") SongForm songForm,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        MultipartFile file = songForm.getSongFile();

        // Validate file presence
        if (file == null || file.isEmpty()) {
            model.addAttribute("errorMessage", "Vui lòng chọn một file nhạc để upload!");
            model.addAttribute("availableGenres", songService.getAvailableGenres());
            return "create";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidAudioFile(originalFilename)) {
            model.addAttribute("errorMessage", "File không hợp lệ! Ứng dụng chỉ chấp nhận các file nhạc có định dạng .mp3, .wav, .ogg, .m4p.");
            model.addAttribute("availableGenres", songService.getAvailableGenres());
            return "create";
        }

        // Process genre list (from checkboxes and/or custom text input)
        List<String> combinedGenres = new ArrayList<>();
        if (songForm.getGenres() != null) {
            combinedGenres.addAll(songForm.getGenres());
        }
        if (songForm.getCustomGenres() != null && !songForm.getCustomGenres().trim().isEmpty()) {
            String[] customArray = songForm.getCustomGenres().split("[,;]");
            for (String item : customArray) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty() && !combinedGenres.contains(trimmed)) {
                    combinedGenres.add(trimmed);
                }
            }
        }
        if (combinedGenres.isEmpty()) {
            combinedGenres.add("Khác");
        }

        // Save file to server's uploads folder
        String uploadPath = servletContext.getRealPath("/uploads/");
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String uniqueFileName = System.currentTimeMillis() + "_" + originalFilename;
        try {
            file.transferTo(new File(uploadPath + File.separator + uniqueFileName));
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Đã xảy ra lỗi khi lưu file: " + e.getMessage());
            model.addAttribute("availableGenres", songService.getAvailableGenres());
            return "create";
        }

        // Save song info
        Song song = new Song();
        song.setName(songForm.getName());
        song.setArtist(songForm.getArtist());
        song.setGenres(combinedGenres);
        song.setFilePath(uniqueFileName);
        songService.save(song);

        redirectAttributes.addFlashAttribute("successMessage", "Bài hát \"" + song.getName() + "\" đã được tải lên thành công!");
        return "redirect:/songs";
    }

    private boolean isValidAudioFile(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
