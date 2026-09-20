package com.besome.sketch.editor.manage.library.daydream;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.Nullable;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.editor.manage.library.LibraryItemView;

import extensions.anbui.daydream.settings.DayDreamProjectSettings;
import pro.sketchware.R;

@SuppressLint("ViewConstructor")
public class DayDreamLibraryItemView extends LibraryItemView {

    private final String sc_id;

    public DayDreamLibraryItemView(Context context, String sc_id) {
        super(context);
        this.sc_id = sc_id;
    }

    @Override
    public void setData(@Nullable ProjectLibraryBean projectLibraryBean) {
        icon.setImageResource(R.drawable.ic_mtrl_cpp);
        title.setText("Native Tools (C / C++)");
        description.setText("Compile native C/C++ libraries using NDK and CMake");
        boolean isEnabled = DayDreamProjectSettings.isEnableDayDream(sc_id);
        enabled.setText(isEnabled ? "ON" : "OFF");
        enabled.setSelected(isEnabled);
    }
}
