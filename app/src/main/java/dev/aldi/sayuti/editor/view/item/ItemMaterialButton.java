package dev.aldi.sayuti.editor.view.item;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.ItemView;
import com.google.android.material.button.MaterialButton;

import a.a.a.wB;

public class ItemMaterialButton extends MaterialButton implements ItemView {

    private final Paint paint;
    private final float paddingFactor;
    private final Rect rect;
    private final ColorStateList defaultBackgroundTint;
    private final ColorStateList defaultTextColors;
    private ViewBean viewBean;
    private boolean hasSelection;
    private boolean hasFixed;

    public ItemMaterialButton(Context context) {
        super(context);
        paddingFactor = wB.a(context, 1.0f);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x9599d5d0);
        rect = new Rect();
        defaultBackgroundTint = getBackgroundTintList();
        defaultTextColors = getTextColors();

        setDrawingCacheEnabled(true);
        setFocusable(false);
    }

    public ColorStateList getDefaultBackgroundTint() {
        return defaultBackgroundTint;
    }

    public ColorStateList getDefaultTextColors() {
        return defaultTextColors;
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
        return hasFixed;
    }

    @Override
    public void setFixed(boolean z) {
        hasFixed = z;
    }

    public boolean getSelection() {
        return hasSelection;
    }

    @Override
    public void setSelection(boolean z) {
        hasSelection = z;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hasSelection) {
            rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            canvas.drawRect(rect, paint);
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        if (color == 0xffffff) {
            setBackgroundTintList(defaultBackgroundTint);
        } else if (color == 0) {
            setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        } else {
            setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding((int) (left * paddingFactor), (int) (top * paddingFactor), (int) (right * paddingFactor), (int) (bottom * paddingFactor));
    }

}
