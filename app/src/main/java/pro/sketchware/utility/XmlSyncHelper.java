package pro.sketchware.utility;

import android.content.Context;
import android.util.Pair;

import com.besome.sketch.beans.HistoryViewBean;
import com.besome.sketch.beans.ViewBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import a.a.a.cC;
import a.a.a.jC;
import pro.sketchware.activities.resourceseditor.components.utils.ColorsEditorManager;
import pro.sketchware.activities.resourceseditor.components.utils.StringsEditorManager;
import pro.sketchware.managers.inject.InjectRootLayoutManager;
import pro.sketchware.tools.ViewBeanParser;

public class XmlSyncHelper {

    private static volatile String pendingRefreshScId = null;

    public static boolean consumePendingViewRefresh(String scId) {
        if (scId != null && scId.equals(pendingRefreshScId)) {
            pendingRefreshScId = null;
            return true;
        }
        return false;
    }

    public static void markPendingViewRefresh(String scId) {
        if (scId != null && !scId.isEmpty()) {
            pendingRefreshScId = scId;
        }
    }

    public static void syncXml(Context context, String scId, String fileName, String xmlContent) {
        if (fileName != null && fileName.endsWith(".xml")) {
            if (isLayoutFile(scId, fileName)) {
                syncLayout(scId, fileName, xmlContent);
            } else if (fileName.equals("strings.xml")) {
                syncStrings(scId, xmlContent);
            } else if (fileName.equals("colors.xml")) {
                syncColors(scId, xmlContent);
            } else {
                markPendingViewRefresh(scId);
            }
        }
    }

    private static boolean isLayoutFile(String scId, String fileName) {
        try {
            if (scId == null || scId.isEmpty() || fileName == null) {
                return false;
            }
            return jC.b(scId).e().contains(fileName);
        } catch (Exception e) {
            return false;
        }
    }

    private static void syncLayout(String scId, String fileName, String xmlContent) {
        try {
            ViewBeanParser parser = new ViewBeanParser(xmlContent);
            parser.setSkipRoot(true);
            parser.setScId(scId);
            ArrayList<ViewBean> parsedLayout = parser.parse();
            Pair<String, Map<String, String>> rootAttrs = parser.getRootAttributes();

            if (rootAttrs != null) {
                new InjectRootLayoutManager(scId).set(fileName, InjectRootLayoutManager.toRoot(rootAttrs));
            }

            HistoryViewBean bean = new HistoryViewBean();
            bean.actionOverride(parsedLayout, jC.a(scId).d(fileName));

            cC cc = cC.c(scId);
            if (!cc.c.containsKey(fileName)) {
                cc.e(fileName);
            }
            cc.a(fileName);
            cc.a(fileName, bean);

            jC.a(scId).c.put(fileName, parsedLayout);
            jC.a(scId).k();
            markPendingViewRefresh(scId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void syncStrings(String scId, String xmlContent) {
        try {
            StringsEditorManager manager = new StringsEditorManager();
            manager.sc_id = scId;
            ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
            manager.convertXmlStringsToListMap(xmlContent, listMap);
            markPendingViewRefresh(scId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void syncColors(String scId, String xmlContent) {
        try {
            ColorsEditorManager manager = new ColorsEditorManager(scId);
            manager.parseColorsXML(manager.getResColorsList(), xmlContent);
            markPendingViewRefresh(scId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
