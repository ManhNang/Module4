package com.codegym.controller;

import com.codegym.model.Song;
import com.codegym.service.SongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class SongController {

    private final SongService songService;

    @Autowired
    public SongController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping({"/", "/songs"})
    public String listSongs(Model model) {
        List<Song> songs = songService.findAll();
        model.addAttribute("songs", songs);
        return "list";
    }

    @GetMapping({"/songs/create", "/create"})
    public String showCreateForm(Model model) {
        model.addAttribute("song", new Song());
        return "create";
    }

    @PostMapping({"/songs/create", "/songs/save", "/create"})
    public String createSong(@Valid @ModelAttribute("song") Song song,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        song.validate(song, bindingResult);

        if (bindingResult.hasErrors()) {
            return "create";
        }

        songService.save(song);
        redirectAttributes.addFlashAttribute("message", "Thêm mới bài hát thành công!");
        return "redirect:/songs";
    }

    @GetMapping({"/songs/edit/{id}", "/edit/{id}"})
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Song song = songService.findById(id);
        if (song == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài hát với mã " + id);
            return "redirect:/songs";
        }
        model.addAttribute("song", song);
        return "edit";
    }

    @PostMapping({"/songs/edit", "/songs/update", "/edit"})
    public String updateSong(@Valid @ModelAttribute("song") Song song,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        song.validate(song, bindingResult);

        if (bindingResult.hasErrors()) {
            return "edit";
        }

        songService.update(song.getId(), song);
        redirectAttributes.addFlashAttribute("message", "Cập nhật bài hát thành công!");
        return "redirect:/songs";
    }

    @GetMapping({"/songs/delete/{id}", "/delete/{id}"})
    public String deleteSong(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        songService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Xóa bài hát thành công!");
        return "redirect:/songs";
    }
}
