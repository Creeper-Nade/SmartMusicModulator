package com.RocTech.musicswitcher.util; // 请改为您的包名

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.RocTech.musicswitcher.MapDisplay;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    private static final String TAG = "PermissionUtils";
    // 定义高德地图所需的核心权限
    private static final String[] CORE_LOCATION_PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /**
     * 检查并申请高德地图所需的所有权限（包括条件性的后台权限）
     * @param activity 当前的Activity
     * @param requestLauncher 用于启动权限请求的ActivityResultLauncher
     */
    public static void checkAndRequestMapPermissions(AppCompatActivity activity, ActivityResultLauncher<String[]> requestLauncher) {
        List<String> permissionsToRequest = new ArrayList<>();

        // 1. 检查并添加核心定位权限
        for (String perm : CORE_LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }

        // 2. 条件性地检查并添加后台定位权限 (Android 10/Q及以上且targetSdk>=29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                activity.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.Q) {
            String backgroundPerm = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
            if (ContextCompat.checkSelfPermission(activity, backgroundPerm) != PackageManager.PERMISSION_GRANTED) {
                // 可选：在申请前，可以在这里向用户解释为什么需要后台权限
                permissionsToRequest.add(backgroundPerm);
            }
        }

        // 3. 如果有权限需要申请，则启动请求
        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);
            requestLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            Log.d(TAG, "All permissions already granted.");
            // 所有权限已授予，可以执行后续操作（如跳转到地图页面）
            onAllPermissionsGranted(activity);
        }
    }

    /**
     * 处理权限申请结果
     * @param activity 当前的Activity
     * @param permissions 申请的权限数组
     * @param grantResults 授权结果数组
     */
    public static void onRequestPermissionsResult(AppCompatActivity activity, String[] permissions, int[] grantResults) {
        boolean allGranted = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                // 处理被拒绝的权限（特别是“不再询问”的情况）
                if (!activity.shouldShowRequestPermissionRationale(permissions[i])) {
                    // 用户勾选了“不再询问”，需要引导用户去设置页手动开启
                    showPermissionGuideDialog(activity, permissions[i]);
                    return; // 遇到“不再询问”，直接显示引导对话框
                }
            }
        }

        if (allGranted) {
            onAllPermissionsGranted(activity);
        } else {
            // 有权限被简单拒绝（未勾选“不再询问”），可以再次尝试申请或给出提示
            showSimpleDenyDialog(activity);
        }
    }

    private static void onAllPermissionsGranted(AppCompatActivity activity) {
        // 所有权限获取成功，这里跳转到你的地图展示页面
        activity.startActivity(new Intent(activity.getApplicationContext(), MapDisplay.class));
        activity.finish(); // 关闭当前权限申请页
    }

    private static void showPermissionGuideDialog(AppCompatActivity activity, String deniedPermission) {
        new AlertDialog.Builder(activity)
                .setTitle("权限被永久拒绝")
                .setMessage("您已拒绝" + getPermissionName(deniedPermission) + "权限，并选择了“不再询问”。地图功能需要此权限才能正常工作。\n\n请前往应用设置手动开启权限。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                    activity.finish(); // 关闭当前Activity
                })
                .setNegativeButton("退出", (dialog, which) -> activity.finish())
                .setCancelable(false)
                .show();
    }

    private static void showSimpleDenyDialog(AppCompatActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("权限申请被拒绝")
                .setMessage("部分必要权限被拒绝，地图功能将无法正常使用。")
                .setPositiveButton("重新申请", (dialog, which) -> {
                    // 重新申请的逻辑可以在这里触发，或者交由Activity的onResume处理
                })
                .setNegativeButton("退出", (dialog, which) -> activity.finish())
                .show();
    }

    private static String getPermissionName(String permission) {
        switch (permission) {
            case android.Manifest.permission.ACCESS_FINE_LOCATION:
                return "精确位置信息";
            case android.Manifest.permission.ACCESS_COARSE_LOCATION:
                return "大致位置信息";
            case android.Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                return "后台位置信息";
            default:
                return "必要";
        }
    }
}