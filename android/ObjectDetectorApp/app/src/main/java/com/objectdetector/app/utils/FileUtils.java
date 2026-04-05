package com.objectdetector.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for file operations — copying URIs to temp files, etc.
 */
public class FileUtils {

    /**
     * Copy a content URI to a temporary file in the app cache directory.
     */
    public static File copyUriToTempFile(Context context, Uri uri, String prefix) throws IOException {
        String extension = getFileExtension(context, uri);
        File tempFile = File.createTempFile(prefix, "." + extension, context.getCacheDir());

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream == null) {
                throw new IOException("Could not open input stream for URI: " + uri);
            }
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    /**
     * Get the MIME type from a URI.
     */
    public static String getMimeType(Context context, Uri uri) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.getType(uri);
    }

    /**
     * Get the file extension from a URI.
     */
    public static String getFileExtension(Context context, Uri uri) {
        String mimeType = getMimeType(context, uri);
        if (mimeType != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (ext != null) return ext;
        }

        // Fallback: try to get from display name
        String displayName = getFileName(context, uri);
        if (displayName != null && displayName.contains(".")) {
            return displayName.substring(displayName.lastIndexOf('.') + 1);
        }

        return "tmp";
    }

    /**
     * Get the display name of a file from its URI.
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    /**
     * Get file size from URI.
     */
    public static long getFileSize(Context context, Uri uri) {
        long size = -1;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            }
        }
        return size;
    }

    /**
     * Clean up temporary files from cache.
     */
    public static void cleanCache(Context context) {
        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("img_") || file.getName().startsWith("vid_")) {
                    file.delete();
                }
            }
        }
    }
}
