package com.codegym.musicstreaming.service;

import com.codegym.musicstreaming.model.Song;

import java.util.List;

public interface ISongService {
    List<Song> findAll();

    Song findById(Long id);

    void save(Song song);

    void update(Song song);

    void remove(Long id);

    List<Song> searchByName(String keyword);
}
