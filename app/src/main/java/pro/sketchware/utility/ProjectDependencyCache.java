package pro.sketchware.utility;

import java.io.File;

public final class ProjectDependencyCache {

    private ProjectDependencyCache() {
    }

    public static File dirFor(String scId) {
        return new File(FileUtil.getExternalStorageDir(), ".sketchware/data/" + scId + "/build_cache/library");
    }

    public static void clear(String scId) {
        FileUtil.deleteFile(dirFor(scId).getAbsolutePath());
    }
}