package com.besome.sketch.editor.view.item;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.appcompat.widget.AppCompatEditText;

import com.besome.sketch.Config;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.ItemView;

import a.a.a.wB;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.project.GetProjectInfo;

public class ItemEditText extends AppCompatEditText implements ItemView {

    public ViewBean viewBean;

    public boolean selected;

    public boolean fixed;

    public Paint paint;

    public float dip;

    public Drawable originalBg;

    public ItemEditText(Context context) {
        super(context);
        initialize(context);
    }

    public void initialize(Context context) {
        dip = wB.a(context, 1.0f);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x9599d5d0);
        setDrawingCacheEnabled(true);
        setFocusable(false);
        originalBg = getBackground();
    }

    @Override
    public ViewBean getBean() {
        return viewBean;
    }

    @Override
    public void setBean(ViewBean viewBean) {
        this.viewBean = viewBean;
    }

    @Override
    public boolean getFixed() {
        return fixed;
    }

    @Override
    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public boolean getSelection() {
        return selected;
    }

    @Override
    public void setSelection(boolean selected) {
        this.selected = selected;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (selected) {
            canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), paint);
        }
        super.onDraw(canvas);
    }

    @Override
    public void setBackgroundColor(int color) {
        if (color == 0xffffff) {
            setBackground(originalBg);
            try {
                setBackgroundTintList(ColorStateList.valueOf(GetProjectInfo.getAccentHexColor(Configs.currentProjectID)));
            } catch (Exception ignored) {

            }
        } else {
            setBackgroundTintList(null);
            super.setBackgroundColor(color);
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding((int) (left * dip), (int) (top * dip), (int) (right * dip), (int) (bottom * dip));
    }
}