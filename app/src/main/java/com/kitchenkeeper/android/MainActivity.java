package com.kitchenkeeper.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;

/**
 * Android entry point for the existing private Kitchenkeeper application.
 *
 * Custom Tabs deliberately uses the device browser's real sign-in session.
 * No credentials, cookies or JavaScript bridges are copied into this app.
 * The browser also handles the existing photo picker, downloads and sharing.
 */
public final class MainActivity extends Activity {
    private boolean launching;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        renderHome();
        if (savedInstanceState == null) {
            getWindow().getDecorView().post(this::openKitchen);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        launching = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        launching = false;
        openKitchen();
    }

    private void openKitchen() {
        if (launching || isFinishing()) return;
        launching = true;
        Uri url = Uri.parse(getString(R.string.kitchen_url));
        try {
            CustomTabColorSchemeParams colors = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(getColor(R.color.forest))
                    .setNavigationBarColor(getColor(R.color.background))
                    .build();
            CustomTabsIntent tab = new CustomTabsIntent.Builder()
                    .setDefaultColorSchemeParams(colors)
                    .setColorScheme(CustomTabsIntent.COLOR_SCHEME_LIGHT)
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(true)
                    .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                    .build();

            // Prefer a supporting browser, including the user's default when possible.
            // A null package leaves normal browser resolution available as a fallback.
            String browserPackage = CustomTabsClient.getPackageName(this, null);
            if (browserPackage != null) tab.intent.setPackage(browserPackage);
            tab.launchUrl(this, url);
        } catch (ActivityNotFoundException exception) {
            launching = false;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.no_browser_title)
                    .setMessage(R.string.no_browser_message)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } catch (SecurityException exception) {
            launching = false;
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void renderHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(28), dp(36), dp(28), dp(36));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.kitchenkeeper);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        content.addView(icon, new LinearLayout.LayoutParams(dp(88), dp(88)));

        TextView title = text(R.string.welcome_title, 30, R.color.foreground);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        add(content, title, 24);

        TextView description = text(R.string.welcome_description, 17, R.color.muted);
        add(content, description, 16);

        Button open = new Button(this);
        open.setText(R.string.open_kitchen);
        open.setTextSize(16);
        open.setAllCaps(false);
        open.setTextColor(getColor(R.color.white));
        open.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.forest)));
        open.setMinHeight(dp(54));
        open.setPadding(dp(24), dp(12), dp(24), dp(12));
        open.setOnClickListener(view -> openKitchen());
        add(content, open, 26);

        add(content, text(R.string.account_hint, 15, R.color.muted), 22);
        add(content, text(R.string.connection_hint, 14, R.color.muted), 14);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private TextView text(int resource, int size, int color) {
        TextView view = new TextView(this);
        view.setText(resource);
        view.setTextSize(size);
        view.setTextColor(getColor(color));
        view.setGravity(Gravity.CENTER);
        view.setLineSpacing(dp(3), 1f);
        return view;
    }

    private void add(LinearLayout parent, View child, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMargin);
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
