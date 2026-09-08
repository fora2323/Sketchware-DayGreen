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

    public static void syncXml(Context context, String scId, String fileName, String xmlContent) {
        if (fileName.endsWith(".xml")) {
            if (isLayoutFile(context, fileName)) {
                syncLayout(scId, fileName, xmlContent);
            } else if (fileName.equals("strings.xml")) {
                syncStrings(scId, xmlContent);
            } else if (fileName.equals("colors.xml")) {
                syncColors(scId, xmlContent);
            }
        }
    }

    private static boolean isLayoutFile(Context context, String fileName) {
        try {
            return jC.b(context.getSharedPreferences("hsce", Context.MODE_PRIVATE).getString("sc_id", "")).e().contains(fileName);
        } catch (Exception e) {
            return false;
        }
    }

    private static void syncLayout(String scId, String fileName, String xmlContent) {
        try {
            ViewBeanParser parser = new ViewBeanParser(xmlContent);
            parser.setSkipRoot(true);
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
            jC.a(scId).h(); 
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
            // manager.convertXmlStringsToListMap already handles saving to file
            // We just need to make sure internal project data is updated if needed.
            // jC.a(scId).h() usually saves everything.
            jC.a(scId).h();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void syncColors(String scId, String xmlContent) {
        try {
            ColorsEditorManager manager = new ColorsEditorManager(scId);
            manager.parseColorsXML(manager.getResColorsList(), xmlContent);
            jC.a(scId).h();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
