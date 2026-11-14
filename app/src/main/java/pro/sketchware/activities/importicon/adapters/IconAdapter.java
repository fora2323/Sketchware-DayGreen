package pro.sketchware.activities.importicon.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import pro.sketchware.R;
import pro.sketchware.activities.importicon.data.IconCatalog;
import pro.sketchware.activities.importicon.data.IconDownloader;
import pro.sketchware.databinding.ImportIconListItemBinding;
import pro.sketchware.utility.SvgUtils;

public class IconAdapter extends ListAdapter<IconCatalog.IconItem, IconAdapter.ViewHolder> {

    private static final int DEFAULT_ICON_COLOR = Color.parseColor("#9E9E9E");

    private static final DiffUtil.ItemCallback<IconCatalog.IconItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull IconCatalog.IconItem oldItem, @NonNull IconCatalog.IconItem newItem) {
                    return oldItem.name.equals(newItem.name);
                }

                @Override
                public boolean areContentsTheSame(@NonNull IconCatalog.IconItem oldItem, @NonNull IconCatalog.IconItem newItem) {
                    return oldItem.name.equals(newItem.name);
                }
            };

    private final SvgUtils svgUtils;
    private final IconDownloader iconDownloader;
    private final OnIconSelectedListener listener;

    private String currentStyle = IconDownloader.DEFAULT_STYLE;
    private String currentFillType = IconDownloader.DEFAULT_FILL_TYPE;
    private int currentOpticalSize = IconDownloader.DEFAULT_OPTICAL_SIZE;

    public IconAdapter(
            @NonNull Context context,
            @NonNull IconDownloader iconDownloader,
            @NonNull OnIconSelectedListener listener
    ) {
        super(DIFF_CALLBACK);
        this.svgUtils = new SvgUtils(context.getApplicationContext());
        this.iconDownloader = iconDownloader;
        this.listener = listener;
    }

    public void updateVariant(@NonNull String style, @NonNull String fillType, int opticalSize) {
        this.currentStyle = IconDownloader.normalizeStyle(style);
        this.currentFillType = IconDownloader.normalizeFillType(fillType);
        this.currentOpticalSize = IconDownloader.normalizeOpticalSize(opticalSize);
        notifyDataSetChanged();
    }

    @NonNull
    public String getCurrentStyle() {
        return currentStyle;
    }

    @NonNull
    public String getCurrentFillType() {
        return currentFillType;
    }

    public int getCurrentOpticalSize() {
        return currentOpticalSize;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IconCatalog.IconItem item = getItem(position);
        String iconName = item.name;
        String style = currentStyle;
        String fillType = currentFillType;
        int opticalSize = currentOpticalSize;
        String variantKey = style + "/" + fillType + "/" + opticalSize + "/" + iconName;

        holder.boundVariantKey = variantKey;
        holder.itemBinding.title.setText(iconName);
        holder.itemBinding.img.setColorFilter(DEFAULT_ICON_COLOR, PorterDuff.Mode.SRC_IN);

        if (iconDownloader.isCached(iconName, style, fillType, opticalSize)) {
            holder.itemBinding.progressLoading.setVisibility(View.GONE);
            holder.itemBinding.img.setVisibility(View.VISIBLE);
            File cachedFile = iconDownloader.getCachedSvgFile(iconName, style, fillType, opticalSize);
            svgUtils.loadImage(holder.itemBinding.img, cachedFile.getAbsolutePath());
        } else {
            holder.itemBinding.img.setImageDrawable(null);
            holder.itemBinding.img.setVisibility(View.INVISIBLE);
            holder.itemBinding.progressLoading.setVisibility(View.VISIBLE);

            iconDownloader.downloadSvgAsync(iconName, style, fillType, opticalSize, new IconDownloader.DownloadCallback() {
                @Override
                public void onSuccess(@NonNull File svgFile) {
                    if (variantKey.equals(holder.boundVariantKey)) {
                        holder.itemBinding.progressLoading.setVisibility(View.GONE);
                        holder.itemBinding.img.setVisibility(View.VISIBLE);
                        svgUtils.loadImage(holder.itemBinding.img, svgFile.getAbsolutePath());
                    }
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    if (variantKey.equals(holder.boundVariantKey)) {
                        holder.itemBinding.progressLoading.setVisibility(View.GONE);
                        holder.itemBinding.img.setVisibility(View.VISIBLE);
                        holder.itemBinding.img.setImageResource(R.drawable.ic_remove_grey600_24dp);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImportIconListItemBinding binding = ImportIconListItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    public interface OnIconSelectedListener {
        void onIconSelected(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImportIconListItemBinding itemBinding;
        private String boundVariantKey;

        public ViewHolder(@NonNull ImportIconListItemBinding binding) {
            super(binding.getRoot());
            this.itemBinding = binding;
            binding.getRoot().setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onIconSelected(position);
                }
            });
        }
    }
}