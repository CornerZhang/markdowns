package com.example.wangshujie.renderscript;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.widget.ImageView;

import com.example.rs.ScriptC_TestRS;

public class TestRsActivity extends Activity {
    private RenderScript testRS;
    private Allocation mInAllocation;
    private Allocation mOutAllocation;
    private ScriptC_TestRS testScript;

    private Bitmap mBitmapIn;
    private Bitmap mBitmapOut;

    //Activity的创建函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBitmapIn = loadBitmap(R.drawable.droid);
        mBitmapOut = Bitmap.createBitmap(mBitmapIn.getWidth(), mBitmapIn.getHeight(),
                mBitmapIn.getConfig());
        ImageView in =  findViewById(R.id.displayin);
        in.setImageBitmap(mBitmapIn);

        ImageView out = findViewById(R.id.displayout);
        out.setImageBitmap(mBitmapOut);

        createScript();
    }

    private void createScript() {
        testRS = RenderScript.create(this);

        mInAllocation = Allocation.createFromBitmap(testRS, mBitmapIn,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        mOutAllocation = Allocation.createTyped(testRS, mInAllocation.getType());

        testScript = new ScriptC_TestRS(testRS, getResources(), R.raw.testrs);       //实例化RenderScript中的函数
        testScript.forEach_invert(mInAllocation, mOutAllocation);                    //调用RenderScript中的函数
        mOutAllocation.copyTo(mBitmapOut);
    }

    //载入Bitmap
    private Bitmap loadBitmap(int resource) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(getResources(), resource, options);
    }
}