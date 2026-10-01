package com.codegym.service;

import com.codegym.model.Song;

import java.util.List;

public interface SongService {
    List<Song> findAll();
    Song findById(Long id);
    void save(Song song);
    void update(Long id, Song song);
    void delete(Long id);
}
