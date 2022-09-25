package io.surprise.file.model;


import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Media extends File {
    public int id;
    public String duration;
    public String width;
    public String height;
    public String album;
    public String artist;
    public String thumb;


    static public List<String> videoAndAudioProjection = Arrays.asList("album", "artist", "duration");
    static public List<String> imageProjection = Arrays.asList("width", "height");


    public Media(File file, int ID, @Nullable HashMap<String, String> optionsHashMap) {
        super(file.name, file.mimeType, file.size, file.url, file.id);
        id = ID;
        if (optionsHashMap != null && !optionsHashMap.isEmpty()) {
            if (optionsHashMap.containsKey("duration"))
                duration = optionsHashMap.get("duration");
            if (optionsHashMap.containsKey("album")) album = optionsHashMap.get("album");
            if (optionsHashMap.containsKey("artist"))
                artist = optionsHashMap.get("artist");
            if (optionsHashMap.containsKey("thumb"))
                thumb = optionsHashMap.get("thumb");
            if (optionsHashMap.containsKey("width"))
                width = optionsHashMap.get("width");
            if (optionsHashMap.containsKey("height"))
                height = optionsHashMap.get("height");
        }
    }
}
