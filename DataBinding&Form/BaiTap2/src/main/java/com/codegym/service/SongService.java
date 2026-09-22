package com.codegym.service;

import com.codegym.model.Song;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SongService implements ISongService {
    private final Map<Long, Song> songs = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public SongService() {
        // Initial sample data
        save(new Song(null, "See Tình", "Hoàng Thùy Linh", Arrays.asList("Pop", "Dance-pop"), "see-tinh.mp3"));
        save(new Song(null, "Nắng Ấm Xa Dần", "Sơn Tùng M-TP", Arrays.asList("R&B", "Pop"), "nang-am-xa-dan.mp3"));
    }

    @Override
    public List<Song> findAll() {
        return new ArrayList<>(songs.values());
    }

    @Override
    public Song findById(Long id) {
        return songs.get(id);
    }

    @Override
    public void save(Song song) {
        if (song.getId() == null) {
            song.setId(idGenerator.getAndIncrement());
        }
        songs.put(song.getId(), song);
    }

    @Override
    public void remove(Long id) {
        songs.remove(id);
    }

    @Override
    public List<String> getAvailableGenres() {
        return Arrays.asList("Pop", "Ballad", "Rock", "EDM", "R&B", "Jazz", "Hip Hop", "Acoustic", "Indie", "Dance-pop");
    }
}
