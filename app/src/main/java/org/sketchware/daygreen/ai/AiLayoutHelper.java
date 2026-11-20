package org.sketchware.daygreen.ai;

import android.util.Log;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ViewBean;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import a.a.a.eC;
import a.a.a.jC;
import a.a.a.wq;
import pro.sketchware.tools.ViewBeanParser;
import pro.sketchware.utility.FileUtil;

public final class AiLayoutHelper {

    private static final String TAG = "AiLayoutHelper";

    private AiLayoutHelper() {}

    public static final class Result {
        public final boolean success;
        public final String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static String readLayout(String scId, String xmlName) {
        if (scId == null || scId.isEmpty()) {
            return "Project ID is missing.";
        }
        xmlName = normalizeXmlName(xmlName);

        eC data = jC.a(scId);
        if (data == null) {
            return "Project data is not loaded. Please reopen the project.";
        }

        ArrayList<ViewBean> views = data.d(xmlName);
        StringBuilder sb = new StringBuilder();
        sb.append("Layout: ").append(xmlName);

        if (views == null || views.isEmpty()) {
            File layoutFile = new File(wq.b(scId), "files/resource/layout/" + xmlName);
            if (layoutFile.exists()) {
                sb.append(" (Loaded from XML file)\n\n```xml\n")
                        .append(FileUtil.readFile(layoutFile.getAbsolutePath()))
                        .append("\n```");
                return sb.toString();
            }
            sb.append(" (No views currently in this layout)");
            return sb.toString();
        }

        sb.append(" (").append(views.size()).append(" views)\n\n");
        for (ViewBean v : views) {
            String typeName = ViewBean.getViewTypeName(v.type);
            sb.append("- id: \"").append(v.id).append("\"")
                    .append(", type: ").append(typeName.isEmpty() ? v.convert : typeName)
                    .append(", parent: ").append(v.parent == null || v.parent.isEmpty() ? "root" : "\"" + v.parent + "\"");

            if (v.text != null && v.text.text != null && !v.text.text.isEmpty()) {
                sb.append(", text: \"").append(v.text.text).append("\"");
            }
            if (v.layout != null) {
                sb.append(", width: ").append(dimenToString(v.layout.width));
                sb.append(", height: ").append(dimenToString(v.layout.height));
                if (v.type == ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
                    sb.append(", orientation: ").append(v.layout.orientation == 0 ? "horizontal" : "vertical");
                }
            }
            sb.append("\n");
        }

        File layoutFile = new File(wq.b(scId), "files/resource/layout/" + xmlName);
        if (layoutFile.exists()) {
            sb.append("\nLayout XML code:\n```xml\n")
                    .append(FileUtil.readFile(layoutFile.getAbsolutePath()))
                    .append("\n```");
        }

        return sb.toString().trim();
    }

    public static Result applyLayoutXml(String scId, String xmlName, String xmlContent) {
        if (scId == null || scId.isEmpty()) {
            return new Result(false, "Project ID is missing.");
        }
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return new Result(false, "XML content is empty.");
        }
        xmlName = normalizeXmlName(xmlName);

        try {
            ViewBeanParser parser = new ViewBeanParser(xmlContent);
            parser.setScId(scId);
            ArrayList<ViewBean> beans = parser.parse();

            if (beans == null || beans.isEmpty()) {
                return new Result(false, "Failed to parse layout XML: no valid views recognized.");
            }

            eC data = jC.a(scId);
            if (data == null) {
                return new Result(false, "Project data is not loaded.");
            }

            if (data.c == null) {
                data.c = new HashMap<>();
            }
            data.c.put(xmlName, beans);
            data.j();

            File layoutDir = new File(wq.b(scId), "files/resource/layout");
            if (!layoutDir.exists()) {
                layoutDir.mkdirs();
            }
            File layoutFile = new File(layoutDir, xmlName);
            FileUtil.writeFile(layoutFile.getAbsolutePath(), xmlContent);

            return new Result(true, "Successfully applied layout to " + xmlName + " (" + beans.size() + " views).");
        } catch (Exception e) {
            Log.e(TAG, "applyLayoutXml failed", e);
            return new Result(false, "Failed to parse and apply layout XML: " + e.getMessage());
        }
    }

    public static Result addWidget(String scId, String xmlName, String widgetType, String id,
                                   String parentId, String text, String width, String height, String orientation) {
        if (scId == null || scId.isEmpty()) {
            return new Result(false, "Project ID is missing.");
        }
        xmlName = normalizeXmlName(xmlName);

        eC data = jC.a(scId);
        if (data == null) {
            return new Result(false, "Project data is not loaded.");
        }

        ArrayList<ViewBean> currentViews = data.d(xmlName);
        if (currentViews == null) {
            currentViews = new ArrayList<>();
        }

        Set<String> existingIds = new HashSet<>();
        for (ViewBean v : currentViews) {
            if (v.id != null) existingIds.add(v.id);
        }

        int type = resolveViewType(widgetType);
        String finalId = (id == null || id.trim().isEmpty())
                ? ViewBeanParser.generateUniqueId(existingIds, type, widgetType)
                : id.trim();

        if (existingIds.contains(finalId)) {
            return new Result(false, "A view with ID \"" + finalId + "\" already exists in " + xmlName + ".");
        }

        ViewBean bean = new ViewBean();
        bean.type = type;
        bean.id = finalId;
        bean.convert = widgetType;
        bean.parent = (parentId != null && !parentId.trim().isEmpty()) ? parentId.trim() : "root";
        bean.parentType = (bean.parent.equals("root")) ? -1 : ViewBean.VIEW_TYPE_LAYOUT_LINEAR;

        LayoutBean layout = bean.layout;
        layout.width = parseDimension(width, -1);
        layout.height = parseDimension(height, -2);
        layout.paddingLeft = 8;
        layout.paddingTop = 8;
        layout.paddingRight = 8;
        layout.paddingBottom = 8;

        if (type == ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
            layout.orientation = "horizontal".equalsIgnoreCase(orientation) ? 0 : 1;
        }

        if (text != null && !text.isEmpty()) {
            bean.text.text = text;
        }

        bean.index = currentViews.size();
        data.a(xmlName, bean);
        data.j();

        return new Result(true, "Added " + widgetType + " with ID \"" + finalId + "\" to " + xmlName + ".");
    }

    private static int resolveViewType(String name) {
        if (name == null) return ViewBean.VIEW_TYPE_WIDGET_BUTTON;
        String lower = name.toLowerCase(Locale.ROOT).trim();
        switch (lower) {
            case "linearlayout":
            case "linear":
            case "linear_layout":
                return ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
            case "relativelayout":
            case "relative":
                return ViewBean.VIEW_TYPE_LAYOUT_RELATIVE;
            case "scrollview":
            case "vscrollview":
                return ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW;
            case "hscrollview":
            case "horizontalscrollview":
                return ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW;
            case "button":
            case "btn":
            case "materialbutton":
                return ViewBean.VIEW_TYPE_WIDGET_BUTTON;
            case "textview":
            case "text":
            case "tv":
                return ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW;
            case "edittext":
            case "edit":
            case "et":
            case "input":
                return ViewBean.VIEW_TYPE_WIDGET_EDITTEXT;
            case "imageview":
            case "img":
            case "image":
                return ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW;
            case "webview":
                return ViewBean.VIEW_TYPE_WIDGET_WEBVIEW;
            case "progressbar":
            case "progress":
                return ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR;
            case "listview":
            case "list":
                return ViewBean.VIEW_TYPE_WIDGET_LISTVIEW;
            case "spinner":
                return ViewBean.VIEW_TYPE_WIDGET_SPINNER;
            case "checkbox":
                return ViewBean.VIEW_TYPE_WIDGET_CHECKBOX;
            case "switch":
                return ViewBean.VIEW_TYPE_WIDGET_SWITCH;
            case "seekbar":
                return ViewBean.VIEW_TYPE_WIDGET_SEEKBAR;
            case "calendarview":
            case "calendar":
                return ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW;
            default:
                int byName = ViewBean.getViewTypeByTypeName(name);
                return byName >= 0 ? byName : ViewBean.VIEW_TYPE_WIDGET_BUTTON;
        }
    }

    private static int parseDimension(String dim, int defaultVal) {
        if (dim == null || dim.trim().isEmpty()) return defaultVal;
        String d = dim.trim().toLowerCase(Locale.ROOT);
        if (d.contains("match")) return -1;
        if (d.contains("wrap")) return -2;
        try {
            return Integer.parseInt(d.replaceAll("[^0-9\\-]", ""));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static String dimenToString(int val) {
        if (val == -1) return "match_parent";
        if (val == -2) return "wrap_content";
        return val + "dp";
    }

    private static String normalizeXmlName(String xmlName) {
        if (xmlName == null || xmlName.trim().isEmpty()) {
            return "main.xml";
        }
        String res = xmlName.trim();
        if (!res.endsWith(".xml")) {
            res = res + ".xml";
        }
        return res;
    }
}
