package pro.sketchware.activities.importicon.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
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
    private final Drawable placeholderDrawable;

    public IconAdapter(@NonNull Context context, @NonNull IconDownloader iconDownloader, @NonNull OnIconSelectedListener listener) {
        super(DIFF_CALLBACK);
        this.svgUtils = new SvgUtils(context);
        this.iconDownloader = iconDownloader;
        this.listener = listener;
        this.placeholderDrawable = createPlaceholderDrawable(context);
    }

    @NonNull
    private Drawable createPlaceholderDrawable(@NonNull Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(12f * density);
        drawable.setColor(0x1F9E9E9E);
        return drawable;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IconCatalog.IconItem item = getItem(position);
        String iconName = item.name;
        holder.boundIconName = iconName;

        holder.itemBinding.title.setText(iconName);
        holder.itemBinding.img.setColorFilter(DEFAULT_ICON_COLOR, PorterDuff.Mode.SRC_IN);

        if (iconDownloader.isCached(iconName)) {
            File cachedFile = iconDownloader.getCachedSvgFile(iconName);
            svgUtils.loadImage(holder.itemBinding.img, cachedFile.getAbsolutePath());
        } else {
            holder.itemBinding.img.setImageDrawable(placeholderDrawable);
            iconDownloader.downloadSvgAsync(iconName, new IconDownloader.DownloadCallback() {
                @Override
                public void onSuccess(@NonNull File svgFile) {
                    if (iconName.equals(holder.boundIconName)) {
                        svgUtils.loadImage(holder.itemBinding.img, svgFile.getAbsolutePath());
                    }
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    if (iconName.equals(holder.boundIconName)) {
                        holder.itemBinding.img.setImageResource(R.drawable.ic_remove_grey600_24dp);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImportIconListItemBinding binding = ImportIconListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    public interface OnIconSelectedListener {
        void onIconSelected(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImportIconListItemBinding itemBinding;
        private String boundIconName;

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