package com.aura.glasstv

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

/**
 * Glass TV v0.1. A normal, remote-driven app on purpose: it never replaces the
 * operator's home screen or changes Android settings.
 */
class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("glass_tv", MODE_PRIVATE) }
    private lateinit var wallpaper: ScenicBackdrop
    private lateinit var appsRow: LinearLayout
    private lateinit var countLabel: TextView
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

    private fun dp(n: Number): Int = (n.toFloat() * resources.displayMetrics.density + 0.5f).toInt()

    private fun label(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif-light", Typeface.NORMAL)
        }
    }

    private fun glassBackground(radius: Float = 24f, focused: Boolean = false): Drawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, if (focused)
            intArrayOf(0xB3FFFFFF.toInt(), 0x76D0E9FF, 0x7094B4D8)
        else
            intArrayOf(0x6AB0C5E0, 0x306C88AA, 0x48647D9C)
        ).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(if (focused) 2 else 1), if (focused) Color.WHITE else 0x7AFFFFFF)
        }

    private fun drawScreen() {
        val screen = FrameLayout(this)
        wallpaper = ScenicBackdrop(this).apply { scene = prefs.getInt("scene", 0) }
        screen.addView(wallpaper, FrameLayout.LayoutParams(-1, -1))

        // A darker wash makes labels readable on every background.
        screen.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x1C050D1F, 0x18050913, 0x82050E21.toInt()))
        }, FrameLayout.LayoutParams(-1, -1))

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        screen.addView(header, FrameLayout.LayoutParams(-1, dp(55), Gravity.TOP).apply {
            leftMargin = dp(50); rightMargin = dp(50); topMargin = dp(25)
        })
        header.addView(label("◈  glass tv", 20f, Color.WHITE, bold = true))
        header.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val headerButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        header.addView(headerButtons)
        headerButtons.addView(topButton("↻  Refresh") { refreshApps() })
        headerButtons.addView(topButton("◐  Wallpaper") {
            wallpaper.scene = (wallpaper.scene + 1) % 3
            prefs.edit().putInt("scene", wallpaper.scene).apply()
            wallpaper.invalidate()
        })
        headerButtons.addView(topButton("✕  Exit") { finish() })

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        screen.addView(hero, FrameLayout.LayoutParams(-1, dp(225), Gravity.TOP).apply {
            leftMargin = dp(58); rightMargin = dp(40); topMargin = dp(106)
        })
        val today = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            .format(Date()).uppercase(Locale.getDefault())
        hero.addView(label(today, 12f, 0xD6E7F0FF.toInt(), bold = true).apply { letterSpacing = 0.22f })
        hero.addView(label("A little more magic.", 39f, Color.WHITE).apply {
            setShadowLayer(dp(9).toFloat(), 0f, dp(2).toFloat(), 0x60000000)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
        hero.addView(label("Your apps, without the clutter.", 17f, 0xF1DBE9FF.toInt()),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        val statusRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        hero.addView(statusRow, LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(14) })
        val clock = TextClock(this).apply {
            format12Hour = "h:mm"
            format24Hour = "HH:mm"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        statusRow.addView(clock)
        statusRow.addView(label("    •    Android TV", 13f, 0xD9DDEAFF.toInt()))

        val dock = LinearLayout(this).apply {
            background = glassBackground(29f)
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(13), dp(18), dp(13))
            elevation = dp(13).toFloat()
        }
        screen.addView(dock, FrameLayout.LayoutParams(-1, dp(170), Gravity.BOTTOM).apply {
            leftMargin = dp(43); rightMargin = dp(43); bottomMargin = dp(29)
        })

        val dockHeading = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        dock.addView(dockHeading, LinearLayout.LayoutParams(-1, dp(24)))
        dockHeading.addView(label("YOUR APPS", 12f, 0xEFF4FAFF.toInt(), true).apply { letterSpacing = 0.19f })
        dockHeading.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        countLabel = label("", 12f, 0xC7D9E9FF.toInt())
        dockHeading.addView(countLabel)

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            isFillViewport = false
            setPadding(dp(4), dp(4), dp(4), dp(1))
        }
        dock.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        appsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        scroll.clipChildren = false
        dock.clipChildren = false
        scroll.addView(appsRow, ViewGroup.LayoutParams(-2, -1))

        setContentView(screen)
        refreshApps()
        initialFocus?.requestFocus()
    }

    private fun topButton(text: String, onClick: () -> Unit): TextView {
        return label(text, 12f, Color.WHITE, bold = true).apply {
            gravity = Gravity.CENTER
            background = glassBackground(20f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            isFocusable = true
            isFocusableInTouchMode = false
            setOnClickListener { onClick() }
            setOnFocusChangeListener { v, selected ->
                v.background = glassBackground(20f, selected)
                v.animate().scaleX(if (selected) 1.07f else 1f)
                    .scaleY(if (selected) 1.07f else 1f)
                    .setDuration(160).start()
            }
            val p = LinearLayout.LayoutParams(-2, dp(38)); p.leftMargin = dp(9)
            layoutParams = p
        }
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
                    info.packageName,
                    info.name,
                    r.loadLabel(pm)?.toString()?.ifBlank { info.packageName } ?: info.packageName,
                    try { r.loadIcon(pm) } catch (_: Exception) { null }
                )
            }
        }
        val pins = prefs.getStringSet("pins", emptySet()).orEmpty()
        return result.values.sortedWith(compareByDescending<TvApp> { it.packageName in pins }
            .thenBy { it.name.lowercase(Locale.getDefault()) })
    }

    private fun refreshApps() {
        apps.clear()
        apps.addAll(queryApps())
        countLabel.text = "${apps.size} installed  ·  hold OK to pin"
        appsRow.removeAllViews()
        initialFocus = null
        if (apps.isEmpty()) {
            val empty = label("No launchable apps detected. Check TV app settings.", 16f)
            appsRow.addView(empty, LinearLayout.LayoutParams(-2, -1).apply { leftMargin = dp(20) })
            return
        }
        for (app in apps) {
            val tile = appTile(app)
            appsRow.addView(tile, LinearLayout.LayoutParams(dp(102), dp(113)).apply {
                leftMargin = dp(7); rightMargin = dp(7)
            })
            if (initialFocus == null) initialFocus = tile
        }
    }

    private fun appTile(app: TvApp): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            isFocusable = true
            isFocusableInTouchMode = false
            clipChildren = false
            contentDescription = "${app.name}. Open. Long press OK to pin or unpin."
        }
        val iconBox = FrameLayout(this).apply {
            background = glassBackground(19f)
            elevation = dp(5).toFloat()
            clipToOutline = true
        }
        tile.addView(iconBox, LinearLayout.LayoutParams(dp(78), dp(77)).apply { topMargin = dp(5) })
        if (app.icon != null) {
            iconBox.addView(ImageView(this).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }, FrameLayout.LayoutParams(-1, -1))
        } else {
            iconBox.addView(label(app.name.take(1).uppercase(Locale.getDefault()), 30f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams(-1, -1))
        }
        val name = label(app.name, 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), Color.BLACK)
        }
        tile.addView(name, LinearLayout.LayoutParams(-1, dp(23)).apply { topMargin = dp(5) })
        tile.setOnClickListener { launch(app) }
        tile.setOnLongClickListener {
            val current = prefs.getStringSet("pins", emptySet()).orEmpty().toMutableSet()
            val wasPinned = !current.add(app.packageName)
            if (wasPinned) current.remove(app.packageName)
            prefs.edit().putStringSet("pins", current).apply()
            Toast.makeText(this, if (wasPinned) "Unpinned ${app.name}" else "Pinned ${app.name}", Toast.LENGTH_SHORT).show()
            refreshApps()
            // Restore focus to the same app in its new position.
            val index = apps.indexOfFirst { a -> a.packageName == app.packageName }
            if (index >= 0) appsRow.getChildAt(index)?.requestFocus()
            true
        }
        tile.setOnFocusChangeListener { _, focused ->
            iconBox.background = glassBackground(19f, focused)
            tile.animate().scaleX(if (focused) 1.12f else 1f)
                .scaleY(if (focused) 1.12f else 1f)
                .translationY(if (focused) -dp(6).toFloat() else 0f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(175).start()
            name.setTextColor(if (focused) Color.WHITE else 0xE0FFFFFF.toInt())
        }
        return tile
    }

    private fun launch(app: TvApp) {
        try {
            // Explicit component from Android's own package manager query.
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
            Toast.makeText(this, "${app.name} couldn't open on this device", Toast.LENGTH_LONG).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Menu is an alternative to a long-press on OK for some TV remotes.
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            val current = currentFocus
            if (current != null && current.parent == appsRow) {
                current.performLongClick()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
