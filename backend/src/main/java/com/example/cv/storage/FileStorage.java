package com.example.cv.storage;

import java.io.InputStream;

public interface FileStorage {

    StoredFile upload(String path, InputStream content, String contentType);

    InputStream download(String path);

    void delete(String path);

    boolean exists(String path);
}