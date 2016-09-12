package com.example.duyti.testfpt.Widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.example.duyti.testfpt.R;

/**
 * Created by duyti on 8/21/2016.
 */
public class CustomAvatar extends View {

    private Bitmap avatar, boder;

    private int parentWidth=0;
    private int parentHeight=0;

    public CustomAvatar(Context context) {
        super(context);
        init();
    }

    public CustomAvatar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public CustomAvatar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

    }

    private void init() {
        boder = BitmapFactory.decodeResource(getResources(), R.drawable.bg_avatar);
    }

    public void setAvatar(Bitmap bitmapAvatar){
        if(bitmapAvatar!=null){
            avatar = bitmapAvatar;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setXfermode( new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        Bitmap bmResult= Bitmap.createBitmap(parentWidth,parentHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas1 = new Canvas(bmResult);
        if(avatar!=null){
            canvas1.drawBitmap(avatar,null,new Rect(0,0,parentWidth,parentHeight),null);
        }
        canvas1.drawBitmap(boder,null,new Rect(0,0,parentWidth,parentHeight),paint);
        canvas.drawBitmap(bmResult,0,0,null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        this.setMeasuredDimension(parentWidth, parentHeight);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
