package org.sketchware.daygreen.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Built-in Activity events, copied from EventsHandler.getActivityEvents().
 * Custom events from events.json are not included.
 */
public final class AiEventCatalog {

    public static final class Event {
        public final String name;
        public final String key;

        Event(String name, String key) {
            this.name = name;
            this.key = key;
        }
    }

    private static final List<String> ACTIVITY_EVENT_NAMES = Collections.unmodifiableList(Arrays.asList(
            "Import",
            "initializeLogic",
            "onActivityResult",
            "onBackPressed",
            "onPostCreate",
            "onStart",
            "onResume",
            "onPause",
            "onStop",
            "onDestroy",
            "onSaveInstanceState",
            "onRestoreInstanceState",
            "onCreateOptionsMenu",
            "onOptionsItemSelected",
            "onCreateContextMenu",
            "onContextItemSelected",
            "onTabLayoutNewTabAdded",
            "shizukuOnRequestPermissionResult",
            "onCustomMethod"
    ));

    private AiEventCatalog() {
    }

    /**
     * Key used in project data for an Activity event.
     * initializeLogic lives under the onCreate target, every other event uses name_name.
     */
    public static String keyFor(String name) {
        if ("initializeLogic".equals(name)) {
            return "onCreate_initializeLogic";
        }
        return name + "_" + name;
    }

    public static List<Event> activityEvents() {
        List<Event> out = new ArrayList<>();
        for (String name : ACTIVITY_EVENT_NAMES) {
            out.add(new Event(name, keyFor(name)));
        }
        return out;
    }

    public static boolean isKnownActivityKey(String key) {
        for (String name : ACTIVITY_EVENT_NAMES) {
            if (keyFor(name).equals(key)) {
                return true;
            }
        }
        return false;
    }
}