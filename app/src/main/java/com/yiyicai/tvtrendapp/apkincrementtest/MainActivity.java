package com.yiyicai.tvtrendapp.apkincrementtest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.yiyicai.tvtrendapp.apkincrementtest.bsdiff.DiffUtils;
import com.yiyicai.tvtrendapp.apkincrementtest.bsdiff.PatchUtils;
import com.yiyicai.tvtrendapp.apkincrementtest.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private ProgressBar loadding;

    // 成功
    private static final int WHAT_SUCCESS = 1;
    // 失败
    private static final int WHAT_FAIL_PATCH = 0;

    private String srcDir = Environment.getExternalStorageDirectory().toString() + "/DaemonProcess-1.apk";
    private String destDir1 = Environment.getExternalStorageDirectory().toString() + "/DaemonProcess-2.apk";
    private String destDir2 = Environment.getExternalStorageDirectory().toString() + "/DaemonProcess-3.apk";
    private String patchDir = Environment.getExternalStorageDirectory().toString() + "/DaemonProcess.patch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("===","srcDir地址：" + srcDir);
        Log.i("===","destDir1地址：" + destDir1);
        Log.i("===","destDir2地址：" + destDir2);
        Log.i("===","patchDir地址：" + patchDir);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadding = (ProgressBar) findViewById(R.id.loadding);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Toast.makeText(getApplicationContext(), "copy successed", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(), "copy failured", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(), "bsdiff successed", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(getApplicationContext(), "bsdiff failured", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    Toast.makeText(getApplicationContext(), "patch successed", Toast.LENGTH_SHORT).show();
                    break;
                case 5:
                    Toast.makeText(getApplicationContext(), "patch failures", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };
    public static final int COPY = 0;
    public static final int BSDIFF = 1;
    public static final int BSPATCH = 2;
    public void copy(View view) {
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},COPY);
        }else {
            loadding.setVisibility(View.VISIBLE);
            new CopyTask().execute(srcDir, "DaemonProcess-1.apk", destDir1, "DaemonProcess-2.apk");
        }
    }

    public void bsdiff(View view) {
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},BSDIFF);
        }else {
            loadding.setVisibility(View.VISIBLE);
            new DiffTask().execute();
        }
    }

    public void bspatch(View view) {
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},BSPATCH);
        }else {
            loadding.setVisibility(View.VISIBLE);
            new PatchTask().execute();
        }
    }

    public void installOld(View view) {
        install(srcDir);
    }

    public void installNew(View view) {
        install(destDir2);
    }

    private class CopyTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {

            for (int i = 0; i < params.length; i += 2) {
                try {
                    File file = new File(params[i]);
                    if (!file.exists())
                        FileUtils.createFile(file);

                    InputStream is;
                    OutputStream os = new FileOutputStream(params[i]);
                    is = getAssets().open(params[i + 1]);
                    byte[] buffer = new byte[1024];
                    int length = is.read(buffer);
                    while (length > 0) {
                        os.write(buffer, 0, length);
                        length = is.read(buffer);
                    }
                    os.flush();
                    is.close();
                    os.close();
                } catch (Exception e) {
                    handler.obtainMessage(1).sendToTarget();
                    return null;
                }
            }
            handler.obtainMessage(0).sendToTarget();
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            loadding.setVisibility(View.GONE);
        }
    }

    /**
     * 生成差分包
     *
     * @author yuyuhang
     * @date 2016-1-25 下午12:24:34
     */
    private class DiffTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            Log.i("===","差分包生成中");
            try {
                Log.i("===","差分包生成中1");
                int result = DiffUtils.getInstance().genDiff(srcDir, destDir1, patchDir);
                Log.i("===","差分包生成完毕");
                if (result == 0) {
                    handler.obtainMessage(2).sendToTarget();
                    return WHAT_SUCCESS;
                } else {
                    handler.obtainMessage(3).sendToTarget();
                    return WHAT_FAIL_PATCH;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return WHAT_FAIL_PATCH;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            loadding.setVisibility(View.GONE);
        }
    }

    /**
     * 差分包合成APK
     *
     * @author yuyuhang
     * @date 2016-1-25 下午12:24:34
     */
    private class PatchTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {

            try {

                int result = PatchUtils.getInstance().patch(srcDir, destDir2, patchDir);
                if (result == 0) {
                    handler.obtainMessage(4).sendToTarget();
                    return WHAT_SUCCESS;
                } else {
                    handler.obtainMessage(5).sendToTarget();
                    return WHAT_FAIL_PATCH;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return WHAT_FAIL_PATCH;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            loadding.setVisibility(View.GONE);
        }
    }

    private void install(String dir) {
        String command = "chmod 777 " + dir;
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(command); // 可执行权限
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.parse("file://" + dir), "application/vnd.android.package-archive");
        startActivity(intent);
    }

    public void appList(View view) {
        Intent intent = new Intent(this, ApplistActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case COPY://读写权限
                if (grantResults.length>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    loadding.setVisibility(View.VISIBLE);
                    new CopyTask().execute(srcDir, "DaemonProcess-1.apk", destDir1, "DaemonProcess-2.apk");
                }
                break;
            case BSDIFF:
                if (grantResults.length>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadding.setVisibility(View.VISIBLE);
                    new DiffTask().execute();
                }
                break;
            case BSPATCH:
                if (grantResults.length>0 &&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadding.setVisibility(View.VISIBLE);
                    new PatchTask().execute();
                }
                break;
        }
    }
}
