package com.codegym.model;

import java.util.List;

public class Song {
    private Long id;
    private String name;
    private String artist;
    private List<String> genres;
    private String filePath;

    public Song() {
    }

    public Song(Long id, String name, String artist, List<String> genres, String filePath) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.genres = genres;
        this.filePath = filePath;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
