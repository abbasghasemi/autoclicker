package ghasemi.abbas.autoclicker

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import android.text.InputFilter
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Consumer
import ghasemi.abbas.autoclicker.stream.SerializedData
import ghasemi.abbas.autoclicker.ui.LauncherActivity
import ghasemi.abbas.autoclicker.ui.ScriptsConfigActivity
import ghasemi.abbas.autoclicker.ui.components.DurationTimeView
import ghasemi.abbas.autoclicker.ui.components.ExpendedView
import ghasemi.abbas.autoclicker.ui.components.LineView
import ghasemi.abbas.autoclicker.ui.components.MoveHelper
import ghasemi.abbas.autoclicker.ui.components.PlayPauseView
import ghasemi.abbas.autoclicker.ui.components.PointView
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.AnimationUtils
import ghasemi.abbas.autoclicker.utils.LayoutHelper
import ghasemi.abbas.autoclicker.utils.rippleBackground
import ghasemi.abbas.autoclicker.utils.value
import java.security.SecureRandom

class AppServiceHelper(val context: Service, val type: Int) : MoveHelper.PointViewMoveListener {
    companion object {
        const val minimumTimeClick = 10
        const val minimumSwipeClick = 300
        var isEnabled: Boolean = false
        var isRunning = false
        private var scriptsConfig: ArrayList<ScriptsConfig>? = null

        fun loadScriptsConfig(): ArrayList<ScriptsConfig> {
            if (scriptsConfig != null) {
                return scriptsConfig!!
            }
            scriptsConfig = ArrayList()
            val configList = AppConfig.instance().scriptsConfigList
            if (configList.isEmpty()) {
                return scriptsConfig!!
            }
            val serializedData = SerializedData(Base64.decode(configList, Base64.DEFAULT))
            val version = serializedData.readInt32(false)
            if (version == 0) {
                val count1 = serializedData.readInt32(false)
                for (i in 0..<count1) {
                    val name = serializedData.readString(false)!!
                    val synchronousExecution = serializedData.readBool(false)
                    val durationTime = serializedData.readInt64(false)
                    val widgets = ArrayList<Widget>()
                    val count2 = serializedData.readInt32(false)
                    for (j in 0..<count2) {
                        val viewType = serializedData.readByte(false).toInt()
                        if (viewType == 0) {
                            widgets.add(
                                Widget(
                                    viewType,
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                    serializedData.readInt64(false),
                                    serializedData.readInt32(false),
                                    0,
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                    0, 0
                                )
                            )
                        } else if (viewType == 1) {
                            widgets.add(
                                Widget(
                                    viewType,
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                    serializedData.readInt64(false),
                                    0,
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                    serializedData.readInt32(false),
                                )
                            )
                        }
                    }
                    scriptsConfig!!.add(
                        ScriptsConfig(
                            name,
                            synchronousExecution,
                            durationTime,
                            widgets
                        )
                    )
                }
            } else {
                AppConfig.instance().scriptsConfigList = ""
            }
            serializedData.cleanup()
            return scriptsConfig!!
        }

        fun saveScriptsConfig(scriptsConfig: ScriptsConfig, delete: Boolean = false) {
            var found = false
            if (delete) {
                loadScriptsConfig().remove(scriptsConfig)
            } else {
                for (script in loadScriptsConfig()) {
                    if (script == scriptsConfig) {
                        found = true
                        break
                    }
                }
            }
            if (!found && !delete) {
                loadScriptsConfig().add(scriptsConfig)
            }
            val serializedData = SerializedData()
            serializedData.writeInt32(0)
            serializedData.writeInt32(loadScriptsConfig().size)
            for (script in loadScriptsConfig()) {
                serializedData.writeString(script.name)
                serializedData.writeBool(script.synchronousExecution)
                serializedData.writeInt64(script.durationTime)
                serializedData.writeInt32(script.widgets.size)
                for (widget in script.widgets) {
                    serializedData.writeByte(widget.type)
                    serializedData.writeInt32(widget.count)
                    serializedData.writeInt32(widget.durationType)
                    serializedData.writeInt64(widget.duration)
                    if (widget.type == 0) {
                        serializedData.writeInt32(widget.longDuration)
                    } else if (widget.type == 1) {
                        serializedData.writeInt32(widget.swipeDuration)
                    }
                    serializedData.writeInt32(widget.number)
                    serializedData.writeInt32(widget.x1)
                    serializedData.writeInt32(widget.y1)
                    if (widget.type == 1) {
                        serializedData.writeInt32(widget.x2)
                        serializedData.writeInt32(widget.y2)
                    }
                }
            }
            AppConfig.instance().scriptsConfigList =
                Base64.encodeToString(serializedData.toByteArray(), Base64.DEFAULT)
            serializedData.cleanup()
        }

    }

    private val screenOffBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) {
                dismissSettings()
            } else if (type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY ||
                type == WindowManager.LayoutParams.TYPE_PHONE
            ) {
                context.stopService(Intent(context, AppService::class.java))
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var scriptsConfig: ScriptsConfig? = null
    private val widgets: ArrayList<WidgetHolder> = ArrayList()
    private var alertDialog: AlertDialog? = null
    private val settingsButtons: ArrayList<View> = ArrayList(7)
    private var timeToExit = 0L
    private var thread: AppThread? = null
    private val _object = Object()
    private val random = SecureRandom()
    private val size = AndroidUtils.dp(40f)

    init {
        windowManager =
            context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
        notifyScreenOff()
    }

    fun createSettings(scriptsConfig: ScriptsConfig? = null) {
        var loadConfig = false
        if (scriptsConfig != null && this.scriptsConfig != scriptsConfig) {
            this.scriptsConfig = scriptsConfig
            loadConfig = true
        }
        if (isEnabled) {
            if (loadConfig)
                loadScriptConfig()
            return
        }
        isEnabled = true
        NotificationCenter.instance()
            .postNotificationName(NotificationCenter.appServiceToggle, isEnabled)
        updateTileAndWidget()
        val linearLayout = LinearLayout(context)
        linearLayout.apply {
            val r = AndroidUtils.dpf(7f)
            orientation = LinearLayout.VERTICAL
            background = ShapeDrawable(
                RoundRectShape(
                    floatArrayOf(r, r, r, r, r, r, r, r), null, null
                )
            ).apply {
                paint.color = Color.argb(100, 0, 0, 0)
            }
            val p = AndroidUtils.dp(5f)
            setPadding(p, p, p, p)
        }
        val playStop = PlayPauseView(context)
        playStop.background = rippleBackground()
        playStop.setOnTouchListener { v, event ->
            if (thread != null) {
                playStop.isEnabled = false
                startStopWorker()
                AndroidUtils.runOnUIThread({
                    playStop.isEnabled = true
                }, 200)
            }
            false
        }
        playStop.setOnClickListener {
            if (thread == null) {
                startStopWorker()
            }
        }
        linearLayout.addView(playStop, LayoutHelper.createLinear(32f, 32f))
        settingsButtons.add(playStop)

        val add = ImageView(context)
        add.setImageResource(R.drawable.round_add_24)
        add.background = rippleBackground()
        add.setOnClickListener {
            addButton(
                Widget(
                    0,
                    AppConfig.instance().repeatCount,
                    AppConfig.instance().clickTimeType,
                    when (AppConfig.instance().clickTimeType) {
                        0 -> AppConfig.instance().clickTime.toLong()
                        1 -> AppConfig.instance().clickTime * 1000L
                        else -> AppConfig.instance().clickTime * 1000L * 60
                    },
                    AppConfig.instance().longClickTime, 0, widgets.size,
                    context.resources.displayMetrics.widthPixels / 2 - size / 2,
                    context.resources.displayMetrics.heightPixels / 2 - size / 2,
                    0, 0
                )
            )
        }
        add.setOnLongClickListener {
            addButton(
                Widget(
                    1,
                    AppConfig.instance().repeatCount,
                    AppConfig.instance().clickTimeType,
                    when (AppConfig.instance().clickTimeType) {
                        0 -> AppConfig.instance().clickTime.toLong()
                        1 -> AppConfig.instance().clickTime * 1000L
                        else -> AppConfig.instance().clickTime * 1000L * 60
                    },
                    0, AppConfig.instance().swipeTime, widgets.size,
                    context.resources.displayMetrics.widthPixels / 2 - size / 2,
                    context.resources.displayMetrics.heightPixels / 2 - size / 2,
                    random.nextInt(context.resources.displayMetrics.widthPixels),
                    random.nextInt(context.resources.displayMetrics.heightPixels)
                )
            )
            true
        }
        linearLayout.addView(add, LayoutHelper.createLinear(32f, 32f).apply {
            topMargin = AndroidUtils.dp(5f)
        })
        settingsButtons.add(add)

        val remove = ImageView(context)
        remove.setImageResource(R.drawable.round_delete_outline_24)
        remove.background = rippleBackground()
        remove.setOnClickListener {
            if (widgets.size > 1) {
                removeViewHolderFromWindow(widgets.removeLast())
            }
        }
        remove.setOnLongClickListener {
            removeAllWidgets()
            true
        }
        linearLayout.addView(remove, LayoutHelper.createLinear(32f, 32f).apply {
            topMargin = AndroidUtils.dp(10f)
        })
        settingsButtons.add(remove)

        val home = ImageView(context)
        home.setImageResource(R.drawable.round_home_24)
        home.background = rippleBackground()
        home.setOnClickListener {
            context.startActivity(Intent(context, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
        }
        linearLayout.addView(home, LayoutHelper.createLinear(32f, 32f).apply {
            topMargin = AndroidUtils.dp(10f)
        })
        settingsButtons.add(home)

        val settings = ImageView(context)
        settings.setImageResource(R.drawable.round_save_as_24)
        settings.background = rippleBackground()
        settings.setOnClickListener {
            openSettingsDialog()
        }
        linearLayout.addView(settings, LayoutHelper.createLinear(32f, 32f).apply {
            topMargin = AndroidUtils.dp(10f)
        })
        settingsButtons.add(settings)

        val exit = ImageView(context)
        exit.setImageResource(R.drawable.round_exit_to_app_24)
        exit.background = rippleBackground()
        exit.setOnClickListener {
            if (System.currentTimeMillis() / 1000 - timeToExit < 2) {
                dismissSettings()
                if (type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY ||
                    type == WindowManager.LayoutParams.TYPE_PHONE
                ) {
                    context.stopSelf()
                }
            }
            timeToExit = System.currentTimeMillis() / 1000
        }
        linearLayout.addView(exit, LayoutHelper.createLinear(32f, 32f).apply {
            topMargin = AndroidUtils.dp(10f)
        })
        settingsButtons.add(exit)

        val frameLayout = FrameLayout(context)
        val openClose = ImageView(context)
        val openCloseMove = ExpendedView(context)
        openClose.setOnTouchListener { _, event ->
            openCloseMove.onTouchEvent(event)
            false
        }
        openClose.setImageResource(R.drawable.round_zoom_in_map_24)
        openClose.background = rippleBackground()
        openClose.tag = true
        openClose.setOnClickListener {
            val open = it.tag as Boolean
            it.tag = !open
            add.visibility = if (open) View.GONE else View.VISIBLE
            remove.visibility = if (open) View.GONE else View.VISIBLE
            home.visibility = if (open) View.GONE else View.VISIBLE
            exit.visibility = if (open) View.GONE else View.VISIBLE
            settings.visibility = if (open) View.GONE else View.VISIBLE
            openClose.setImageResource(if (!open) R.drawable.round_zoom_in_map_24 else R.drawable.round_zoom_out_map_24)
            AnimationUtils.changeBounds(linearLayout)
        }
        openClose.setOnLongClickListener {
            for (i in 0..<linearLayout.childCount) {
                if (linearLayout.getChildAt(i).layoutParams is LinearLayout.LayoutParams) {
                    val params =
                        linearLayout.getChildAt(i).layoutParams as LinearLayout.LayoutParams
                    val tm = params.topMargin
                    params.topMargin = params.leftMargin
                    params.leftMargin = tm
                }
            }
            linearLayout.orientation =
                if (linearLayout.orientation == LinearLayout.VERTICAL) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            AnimationUtils.changeBounds(linearLayout)
            true
        }

        openCloseMove.pointViewMoveListener = object : MoveHelper.PointViewMoveListener {

            override fun findLayoutParams(view: View): WindowManager.LayoutParams? {
                return widgets[0].params
            }

            override fun onMove(
                view: View, x: Int, y: Int, z: Int, screenWidth: Int, screenHeight: Int
            ) {
                val params = findLayoutParams(view)
                params!!.x = x
                params.y = y
                windowManager?.updateViewLayout(linearLayout, params)
            }
        }

        frameLayout.addView(
            openCloseMove,
            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        )
        frameLayout.addView(
            openClose,
            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        )
        linearLayout.addView(frameLayout, LayoutHelper.createLinear(32f, 32f).apply {
            topMargin = AndroidUtils.dp(10f)
        })
        settingsButtons.add(frameLayout)

        val prams: WindowManager.LayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        prams.gravity = Gravity.TOP or Gravity.LEFT
        prams.x = 0
        prams.y = context.resources.displayMetrics.heightPixels / 2 - linearLayout.height / 2
        widgets.add(WidgetHolder(linearLayout, prams))
        windowManager!!.addView(linearLayout, prams)
        linearLayout.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                linearLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                prams.y =
                    context.resources.displayMetrics.heightPixels / 2 - linearLayout.height / 2
                windowManager?.updateViewLayout(linearLayout, prams)
            }
        })

        if (loadConfig)
            loadScriptConfig()
    }

    private fun openSettingsDialog() {
        val etName = EditText(context)
        val durationTimeView = DurationTimeView(context)
        var durationTime: Long = scriptsConfig?.durationTime
            ?: AppConfig.instance().durationTime
        val cbSynchronous = CheckBox(context)
        alertDialog =
            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(R.string.app_name)
                .setView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    etName.setHint(R.string.script_name)
                    durationTimeView.consumer = Consumer {
                        durationTime = it
                    }
                    durationTimeView.updateDurationTime(durationTime)
                    addView(
                        etName.apply {
                            value = scriptsConfig?.name ?: "Script ${loadScriptsConfig().size + 1}"
                            filters = arrayOf(InputFilter.LengthFilter(32))
                            typeface = ResourcesCompat.getFont(context, R.font.sans)
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.MATCH_PARENT,
                            LayoutHelper.WRAP_CONTENT,
                            15f,
                            5f,
                            15f,
                            0f
                        )
                    )
                    addView(
                        TextView(context).apply {
                            setText(R.string.maximum_duration_time)
                            setTextColor(Color.BLACK)
                            textSize = 14f
                            typeface = ResourcesCompat.getFont(context, R.font.sans)
                        }, LayoutHelper.createRelative(
                            LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                            15f, 10f, 15f, 0f, RelativeLayout.CENTER_HORIZONTAL
                        )
                    )
                    addView(
                        durationTimeView,
                        LayoutHelper.createLinear(
                            LayoutHelper.MATCH_PARENT,
                            LayoutHelper.WRAP_CONTENT,
                            15f,
                            0f,
                            15f,
                            0f
                        )
                    )
                    addView(
                        cbSynchronous.apply {
                            setText(R.string.synchronous_execution_of_widgets)
                            isChecked = scriptsConfig?.synchronousExecution
                                ?: AppConfig.instance().synchronousExecution
                        },
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40f, 15f, 0f, 15f, 20f)
                    )
                })
                .setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
                    if (etName.text.toString().isNotEmpty()) {
                        saveScriptConfig(
                            etName.text.toString(),
                            cbSynchronous.isChecked,
                            durationTime
                        )
                    } else {
                        AndroidUtils.toast(context.getString(R.string.name_cant_empty))
                    }
                }
                .setNegativeButton(R.string.scripts_config) { _: DialogInterface, _: Int ->
                    context.startActivity(Intent(context, LauncherActivity::class.java).apply {
                        putExtra("fragment", ScriptsConfigActivity::class.java.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                .create()
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )
        try {
            alertDialog!!.window!!.attributes = layoutParams
            alertDialog!!.show()
        } catch (e: Exception) {
            //
        }
    }

    private fun removeAllWidgets(withSettings: Boolean = false) {
        if (widgets.isEmpty()) {
            return
        }
        if (withSettings) {
            for (pointHolder in widgets) {
                removeViewHolderFromWindow(pointHolder)
            }
            widgets.clear()
            settingsButtons.clear()
        } else {
            val holder = widgets.removeFirst()
            for (pointHolder in widgets.iterator()) {
                removeViewHolderFromWindow(pointHolder)
            }
            widgets.clear()
            widgets.add(holder)
        }
    }

    private fun loadScriptConfig() {
        removeAllWidgets()
        for (widget in scriptsConfig!!.widgets) {
            addButton(widget)
        }
    }

    private fun saveScriptConfig(name: String, synchronousExecution: Boolean, durationTime: Long) {
        val newScript = scriptsConfig == null
        if (newScript) {
            scriptsConfig = ScriptsConfig()
        }
        scriptsConfig!!.name = name
        scriptsConfig!!.synchronousExecution = synchronousExecution
        scriptsConfig!!.durationTime = durationTime
        scriptsConfig!!.widgets = ArrayList()
        for (widget in widgets) {
            if (widget.view is PointView) {
                scriptsConfig!!.widgets.add(
                    Widget(
                        0,
                        widget.count,
                        widget.durationType,
                        widget.duration,
                        widget.longDuration,
                        0,
                        widget.view.number,
                        widget.params.x,
                        widget.params.y,
                        0, 0
                    )
                )
            } else if (widget.view is LineView) {
                scriptsConfig!!.widgets.add(
                    Widget(
                        1,
                        widget.count,
                        widget.durationType,
                        widget.duration,
                        0,
                        widget.swipeDuration,
                        widget.view.view1!!.number,
                        widget.view.params1!!.x,
                        widget.view.params1!!.y,
                        widget.view.params2!!.x,
                        widget.view.params2!!.y,
                    )
                )
            }
        }
        saveScriptsConfig(scriptsConfig!!)
        NotificationCenter.instance().postNotificationName(
            NotificationCenter.scriptsSaved,
            newScript,
            loadScriptsConfig().indexOf(scriptsConfig)
        )
    }

    private fun updateTileAndWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        for (i in appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidget::class.java))) {
            AppWidget.initWidget(appWidgetManager, i)
        }
        TileService.requestListeningState(
            context,
            ComponentName(context, AppQSTileService::class.java)
        )
    }

    fun dismissSettings() {
        synchronized(_object) {
            alertDialog?.apply {
                if (isShowing) {
                    dismiss()
                }
            }
            if (isRunning) {
                startStopWorker()
            }
            removeAllWidgets(true)
            isEnabled = false
            scriptsConfig = null
            NotificationCenter.instance()
                .postNotificationName(NotificationCenter.appServiceToggle, isEnabled)
            updateTileAndWidget()
        }
    }

    private fun removeViewHolderFromWindow(pointHolder: WidgetHolder) {
        if (pointHolder.view is LineView) {
            windowManager?.removeView(pointHolder.view.view1)
            windowManager?.removeView(pointHolder.view.view2)
        }
        windowManager?.removeView(pointHolder.view)
    }

    private fun startStopWorker() {
        val playStop = settingsButtons[0] as PlayPauseView
        NotificationCenter.instance().postNotificationName(NotificationCenter.accessibilityClear)
        if (!isRunning && widgets.size == 1) {
            AndroidUtils.toast(context.getString(R.string.points_is_empty))
            return
        }
        isRunning = !isRunning
        if (isRunning) {
            playStop.setState(PlayPauseView.STATE_PLAY)
        } else {
            playStop.setState(PlayPauseView.STATE_PAUSE)
        }
        for (btn in settingsButtons) {
            if (btn is PlayPauseView) continue
            btn.apply {
                if (isRunning) {
                    alpha = 0.5f
                    isEnabled = false
                } else {
                    alpha = 1f
                    isEnabled = true
                    isFocusable = true
                }
                if (this is ViewGroup) {
                    for (i in 0..<childCount) {
                        getChildAt(i).apply {
                            isEnabled = !isRunning
                            isFocusable = !isRunning
                        }
                    }
                }
            }
        }
        if (isRunning) {
            changeLayoutParamsFlag()
        }
        if (isRunning) {
            runClickSwipe()
        } else {
            if (thread != null) {
                thread!!.cancel()
                thread = null
            }
            changeLayoutParamsFlag()
        }
    }

    private fun changeLayoutParamsFlag() {
        val hidden = isRunning && AppConfig.instance().hiddenWidgetsExecution
        for (i in 1..<widgets.size) {
            widgets[i].apply {
                if (view is PointView) {
                    params.apply {
                        flags = if (isRunning) {
                            flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        } else {
                            flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                        }
                        windowManager?.updateViewLayout(view, this)
                    }
                    if (hidden) {
                        view.visibility = View.INVISIBLE
                    } else {
                        view.visibility = View.VISIBLE
                    }
                } else if (view is LineView) {
                    view.params1?.apply {
                        flags = if (isRunning) {
                            flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        } else {
                            flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                        }
                        view.params2?.flags = flags
                        windowManager?.updateViewLayout(view.view1, this)
                        windowManager?.updateViewLayout(view.view2, view.params2)
                    }
                    if (hidden) {
                        view.visibility = View.INVISIBLE
                        view.view1!!.visibility = View.INVISIBLE
                        view.view2!!.visibility = View.INVISIBLE
                    } else {
                        view.visibility = View.VISIBLE
                        view.view1!!.visibility = View.VISIBLE
                        view.view2!!.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun runClickSwipe() {
        if (thread != null) {
            thread!!.cancel()
            thread = null
        }
        thread = AppThread()
        thread!!.start()
    }

    private fun addButton(widget: Widget) {
        val prams: WindowManager.LayoutParams = WindowManager.LayoutParams(
            size,
            size,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        prams.gravity = Gravity.TOP or Gravity.LEFT
        prams.x = widget.x1
        prams.y = widget.y1
        val pointView = PointView(context)
        pointView.number = widget.number
        val durationType = widget.durationType
        val duration = widget.duration
        pointView.pointViewMoveListener = this
        pointView.setOnClickListener {
            openWidgetDialog(it)
        }
        pointView.setOnLongClickListener {
            var index = -1
            for (i in 0..<widgets.size) {
                if (widgets[i].view == it) {
                    index = i
                    break
                } else if (widgets[i].view is LineView) {
                    if ((widgets[i].view as LineView).view1 == it) {
                        index = i
                        break
                    }
                }
            }
            if (index > 0) {
                removeViewHolderFromWindow(widgets[index])
                widgets.removeAt(index)
                for (i in index..<widgets.size) {
                    widgets[i].apply {
                        if (widgets[i].view is LineView) {
                            (widgets[i].view as LineView).view1!!.apply {
                                number -= 1
                                invalidate()
                            }
                        } else if (widgets[i].view is PointView) {
                            (widgets[i].view as PointView).apply {
                                number -= 1
                                invalidate()
                            }
                        }
                    }
                }
            }
            true
        }
        if (widget.type == 0) {
            widgets.add(
                WidgetHolder(
                    pointView,
                    prams,
                    duration,
                    durationType,
                    widget.longDuration,
                    0,
                    widget.count,
                )
            )
            windowManager!!.addView(pointView, prams)
        } else {
            pointView.isEnd = false
            val prams0: WindowManager.LayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            prams0.gravity = Gravity.TOP or Gravity.LEFT
            prams0.x = 0
            prams0.y = 0
            val pointView2 = PointView(context)
            pointView2.isEnd = true
            pointView2.number = widget.number
            pointView2.pointViewMoveListener = this
            pointView2.setOnClickListener {
                openWidgetDialog(pointView)
            }
            val prams2: WindowManager.LayoutParams = WindowManager.LayoutParams(
                size,
                size,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            prams2.gravity = Gravity.TOP or Gravity.LEFT
            prams2.x = widget.x2
            prams2.y = widget.y2

            val lineView = LineView(context)
            lineView.onAddView(
                pointView, prams,
                pointView2, prams2,
            )
            widgets.add(
                WidgetHolder(
                    lineView,
                    prams0,
                    duration,
                    durationType,
                    0,
                    widget.swipeDuration,
                    widget.count,
                )
            )
            windowManager!!.addView(pointView, prams)
            windowManager!!.addView(pointView2, prams2)
            windowManager!!.addView(lineView, prams0)
        }
    }

    private fun openWidgetDialog(view: View) {
        var widgetHolder: WidgetHolder? = null
        var durationType = 0
        var swipe = false
        for (holder in widgets) {
            if (holder.view == view) {
                widgetHolder = holder
                durationType = holder.durationType
                break
            } else if (holder.view is LineView) {
                if (holder.view.view1 == view) {
                    swipe = true
                    widgetHolder = holder
                    durationType = holder.durationType
                    break
                }
            }
        }
        if (widgetHolder == null) return
        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.addView(
            TextView(context).apply {
                setText(R.string.click_time)
                textSize = 16f
                setTextColor(Color.BLACK)
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(4))
            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                15f, 5f, 15f, 10f
            )
        )
        val etClickTime = EditText(context)
        etClickTime.hint = minimumTimeClick.toString()
        etClickTime.value = when (durationType) {
            0 -> widgetHolder.duration
            1 -> widgetHolder.duration / 1000
            else -> widgetHolder.duration / (1000 * 60)
        }.toString()
        etClickTime.inputType = InputType.TYPE_CLASS_NUMBER
        etClickTime.filters = arrayOf(InputFilter.LengthFilter(4))
        root.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    Spinner(context).apply {
                        adapter = ArrayAdapter(
                            context,
                            android.R.layout.simple_dropdown_item_1line,
                            arrayOf("میلی ثانیه", "ثانیه", "دقیقه")
                        )
                        setSelection(durationType, false)
                        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                durationType = position
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {

                            }
                        }
                    },
                    LayoutHelper.createLinear(
                        LayoutHelper.WRAP_CONTENT,
                        LayoutHelper.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL
                    )
                )
                addView(
                    etClickTime,
                    LayoutHelper.createLinear(
                        0f,
                        LayoutHelper.WRAP_CONTENT,
                        1f,
                        Gravity.CENTER_VERTICAL
                    )
                )
            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                15f, 0f, 15f, 0f
            )
        )

        root.addView(
            TextView(context).apply {
                setText(if (swipe) R.string.swipe_time else R.string.long_click_time)
                textSize = 16f
                setTextColor(Color.BLACK)
            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                15f, 5f, 15f, 5f
            )
        )

        val etTime = EditText(context)
        etTime.hint = if (swipe) minimumSwipeClick.toString() else "1"
        etTime.value =
            (if (swipe) widgetHolder.swipeDuration else widgetHolder.longDuration).toString()
        etTime.inputType = InputType.TYPE_CLASS_NUMBER
        etTime.filters = arrayOf(InputFilter.LengthFilter(4))
        root.addView(
            etTime,
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                15f,
                5f,
                15f,
                10f
            )
        )

        root.addView(
            TextView(context).apply {
                setText(R.string.repeat_count)
                textSize = 16f
                setTextColor(Color.BLACK)
            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                15f, 5f, 15f, 5f
            )
        )
        val etCount = EditText(context)
        etCount.hint = "0 (${context.resources.getString(R.string.infinity)})"
        etCount.value = widgetHolder.count.toString()
        etCount.inputType = InputType.TYPE_CLASS_NUMBER
        etCount.filters = arrayOf(InputFilter.LengthFilter(4))
        root.addView(
            etCount,
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                15f,
                5f,
                15f,
                20f
            )
        )

        alertDialog =
            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle(R.string.app_name)
                .setView(root)
                .setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
                    val minimumTimeClick = if (durationType == 0) minimumTimeClick else 1
                    val clickTime: Long =
                        if (etClickTime.value.isEmpty() || etClickTime.value.toLong() < minimumTimeClick) minimumTimeClick.toLong() else etClickTime.value.toLong()
                    widgetHolder.durationType = durationType
                    widgetHolder.duration =
                        when (durationType) {
                            0 -> clickTime
                            1 -> clickTime * 1000L
                            else -> clickTime * 1000L * 60
                        }
                    if (swipe) {
                        widgetHolder.swipeDuration =
                            if (etTime.value.isEmpty() || etTime.value.toLong() < minimumSwipeClick) minimumSwipeClick else etTime.value.toInt()
                    } else {
                        widgetHolder.longDuration =
                            if (etTime.value.isEmpty() || etTime.value.toLong() < 1) 1 else etTime.value.toInt()
                    }
                    widgetHolder.count =
                        if (etCount.value.isEmpty() || etCount.value.toLong() < 0) 0 else etCount.value.toInt()
                }.create()
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )
        try {
            alertDialog!!.window!!.attributes = layoutParams
            alertDialog!!.show()
        } catch (e: Exception) {
            //
        }
    }

    private fun notifyScreenOff() {
        try {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
            context.registerReceiver(screenOffBroadcast, intentFilter)
        } catch (e: Exception) {
            //
        }
    }

    private fun removeNotifyScreenOff() {
        try {
            context.unregisterReceiver(screenOffBroadcast)
        } catch (e: Exception) {
            //
        }
    }

    private val isLowMemory: Boolean
        get() = try {
            val systemService =
                context.getSystemService(AccessibilityService.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            systemService.getMemoryInfo(memoryInfo)
            memoryInfo.lowMemory
        } catch (e: Exception) {
            false
        }

    override fun findLayoutParams(view: View): WindowManager.LayoutParams? {
        for (holder in widgets) {
            if (holder.view == view) {
                return holder.params
            } else if (holder.view is LineView) {
                if (holder.view.view1 == view) {
                    holder.view.invalidate()
                    return holder.view.params1
                } else if (holder.view.view2 == view) {
                    holder.view.invalidate()
                    return holder.view.params2
                }
            }
        }
        return null
    }

    override fun onMove(
        view: View, x: Int, y: Int, z: Int, screenWidth: Int, screenHeight: Int
    ) {
        val params = findLayoutParams(view) ?: return
        //                var x = x
//                var y = y
//                if (x < z) {
//                    x = z
//                } else if (x - screenWidth > z) {
//                    x = screenWidth + z
//                }
//                if (y < z) {
//                    y = z
//                } else if (y - screenHeight > z) {
//                    y = screenHeight + z
//                }
        params.x = x
        params.y = y
        windowManager?.updateViewLayout(view, params)
    }

    fun onDestroy() {
        dismissSettings()
        removeNotifyScreenOff()
        windowManager = null
    }

    private data class WidgetHolder(
        val view: View,
        val params: WindowManager.LayoutParams,
        var duration: Long = 0,
        var durationType: Int = 0,
        var longDuration: Int = 0,
        var swipeDuration: Int = 0,
        var count: Int = 0,
        var timer: Long = 0,
        var counter: Int = 0,
    )

    data class ScriptsConfig(
        var name: String = "",
        var synchronousExecution: Boolean = false,
        var durationTime: Long = 0,
        var widgets: ArrayList<Widget> = ArrayList(),
    )

    data class Widget(
        val type: Int,
        val count: Int,
        val durationType: Int,
        val duration: Long,
        val longDuration: Int,
        val swipeDuration: Int,
        val number: Int,
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int,
    )

    inner class AppThread : Thread() {
        private var widgetIndex = 1
        private var liveStatus = true
        private val synchronousExecution =
            scriptsConfig?.synchronousExecution ?: AppConfig.instance().synchronousExecution
        private val durationTime =
            (scriptsConfig?.durationTime ?: AppConfig.instance().durationTime) * 1000
        private var realTime = 0L
        private var widgetActiveCount = widgets.size -1
        private lateinit var handler : Handler

        override fun run() {
            name = "AppServiceThread"
            for (i in 1..<widgets.size) {
                widgets[i].timer = 0
                widgets[i].counter = 0
            }
            sleep(110)
            Looper.prepare()
            handler = Handler(Looper.myLooper()!!)
            if (synchronousExecution) {
                synchronousExecution()
            } else {
                doWhile()
            }
            Looper.loop()
        }

        private fun synchronousExecution() {
            if (isLive()) {
                for (index in 1..<widgets.size) {
                    val viewHolder = widgets[index]
                    if (viewHolder.timer < 1) {
                        if (runWidget(viewHolder)) {
                            viewHolder.timer =
                                viewHolder.duration + viewHolder.swipeDuration + viewHolder.longDuration
                        }
                        continue
                    }
                    viewHolder.timer -= minimumTimeClick
                }
                duration(minimumTimeClick.toLong())
            }
            handler.post {
                synchronousExecution()
            }
        }

        private fun doWhile() {
            handler.post {
                widgets[widgetIndex].apply {
                    if (runWidget(this)) {
                        next(duration + swipeDuration + longDuration)
                    } else {
                        next(0)
                    }
                }
            }
        }

        private fun runWidget(widgetHolder: WidgetHolder): Boolean {
            if (widgetHolder.counter == -1) return false
            if (widgetHolder.count != 0) {
                if (widgetHolder.counter >= widgetHolder.count) {
                    widgetActiveCount--
                    widgetHolder.counter = -1
                    if (widgetActiveCount == 0) {
                        cancel()
                    }
                    return false
                }
                widgetHolder.counter++
            }
            widgetHolder.view.apply {
                if (this is PointView) {
                    post {
                        val pos = IntArray(2)
                        getLocationOnScreen(pos)
                        NotificationCenter.instance().postNotificationNameInterval(
                            NotificationCenter.accessibilityClick,
                            pos[0] + width / 2,
                            pos[1] + height / 2,
                            widgetHolder.longDuration.toLong()
                        )
                    }
                } else if (this is LineView) {
                    post {
                        NotificationCenter.instance().postNotificationNameInterval(
                            NotificationCenter.accessibilitySwipe,
                            xFromRequest, yFromRequest,
                            xToRequest, yToRequest,
                            widgetHolder.swipeDuration.toLong()
                        )
                    }
                }
            }
            return true
        }

        private fun next(duration: Long) {
            if (isLive() && widgets.size > 1) {
                widgetIndex++
                if (widgetIndex >= widgets.size) {
                    widgetIndex = 1
                }
                if (duration == 0L) {
                    doWhile()
                } else {
                    duration(duration)
                    if (isLive()) {
                        doWhile()
                    }
                }
            }
        }

        private fun duration(duration: Long) {
            if (durationTime > 0) {
                if (durationTime > realTime) {
                    realTime += duration
                } else {
                    cancel()
                    return
                }
            }
            try {
                Thread.sleep(duration)
            } catch (e: Exception) {
                cancel()
            }
        }

        private fun isLive(): Boolean {
            synchronized(_object) {
                if (!liveStatus || !isRunning) {
                    cancel()
                    return false
                }
                return true
            }
        }

        fun cancel() {
            if (!liveStatus) return
            handler.looper.quit()
            liveStatus = false
            if (isRunning) {
                AndroidUtils.runOnUIThread({ startStopWorker() })
            }
        }
    }
}