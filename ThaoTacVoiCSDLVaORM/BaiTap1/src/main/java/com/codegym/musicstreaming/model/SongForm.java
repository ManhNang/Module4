package com.codegym.musicstreaming.model;

import org.springframework.web.multipart.MultipartFile;

public class SongForm {
    private Long id;
    private String name;
    private String artist;
    private String genre;
    private MultipartFile musicFile;
    private String currentFilePath;

    public SongForm() {
    }

    public SongForm(Long id, String name, String artist, String genre, String currentFilePath) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.genre = genre;
        this.currentFilePath = currentFilePath;
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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public MultipartFile getMusicFile() {
        return musicFile;
    }

    public void setMusicFile(MultipartFile musicFile) {
        this.musicFile = musicFile;
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public void setCurrentFilePath(String currentFilePath) {
        this.currentFilePath = currentFilePath;
    }
}
