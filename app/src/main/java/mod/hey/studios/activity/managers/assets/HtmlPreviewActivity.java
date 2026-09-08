package mod.hey.studios.activity.managers.assets;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import pro.sketchware.utility.FileUtil;

public class HtmlPreviewActivity extends BaseAppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        
        WebView webView = new WebView(this);
        setContentView(webView);
        
        String path = getIntent().getStringExtra("path");
        if (path != null) {
            String content = FileUtil.readFile(path);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient());
            webView.loadDataWithBaseURL("file://" + path, content, "text/html", "UTF-8", null);
        }
    }
}
