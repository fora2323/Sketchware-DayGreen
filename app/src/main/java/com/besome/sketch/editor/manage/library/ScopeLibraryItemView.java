package com.besome.sketch.editor.manage.library;

import android.content.Context;

import androidx.annotation.Nullable;

import com.besome.sketch.beans.ProjectLibraryBean;

import pro.sketchware.R;

public class ScopeLibraryItemView extends LibraryItemView {

    public ScopeLibraryItemView(Context context) {
        super(context);
    }

    @Override
    public void setData(@Nullable ProjectLibraryBean projectLibraryBean) {
        icon.setImageResource(R.drawable.ic_mtrl_library);
        title.setText("Scope Library");
        description.setText("Set compileOnly and runtime scope for dependencies");
    }
}