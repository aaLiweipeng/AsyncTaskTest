package com.lwp.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * <pre>
 *     author : 李蔚蓬（简书_凌川江雪）
 *     time   : 2019/11/9 17:29
 *     desc   :三个泛型参数，
 *     第一个表示在执行AsyncTask时需传入一个字符串参数给后台任务，
 *     第二个使用整型数据最为进度显示单位，
 *     第三个表示使用整型数据来反馈结果执行
 * </pre>
 */
public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    //定义四个整型常量分别表示下载的不同状态
    public static final int TYPE_SUCCESS = 0;//表示下载取消
    public static final int TYPE_FAILED = 1;//表示下载失败
    public static final int TYPE_PAUSE = 2;//表示下载暂停
    public static final int TYPE_CANCELED = 3;//表示下载取消

    private DownloadListener listener;

    //取消位以及暂停位
    // 由外部调用，在doInBackground()中生效
    private boolean isCanceled = false;
    private boolean isPaused = false;

    private int lastProgress;//记录上次的进度

    //构造方法
    public DownloadTask(DownloadListener listener){
        //将下载的状态通过此参数进行回调，此处负责调用，外部具体编写逻辑
        this.listener = listener;
    }

    //在后台执行具体的下载逻辑
    // String... params:可变长参数列表，必须是String类型，转化为数组处理
    @Override
    protected Integer doInBackground(String... params) {

        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;

        try{

            long downloadedLength = 0;//记录 已下载的文件 长度！！！！！！！

            String downloadUrl = params[0];//获取 下载的URL地址！！！！！！！！！

            // 根据URL地址解析出下载的文件名
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            // 指定文件下载到 Environment.DIRECTORY_DOWNLOADS 目录下，即SD卡的Download目录
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            //用以上的 文件下载路径 以及 要下载的文件名 得到 file句柄！！！！！！！！！！！！
            file = new File(directory + fileName);


            //判断是否已存在要下载的文件，
            // 存在则 读取 已下载的字节数（以 启用 断点续传 功能）
            if (file.exists()){
                downloadedLength = file.length();
            }


            //获取 待下载文件 的总长度！！！！！！
            // 判断 文件情况—— 有问题 或者 已下载完毕！！！！！
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0){//总长度为0，说明文件有问题
                return TYPE_FAILED;

            }else if (contentLength == downloadedLength){//已下载字节和文件总字节相等，说明已经下载完成了
                return TYPE_SUCCESS;

            }


            //注意这里，断点续传 功能！！！！！！！！！！
            //使用.addHeader 往请求中添加一个Header，用于告诉服务器我们想要
            // 从哪个字节开始下载（已下载部分不需再重新下载）
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();//得到服务器响应的数据

            //使用 Java文件流方式 不断从网络上 读取数据！！
            // 不断写入到本地，
            // 直到文件全部下载完为止！！
            if (response != null){

                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");//封装本地文件句柄
                savedFile.seek(downloadedLength);//跳过已下载的字节


                byte[] b = new byte[1024];
                int total = 0; //本轮！！！下载的总长度！！
                int len;

                //使用 Java文件流方式 不断从网络上 读取数据！！
                // 不断写入到本地，直到文件全部下载完为止！！
                while ((len = is.read(b)) != -1){

                    //判断用户有没触发暂停或取消操作，如果有则返回相应值来中断下载
                    if (isCanceled){
                        return TYPE_CANCELED;

                    }else if (isPaused){
                        return TYPE_PAUSE;


                    }else {

                        //用户没有触发暂停或取消操作，继续下载
                        total += len;
                        savedFile.write(b, 0, len);

                        //计算已下载的百分比 == （本轮下载的长度 + 已经下载的长度）/ 要下载的 文件总长度
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);

                        publishProgress(progress);//抛出进度给 onProgressUpdate(），回调之！！！！
                    }
                }

                //执行到此，说明以上循环已执行完毕，文件下载完毕
                response.body().close();

                return TYPE_SUCCESS;

            }
        } catch (Exception e) {
            e.printStackTrace();

        }finally {

            //分开关闭资源！！！！！！
            try {

                if (is != null){
                    is.close();
                }

                if (savedFile != null){
                    savedFile.close();
                }

                if (isCanceled && file != null){
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //不从上面成功退出则执行至此，证明失败！！！
        return TYPE_FAILED;
    }

    /**
     * 在界面更新当前的下载进度
     *
     * doInBackground()的每一次！！！while 读 输入流 ，
     * 写入file，都会publishProgress(progress); 抛出进度
     * 此时就会回调此方法！！！ 对进度进行处理！！！
     *
     * @param values
     */
    @Override
    protected void onProgressUpdate(Integer... values) {

        //获取当前下载进度，
        // 参数来自 doInBackground()中 publishProgress()抛出的进度
        int progress  = values[0];

        if (progress > lastProgress){//与上一次下载进度对比

            listener.onProgress(progress);//有变化则调用DownloadListener的onProgress()通知下载进度更新

            lastProgress = progress;//更新记录
        }
    }

    /**
     *  通知最终的下载结果
     *
     * 当任务执行完了，即doInBackground()一旦return，
     * 其return的值就会传到这里，作为参数，
     * 参数类型即定义泛型时的第三个参数
     *
     * 这里用了回调机制，listener负责抽象调用！！！
     * 外部负责具体实现！！！
     */
    @Override
    protected void onPostExecute(Integer status) {
        switch (status){//根据传入的下载状态进行回调
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;

            case TYPE_FAILED:
                listener.onFailed();
                break;

            case TYPE_PAUSE:
                listener.onPaused();
                break;

            case TYPE_CANCELED:
                listener.onCanceled();
                break;

            default:
                break;
        }
    }

    //取消位以及暂停位
    // 由外部调用，在doInBackground()中生效
    public void pauseDownload(){
        isPaused = true;
    }
    public void cancelDownload(){
        isCanceled = true;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        //请求得到需下载的文件
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();

        //得到文件长度
        if (response != null && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.close();

            return contentLength;
        }
        return 0;
    }

}
