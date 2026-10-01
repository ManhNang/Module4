package com.codegym.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class Song implements Validator {

    private Long id;

    @NotBlank(message = "{song.name.empty}")
    @Size(max = 800, message = "{song.name.size}")
    @Pattern(regexp = "^[^@;,.=\\-+]*$", message = "{song.name.pattern}")
    private String name;

    @NotBlank(message = "{song.artist.empty}")
    @Size(max = 300, message = "{song.artist.size}")
    @Pattern(regexp = "^[^@;,.=\\-+]*$", message = "{song.artist.pattern}")
    private String artist;

    @NotBlank(message = "{song.genre.empty}")
    @Size(max = 1000, message = "{song.genre.size}")
    @Pattern(regexp = "^[^@;.=\\-+]*$", message = "{song.genre.pattern}")
    private String genre;

    public Song() {
    }

    public Song(Long id, String name, String artist, String genre) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.genre = genre;
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

    public String getTitle() {
        return name;
    }

    public void setTitle(String title) {
        this.name = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSinger() {
        return artist;
    }

    public void setSinger(String singer) {
        this.artist = singer;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getKind() {
        return genre;
    }

    public void setKind(String kind) {
        this.genre = kind;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Song.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Song song = (Song) target;

        // Validate name: Not blank, max 800 chars, no special characters (@ ; , . = - +)
        if (song.getName() == null || song.getName().trim().isEmpty()) {
            if (!errors.hasFieldErrors("name")) {
                errors.rejectValue("name", "song.name.empty");
            }
        } else {
            if (song.getName().length() > 800) {
                if (!errors.hasFieldErrors("name")) {
                    errors.rejectValue("name", "song.name.size");
                }
            }
            if (!song.getName().matches("^[^@;,.=\\-+]*$")) {
                if (!errors.hasFieldErrors("name")) {
                    errors.rejectValue("name", "song.name.pattern");
                }
            }
        }

        // Validate artist: Not blank, max 300 chars, no special characters (@ ; , . = - +)
        if (song.getArtist() == null || song.getArtist().trim().isEmpty()) {
            if (!errors.hasFieldErrors("artist")) {
                errors.rejectValue("artist", "song.artist.empty");
            }
        } else {
            if (song.getArtist().length() > 300) {
                if (!errors.hasFieldErrors("artist")) {
                    errors.rejectValue("artist", "song.artist.size");
                }
            }
            if (!song.getArtist().matches("^[^@;,.=\\-+]*$")) {
                if (!errors.hasFieldErrors("artist")) {
                    errors.rejectValue("artist", "song.artist.pattern");
                }
            }
        }

        // Validate genre: Not blank, max 1000 chars, no special characters except comma (,)
        if (song.getGenre() == null || song.getGenre().trim().isEmpty()) {
            if (!errors.hasFieldErrors("genre")) {
                errors.rejectValue("genre", "song.genre.empty");
            }
        } else {
            if (song.getGenre().length() > 1000) {
                if (!errors.hasFieldErrors("genre")) {
                    errors.rejectValue("genre", "song.genre.size");
                }
            }
            if (!song.getGenre().matches("^[^@;.=\\-+]*$")) {
                if (!errors.hasFieldErrors("genre")) {
                    errors.rejectValue("genre", "song.genre.pattern");
                }
            }
        }
    }
}
