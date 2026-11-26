package com.besome.sketch.editor.logic;

import static com.besome.sketch.editor.logic.PaletteSelector.paletteSelectorRecord;
import static com.google.android.material.color.MaterialColors.harmonizeWithPrimary;
import static com.google.android.material.color.MaterialColors.isColorLight;
import static pro.sketchware.utility.ThemeUtils.getColor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import a.a.a.Vs;
import a.a.a.wB;
import pro.sketchware.R;
import pro.sketchware.databinding.PaletteSelectorItemBinding;

public class PaletteSelectorAdapter extends RecyclerView.Adapter<PaletteSelectorAdapter.PaletteSelectorViewHolder> {

    private final PaletteSelector paletteSelector;
    private final List<paletteSelectorRecord> paletteList = new ArrayList<>();
    private final Context context;
    private final Vs onBlockCategorySelectListener;
    private int selectedPosition = -1;

    public PaletteSelectorAdapter(PaletteSelector paletteSelector, Vs onBlockCategorySelectListener) {
        this.paletteSelector = paletteSelector;
        context = paletteSelector.getContext();
        this.onBlockCategorySelectListener = onBlockCategorySelectListener;
    }

    public void setPalettes(List<paletteSelectorRecord> list) {
        paletteList.clear();
        for (paletteSelectorRecord palette : list) {
            if (paletteSelector.matchesSearch(palette.text())) {
                paletteList.add(palette);
            }
        }
        if (!paletteList.isEmpty() && (selectedPosition < 0 || selectedPosition >= paletteList.size())) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaletteSelectorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PaletteSelectorItemBinding binding = PaletteSelectorItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PaletteSelectorViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PaletteSelectorViewHolder holder, int position) {
        paletteSelectorRecord item = paletteList.get(position);
        int id = item.index();
        String title = item.text();
        int color = harmonizeWithPrimary(context, item.color());
        boolean isSelected = position == selectedPosition;

        holder.binding.tvCategory.setText(title);
        holder.binding.tvCategory.setTextColor(
                isSelected ?
                        (isColorLight(color) ? 0xFF1C1B1F : Color.WHITE)
                        : getColor(context, R.attr.colorOnSurface));
        holder.binding.tvCategory.setTypeface(
                isSelected ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.SANS_SERIF);

        FrameLayout.LayoutParams bgParams = (FrameLayout.LayoutParams) holder.binding.bg.getLayoutParams();
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setColor(color);
        if (isSelected) {
            bgParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            bgParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            bgParams.setMarginStart(0);
            bgDrawable.setCornerRadius(wB.a(context, 8f));
        } else {
            bgParams.width = (int) wB.a(context, 4f);
            bgParams.height = (int) wB.a(context, 16f);
            bgParams.setMarginStart((int) wB.a(context, 4f));
            bgDrawable.setCornerRadius(wB.a(context, 4f));
        }
        holder.binding.bg.setLayoutParams(bgParams);
        holder.binding.bg.setBackground(bgDrawable);

        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getAbsoluteAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) {
                return;
            }
            selectedPosition = adapterPos;
            notifyDataSetChanged();
            if (onBlockCategorySelectListener != null) {
                onBlockCategorySelectListener.a(id, color);
            }
        });
    }

    @Override
    public int getItemCount() {
        return paletteList.size();
    }

    public void selectPaletteById(int tag) {
        for (int i = 0; i < paletteList.size(); i++) {
            int paletteId = paletteList.get(i).index();
            if (paletteId == tag) {
                selectedPosition = i;
                notifyDataSetChanged();
                paletteSelector.scrollToPosition(i);
                if (onBlockCategorySelectListener != null) {
                    onBlockCategorySelectListener.a(paletteId, harmonizeWithPrimary(context, paletteList.get(i).color()));
                }
                break;
            }
        }
    }

    public void selectPosition(int pos) {
        if (pos >= 0 && pos < paletteList.size()) {
            selectedPosition = pos;
            notifyDataSetChanged();
            if (onBlockCategorySelectListener != null) {
                onBlockCategorySelectListener.a(paletteList.get(pos).index(), harmonizeWithPrimary(context, paletteList.get(pos).color()));
            }
        }
    }

    public static class PaletteSelectorViewHolder extends RecyclerView.ViewHolder {
        final PaletteSelectorItemBinding binding;

        public PaletteSelectorViewHolder(PaletteSelectorItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
