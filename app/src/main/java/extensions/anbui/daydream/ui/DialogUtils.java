package extensions.anbui.daydream.ui;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.utils.ColorUtils;
import pro.sketchware.R;

public class DialogUtils {

    public static String TAG = Configs.universalTAG + "DialogUtils";

    public static void oneDialog(Activity _context, String _title, String _message, String _textPositiveButton, boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onDismiss) {
        View buttonsView = LayoutInflater.from(_context).inflate(R.layout.daydream_dialog_layout, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(_context).create();
        dialog.setCancelable(_cancel);
        dialog.setView(buttonsView);

        ImageView icon = buttonsView.findViewById(R.id.icon);
        TextView title = buttonsView.findViewById(R.id.tv_title);
        TextView content = buttonsView.findViewById(R.id.tv_content);
        TextView positiveButton = buttonsView.findViewById(R.id.positiveButton);
        TextView negativeButton = buttonsView.findViewById(R.id.negativeButton);
        TextView neutralButton = buttonsView.findViewById(R.id.neutralButton);

        if (_isicon) {
            icon.setImageResource(_iconid);
        } else {
            icon.setVisibility(View.GONE);
        }

        title.setText(_title);
        content.setText(_message);

        if (UIController.isUsingThemeNightMode()
                || !ColorUtils.isColorLight(MaterialColors.getColor(positiveButton, com.google.android.material.R.attr.colorSecondaryContainer)))
            positiveButton.setTextColor(Color.WHITE);

        positiveButton.setText(_textPositiveButton);
        positiveButton.setBackgroundResource(R.drawable.dr_dialog_shape_single_button);
        negativeButton.setVisibility(View.GONE);
        neutralButton.setVisibility(View.GONE);

        positiveButton.setOnClickListener(v -> {
            if (_onPositive != null) _onPositive.run();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(dialog1 -> {
            if (_onDismiss != null) _onDismiss.run();
        });
        dialog.show();
        Log.i(TAG, "oneDialog: " + _title);
    }

    public static void twoDialog(Activity _context, String _title, String _message, String _textPositiveButton, String _textNegativeButton, boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onNegative, Runnable _onDismiss) {
        View buttonsView = LayoutInflater.from(_context).inflate(R.layout.daydream_dialog_layout, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(_context).create();
        dialog.setCancelable(_cancel);
        dialog.setView(buttonsView);

        ImageView icon = buttonsView.findViewById(R.id.icon);
        TextView title = buttonsView.findViewById(R.id.tv_title);
        TextView content = buttonsView.findViewById(R.id.tv_content);
        TextView positiveButton = buttonsView.findViewById(R.id.positiveButton);
        TextView negativeButton = buttonsView.findViewById(R.id.negativeButton);
        TextView neutralButton = buttonsView.findViewById(R.id.neutralButton);

        if (_isicon) {
            icon.setImageResource(_iconid);
        } else {
            icon.setVisibility(View.GONE);
        }

        title.setText(_title);
        content.setText(_message);

        if (UIController.isUsingThemeNightMode()
                || !ColorUtils.isColorLight(MaterialColors.getColor(positiveButton, com.google.android.material.R.attr.colorSecondaryContainer))) {
            positiveButton.setTextColor(Color.WHITE);
            negativeButton.setTextColor(Color.WHITE);
        }

        positiveButton.setText(_textPositiveButton);
        negativeButton.setText(_textNegativeButton);
        negativeButton.setBackgroundResource(R.drawable.dr_dialog_shape_bottom_button);
        neutralButton.setVisibility(View.GONE);

        positiveButton.setOnClickListener(v -> {
            if (_onPositive != null) _onPositive.run();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> {
            if (_onNegative != null) _onNegative.run();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(dialog1 -> {
            if (_onDismiss != null) _onDismiss.run();
        });
        dialog.show();
        Log.i(TAG, "twoDialog: " + _title);
    }

    public static void threeDialog(Activity _context, String _title, String _message, String _textPositiveButton, String _textNegativeButton, String _textNeutralButton, boolean _isicon, int _iconid, boolean _cancel, Runnable _onPositive, Runnable _onNegative, Runnable _onNeutral, Runnable _onDismiss) {
        View buttonsView = LayoutInflater.from(_context).inflate(R.layout.daydream_dialog_layout, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(_context).create();
        dialog.setCancelable(_cancel);
        dialog.setView(buttonsView);

        ImageView icon = buttonsView.findViewById(R.id.icon);
        TextView title = buttonsView.findViewById(R.id.tv_title);
        TextView content = buttonsView.findViewById(R.id.tv_content);
        TextView positiveButton = buttonsView.findViewById(R.id.positiveButton);
        TextView negativeButton = buttonsView.findViewById(R.id.negativeButton);
        TextView neutralButton = buttonsView.findViewById(R.id.neutralButton);

        if (_isicon) {
            icon.setImageResource(_iconid);
        } else {
            icon.setVisibility(View.GONE);
        }

        title.setText(_title);
        content.setText(_message);

        if (UIController.isUsingThemeNightMode()
                || !ColorUtils.isColorLight(MaterialColors.getColor(positiveButton, com.google.android.material.R.attr.colorSecondaryContainer))) {
            positiveButton.setTextColor(Color.WHITE);
            negativeButton.setTextColor(Color.WHITE);
            neutralButton.setTextColor(Color.WHITE);
        }

        positiveButton.setText(_textPositiveButton);
        negativeButton.setText(_textNegativeButton);
        neutralButton.setText(_textNeutralButton);

        positiveButton.setOnClickListener(v -> {
            if (_onPositive != null) _onPositive.run();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> {
            if (_onNegative != null) _onNegative.run();
            dialog.dismiss();
        });

        neutralButton.setOnClickListener(v -> {
            if (_onNeutral != null) _onNeutral.run();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(dialog1 -> {
            if (_onDismiss != null) _onDismiss.run();
        });
        dialog.show();
        Log.i(TAG, "threeDialog: " + _title);
    }
}
