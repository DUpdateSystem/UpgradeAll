package net.xzos.UpgradeAll.activity;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LiveData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;
import net.xzos.UpgradeAll.gson.HubConfig;
import net.xzos.UpgradeAll.server.JSEngine.JSEngineDataProxy;
import net.xzos.UpgradeAll.server.JSEngine.JSUtils.JSLog;
import net.xzos.UpgradeAll.server.JSEngine.JavaScriptJEngine;
import net.xzos.UpgradeAll.server.hub.HubManager;
import net.xzos.UpgradeAll.server.log.LogDataProxy;
import net.xzos.UpgradeAll.server.log.LogUtil;
import net.xzos.UpgradeAll.utils.FileUtil;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class HubLocalActivity extends AppCompatActivity {


    protected static final LogUtil Log = MyApplication.getServerContainer().getLog();

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;

    private static final int READ_JS_REQUEST_CODE = 2;
    private static final int READ_CONFIG_REQUEST_CODE = 3;
    private static final int WRITE_CONFIG_REQUEST_CODE = READ_CONFIG_REQUEST_CODE;
    private static final int READ_CONFIG_JS_REQUEST_CODE = 4;

    private Uri HUBCONFIG_URI = null;
    private Uri JS_URI = null;

    private int JS_CARDVIEW_WRAP_HEIGHT = 0;
    private int CONFIG_CARDVIEW_WRAP_HEIGHT = 0;

    private int SCREEN_HEIGHT = 0;

    private String SELECTED_FILE_ADDRESS = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub_setting);
        // toolbar 点击事件
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        Intent intent = getIntent();
        int databaseId = intent.getIntExtra("database_id", 0);

        // 读取 JS 脚本
        Button selectJsFileButton = findViewById(R.id.selectJsFileButton);
        selectJsFileButton.setOnClickListener(v -> FileUtil.performFileSearch(this, READ_JS_REQUEST_CODE, "application/*"));

        // 读取配置文件
        Button selectConfigFileButton = findViewById(R.id.selectConfigFileButton);
        selectConfigFileButton.setOnClickListener(v -> FileUtil.performFileSearch(this, READ_CONFIG_REQUEST_CODE, "application/*"));
        selectConfigFileButton.setOnLongClickListener(v -> {
            EditText configHubNameEditText = findViewById(R.id.configHubNameEditText);
            String configName = configHubNameEditText.getText().toString();
            if (!configName.equals("")) {
                configName += ".json";
            } else
                configName = "config.json";
            FileUtil.createFile(this, WRITE_CONFIG_REQUEST_CODE, "application/*", configName);
            return true;
        });

        // 运行 JS 脚本
        Button jsRunButton = findViewById(R.id.jsRunButton);
        jsRunButton.setOnClickListener(v -> runTestJs());

        jsFilePathEditTextPerformLongClick();

        // 保存配置
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.menu_save_config, popupMenu.getMenu());
            popupMenu.show();
            //设置item的点击事件
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.saveToFileButton:
                        writeHubConfigToFile();
                        break;
                    case R.id.saveToDatabaseButton:
                        addHubConfigToDatabase(databaseId);
                        break;
                    case R.id.saveToFileAndDatabaseButton:
                        addHubConfigToDatabase(databaseId);
                        writeHubConfigToFile();
                        break;
                }
                return true;
            });
        });
        // 折叠展开卡片
        RelativeLayout jsShrinkLayout = findViewById(R.id.jsShrinkLayout);
        jsShrinkLayout.setOnClickListener(v -> {
            TextView jsTextView = findViewById(R.id.jsTextView);
            CardView jsTestCardView = findViewById(R.id.jsTestCardView);
            RelativeLayout jsContentLayout = findViewById(R.id.jsContentLayout);
            cardViewAnim(jsTextView, jsTestCardView, jsContentLayout);
        });
        RelativeLayout configShrinkLayout = findViewById(R.id.configShrinkLayout);
        configShrinkLayout.setOnClickListener(v -> {
            TextView configTextView = findViewById(R.id.configTextView);
            CardView hubConfigCardView = findViewById(R.id.hubConfigCardView);
            RelativeLayout configContentLayout = findViewById(R.id.configContentLayout);
            cardViewAnim(configTextView, hubConfigCardView, configContentLayout);
        });

        if (databaseId != 0) loadFromDatabase(databaseId);

        setUI();

        fixScrollTouch();

        FileUtil.requestPermission(this, PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            if (uri != null)
                switch (requestCode) {
                    case READ_JS_REQUEST_CODE:
                        loadJSFromUri(uri);
                        break;
                    case READ_CONFIG_REQUEST_CODE:
                        loadConfigFromUri(uri);
                        break;
                    case READ_CONFIG_JS_REQUEST_CODE:
                        loadConfigJSFormUri(uri);
                        break;
                }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length <= 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(HubLocalActivity.this, "编辑测试生成配置需要读写本地文件", Toast.LENGTH_LONG).show();
                onBackPressed();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new LogDataProxy(Log).clearLogSort("DeBug");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.app_help:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://xzos.net/the-customizing-configuration-rules-for-a-software-depot/"));
                intent = Intent.createChooser(intent, "请选择浏览器以查看帮助文档");
                startActivity(intent);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // 修复滑动动作无法传递
    @SuppressLint("ClickableViewAccessibility")
    private void fixScrollTouch() {
        ScrollView scrollView = findViewById(R.id.scrollView);
        TextView jsTestTextView = findViewById(R.id.jsTestTextView);
        TextView jsLogTextView = findViewById(R.id.jsLogTextView);
        scrollView.setOnTouchListener((v, event) -> {
            jsTestTextView.getParent().requestDisallowInterceptTouchEvent(false);
            jsLogTextView.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });

        jsTestTextView.setOnTouchListener((v, event) -> {
            jsTestTextView.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        jsLogTextView.setOnTouchListener((v, event) -> {
            jsLogTextView.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    private void setUI() {
        // 限制 JS 代码展示区域
        DisplayMetrics displaymetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        SCREEN_HEIGHT = displaymetrics.heightPixels;
        TextView jsTestTextView = findViewById(R.id.jsTestTextView);
        jsTestTextView.setMaxHeight(SCREEN_HEIGHT / 3);
        jsTestTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        // 限制 Log 展示区域
        TextView jsLogTextView = findViewById(R.id.jsLogTextView);
        jsLogTextView.setMaxHeight(SCREEN_HEIGHT * 5 / 12);
        jsLogTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        // 初始设置文件选择按钮提示信息
        SELECTED_FILE_ADDRESS = getString(R.string.selected_file_address);
        String nullSelectedFileAddress = SELECTED_FILE_ADDRESS + "NULL";

        Button selectJsFileButton = findViewById(R.id.selectJsFileButton);
        selectJsFileButton.setText(nullSelectedFileAddress);
        Button selectConfigFileButton = findViewById(R.id.selectConfigFileButton);
        selectConfigFileButton.setText(nullSelectedFileAddress);

        // 折叠 JS 测试卡片
        TextView jsTextView = findViewById(R.id.jsTextView);
        CardView jsTestCardView = findViewById(R.id.jsTestCardView);
        jsTestCardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                jsTestCardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                JS_CARDVIEW_WRAP_HEIGHT = jsTestCardView.getHeight();
                // 折叠 JS 测试区
                RelativeLayout jsContentLayout = findViewById(R.id.jsContentLayout);
                cardViewAnim(jsTextView, jsTestCardView, jsContentLayout);
            }
        });
        CardView hubConfigCardView = findViewById(R.id.hubConfigCardView);
        hubConfigCardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                hubConfigCardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                CONFIG_CARDVIEW_WRAP_HEIGHT = hubConfigCardView.getHeight();
            }
        });
    }

    // 自定义 jsFilePathEditText 长按菜单
    private void jsFilePathEditTextPerformLongClick() {
        EditText jsFilePathEditText = findViewById(R.id.jsFilePathEditText);
        jsFilePathEditText.setOnLongClickListener(v -> {
            if (HUBCONFIG_URI != null) {
                FileUtil.performFileSearch(HubLocalActivity.this, READ_CONFIG_JS_REQUEST_CODE, "application/*");
                Toast.makeText(HubLocalActivity.this, "如果你需要编辑文字，可以双击编辑框而非长按", Toast.LENGTH_LONG).show();
                return true;
            } else {
                Toast.makeText(HubLocalActivity.this, "请先选择配置文件位置", Toast.LENGTH_LONG).show();
                return false;
            }
        });
        jsFilePathEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater menuInflater = mode.getMenuInflater();
                menuInflater.inflate(R.menu.menu_long_click_js_edit, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.testJS) {
                    EditText jsFilePathEditText = findViewById(R.id.jsFilePathEditText);
                    String jsRelativePath = jsFilePathEditText.getText().toString();
                    String configPath = FileUtil.uriToPath(HUBCONFIG_URI);
                    if (HUBCONFIG_URI != null && configPath != null && !jsRelativePath.equals("")) {
                        configPath = configPath.substring(0, configPath.lastIndexOf("/"));
                        String jsPath = FileUtil.pathTransformRelativeToAbsolute(configPath, jsRelativePath);
                        if (FileUtil.fileIsExistsByPath(jsPath)) {
                            loadJSFromUri(Uri.fromFile(new File(jsPath)));
                            Toast.makeText(HubLocalActivity.this, "现在打开第一个卡片进行 JS 脚本测试", Toast.LENGTH_LONG).show();
                        } else
                            Toast.makeText(HubLocalActivity.this, "文件不存在", Toast.LENGTH_LONG).show();
                    } else
                        Toast.makeText(HubLocalActivity.this, "请先选择一个正确的 JS脚本", Toast.LENGTH_LONG).show();
                    mode.finish();
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });
    }

    private void runTestJs() {
        if (JS_URI != null) {
            loadJSFromUri(JS_URI);
        }
        TextView jsTestTextView = findViewById(R.id.jsTestTextView);
        String jsCode = jsTestTextView.getText().toString();
        if (jsCode.equals("")) {
            Toast.makeText(HubLocalActivity.this, "请选择一个正确的 JS 脚本文件", Toast.LENGTH_LONG).show();
            return;
        }
        EditText testUrlEditText = findViewById(R.id.testUrlEditText);
        String testUrl = testUrlEditText.getText().toString();
        if (testUrl.equals("")) {
            Toast.makeText(HubLocalActivity.this, "请填写 测试网址", Toast.LENGTH_LONG).show();
            return;
        }
        // 创建 JS 引擎
        String[] logObjectTag = {"DeBug", "0"};
        JavaScriptJEngine javaScriptJEngine = new JavaScriptJEngine(logObjectTag, testUrl, jsCode);
        javaScriptJEngine.setEnableLogJsCode(false);
        JSEngineDataProxy jsEngineDataProxy = new JSEngineDataProxy(javaScriptJEngine);
        JSLog jsLog = new JSLog(logObjectTag);
        TextView jsLogTextView = findViewById(R.id.jsLogTextView);
        LiveData<List<String>> logListLiveData = new LogDataProxy(Log).getLogMessageListLiveData(logObjectTag);
        logListLiveData.observe(this, logList -> {
            StringBuilder textViewMessage = new StringBuilder();
            for (String logMessage : logList)
                textViewMessage.append(StringEscapeUtils.unescapeJava(logMessage)).append("\n");
            jsLogTextView.setText(textViewMessage.toString());
        });
        jsLogTextView.setVisibility(View.VISIBLE);
        // JS 初始化
        new Thread(() -> {
            // 分步测试
            jsLog.d(String.format("1. 获取默认名称(getDefaultName): %s \n", jsEngineDataProxy.getDefaultName()));
            jsLog.d(String.format("2. 获取发布版本号总数(getReleaseNum): %s \n", jsEngineDataProxy.getReleaseNum()));
            for (int i = 0; i < jsEngineDataProxy.getReleaseNum(); i++) {
                jsLog.d(String.format("3. (%s) 获取发布版本号(getVersionNumber): %s \n", i, jsEngineDataProxy.getVersionNumber(i)));
            }
            for (int i = 0; i < jsEngineDataProxy.getReleaseNum(); i++) {
                JSONObject releaseDownload = jsEngineDataProxy.getReleaseDownload(i);
                if (releaseDownload != null) {
                    jsLog.d(String.format("4. (%s) 获取下载链接(getReleaseDownload): %s \n", i, releaseDownload.toString()));
                }
            }
        }).start();

    }

    private void cardViewAnim(TextView titleTextView, @NonNull CardView cardView, RelativeLayout contentLayout) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) cardView.getLayoutParams();
        boolean animStatus; // true 展开，false 折叠
        int cardViewHeight = cardView.getHeight();
        int cardViewFoldHeight = SCREEN_HEIGHT / 15;
        int animTime = 1000;
        int CARDVIEW_WRAP_HEIGHT = 0;
        if (cardView == findViewById(R.id.jsTestCardView))
            CARDVIEW_WRAP_HEIGHT = JS_CARDVIEW_WRAP_HEIGHT;
        if (cardView == findViewById(R.id.hubConfigCardView))
            CARDVIEW_WRAP_HEIGHT = CONFIG_CARDVIEW_WRAP_HEIGHT;
        // 获取 wrap 高度
        if (layoutParams.height == WindowManager.LayoutParams.WRAP_CONTENT)
            CARDVIEW_WRAP_HEIGHT = cardViewHeight;
        int start = cardViewHeight;
        int end;
        if (titleTextView.getVisibility() != View.VISIBLE) {
            // 折叠
            animStatus = false;
            end = cardViewFoldHeight;
            if (end > start) {
                int tmp = end;
                end = start;
                start = tmp;
            }
        } else {
            // 展开
            animStatus = true;
            end = CARDVIEW_WRAP_HEIGHT;
            if (end < start) {
                int tmp = end;
                end = start;
                start = tmp;
            }
            // 为了美观，延迟卡片信息更新至最后
        }
        // 启动动画
        ValueAnimator valueAnimator = ValueAnimator.ofInt(start, end);
        valueAnimator.setDuration(animTime);
        valueAnimator.addUpdateListener(animator -> {
            layoutParams.height = (int) (Integer) animator.getAnimatedValue();
            cardView.setLayoutParams(layoutParams);
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (!animStatus) {
                    // 折叠
                    titleTextView.setVisibility(View.VISIBLE);
                    contentLayout.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (animStatus) {
                    // 展开
                    contentLayout.setVisibility(View.VISIBLE);
                    titleTextView.setVisibility(View.INVISIBLE);
                    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    cardView.setLayoutParams(layoutParams);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        valueAnimator.start();
        // 设置点击事件
        if (animStatus)
            cardView.setOnClickListener(null);
        else
            cardView.setOnClickListener(v -> cardViewAnim(titleTextView, cardView, contentLayout));
        // 保存数据
        if (cardView == findViewById(R.id.jsTestCardView))
            JS_CARDVIEW_WRAP_HEIGHT = CARDVIEW_WRAP_HEIGHT;
        if (cardView == findViewById(R.id.hubConfigCardView))
            CONFIG_CARDVIEW_WRAP_HEIGHT = CARDVIEW_WRAP_HEIGHT;
    }

    private void loadFromDatabase(int databaseId) {
        String jsCode = HubManager.getHubJsCodeByDatabaseId(databaseId);
        TextView jsTestTextView = findViewById(R.id.jsTestTextView);
        jsTestTextView.setText(jsCode);
        loadConfigFromHubConfig(HubManager.getHubConfigByDatabaseId(databaseId));
    }

    @Nullable
    private HubConfig getHubConfigGson() {
        HubConfig hubConfig = new HubConfig();
        EditText configBaseVersionEditText = findViewById(R.id.configBaseVersionEditText);
        try {
            hubConfig.setBaseVersion(Integer.parseInt(configBaseVersionEditText.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(HubLocalActivity.this, "请填写 适配配置版本", Toast.LENGTH_LONG).show();
            return null;
        }
        EditText configUuidEditText = findViewById(R.id.configUuidEditText);
        hubConfig.setUuid(configUuidEditText.getText().toString());
        EditText configHubNameEditText = findViewById(R.id.configHubNameEditText);
        hubConfig.setInfo(new HubConfig.InfoBean());
        hubConfig.getInfo().setHubName(configHubNameEditText.getText().toString());
        EditText configVersionEditText = findViewById(R.id.configVersionEditText);
        try {
            hubConfig.getInfo().setConfigVersion(Integer.parseInt(configVersionEditText.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(HubLocalActivity.this, "请填写 配置版本", Toast.LENGTH_LONG).show();
            return null;
        }
        EditText configToolEditText = findViewById(R.id.configToolEditText);
        hubConfig.setWebCrawler(new HubConfig.WebCrawlerBean());
        hubConfig.getWebCrawler().setTool(configToolEditText.getText().toString());
        EditText jsFilePathEditText = findViewById(R.id.jsFilePathEditText);
        hubConfig.getWebCrawler().setFilePath(jsFilePathEditText.getText().toString());
        return hubConfig;
    }

    private void loadJSFromUri(Uri uri) {
        if (uri == null) return;
        String jsCode = FileUtil.readTextFromUri(uri);
        if (jsCode == null) return;
        JS_URI = uri;
        // 更新按钮提示信息
        String path = FileUtil.uriToPath(uri);
        Button selectJsFileButton = findViewById(R.id.selectJsFileButton);
        String selectedJsFileText = SELECTED_FILE_ADDRESS + path;
        selectJsFileButton.setText(selectedJsFileText);
        TextView jsTestTextView = findViewById(R.id.jsTestTextView);
        jsTestTextView.setText(jsCode);
    }

    private void loadConfigJSFormUri(@NonNull Uri uri) {
        String configPath = FileUtil.uriToPath(HUBCONFIG_URI);
        if (configPath == null) return;
        String jsAbsolutePath = FileUtil.uriToPath(uri);
        String jsPath = FileUtil.pathTransformAbsoluteToRelative(configPath, jsAbsolutePath);
        EditText jsFilePathEditText = findViewById(R.id.jsFilePathEditText);
        jsFilePathEditText.setText(jsPath);
    }

    private void loadConfigFromUri(Uri uri) {
        String configString = FileUtil.readTextFromUri(uri);
        HUBCONFIG_URI = uri;
        // 更新按钮提示信息
        String path = FileUtil.uriToPath(uri);
        Button selectConfigFileButton = findViewById(R.id.selectConfigFileButton);
        String selectedJsFileText = SELECTED_FILE_ADDRESS + path;
        selectConfigFileButton.setText(selectedJsFileText);
        Gson gson = new Gson();
        HubConfig hubConfig;
        try {
            hubConfig = gson.fromJson(configString, HubConfig.class);
        } catch (RuntimeException e) {
            Toast.makeText(HubLocalActivity.this, "请选择一个正确的配置文件", Toast.LENGTH_LONG).show();
            return;
        }
        loadConfigFromHubConfig(hubConfig);
    }


    private void loadConfigFromHubConfig(HubConfig hubConfig) {
        // 载入数据
        if (hubConfig != null) {
            EditText configBaseVersionEditText = findViewById(R.id.configBaseVersionEditText);
            configBaseVersionEditText.setText(String.valueOf(hubConfig.getBaseVersion()));
            EditText configUuidEditText = findViewById(R.id.configUuidEditText);
            configUuidEditText.setText(hubConfig.getUuid());
            EditText configHubNameEditText = findViewById(R.id.configHubNameEditText);
            configHubNameEditText.setText(hubConfig.getInfo().getHubName());
            EditText configVersionEditText = findViewById(R.id.configVersionEditText);
            configVersionEditText.setText(String.valueOf(hubConfig.getInfo().getConfigVersion()));
            EditText configToolEditText = findViewById(R.id.configToolEditText);
            configToolEditText.setText(hubConfig.getWebCrawler().getTool());
            EditText jsFilePathEditText = findViewById(R.id.jsFilePathEditText);
            jsFilePathEditText.setText(hubConfig.getWebCrawler().getFilePath());
        }
    }

    private void addHubConfigToDatabase(int databaseId) {
        // 获取数据
        HubConfig hubConfigGson = getHubConfigGson();
        // 存入数据
        if (hubConfigGson != null) {
            loadJSFromUri(JS_URI);
            TextView jsTestTextView = findViewById(R.id.jsTestTextView);
            String jsCode = jsTestTextView.getText().toString();
            if (!jsCode.equals("")) {
                boolean addHubSuccess = HubManager.addHubDatabase(databaseId, hubConfigGson, jsCode);
                if (addHubSuccess) {
                    Toast.makeText(HubLocalActivity.this, "数据库添加成功", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(HubLocalActivity.this, "什么？数据库添加失败！", Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(HubLocalActivity.this, "JS 代码为空", Toast.LENGTH_LONG).show();
        }
    }

    private void writeHubConfigToFile() {
        // 获取数据
        HubConfig hubConfigGson = getHubConfigGson();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String gsonText = gson.toJson(hubConfigGson);
        // 存入数据
        if (HUBCONFIG_URI == null)
            Toast.makeText(HubLocalActivity.this, "请选择配置文件，若无配置文件，你可以长按文件选择框创建新文件", Toast.LENGTH_LONG).show();
        else if (HUBCONFIG_URI.getPath() != null && hubConfigGson != null) {
            boolean writeSuccess;
            writeSuccess = FileUtil.writeTextFromUri(HUBCONFIG_URI, gsonText);
            if (writeSuccess) {
                Toast.makeText(HubLocalActivity.this, "文件保存成功", Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(HubLocalActivity.this, "什么？文件保存失败！", Toast.LENGTH_LONG).show();
        }
    }
}