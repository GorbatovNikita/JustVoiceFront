package ru.myitschool.justvoice.ui.callback;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final SwipeListener listener;
    private final Paint backgroundPaint;
    private final Paint textPaint;
    private final String swipeText;
    private final float cornerRadius;

    public interface SwipeListener {
        void onSwiped(int position);
    }

    public SwipeToDeleteCallback(SwipeListener listener) {
        super(0, ItemTouchHelper.RIGHT);
        this.listener = listener;
        this.backgroundPaint = new Paint();
        this.backgroundPaint.setColor(Color.parseColor("#FF4444"));
        this.backgroundPaint.setAntiAlias(true);
        this.textPaint = new Paint();
        this.textPaint.setColor(Color.WHITE);
        this.textPaint.setTextSize(48);
        this.textPaint.setAntiAlias(true);
        this.swipeText = "DELETE";
        this.cornerRadius = 48f;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        listener.onSwiped(position);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        int itemTop = itemView.getTop();
        int itemLeft = itemView.getLeft();

        if (dX > 0) {
            float right = itemLeft + dX;
            RectF rect = new RectF(itemLeft, itemTop, right, itemTop + itemHeight);
            c.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint);

            float textWidth = textPaint.measureText(swipeText);
            float textX = itemLeft + 32;
            float textY = itemTop + (itemHeight / 2f) + (textPaint.getTextSize() / 3f);

            if (dX > textWidth + 64) {
                c.drawText(swipeText, textX, textY, textPaint);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}