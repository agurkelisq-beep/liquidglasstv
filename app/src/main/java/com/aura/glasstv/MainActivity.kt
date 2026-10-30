package com.aura.glasstv

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("glass_tv", MODE_PRIVATE) }

    private lateinit var wallpaper: ScenicBackdrop
    private lateinit var appsRow: LinearLayout
    private lateinit var bottomTitle: TextView
    private lateinit var bottomSubTitle: TextView

    private val apps = mutableListOf<TvApp>()
    private var initialFocus: View? = null

    data class TvApp(
        val packageName: String,
        val activityName: String,
        val name: String,
        val icon: Drawable?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        drawScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::appsRow.isInitialized) refreshApps()
    }

    private fun dp(value: Number): Int =
        (value.toFloat() * resources.displayMetrics.density + 0.5f).toInt()

    private fun label(
        text: String,
        size: Float,
        color: Int = Color.WHITE,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            typeface = Typeface.create(
                if (bold) "sans-serif-medium" else "sans-serif-light",
                Typeface.NORMAL
            )
        }
    }

    private fun glassBackground(radius: Float = 26f, focused: Boolean = false): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (focused) {
                intArrayOf(
                    0xB8F3FBFF.toInt(),
                    0x80CDE5FF.toInt(),
                    0x7092B8D8
                )
            } else {
                intArrayOf(
                    0x66CFE4FF,
                    0x3A85A3C6,
                    0x44718BA6
                )
            }
        ).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(
                dp(if (focused) 2 else 1),
                if (focused) 0xE6FFFFFF.toInt() else 0x66FFFFFF
            )
        }
    }

    private fun darkGlassBackground(radius: Float = 26f): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                0x60080E18,
                0x7A101A26,
                0x8C0D1621.toInt()
            )
        ).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), 0x50FFFFFF)
        }
    }

    private fun drawScreen() {
        val root = FrameLayout(this)

        wallpaper = ScenicBackdrop(this).apply {
            scene = prefs.getInt("scene", 0)
        }
        root.addView(wallpaper, FrameLayout.LayoutParams(-1, -1))

        root.addView(
            View(this).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        0x24000000,
                        0x14000000,
                        0x88000000.toInt()
                    )
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(
            hero,
            FrameLayout.LayoutParams(-1, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dp(70)
            }
        )

        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        hero.addView(
            label(dateFormat.format(Date()), 15f, 0xD8E7F5FF.toInt(), false).apply {
                gravity = Gravity.CENTER
            }
        )

        hero.addView(
            TextClock(this).apply {
                format12Hour = "hh:mm"
                format24Hour = "HH:mm"
                textSize = 78f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                setShadowLayer(dp(12).toFloat(), 0f, dp(3).toFloat(), 0x50000000)
            },
            LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = dp(6)
            }
        )

        hero.addView(
            label("Liquid Glass TV", 16f, 0xDCEBFAFF.toInt(), false).apply {
                gravity = Gravity.CENTER
            }
        )

        val dockWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(
            dockWrap,
            FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                leftMargin = dp(30)
                rightMargin = dp(30)
                bottomMargin = dp(42)
            }
        )

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = glassBackground(30f, false)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            elevation = dp(12).toFloat()
            clipChildren = false
        }

        dockWrap.addView(
            dock,
            LinearLayout.LayoutParams(-2, dp(124))
        )

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        dock.addView(scroll, LinearLayout.LayoutParams(-2, -1))

        appsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
        }
        scroll.addView(appsRow, ViewGroup.LayoutParams(-2, -1))

        bottomTitle = label("", 17f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
        }
        dockWrap.addView(
            bottomTitle,
            LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = dp(12)
            }
        )

        bottomSubTitle = label("", 12f, 0xC8D8E8FF.toInt(), false).apply {
            gravity = Gravity.CENTER
        }
        dockWrap.addView(
            bottomSubTitle,
            LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = dp(4)
            }
        )

        setContentView(root)
        refreshApps()
        initialFocus?.requestFocus()
    }

    private fun queryApps(): List<TvApp> {
        val pm = packageManager
        val result = linkedMapOf<String, TvApp>()
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)

        for (category in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            @Suppress("DEPRECATION")
            val matches: List<ResolveInfo> = pm.queryIntentActivities(query, 0)

            for (r in matches) {
                val info = r.activityInfo ?: continue
                if (info.packageName == packageName) continue
                if (!info.enabled || !info.applicationInfo.enabled) continue
                if (result.containsKey(info.packageName)) continue

                result[info.packageName] = TvApp(
                    packageName = info.packageName,
                    activityName = info.name,
                    name = r.loadLabel(pm)?.toString()?.ifBlank { info.packageName } ?: info.packageName,
                    icon = try { r.loadIcon(pm) } catch (_: Exception) { null }
                )
            }
        }

        val preferredOrder = listOf(
            "com.google.android.youtube.tv",
            "com.spotify.tv.android",
            "com.spotify.music",
            "com.stremio.one",
            "com.netflix.ninja",
            "com.disney.disneyplus",
            "com.amazon.amazonvideo.livingroom",
            "com.apple.atve.androidtv.appletv"
        )

        return result.values.sortedWith(
            compareBy<TvApp> {
                val idx = preferredOrder.indexOf(it.packageName)
                if (idx == -1) 999 else idx
            }.thenBy { it.name.lowercase(Locale.getDefault()) }
        )
    }

    private fun refreshApps() {
        apps.clear()
        apps.addAll(queryApps())

        appsRow.removeAllViews()
        initialFocus = null

        if (apps.isEmpty()) {
            val empty = label("No TV apps found", 16f, Color.WHITE, true)
            appsRow.addView(empty)
            bottomTitle.text = "No apps"
            bottomSubTitle.text = "Install apps to continue"
            return
        }

        val shownApps = apps.take(8)

        for (app in shownApps) {
            val tile = appTile(app)
            appsRow.addView(
                tile,
                LinearLayout.LayoutParams(dp(150), dp(92)).apply {
                    leftMargin = dp(6)
                    rightMargin = dp(6)
                }
            )
            if (initialFocus == null) initialFocus = tile
        }

        val settingsTile = settingsTile()
        appsRow.addView(
            settingsTile,
            LinearLayout.LayoutParams(dp(92), dp(92)).apply {
                leftMargin = dp(6)
                rightMargin = dp(6)
            }
        )

        bottomTitle.text = shownApps.firstOrNull()?.name ?: "Apps"
        bottomSubTitle.text = "Press OK to open"
    }

    private fun appTile(app: TvApp): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = darkGlassBackground(22f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isFocusable = true
            isFocusableInTouchMode = false
            clipChildren = false
            contentDescription = app.name
        }

        val iconBox = FrameLayout(this).apply {
            background = glassBackground(16f, false)
            clipToOutline = true
        }

        tile.addView(iconBox, LinearLayout.LayoutParams(dp(52), dp(52)))

        if (app.icon != null) {
            iconBox.addView(
                ImageView(this).apply {
                    setImageDrawable(app.icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                },
                FrameLayout.LayoutParams(-1, -1)
            )
        } else {
            iconBox.addView(
                label(app.name.take(1).uppercase(Locale.getDefault()), 24f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                },
                FrameLayout.LayoutParams(-1, -1)
            )
        }

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        texts.addView(
            label(app.name, 15f, Color.WHITE, true).apply {
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
            }
        )

        texts.addView(
            label("Open app", 11f, 0xC9DCEEFF.toInt(), false)
        )

        tile.addView(
            texts,
            LinearLayout.LayoutParams(0, -2, 1f).apply {
                leftMargin = dp(12)
            }
        )

        tile.setOnClickListener { launch(app) }

        tile.setOnFocusChangeListener { _, focused ->
            tile.background = if (focused) glassBackground(22f, true) else darkGlassBackground(22f)
            iconBox.background = glassBackground(16f, focused)
            tile.animate()
                .scaleX(if (focused) 1.05f else 1f)
                .scaleY(if (focused) 1.05f else 1f)
                .translationY(if (focused) -dp(4).toFloat() else 0f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(170)
                .start()

            if (focused) {
                bottomTitle.text = app.name
                bottomSubTitle.text = "Press OK to open"
            }
        }

        return tile
    }

    private fun settingsTile(): View {
        val tile = FrameLayout(this).apply {
            background = darkGlassBackground(22f)
            isFocusable = true
            isFocusableInTouchMode = false
            contentDescription = "Settings"
        }

        tile.addView(
            label("⚙", 28f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
            },
            FrameLayout.LayoutParams(-2, -2, Gravity.CENTER)
        )

        tile.setOnClickListener { showSettings() }

        tile.setOnFocusChangeListener { _, focused ->
            tile.background = if (focused) glassBackground(22f, true) else darkGlassBackground(22f)
            tile.animate()
                .scaleX(if (focused) 1.05f else 1f)
                .scaleY(if (focused) 1.05f else 1f)
                .translationY(if (focused) -dp(4).toFloat() else 0f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(170)
                .start()

            if (focused) {
                bottomTitle.text = "Settings"
                bottomSubTitle.text = "Wallpaper, clock, refresh, exit"
            }
        }

        return tile
    }

    private fun showSettings() {
        val options = arrayOf(
            "Refresh apps",
            "Change wallpaper",
            "Toggle 12/24h clock",
            "Exit launcher"
        )

        AlertDialog.Builder(this)
            .setTitle("Liquid Glass TV")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> refreshApps()
                    1 -> {
                        wallpaper.scene = (wallpaper.scene + 1) % 3
                        prefs.edit().putInt("scene", wallpaper.scene).apply()
                        wallpaper.invalidate()
                    }
                    2 -> {
                        val current = prefs.getBoolean("clock24", true)
                        prefs.edit().putBoolean("clock24", !current).apply()
                        Toast.makeText(
                            this,
                            if (!current) "24-hour clock enabled" else "12-hour clock enabled",
                            Toast.LENGTH_SHORT
                        ).show()
                        recreate()
                    }
                    3 -> finish()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun launch(app: TvApp) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            retryLaunch(app)
        } catch (_: SecurityException) {
            retryLaunch(app)
        } catch (_: Exception) {
            retryLaunch(app)
        }
    }

    private fun retryLaunch(app: TvApp) {
        try {
            val fallback = packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
                ?: packageManager.getLaunchIntentForPackage(app.packageName)

            if (fallback == null) throw ActivityNotFoundException()
            startActivity(fallback)
        } catch (_: Exception) {
            Toast.makeText(this, "${app.name} couldn't open", Toast.LENGTH_LONG).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showSettings()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
