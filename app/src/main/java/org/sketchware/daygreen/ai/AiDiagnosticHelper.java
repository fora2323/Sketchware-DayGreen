package org.sketchware.daygreen.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import a.a.a.eC;
import a.a.a.jC;
import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ViewBean;
import mod.jbk.diagnostic.CompileErrorSaver;

public final class AiDiagnosticHelper {

    private AiDiagnosticHelper() {}

    public static String getCompileError(String scId) {
        if (scId == null || scId.isEmpty()) {
            return "Project ID is missing.";
        }
        CompileErrorSaver saver = new CompileErrorSaver(scId);
        if (saver.logFileExists()) {
            String log = saver.getLogsFromFile();
            if (log != null && !log.trim().isEmpty()) {
                return "Last Compile Error Log:\n\n```log\n" + log.trim() + "\n```";
            }
        }
        return "No compile errors recorded. The project either compiled successfully or has not been built yet.";
    }

    public static String diagnoseProject(String scId, String javaName, String xmlName) {
        if (scId == null || scId.isEmpty()) {
            return "Project ID is missing.";
        }
        if (xmlName == null || xmlName.isEmpty()) {
            xmlName = "main.xml";
        }
        if (!xmlName.endsWith(".xml")) {
            xmlName = xmlName + ".xml";
        }
        if (javaName == null || javaName.isEmpty()) {
            javaName = "MainActivity";
        }

        eC data = jC.a(scId);
        if (data == null) {
            return "Project data is not loaded.";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Project Diagnostic & Bug Analysis ===\n");
        report.append("Target Screen: ").append(javaName).append(" (").append(xmlName).append(")\n\n");

        // 1. Check compile logs
        CompileErrorSaver saver = new CompileErrorSaver(scId);
        if (saver.logFileExists()) {
            String log = saver.getLogsFromFile();
            if (log != null && !log.trim().isEmpty()) {
                report.append("🔴 [Recent Compile Error Found]:\n");
                String[] lines = log.split("\n");
                int maxLines = Math.min(lines.length, 30);
                for (int i = 0; i < maxLines; i++) {
                    report.append("  ").append(lines[i]).append("\n");
                }
                if (lines.length > 30) {
                    report.append("  ... (").append(lines.length - 30).append(" more lines)\n");
                }
                report.append("\n");
            }
        }

        // 2. Collect layout view IDs
        ArrayList<ViewBean> views = data.d(xmlName);
        Set<String> viewIds = new HashSet<>();
        if (views != null) {
            for (ViewBean v : views) {
                if (v.id != null && !v.id.isEmpty()) {
                    viewIds.add(v.id);
                }
            }
        }
        report.append("Layout Views Detected: ").append(viewIds.size()).append(" views (").append(viewIds).append(")\n");

        // 3. Check event blocks for view ID references
        Map<String, ArrayList<BlockBean>> fileBlocks = data.b(javaName);
        List<String> warnings = new ArrayList<>();
        int totalBlocks = 0;
        if (fileBlocks != null) {
            for (Map.Entry<String, ArrayList<BlockBean>> entry : fileBlocks.entrySet()) {
                String eventKey = entry.getKey();
                ArrayList<BlockBean> blocks = entry.getValue();
                totalBlocks += blocks.size();
                for (BlockBean b : blocks) {
                    if (b.parameters != null && !b.parameters.isEmpty()) {
                        for (String param : b.parameters) {
                            if (isViewOpcode(b.opCode) && !viewIds.contains(param) && !param.isEmpty()) {
                                warnings.add("Event \"" + eventKey + "\" uses view block '" + b.opCode
                                        + "' referencing view ID '" + param + "', but view '" + param
                                        + "' does not exist in " + xmlName + "!");
                            }
                        }
                    }
                }
            }
        }
        report.append("Total Logic Blocks in ").append(javaName).append(": ").append(totalBlocks).append("\n");

        if (!warnings.isEmpty()) {
            report.append("\n⚠️ [Potential Bugs / Mismatches Detected]:\n");
            for (String w : warnings) {
                report.append("• ").append(w).append("\n");
            }
            report.append("\nRecommendation: Rename the missing view ID in the layout or update the block to point to an existing view.\n");
        } else {
            report.append("\n✅ Logic & Layout Consistency: All view block references match existing layout views.\n");
        }

        return report.toString().trim();
    }

    private static boolean isViewOpcode(String op) {
        if (op == null) return false;
        return op.startsWith("setText") || op.startsWith("getText") || op.startsWith("setImage")
                || op.startsWith("setVisible") || op.startsWith("setEnable") || op.startsWith("setClickable")
                || op.startsWith("setBgColor") || op.startsWith("setTextColor") || op.startsWith("setChecked");
    }
}
