package io.surprise.file.model;

import io.surprise.file.adapter.rvAdapter;

public class Options {

    public String title;
    public String id;
    public String query;
    public rvAdapter adapter;
    public Options() {

    }
    public Options(String title, String id, String query) {
        title = title;
        id = id;
        query = query;
    }
}
