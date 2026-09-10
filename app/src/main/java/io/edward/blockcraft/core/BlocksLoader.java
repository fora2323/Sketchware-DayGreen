package io.edward.blockcraft.core;

import android.util.Log;

public class BlocksLoader {
    private static final String TAG = "BlocksLoader";
    private static boolean isLoaded = false;
    private static String loadError = null;

    static {
        try {
            System.loadLibrary("rust_language");
            isLoaded = true;
        } catch (UnsatisfiedLinkError | SecurityException e) {
            loadError = e.getMessage();
            Log.e(TAG, "Failed to load librust_language: " + loadError);
        }
    }

    public static boolean isLibraryLoaded() {
        return isLoaded;
    }

    public static String getLoadError() {
        return loadError;
    }

    public static native String generateBlocksFromCode(String code);

    public static native String BlockBeansGenerator(String filePath);

    public static native String loadBlocksCollection();

    public static native String nativeBlockTokenizer(String spec);
}
