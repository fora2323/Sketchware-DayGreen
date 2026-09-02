package pro.sketchware.editor;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import io.github.rosemoe.sora.lang.styling.span.SpanExternalRenderer;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class ColorPreviewRenderer implements SpanExternalRenderer {

    private final int color;
    private final boolean isStart;
    private final boolean isEnd;
    private final Paint paint = new Paint();

    public ColorPreviewRenderer(int color, boolean isStart, boolean isEnd) {
        this.color = color;
        this.isStart = isStart;
        this.isEnd = isEnd;
    }

    @Override
    public boolean requirePreDraw() {
        return true;
    }

    @Override
    public boolean requirePostDraw() {
        return false;
    }

    @Override
    public void draw(Canvas canvas, Paint paint, EditorColorScheme colorScheme, boolean preOrPost) {
        if (preOrPost && canvas != null) {
            float width = canvas.getClipBounds().width();
            float height = canvas.getClipBounds().height();
            
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setAntiAlias(true);

            float radius = 6f;

            if (isStart && isEnd) {
                // Single span match - round all corners
                canvas.drawRoundRect(new RectF(0, 0, width, height), radius, radius, this.paint);
            } else if (isStart) {
                // First span of multi-span match - round only left corners
                canvas.save();
                canvas.clipRect(0, 0, width, height);
                canvas.drawRoundRect(new RectF(0, 0, width + radius, height), radius, radius, this.paint);
                canvas.restore();
            } else if (isEnd) {
                // Last span of multi-span match - round only right corners
                canvas.save();
                canvas.clipRect(0, 0, width, height);
                canvas.drawRoundRect(new RectF(-radius, 0, width, height), radius, radius, this.paint);
                canvas.restore();
            } else {
                // Middle span - simple rectangle
                canvas.drawRect(0, 0, width, height, this.paint);
            }
        }
    }

    public static boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(color) + 0.114 * android.graphics.Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
}
