package extensions.anbui.daydream.project;

import android.util.Log;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.java.JavaFileUtils;

public class ProjectApplication {

    public static String TAG = Configs.universalTAG + "ProjectApplication";
    public static void createApplicationFile(String projectID, String packageName, String className) {
        //Example: app..MyApplication
        //1: app..MyApplication
        //2: .app..MyApplication
        //3: .app.MyApplication
        //4: /app/MyApplication
        String finalClassNamePath = className;
        if (!className.startsWith(".")) finalClassNamePath = "." + finalClassNamePath;
        finalClassNamePath = finalClassNamePath.replaceAll("\\.\\.", ".");
        finalClassNamePath = finalClassNamePath.replaceAll("\\.", "/");

        if (!className.isEmpty() && !className.equals(".SketchApplication")
                && !JavaFileUtils.isJavaFileExistInProject(projectID, finalClassNamePath + ".java"))
            JavaFileUtils.addJavaFileToProject(projectID, finalClassNamePath + ".java", preparecodesForApplication(packageName, className));

        Log.i(TAG, "createApplicationFile: " + finalClassNamePath);
    }

    public static String preparecodesForApplication(String packageName, String className) {
        Log.i(TAG, "preparecodesForApplication: " + packageName + " " + className);
        //Example: app..MyApplication
        //1: .app..MyApplication
        //2: .app.MyApplication
        String finalClassName = className;
        if (!className.startsWith(".")) finalClassName = "." + finalClassName;
        finalClassName = finalClassName.replaceAll("\\.\\.", ".");
        //Example: com.my.project and .app.MyApplication
        //1: com.my.project.app.MyApplication
        //2: com.my.project.app
        String finalPackageName = packageName + finalClassName;
        finalPackageName = finalPackageName.replaceAll(className.substring(className.lastIndexOf('.')), "");

        return "package " + finalPackageName + ";\n" +
                "\n" +
                "import android.app.Application;\n" +
                "import android.content.Context;\n" +
                "//import android.content.Intent;\n" +
                "//import android.os.Process;\n" +
                "//import android.util.Log;\n" +
                "\n" +
                //Example: .app.MyApplication
                //1: MyApplication
                "public class " + className.substring(className.lastIndexOf('.') + 1) + " extends Application {\n" +
                "\n" +
                "    private static Context mApplicationContext;\n" +
                "\n" +
                "    public static Context getContext() {\n" +
                "        return mApplicationContext;\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void onCreate() {\n" +
                "        mApplicationContext = getApplicationContext();\n" +
                "\n" +
                "//        Thread.setDefaultUncaughtExceptionHandler(\n" +
                "//                new Thread.UncaughtExceptionHandler() {\n" +
                "//                    @Override\n" +
                "//                    public void uncaughtException(Thread thread, Throwable throwable) {\n" +
                "//                        Intent intent = new Intent(getApplicationContext(), DebugActivity.class);\n" +
                "//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);\n" +
                "//                        intent.putExtra(\"error\", Log.getStackTraceString(throwable));\n" +
                "//                        startActivity(intent);\n" +
                "//                        SketchLogger.broadcastLog(Log.getStackTraceString(throwable));\n" +
                "//                        Process.killProcess(Process.myPid());\n" +
                "//                        System.exit(1);\n" +
                "//                    }\n" +
                "//                });\n" +
                "//        SketchLogger.startLogging();\n" +
                "        super.onCreate();\n" +
                "    }\n" +
                "}";

    }
}
