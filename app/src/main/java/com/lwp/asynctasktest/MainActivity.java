package com.lwp.asynctasktest;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

/**
 * 1.网络上请求数据：申请网络权限 读写存储权限
 * 2.布局
 * 3.下载前、中、后，需要写什么逻辑？（UI、数据、UI）
 */

public class MainActivity extends AppCompatActivity {

    //logt 然后回车，快速生成！！！！！！！！
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new DownloadAsyncTask().execute("哈喽", "早上好");
    }

    public class DownloadAsyncTask extends AsyncTask<String, Integer, Boolean> {

        /**
         * 方法执行在异步任务之前，执行在主线程中！！
         * 方法体中，可以 ；‘’；操作UI ！！
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * 本方法运行在子线程中，
         * 处理工作内容、耗时操作
         *
         * @param params
         * @return
         */
        @Override
        protected Boolean doInBackground(String... params) {

            // fori 回车 快速生成！！！！！！！！！！！
            for (int i = 0; i < 10000; i++) {
                Log.i(TAG, "doInBackground " + i +
                        ": " + params[0] + "," + params[1]);

                publishProgress(i);//抛出进度
            }
            return true;
        }

        /**
         * 本方法运行在主线程中，可以处理结果
         *
         * 当任务执行完了，会把上一个doInBackground(）的参数传递过来
         * @param aBoolean
         */
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
        }

        /**
         * 本方法运行在主线程
         * 进度变化就在这个方法处理
         * 接收来自 doInBackground()中 publishProgress()抛出的进度，
         * 并处理
         *
         * @param values 类型是定义时的第二个泛型参数，
         *               数据来自 doInBackground()的 publishProgress()
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }
}
