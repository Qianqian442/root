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
import java.io.InputStreamReader;
import java.util.Locale;

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
        note.setText("设备信息与现有 Root 状态检查。此版本不会写入内核、加载未知模块或执行提权载荷。");
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
        out.append("Android：").append(Build.VERSION.RELEASE)
           .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        out.append("ABI：");
        for (String abi : Build.SUPPORTED_ABIS) out.append(abi).append(' ');
        out.append("\n内核版本：").append(readKernel()).append("\n");
        out.append("目标设备 PEMM00：")
           .append("PEMM00".equalsIgnoreCase(Build.MODEL) ? "型号字段匹配（仍需核对设备树/内核）" : "型号字段不匹配或需人工确认")
           .append("\n\n");
        out.append("Root 状态：").append(checkExistingRoot()).append("\n");
        out.append("\n适配结论：仅凭 Android 版本和内核主版本不能确认模块 ABI、符号地址或结构体偏移。未通过设备专属验证前，不会加载临时驱动。\n");
        out.append("\n注意：临时 Root 需要设备本身存在可验证且适用的授权/漏洞路径；普通 APK 无法凭空获得内核权限。");
        report.setText(out.toString());
    }

    private String readKernel() {
        try {
            Process p = new ProcessBuilder("uname", "-r").redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.waitFor();
            return line == null ? "读取失败" : line;
        } catch (Exception e) { return "读取失败：" + e.getClass().getSimpleName(); }
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
