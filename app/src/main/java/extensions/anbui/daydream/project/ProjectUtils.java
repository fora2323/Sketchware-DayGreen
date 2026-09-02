package extensions.anbui.daydream.project;

import android.util.Log;

import extensions.anbui.daydream.configs.Configs;

public class ProjectUtils {

    public static String TAG = Configs.universalTAG + "ProjectUtils";

    public static String convertJavaNameToXMLName(String javaName) {
        //Example: HomePlusActivity.java
        if (javaName.endsWith(".java")) {
            //1: _Home_Plus_Activity.java
            //2: _home_plus_activity.java
            //3: home_plus_activity.java
            //4: home_plus
            return javaName.replaceAll("([A-Z])", "_$1")
                    .toLowerCase()
                    .replaceFirst("_", "")
                    .replace("_activity.java", "");
        } else if (javaName.endsWith("Activity")) {
            //1: _Home_Plus_Activity
            //2: _home_plus_activity
            //3: home_plus_activity
            //4: home_plus
            return javaName.replaceAll("([A-Z])", "_$1")
                    .toLowerCase()
                    .replaceFirst("_", "")
                    .replace("_activity", "");
        }
        Log.i(TAG, "convertJavaNameToXMLName: " + javaName);
        return javaName;
    }

    public static String convertXMLNameToJavaName(String xmlName, boolean withDotJava) {
        if (xmlName.endsWith(".java") || xmlName.endsWith("Activity")) return xmlName;

        // "home_plus" -> "HomePlusActivity" or "HomePlusActivity.java"
        String[] parts = xmlName.split("_");
        StringBuilder javaName = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                javaName.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
        }
        javaName.append("Activity");

        if (withDotJava) {
            javaName.append(".java");
        }
        Log.i(TAG, "convertXMLNameToJavaName: " + xmlName + " -> " + javaName);
        return javaName.toString();
    }

}
