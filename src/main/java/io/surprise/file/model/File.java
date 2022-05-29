package io.surprise.file.model;


import android.provider.MediaStore;

public class File {
    public final String name;
    public final String mimeType;
    public final long size;
    public final String url;
    public final int id;
    final static public String[] baseProjection = new String[]{
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.TITLE,
    };
    final static public String baseSelection = MediaStore.Files.FileColumns.SIZE + " > 0";
    static public String titleSelection;
    static public String mimeSelection;
    static public String[] mimeSelectionArgs;

    public File(String fileName, String fileMimeType, long fileSize, String filePath,int Id) {
        name = fileName;
        mimeType = fileMimeType;
        size = fileSize;
        url = filePath;
        id = Id;
    }
}
