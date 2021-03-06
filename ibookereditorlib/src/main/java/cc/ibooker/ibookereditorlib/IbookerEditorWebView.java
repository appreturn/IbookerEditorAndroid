package cc.ibooker.ibookereditorlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;

/**
 * 书客编辑器 - 预览界面 - 自定义WebView
 * Created by 邹峰立 on 2018/2/11.
 */
public class IbookerEditorWebView extends WebView {
    private boolean isLoadFinished = false;// 本地文件是否加载完成
    private boolean isExecuteCompile = false;// 是否执行预览
    private boolean isExecuteHtmlCompile = false;// 是否执行HTML预览
    private String ibookerEditorText, ibookerEditorHtml;

    private ArrayList<String> imgPathList;// WebView所有图片地址
    private IbookerEditorJsCheckImgEvent ibookerEditorJsCheckImgEvent;

    public IbookerEditorWebView(Context context) {
        this(context, null);
    }

    public IbookerEditorWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IbookerEditorWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setVerticalScrollBarEnabled(false);
        setVerticalScrollbarOverlay(false);
        setHorizontalScrollBarEnabled(false);
        setHorizontalScrollbarOverlay(false);

        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }

    // 初始化
    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    private void init() {
        WebSettings webSettings = this.getSettings();
        // 支持内容重新布局
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        // 允许JS
        webSettings.setJavaScriptEnabled(true);
        // 支持插件
        webSettings.setPluginState(WebSettings.PluginState.ON);
        // 设置允许JS弹窗
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        // access Assets and resources
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(false);
        // 提高渲染优先级
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        // 设置编码格式
        webSettings.setDefaultTextEncodingName("utf-8");
        // 支持自动加载图片
        webSettings.setLoadsImagesAutomatically(true);
        // 关闭webview中缓存
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // 将图片调整到适合webview的大小
        webSettings.setUseWideViewPort(true);
        // 缩放至屏幕的大小
        webSettings.setLoadWithOverviewMode(true);
        // 支持缩放，默认为true。
        webSettings.setSupportZoom(true);
        // 设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setBuiltInZoomControls(true);
        // 隐藏原生的缩放控件
        webSettings.setDisplayZoomControls(false);

        // 隐藏滚动条
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        // 使页面获取焦点，防止点击无响应
        requestFocus();
        // 设置WebViewClient
        setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    view.loadUrl(request.getUrl().toString());
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (ibookerEditorWebViewUrlLoadingListener != null)
                    ibookerEditorWebViewUrlLoadingListener.onReceivedError(view, request, error);
                else
                    // 当网页加载出错时，加载本地错误文件
                    IbookerEditorWebView.this.loadUrl("file:///android_asset/error.html");
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (ibookerEditorWebViewUrlLoadingListener != null)
                    ibookerEditorWebViewUrlLoadingListener.onReceivedSslError(view, handler, error);
                else
                    // 当网页加载出错时，加载本地错误文件
                    IbookerEditorWebView.this.loadUrl("file:///android_asset/error.html");
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (ibookerEditorWebViewUrlLoadingListener != null)
                    ibookerEditorWebViewUrlLoadingListener.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isLoadFinished = true;
                if (isExecuteCompile) {
                    ibookerCompile(ibookerEditorText);
                } else if (isExecuteHtmlCompile) {
                    ibookerHtmlCompile(ibookerEditorHtml);
                } else {
                    addWebViewListener();
                }
                if (ibookerEditorWebViewUrlLoadingListener != null)
                    ibookerEditorWebViewUrlLoadingListener.onPageFinished(view, url);
            }
        });
        // 添加js
        ibookerEditorJsCheckImgEvent = new IbookerEditorJsCheckImgEvent();
        addJavascriptInterface(ibookerEditorJsCheckImgEvent, "ibookerEditorJsCheckImgEvent");
        // 加载本地HTML
        loadUrl("file:///android_asset/ibooker_editor_index.html");
    }

    // 给WebView添加相关监听
    private void addWebViewListener() {
        // 获取WebView中全部图片地址
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.evaluateJavascript("javascript:getImgAllPaths()", new ValueCallback<String>() {

                @Override
                public void onReceiveValue(String value) {
                    // value即为所有图片地址
                    if (!TextUtils.isEmpty(value)) {
                        value = value.replace("\"", "").replace("\"", "");
                        if (!TextUtils.isEmpty(value)) {
                            if (imgPathList == null)
                                imgPathList = new ArrayList<>();
                            imgPathList.clear();
                            String[] imgPaths = value.split(";ibookerEditor;");
                            for (String imgPath : imgPaths) {
                                if (!TextUtils.isEmpty(imgPath))
                                    imgPathList.add(imgPath);
                            }
                            ibookerEditorJsCheckImgEvent.setmImgPathList(imgPathList);
                        }
                    }
                }
            });
        }

        // 动态添加图片点击事件
        this.loadUrl("javascript:(function() {"
                + "  var objs = document.getElementsByTagName(\"img\"); "
                + "  for(var i = 0; i < objs.length; i++) {"
                + "     objs[i].onclick = function() {"
                + "          window.ibookerEditorJsCheckImgEvent.onCheckImg(this.src);"
                + "     }"
                + "  }"
                + "})()");
    }

    /**
     * 是否完成本地文件加载
     */
    public boolean isLoadFinished() {
        return isLoadFinished;
    }

    /**
     * 执行预览
     *
     * @param ibookerEditorText 待预览内容 非HTML
     */
    public void ibookerCompile(String ibookerEditorText) {
        if (isLoadFinished) {
            ibookerEditorText = ibookerEditorText.replaceAll("\\n", "\\\\n");
            String js = "javascript:ibookerCompile('" + ibookerEditorText + "')";
            this.loadUrl(js);

            // 重新WebView添加监听
            addWebViewListener();

            this.isExecuteCompile = false;
            this.ibookerEditorText = null;
            this.isExecuteHtmlCompile = false;
            this.ibookerEditorHtml = null;
        } else {
            this.isExecuteCompile = true;
            this.ibookerEditorText = ibookerEditorText;
        }

    }

    /**
     * 执行Html预览
     *
     * @param ibookerEditorHtml 待预览内容 HTML
     */
    public void ibookerHtmlCompile(String ibookerEditorHtml) {
        if (isLoadFinished) {
            String js = "javascript:ibookerHtmlCompile('" + ibookerEditorHtml + "')";
            this.loadUrl(js);

            // 重新WebView添加监听
            addWebViewListener();

            this.isExecuteHtmlCompile = false;
            this.ibookerEditorHtml = null;
            this.isExecuteCompile = false;
            this.ibookerEditorText = null;
        } else {
            this.isExecuteHtmlCompile = true;
            this.ibookerEditorHtml = ibookerEditorHtml;
        }
    }

    /**
     * 获取整个WebView截图
     */
    public Bitmap getWebViewBitmap() {
        Picture picture = this.capturePicture();
        Bitmap bitmap = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        picture.draw(canvas);
        return bitmap;
    }

    // 图片预览接口
    public interface IbookerEditorImgPreviewListener {
        void onIbookerEditorImgPreview(String currentPath, int position, ArrayList<String> imgAllPathList);
    }

    public void setIbookerEditorImgPreviewListener(IbookerEditorImgPreviewListener ibookerEditorImgPreviewListener) {
        ibookerEditorJsCheckImgEvent.setmIbookerEditorImgPreviewListener(ibookerEditorImgPreviewListener);
    }

    // Url加载状态监听
    public interface IbookerEditorWebViewUrlLoadingListener {
        void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error);

        void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error);

        void onPageStarted(WebView view, String url, Bitmap favicon);

        void onPageFinished(WebView view, String url);
    }

    private IbookerEditorWebViewUrlLoadingListener ibookerEditorWebViewUrlLoadingListener;

    public void setIbookerEditorWebViewUrlLoadingListener(IbookerEditorWebViewUrlLoadingListener ibookerEditorWebViewUrlLoadingListener) {
        this.ibookerEditorWebViewUrlLoadingListener = ibookerEditorWebViewUrlLoadingListener;
    }
}
