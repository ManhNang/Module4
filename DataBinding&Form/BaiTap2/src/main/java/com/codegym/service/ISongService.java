package com.codegym.service;

import com.codegym.model.Song;
import java.util.List;

public interface ISongService {
    List<Song> findAll();
    Song findById(Long id);
    void save(Song song);
    void remove(Long id);
    List<String> getAvailableGenres();
}
