package com.besome.sketch.editor.event;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.besome.sketch.lib.base.CollapsibleLayout;
import com.besome.sketch.lib.ui.CollapsibleButton;
import com.google.android.material.color.MaterialColors;

import java.util.List;

import a.a.a.wB;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class CollapsibleEventLayout extends CollapsibleLayout<CollapsibleButton> {
    private CollapsibleButton reset;
    private CollapsibleButton delete;
    private CollapsibleButton addToCollection;
    private CollapsibleButton edit;

    public CollapsibleEventLayout(Context context) {
        super(context);
    }

    public CollapsibleEventLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected List<CollapsibleButton> initializeButtons(@NonNull Context context) {
        reset = CollapsibleButton.create(context, 0, R.drawable.ic_mtrl_reset, R.string.common_word_reset);

        edit = CollapsibleButton.create(context, 3, R.drawable.ic_mtrl_edit, R.string.common_word_edit);
        edit.setVisibility(GONE);

        delete = CollapsibleButton.create(context, 1, R.drawable.ic_mtrl_delete, R.string.common_word_delete);
        var colorError = MaterialColors.getColor(delete, R.attr.colorError);
        var iconFilter = new PorterDuffColorFilter(colorError, PorterDuff.Mode.SRC_ATOP);
        delete.getLabel().setTextColor(colorError);
        delete.getIcon().setColorFilter(iconFilter);

        addToCollection = CollapsibleButton.create(context, 2, R.drawable.ic_mtrl_bookmark, R.string.logic_list_menu_add_to_collection);
        addToCollection.setVisibility(GONE);
        return List.of(reset, edit, delete, addToCollection);
    }

    @Override
    protected CharSequence getWarningMessage() {
        return Helper.getResString(this, R.string.common_message_confirm);
    }

    @Override
    protected CharSequence getYesLabel() {
        return Helper.getResString(this, R.string.common_word_continue);
    }

    @Override
    protected CharSequence getNoLabel() {
        return Helper.getResString(this, R.string.common_word_cancel);
    }

    public void hideDelete() {
        delete.setVisibility(GONE);
    }

    public void hideAddToCollection() {
        addToCollection.setVisibility(GONE);
    }

    public void hideEdit() {
        edit.setVisibility(GONE);
        applyCompactButtonStyle(false);
    }

    public void showDelete() {
        delete.setVisibility(VISIBLE);
    }

    public void showAddToCollection() {
        addToCollection.setVisibility(VISIBLE);
    }

    public void showEdit() {
        edit.setVisibility(VISIBLE);
        applyCompactButtonStyle(true);
    }

    private void applyCompactButtonStyle(boolean compact) {
        int orientation = compact ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL;
        int startMargin = compact ? 0 : (int) wB.a(getContext(), 6.0f);
        int topMargin = compact ? (int) wB.a(getContext(), 2.0f) : 0;
        float textSizeSp = compact ? 11.0f : 13.0f;

        for (CollapsibleButton button : new CollapsibleButton[]{reset, edit, delete, addToCollection}) {
            LinearLayout inner = button.findViewById(R.id.img_button);
            if (inner != null) {
                inner.setOrientation(orientation);
            }
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) button.getLabel().getLayoutParams();
            lp.setMarginStart(startMargin);
            lp.topMargin = topMargin;
            button.getLabel().setLayoutParams(lp);
            button.getLabel().setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        }
    }
}