package com.codegym.musicstreaming.controller;

import com.codegym.musicstreaming.configuration.AppConfiguration;
import com.codegym.musicstreaming.model.Song;
import com.codegym.musicstreaming.model.SongForm;
import com.codegym.musicstreaming.service.ISongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping({"/", "/songs"})
public class SongController {

    @Autowired
    private ISongService songService;

    // Display song list with optional search and current playing song
    @GetMapping
    public String listSongs(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "playId", required = false) Long playId,
                            Model model) {
        List<Song> songs;
        if (search != null && !search.trim().isEmpty()) {
            songs = songService.searchByName(search.trim());
            model.addAttribute("search", search.trim());
        } else {
            songs = songService.findAll();
        }
        model.addAttribute("songs", songs);

        if (playId != null) {
            Song playingSong = songService.findById(playId);
            model.addAttribute("playingSong", playingSong);
        } else if (!songs.isEmpty()) {
            model.addAttribute("playingSong", songs.get(0));
        }

        return "list";
    }

    // Show form to add a new song
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("songForm", new SongForm());
        return "create";
    }

    // Handle adding new song with file upload
    @PostMapping("/create")
    public String saveSong(@ModelAttribute("songForm") SongForm songForm,
                           RedirectAttributes redirectAttributes) {
        MultipartFile multipartFile = songForm.getMusicFile();
        String fileName = "";

        if (multipartFile != null && !multipartFile.isEmpty()) {
            fileName = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
            File uploadDir = new File(AppConfiguration.UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            try {
                FileCopyUtils.copy(multipartFile.getBytes(), new File(AppConfiguration.UPLOAD_DIR + fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Song song = new Song(
                songForm.getName(),
                songForm.getArtist(),
                songForm.getGenre(),
                fileName
        );
        songService.save(song);

        redirectAttributes.addFlashAttribute("successMessage", "Thêm bài hát \"" + song.getName() + "\" thành công!");
        return "redirect:/songs";
    }

    // Show form to edit an existing song
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Song song = songService.findById(id);
        if (song == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài hát yêu cầu!");
            return "redirect:/songs";
        }
        SongForm songForm = new SongForm(
                song.getId(),
                song.getName(),
                song.getArtist(),
                song.getGenre(),
                song.getFilePath()
        );
        model.addAttribute("songForm", songForm);
        return "edit";
    }

    // Handle updating song
    @PostMapping("/update")
    public String updateSong(@ModelAttribute("songForm") SongForm songForm,
                             RedirectAttributes redirectAttributes) {
        MultipartFile multipartFile = songForm.getMusicFile();
        String fileName = songForm.getCurrentFilePath();

        // If user uploaded a replacement music file
        if (multipartFile != null && !multipartFile.isEmpty()) {
            fileName = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
            File uploadDir = new File(AppConfiguration.UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            try {
                FileCopyUtils.copy(multipartFile.getBytes(), new File(AppConfiguration.UPLOAD_DIR + fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Song song = new Song(
                songForm.getId(),
                songForm.getName(),
                songForm.getArtist(),
                songForm.getGenre(),
                fileName
        );
        songService.update(song);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin bài hát thành công!");
        return "redirect:/songs";
    }

    // Delete a song
    @GetMapping("/delete/{id}")
    public String deleteSong(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Song song = songService.findById(id);
        if (song != null) {
            // Optional: delete physical file if needed
            if (song.getFilePath() != null && !song.getFilePath().isEmpty()) {
                File file = new File(AppConfiguration.UPLOAD_DIR + song.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }
            songService.remove(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xoá bài hát thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Bài hát không tồn tại!");
        }
        return "redirect:/songs";
    }

    // Dedicated Play view
    @GetMapping("/play/{id}")
    public String playSongDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Song song = songService.findById(id);
        if (song == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài hát!");
            return "redirect:/songs";
        }
        List<Song> allSongs = songService.findAll();
        model.addAttribute("song", song);
        model.addAttribute("playlist", allSongs);
        return "play";
    }
}
