package com.lwp.servicebestpractice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask downloadTask;
    private String downloadUrl;

    private String notificationId = "nyd001";
    private String notificationName = "downloadTask";

    /**
     * 创建DownloadListener 匿名内部类实例，
     * 然后赋值给其父类类型DownloadListener引用
     *
     * 这里实现的方法！！
     * 直接在DownloadTask 的 onPostExecute()中被调用
     *
     * 而onPostExecute() 中要调用那个回调方法
     *
     * 则由doInBackground() 的返回值位决定
     *
     * 而doInBackground() 的返回值 中
     * 成功位 和 失败位 是 客观判断的结果
     * 暂停位 和 取消位 可以 由人为点击置位
     */
    private DownloadListener listener = new DownloadListener() {

        /**
         * 在 DownloadTask 中的 onProgressUpdate()处调用
         * @param progress 来自对应的DownloadTask 的 doInBackground() 中的 publishProgress(progress);
         */
        @Override
        public void onProgress(int progress) {
            //getNotification()是自定义的封装方法，
            // 其中构造了一个用于显示下载进度的通知，
            //调用NotificationManager的 notify() 去触发这个通知，
            // 这样就可以在下拉状态栏中实时看到当前的下载进度了
            getNotificationManager().notify(1, getNotification("Downloading...", progress));
        }

        @Override
        public void onSuccess() {

            downloadTask = null;

            //下载成功时将正在下载的前台服务通知关闭
            stopForeground(true);

            //创建一个下载成功的通知
            getNotificationManager().notify(1, getNotification("Download Success", -1));
            Toast.makeText(DownloadService.this, "Download Success", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onFailed() {
            downloadTask  = null;

            //下载失败时将前台服务通知关闭，并创建一个下载失败的通知,
            // ！！！！！后面几个方法（暂停、取消）的逻辑 与此类似！！！！
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Failed", -1));
            Toast.makeText(DownloadService.this, "Download Failed", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onPaused() {
            downloadTask  = null;
            Toast.makeText(DownloadService.this, "Paused", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onCanceled() {
            downloadTask  = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();

        }
    };


    /**
     * 创建DownloadBinder内部类，
     * 把需要放给外部调用的Service服务方法写好，
     * 实例化一个DownloadBinder内部类示例，在onBind()中返回，
     * 这样，
     * 当外部界面与本Service绑定，
     * 就可以在 ServiceConnection实例 的 onServiceConnected 回调方法中，
     * 获得这个 具备了 各种准备好的业务方法的 DownloadBinder（Binder、IBinder）实例了
     *
     */
    private DownloadBinder mBinder = new DownloadBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    //创建DownloadBinder内部类，
    //把需要放给外部调用的Service服务方法写好
    class DownloadBinder extends Binder {

        /**
         * 开启下载任务
         * @param url 要下载的资源地址
         */
        public void startDownload(String url){

            if (downloadTask == null){

                downloadUrl = url;

                //创建DownloadTask实例
                downloadTask = new DownloadTask(listener);

                //传入下载地址，启动下载任务！！！！
                downloadTask.execute(downloadUrl);

                //让这个下载任务服务成为一个前台服务！！！
                // 使用时在Activity处 先 startService(intent);  启动！ 本服务DownloadService
                //
                // 然后 绑定本服务 bindService(intent, connection, BIND_AUTO_CREATE);！！！！
                // 再调用本方法 downloadBinder【即这里的mBinder】.startDownload(url);
                // 运行到下面的startForeground()！！
                // 从而使刚刚已经启动（start）的服务变成前台服务！！！！！
                //这样就会在 系统状态栏 中 创建一个持续运行的通知了
                // .
                // 注意这里有个id！！！ 后续取消时 可以用！！

                getNotificationManager();// 配置 NotificationManager！！！！！！！！
                startForeground(1, getNotification("Downloading...", 0));
                //！！！！！！！！！！！

                Toast.makeText(DownloadService.this, "Downloading...", Toast.LENGTH_SHORT).show();
            }
        }
        public void pauseDownload(){
            if (downloadTask != null){
                //使下载任务downloadTask 的 暂停位 置位
                downloadTask.pauseDownload();
            }
        }
        public void cancelDownload(){
            if (downloadTask != null){

                //首先，使下载任务downloadTask 的 取消位 置位，终止下载！！！！
                downloadTask.cancelDownload();
                //调用流程：
                // downloadTask.cancelDownload();
                // --> isCanceled = true;   取消位 置位
                // .
                // -->downloadTask 的 doInBackground 中 取消位 置位生效
                // doInBackground() 中的 下载文件的while循环中
                // if (isCanceled){ return TYPE_CANCELED;} 返回取消位 并终止下载！！！
                // .
                // -->onPostExecute() 接收到 doInBackground()返回的取消位
                // （只要onPostExecute() 接收到了取消位， 便已经终止下载了！！ 这时候回调接口...）
                // .
                // --> listener.onCanceled(); 回调 接口的 取消方法 ，
                // 即这里 DownloadService 实现的方法， 接着进行下一步操作...
                // .
                // --> downloadTask  = null;

            }else {
                //如果 downloadTask  = null; 则 执行到此

                //纵观 接口处几个方法 无论成功、失败、暂停、取消
                // 都会执行 downloadTask  = null;
                // .
                // 也就是说 只要 downloadTask 调用过 一次 接口方法！！！！
                // 之后再调用  downloadBinder.cancelDownload(); 的话，
                // 都会已 downloadTask  = null;
                // 即 会执行至此， 删除文件，关闭通知 ！！！

                if (downloadUrl != null){

                    //取消下载时需将文件删除，并将通知关闭

                    //获取file 的过程 同DownloadTask 的 doInBackground()
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));//得到文件名
                    String directroy = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directroy + fileName);
                    if (file.exists()){
                        file.delete();
                    }

                    //取消对应id 前台通知或者服务
                    getNotificationManager().cancel(1);
                    stopForeground(true);

                    Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    //封装 NotificationManager
    private NotificationManager getNotificationManager(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationId, notificationName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

            return notificationManager;
        } else {
            return notificationManager;
        }

    }

    /**
     * 封装进度条通知
     * 返回一个封装配置好的 Notification
     *
     * Notification
     * 遇 startForeground() 则成前台服务！！！
     * 遇 NotificationManager.notify() 则成通知！！！
     */
    private Notification getNotification(String title, int progress){

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

        //拿着Notification 的 建造者Builder， 去各种配置（set()），
        // 配置完毕了，调用builder.build()，返回 一个 Notification ！！！
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            builder.setChannelId(notificationId);
//        }

        if (progress > 0){
            //当progress大于或等于0时才需显示下载进度
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);//三个参数：通知的最大进度，通知的当前进度，是否使用模糊进度条
        }

        return builder.build();
    }
}
