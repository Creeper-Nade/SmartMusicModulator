package com.RocTech.musicswitcher;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.maps.MapsInitializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // 定义需要的权限
    private static final String[] FOREGROUND_LOCATION_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final String BACKGROUND_LOCATION_PERM = Manifest.permission.ACCESS_BACKGROUND_LOCATION;

    // 权限申请启动器
    private ActivityResultLauncher<String[]> requestForegroundLocationLauncher;
    private ActivityResultLauncher<String> requestBackgroundLocationLauncher;

    // 当前申请阶段
    private enum PermissionStage { IDLE, REQUESTING_FOREGROUND, REQUESTING_BACKGROUND, COMPLETE }
    private PermissionStage currentStage = PermissionStage.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 启用边缘到边缘
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. 初始化权限启动器 (必须在任何对话框之前完成)
        initPermissionLaunchers();

        // 2. 显示隐私协议
        showPrivacyComplianceDialog();
    }

    /**
     * 初始化权限启动器
     */
    private void initPermissionLaunchers() {
        // 启动器1：申请前台定位权限（多个）
        requestForegroundLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Log.d(TAG, "前台权限申请结果返回: " + result);
                    onForegroundLocationPermissionResult(result);
                });

        // 启动器2：单独申请后台定位权限（单个）
        requestBackgroundLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    Log.d(TAG, "后台权限申请结果返回: " + (isGranted ? "GRANTED" : "DENIED"));
                    onBackgroundLocationPermissionResult(isGranted);
                });
    }

    /**
     * 显示高德隐私协议对话框
     */
    private void showPrivacyComplianceDialog() {
        MapsInitializer.updatePrivacyShow(this, true, true);

        SpannableStringBuilder spannable = new SpannableStringBuilder(
                "Dear user, Thank you for supporting RocTech all along\n" +
                        "Now CLICK AGREE OR WE WILL SERVE YOU NO MORE!"
        );
        spannable.setSpan(
                new ForegroundColorSpan(Color.BLUE),
                spannable.toString().indexOf("AGREE"),
                spannable.toString().indexOf("AGREE") + "AGREE".length(),
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        new AlertDialog.Builder(this)
                .setTitle("Privacy Protocol")
                .setMessage(spannable)
                .setPositiveButton("Click me", (dialog, which) -> onPrivacyAgreed())
                .setNegativeButton("Don't Click Me", (dialog, which) -> onPrivacyRejected())
                .setCancelable(false)
                .show();
    }

    /**
     * 用户同意隐私协议
     */
    private void onPrivacyAgreed() {
        Log.i(TAG, "用户同意隐私协议，开始权限流程");
        MapsInitializer.updatePrivacyAgree(this, true);
        startPermissionFlow();
    }

    /**
     * 用户拒绝隐私协议
     */
    private void onPrivacyRejected() {
        Log.w(TAG, "用户拒绝隐私协议");
        MapsInitializer.updatePrivacyAgree(this, false);
        new AlertDialog.Builder(this)
                .setTitle("隐私协议")
                .setMessage("必须同意隐私协议才能使用应用。")
                .setPositiveButton("退出", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * 开始顺序权限申请流程
     */
    private void startPermissionFlow() {
        Log.d(TAG, "开始权限申请流程");
        checkAndRequestForegroundLocationPermission();
    }

    /**
     * 步骤1：检查并申请前台定位权限
     */
    private void checkAndRequestForegroundLocationPermission() {
        currentStage = PermissionStage.REQUESTING_FOREGROUND;

        // 检查是否已有前台权限
        boolean allGranted = true;
        for (String perm : FOREGROUND_LOCATION_PERMS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            Log.d(TAG, "前台定位权限已拥有，进入下一步");
            checkAndRequestBackgroundLocationPermission();
        } else {
            Log.d(TAG, "申请前台定位权限...");
            // 触发系统弹窗
            requestForegroundLocationLauncher.launch(FOREGROUND_LOCATION_PERMS);
        }
    }

    /**
     * 处理前台定位权限申请结果
     */
    private void onForegroundLocationPermissionResult(Map<String, Boolean> result) {
        Log.d(TAG, "处理前台权限结果，Stage: " + currentStage);

        // 验证是否所有需要的权限都被授予
        boolean allGranted = true;
        for (String perm : FOREGROUND_LOCATION_PERMS) {
            Boolean granted = result.get(perm);
            if (granted == null || !granted) {
                allGranted = false;
                Log.d(TAG, "权限被拒绝: " + perm);

                // 检查用户是否勾选了“不再询问”
                if (!shouldShowRequestPermissionRationale(perm)) {
                    Log.d(TAG, "用户勾选了‘不再询问’: " + perm);
                    showPermissionPermanentlyDeniedDialog("前台定位");
                    return; // 流程终止
                }
            }
        }

        if (allGranted) {
            Log.d(TAG, "前台定位权限全部授予成功");
            checkAndRequestBackgroundLocationPermission();
        } else {
            Log.w(TAG, "前台定位权限被部分或全部拒绝");
            showPermissionDeniedDialog("没有定位权限，无法使用地图功能。");
        }
    }

    /**
     * 步骤2：检查并申请后台定位权限
     */
    private void checkAndRequestBackgroundLocationPermission() {
        currentStage = PermissionStage.REQUESTING_BACKGROUND;

        // 条件判断：仅当 Android 10 (API 29) 及以上且 targetSdk >= 29 时才需要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.Q) {

            if (ContextCompat.checkSelfPermission(this, BACKGROUND_LOCATION_PERM) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "后台定位权限已拥有，进入地图");
                enterMapActivity();
            } else {
                Log.d(TAG, "需要申请后台定位权限，先向用户解释");
                showBackgroundPermissionRationaleDialog();
            }
        } else {
            // 低版本系统，无需后台权限，直接进入地图
            Log.d(TAG, "系统版本较低，无需后台定位权限");
            enterMapActivity();
        }
    }

    /**
     * 显示后台权限申请解释对话框
     */
    private void showBackgroundPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要后台定位权限")
                .setMessage("为了在应用切换到后台时也能获取位置（例如持续记录轨迹），需要您授予“始终允许”定位权限。\n\n接下来系统会弹窗询问，请选择“始终允许”。")
                .setPositiveButton("去申请", (dialog, which) -> {
                    Log.d(TAG, "用户确认，开始申请后台权限");
                    // 触发系统弹窗
                    requestBackgroundLocationLauncher.launch(BACKGROUND_LOCATION_PERM);
                })
                .setNegativeButton("仅前台使用", (dialog, which) -> {
                    Log.d(TAG, "用户选择不申请后台权限，仍进入地图");
                    enterMapActivity();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 处理后台上权限申请结果
     */
    private void onBackgroundLocationPermissionResult(boolean granted) {
        Log.d(TAG, "后台权限结果处理: " + (granted ? "GRANTED" : "DENIED"));

        if (!granted) {
            // 即使后台权限被拒绝，也允许进入地图（前台功能可用）
            new AlertDialog.Builder(this)
                    .setMessage("您拒绝了后台定位权限。某些后台运行功能将受限，但基础地图功能仍可使用。")
                    .setPositiveButton("确定", null)
                    .show();
        }
        // 无论结果如何，都进入地图页面
        enterMapActivity();
    }

    /**
     * 最终步骤：所有权限流程结束，进入地图页面
     */
    private void enterMapActivity() {
        currentStage = PermissionStage.COMPLETE;
        Log.i(TAG, "所有权限流程完成，跳转到 MapDisplay");
        startActivity(new Intent(this, MapDisplay.class));
        finish(); // 关闭当前Activity
    }

    /**
     * 显示权限被简单拒绝的对话框
     */
    private void showPermissionDeniedDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("权限被拒绝")
                .setMessage(message)
                .setPositiveButton("退出", (dialog, which) -> finish())
                .show();
    }

    /**
     * 显示权限被永久拒绝（不再询问）的引导对话框
     */
    private void showPermissionPermanentlyDeniedDialog(String permissionName) {
        new AlertDialog.Builder(this)
                .setTitle("权限被永久拒绝")
                .setMessage(permissionName + "权限已被永久拒绝。请前往系统设置手动开启。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("退出", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}