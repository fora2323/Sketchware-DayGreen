package mod.jbk.util;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import a.a.a.FB;
import a.a.a.Rs;
import a.a.a.Ts;
import a.a.a.kq;
import mod.hey.studios.moreblock.ReturnMoreblockManager;

public class BlockUtil {
    public static void loadMoreblockPreview(ViewGroup blockArea, String spec) {
        var moreblock = new Rs(blockArea.getContext(), 0, ReturnMoreblockManager.getMbName(spec), ReturnMoreblockManager.getMoreblockType(spec), "definedFunc");
        blockArea.addView(moreblock);

        loadPreviewBlockVariables(blockArea, moreblock, spec);
        moreblock.k();
    }

    /**
     * Loads the Variable Blocks of a Block that's for preview only.
     */
    public static void loadPreviewBlockVariables(ViewGroup blockArea, Rs previewBlock, String spec) {
        int id = 0;
        for (var specPart : FB.c(spec)) {
            if (specPart.charAt(0) != '%') {
                continue;
            }

            var variable = getVariableBlock(blockArea.getContext(), id + 1, specPart, "getVar");
            if (variable != null && id < previewBlock.V.size()) {
                blockArea.addView(variable);
                previewBlock.a((Ts) previewBlock.V.get(id), variable);
                id++;
            }
        }
    }

    /**
     * @param opCode Block op code like <code>"getArg"</code> (used in Events' heading/start Block)
     *               or <code>"getVar"</code> (type of Blocks in the Palette)
     * @return The Variable Block that's part of for example a MoreBlock or an Event,
     * or <code>null</code> if its spec wasn't recognized.
     */
    @Nullable
    public static Rs getVariableBlock(Context context, int id, String spec, String opCode) {
        if (spec == null || spec.length() < 2) {
            return null;
        }
        var type = spec.charAt(1);
        return switch (type) {
            case 'b', 'd', 's' -> spec.length() >= 3
                    ? new Rs(context, id, spec.substring(3), Character.toString(type), opCode)
                    : null;
            case 'm' -> {
                int firstDot = spec.indexOf('.');
                int lastDot = spec.lastIndexOf('.');
                if (firstDot == -1 || lastDot <= firstDot) {
                    yield null;
                }
                String specLast = spec.substring(lastDot + 1);
                String specFirst = spec.substring(firstDot + 1, lastDot);
                yield new Rs(context, id, specLast, kq.a(specFirst), kq.b(specFirst), opCode);
            }
            default -> null;
        };
    }
}