package com.codegym.service.impl;

import com.codegym.model.Song;
import com.codegym.service.SongService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SongServiceImpl implements SongService {

    private static final Map<Long, Song> songs = new LinkedHashMap<>();
    private static final AtomicLong idGenerator = new AtomicLong(0);

    static {
        Song song1 = new Song(idGenerator.incrementAndGet(), "Nơi này có anh", "Sơn Tùng M-TP", "Pop, RnB");
        Song song2 = new Song(idGenerator.incrementAndGet(), "Ánh nắng của anh", "Đức Phúc", "Ballad, Pop");
        Song song3 = new Song(idGenerator.incrementAndGet(), "Ngày mai người ta lấy chồng", "Thành Đạt", "Ballad");
        songs.put(song1.getId(), song1);
        songs.put(song2.getId(), song2);
        songs.put(song3.getId(), song3);
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
            song.setId(idGenerator.incrementAndGet());
        }
        songs.put(song.getId(), song);
    }

    @Override
    public void update(Long id, Song song) {
        song.setId(id);
        songs.put(id, song);
    }

    @Override
    public void delete(Long id) {
        songs.remove(id);
    }
}
