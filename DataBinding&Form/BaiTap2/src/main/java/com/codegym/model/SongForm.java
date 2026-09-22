package com.codegym.model;

import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

public class SongForm {
    private Long id;
    private String name;
    private String artist;
    private List<String> genres = new ArrayList<>();
    private String customGenres;
    private MultipartFile songFile;

    public SongForm() {
    }

    public SongForm(Long id, String name, String artist, List<String> genres, String customGenres, MultipartFile songFile) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.genres = genres;
        this.customGenres = customGenres;
        this.songFile = songFile;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getCustomGenres() {
        return customGenres;
    }

    public void setCustomGenres(String customGenres) {
        this.customGenres = customGenres;
    }

    public MultipartFile getSongFile() {
        return songFile;
    }

    public void setSongFile(MultipartFile songFile) {
        this.songFile = songFile;
    }
}
