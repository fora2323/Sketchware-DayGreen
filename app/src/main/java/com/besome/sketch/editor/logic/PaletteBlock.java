package com.besome.sketch.editor.logic;

import static pro.sketchware.utility.ThemeUtils.getColor;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import a.a.a.Rs;
import a.a.a.Ts;
import a.a.a.wB;
import pro.sketchware.R;
import pro.sketchware.databinding.PaletteBlockBinding;

public class PaletteBlock extends LinearLayout {

    public float f = 0.0F;
    private PaletteBlockBinding binding;
    private Context context;

    public PaletteBlock(Context context) {
        super(context);
        initialize(context);
    }

    public PaletteBlock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(Context context) {
        this.context = context;
        binding = PaletteBlockBinding.inflate(LayoutInflater.from(context), this, true);
        f = wB.a(context, 1.0F);
    }

    public Ts a(String var1, String var2, String var3) {
        View view = new View(context);
        view.setLayoutParams(getLayoutParams(8.0F));
        binding.blockBuilder.addView(view);
        Rs blockView = new Rs(context, -1, var1, var2, var3);
        blockView.setContentDescription(generateContentDescription(var3));
        blockView.setBlockType(1);
        binding.blockBuilder.addView(blockView);
        return blockView;
    }

    public Ts a(String var1, String var2, String var3, String var4) {
        View view = new View(context);
        view.setLayoutParams(getLayoutParams(8.0F));
        binding.blockBuilder.addView(view);
        Rs blockView = new Rs(context, -1, var1, var2, var3, var4);
        blockView.setContentDescription(generateContentDescription(var4));
        blockView.setBlockType(1);
        binding.blockBuilder.addView(blockView);
        return blockView;
    }

    public TextView a(String title) {
        binding.actionsContainer.setVisibility(View.VISIBLE);

        var cardView = new MaterialCardView(context);
        var params = getLayoutParams(32.0F);
        params.setMargins(0, 0, 0, (int) (f * 6.0F));
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(getColor(context, R.attr.colorSecondaryContainer));
        cardView.setRadius(f * 10.0F);
        cardView.setCardElevation(0.0F);
        cardView.setStrokeWidth(0);

        var textView = new TextView(context);
        textView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        textView.setText(title);
        textView.setTextColor(getColor(context, R.attr.colorOnSecondaryContainer));
        textView.setTextSize(11.5F);
        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textView.setIncludeFontPadding(false);
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding((int) (f * 10.0F), 0, (int) (f * 10.0F), 0);

        TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            textView.setBackgroundResource(outValue.resourceId);
        }

        cardView.addView(textView);
        binding.actionsContainer.addView(cardView);
        return textView;
    }

    public void a() {
        binding.blockBuilder.removeAllViews();
        binding.actionsContainer.removeAllViews();
        binding.actionsContainer.setVisibility(View.GONE);
        binding.scroll.scrollTo(0, 0);
        binding.scrollHorizontal.scrollTo(0, 0);
    }

    public void a(String title, int color) {
        boolean isError = color == getColor(context, R.attr.colorError);
        boolean isFirst = binding.blockBuilder.getChildCount() == 0;

        if (isError) {
            var cardView = new MaterialCardView(context);
            var params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = (int) (f * (isFirst ? 4.0F : 12.0F));
            params.bottomMargin = (int) (f * 4.0F);
            cardView.setLayoutParams(params);
            cardView.setCardBackgroundColor(getColor(context, R.attr.colorErrorContainer));
            cardView.setRadius(f * 8.0F);
            cardView.setCardElevation(0.0F);
            cardView.setStrokeWidth(0);

            TextView textView = new TextView(context);
            textView.setText(title);
            textView.setTextColor(getColor(context, R.attr.colorOnErrorContainer));
            textView.setTextSize(11.0F);
            textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            textView.setIncludeFontPadding(false);
            textView.setPadding((int) (f * 10.0F), (int) (f * 5.0F), (int) (f * 10.0F), (int) (f * 5.0F));
            cardView.addView(textView);

            binding.blockBuilder.addView(cardView);
            return;
        }

        LinearLayout header = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (f * (isFirst ? 4.0F : 14.0F));
        header.setLayoutParams(params);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        View accent = new View(context);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams((int) (f * 3.0F), (int) (f * 12.0F));
        accentParams.setMarginEnd((int) (f * 6.0F));
        accent.setLayoutParams(accentParams);
        GradientDrawable accentDrawable = new GradientDrawable();
        accentDrawable.setColor(getColor(context, R.attr.colorPrimary));
        accentDrawable.setCornerRadius(f * 2.0F);
        accent.setBackground(accentDrawable);
        header.addView(accent);

        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextColor(getColor(context, R.attr.colorPrimary));
        textView.setTextSize(11.5F);
        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textView.setLetterSpacing(0.02F);
        textView.setIncludeFontPadding(false);
        header.addView(textView);

        binding.blockBuilder.addView(header);
    }

    public void addDeprecatedBlock(String message, String type, String opCode) {
        if (message != null && !message.isEmpty()) {
            a(message, getColor(context, R.attr.colorSurfaceContainerHigh));
        }
        Ts blockView = a("", type, opCode);
        blockView.e = 0xFFBDBDBD;
        blockView.setTag(opCode);
    }

    private String generateContentDescription(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(name.charAt(0));
        for (int i = 1; i < name.length(); i++) {
            char currentChar = name.charAt(i);
            if (Character.isUpperCase(currentChar)) {
                if (i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1)) || Character.isLowerCase(name.charAt(i - 1))) {
                    result.append(' ');
                }
            }
            result.append(currentChar);
        }
        return result.toString();
    }

    private LinearLayout.LayoutParams getLayoutParams(float heightMultiplier) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (f * heightMultiplier));
    }

    public void setDragEnabled(boolean dragEnabled) {
        if (dragEnabled) {
            binding.scroll.b();
            binding.scrollHorizontal.b();
        } else {
            binding.scroll.a();
            binding.scrollHorizontal.a();
        }
    }

    public void setMinWidth(int minWidth) {
        binding.scroll.setMinimumWidth(minWidth - (int) (f * 5.0F));
        binding.scrollHorizontal.setMinimumWidth(minWidth - (int) (f * 5.0F));
        getLayoutParams().width = minWidth;
    }

    public void setUseScroll(boolean useScroll) {
        binding.scroll.setUseScroll(useScroll);
        binding.scrollHorizontal.setUseScroll(useScroll);
    }
}
