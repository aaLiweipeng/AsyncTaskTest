package com.lwp.servicebestpractice;

/**
 * <pre>
 *     author : 李蔚蓬（简书_凌川江雪）
 *     time   : 2019/11/9 17:26
 *     desc   :
 * </pre>
 */
public interface DownloadListener {
    void onProgress(int progress);//通知当前下载进度
    void onSuccess();//通知下载成功事件
    void onFailed();//通知下载失败事件
    void onPaused();//通知下载暂停事件
    void onCanceled();//通知下载取消事件
}
