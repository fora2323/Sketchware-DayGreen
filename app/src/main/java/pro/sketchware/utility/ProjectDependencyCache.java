package pro.sketchware.utility;

import java.io.File;

public final class ProjectDependencyCache {

    private ProjectDependencyCache() {
    }

    public static File dirFor(String scId) {
        return new File(FileUtil.getExternalStorageDir(), ".sketchware/data/" + scId + "/files/library/cache");
    }

    public static void clear(String scId) {
        FileUtil.deleteFile(dirFor(scId).getAbsolutePath());
    }
}