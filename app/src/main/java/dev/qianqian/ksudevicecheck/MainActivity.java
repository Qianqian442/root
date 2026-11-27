package dev.qianqian.ksudevicecheck;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private TextView report;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 32, 28, 24);
        root.setBackgroundColor(0xFFF5F7FA);

        TextView title = new TextView(this);
        title.setText("KernelSU 四系适配检测");
        title.setTextSize(24);
        title.setTextColor(0xFF172033);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("检测设备信息、内核环境和现有 Root 状态。此版本只读取状态，不写入内核、不加载未知模块，也不执行提权载荷。");
        note.setTextSize(14);
        note.setTextColor(0xFF596579);
        note.setPadding(0, 14, 0, 18);
        root.addView(note);

        Button refresh = new Button(this);
        refresh.setText("一键检测设备与 Root 状态");
        root.addView(refresh);

        report = new TextView(this);
        report.setTextSize(14);
        report.setTextColor(0xFF202A3A);
        report.setPadding(0, 18, 0, 8);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(report);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        refresh.setOnClickListener(v -> runChecks());
        setContentView(root);
        runChecks();
    }

    private void runChecks() {
        StringBuilder out = new StringBuilder();
        out.append("设备型号：").append(Build.MODEL).append('\n');
        out.append("设备代号：").append(Build.DEVICE).append('\n');
        out.append("产品：").append(Build.PRODUCT).append('\n');
        out.append("制造商：").append(Build.MANUFACTURER).append('\n');
        out.append("系统指纹：").append(Build.FINGERPRINT).append('\n');
        out.append("Android：").append(Build.VERSION.RELEASE)
           .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        out.append("ABI：");
        for (String abi : Build.SUPPORTED_ABIS) out.append(abi).append(' ');
        out.append("\n内核版本：").append(readCommand("uname", "-r")).append("\n");
        out.append("SELinux：").append(readFile("/sys/fs/selinux/enforce")).append("\n");
        out.append("KernelSU 模块目录：").append(new File("/sys/module/kernelsu").exists() ? "存在" : "未发现").append("\n");
        out.append("常见 su 路径：").append(findSuPaths()).append("\n\n");
        out.append("目标设备 PEMM00：")
           .append("PEMM00".equalsIgnoreCase(Build.MODEL) || "PEMM00".equalsIgnoreCase(Build.DEVICE)
               ? "字段匹配（仍需核对真实内核构建与设备树）" : "未能从公开属性确认，请以设备实际型号为准")
           .append("\n\n");
        out.append("Root 状态：").append(checkExistingRoot()).append("\n");
        out.append("\n适配结论：仅凭 Android 版本和 4.19.191+ 版本号不能确认模块 ABI、内核符号地址或结构体偏移。\n");
        out.append("未完成设备专属验证前，不会加载临时驱动。错误的内核偏移可能导致系统崩溃或无法启动。\n");
        out.append("\n本工具当前是只读检测版，不提供一键临时 Root。");
        report.setText(out.toString());
    }

    private String readCommand(String... command) {
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) b.append(line).append(' ');
            int code = p.waitFor();
            String value = b.toString().trim();
            return value.isEmpty() ? "无输出（退出码 " + code + "）" : value;
        } catch (Exception e) { return "读取失败：" + e.getClass().getSimpleName(); }
    }

    private String readFile(String path) {
        try {
            BufferedReader r = new BufferedReader(new java.io.FileReader(path));
            String value = r.readLine();
            r.close();
            return value == null ? "无数据" : value.trim();
        } catch (Exception e) { return "不可读取"; }
    }

    private String findSuPaths() {
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/bin/.ext/su"};
        StringBuilder b = new StringBuilder();
        for (String path : paths) if (new File(path).exists()) b.append(path).append(' ');
        return b.length() == 0 ? "未发现" : b.toString().trim();
    }

    private String checkExistingRoot() {
        try {
            Process p = new ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) result.append(line).append(' ');
            int code = p.waitFor();
            String value = result.toString().trim();
            if (code == 0 && value.contains("uid=0")) return "已检测到可用 su：" + value;
            return "未检测到可用 Root（su 退出码 " + code + "）";
        } catch (Exception e) { return "未检测到可用 Root：" + e.getClass().getSimpleName(); }
    }
}
