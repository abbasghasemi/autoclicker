package ghasemi.abbas.autoclicker

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import androidx.appcompat.app.AlertDialog
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.res.ColorStateList
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
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
import android.view.ContextThemeWrapper
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Consumer
import androidx.appcompat.widget.AppCompatCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import ghasemi.abbas.autoclicker.stream.SerializedData
import ghasemi.abbas.autoclicker.ui.LauncherActivity
import ghasemi.abbas.autoclicker.ui.AppDialogUi
import ghasemi.abbas.autoclicker.ui.components.DurationTimeView
import ghasemi.abbas.autoclicker.ui.components.AppChoiceButton
import ghasemi.abbas.autoclicker.ui.components.ExpendedView
import ghasemi.abbas.autoclicker.ui.components.LineView
import ghasemi.abbas.autoclicker.ui.components.MoveHelper
import ghasemi.abbas.autoclicker.ui.components.PlayPauseView
import ghasemi.abbas.autoclicker.ui.components.ScriptTargetSelector
import ghasemi.abbas.autoclicker.ui.components.PointView
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.AnimationUtils
import ghasemi.abbas.autoclicker.utils.LayoutHelper
import ghasemi.abbas.autoclicker.utils.rippleBackground
import ghasemi.abbas.autoclicker.utils.value
import java.security.SecureRandom

class AppServiceHelper(val context: Service, val type: Int) : MoveHelper.PointViewMoveListener {
    companion object {
        const val ON_LEAVE_NONE = 0
        const val ON_LEAVE_PAUSE = 1
        const val ON_LEAVE_STOP = 2
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
            if (version in 0..5) {
                val count1 = serializedData.readInt32(false)
                for (i in 0..<count1) {
                    val name = serializedData.readString(false)!!
                    val synchronousExecution = serializedData.readBool(false)
                    val durationTime = serializedData.readInt64(false)
                    val relativeCoordinates = version >= 1 && serializedData.readBool(false)
                    val referenceWidth = if (version >= 1) serializedData.readInt32(false) else 0
                    val referenceHeight = if (version >= 1) serializedData.readInt32(false) else 0
                    val widgets = ArrayList<Widget>()
                    val count2 = serializedData.readInt32(false)
                    for (j in 0..<count2) {
                        val viewType = serializedData.readByte(false).toInt()
                        if (viewType == 0 || viewType == 1) {
                            val count = serializedData.readInt32(false)
                            val durationType = serializedData.readInt32(false)
                            val duration = serializedData.readInt64(false)
                            val gestureDuration = serializedData.readInt32(false)
                            val number = serializedData.readInt32(false)
                            val x1 = serializedData.readInt32(false)
                            val y1 = serializedData.readInt32(false)
                            val x2 = if (viewType == 1) serializedData.readInt32(false) else 0
                            val y2 = if (viewType == 1) serializedData.readInt32(false) else 0
                            val enabled = version < 2 || serializedData.readBool(false)
                            val waitText = if (version >= 3) serializedData.readString(false).orEmpty() else ""
                            val waitTimeoutSeconds = if (version >= 3) serializedData.readInt32(false) else 0
                            val gestureMode = if (version >= 4) serializedData.readInt32(false) else 0
                            val curvePercent = if (version >= 4) serializedData.readInt32(false) else 0
                            val waypoints = if (version >= 5) serializedData.readString(false).orEmpty() else ""
                            widgets.add(Widget(viewType, count, durationType, duration,
                                if (viewType == 0) gestureDuration else 0,
                                if (viewType == 1) gestureDuration else 0,
                                number, x1, y1, x2, y2, enabled, waitText, waitTimeoutSeconds,
                                gestureMode, curvePercent, waypoints))
                        }
                    }
                    scriptsConfig!!.add(
                        ScriptsConfig(
                            name,
                            synchronousExecution,
                            durationTime,
                            widgets,
                            relativeCoordinates,
                            referenceWidth,
                            referenceHeight
                        )
                    )
                }
            } else {
                AppConfig.instance().scriptsConfigList = ""
            }
            // Version 5 records stay intact. Older releases ignore this optional trailer.
            ScriptOptionsTrailer.read(serializedData, scriptsConfig!!.size)?.forEachIndexed { index, options ->
                scriptsConfig!![index].targetPackage = options.targetPackage
                scriptsConfig!![index].onLeaveAction = options.onLeaveAction
            }
            serializedData.cleanup()
            return scriptsConfig!!
        }

        fun saveScriptsConfig(scriptsConfig: ScriptsConfig, delete: Boolean = false) {
            val saved = loadScriptsConfig()
            val existingIndex = saved.indexOfFirst { it === scriptsConfig }
            if (delete) {
                if (existingIndex >= 0) saved.removeAt(existingIndex)
            }
            if (existingIndex < 0 && !delete) {
                saved.add(scriptsConfig)
            }
            val serializedData = SerializedData()
            serializedData.writeInt32(5)
            serializedData.writeInt32(loadScriptsConfig().size)
            for (script in loadScriptsConfig()) {
                serializedData.writeString(script.name)
                serializedData.writeBool(script.synchronousExecution)
                serializedData.writeInt64(script.durationTime)
                serializedData.writeBool(script.relativeCoordinates)
                serializedData.writeInt32(script.referenceWidth)
                serializedData.writeInt32(script.referenceHeight)
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
                    serializedData.writeBool(widget.enabled)
                    serializedData.writeString(widget.waitText)
                    serializedData.writeInt32(widget.waitTimeoutSeconds)
                    serializedData.writeInt32(widget.gestureMode)
                    serializedData.writeInt32(widget.curvePercent)
                    serializedData.writeString(widget.waypoints)
                }
            }
            ScriptOptionsTrailer.write(serializedData, loadScriptsConfig().map {
                ScriptOptionsTrailer.Options(it.targetPackage, it.onLeaveAction)
            })
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    private var countdownLabel: TextView? = null
    private var emergencyStopButton: ImageView? = null
    private val _object = Object()
    private val random = SecureRandom()
    private val size = AndroidUtils.dp(40f)

    init {
        windowManager =
            context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
        notifyScreenOff()
    }

    fun showSettings(scriptsConfig: ScriptsConfig? = null) {
        var loadConfig = false
        if (scriptsConfig != null) {
            this.scriptsConfig = scriptsConfig
            loadConfig = true
        }
        if (isEnabled) {
            if (loadConfig && !isRunning)
                loadScriptConfig()
            return
        }
        isEnabled = true
        NotificationCenter.instance()
            .postNotificationName(NotificationCenter.appServiceToggle, isEnabled)
        AndroidUtils.updateTileAndWidget()
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

        emergencyStopButton = ImageView(context).apply {
            setImageResource(R.drawable.round_exit_to_app_24)
            contentDescription = context.getString(R.string.stop_now)
            setColorFilter(Color.RED)
            background = rippleBackground()
            visibility = View.GONE
            setOnClickListener { if (isRunning || countdownRunnable != null) startStopWorker() }
        }
        linearLayout.addView(emergencyStopButton, LayoutHelper.createLinear(32f, 32f))
        countdownLabel = TextView(context).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        linearLayout.addView(countdownLabel, LayoutHelper.createLinear(32f, 32f))

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
                    params.topMargin = params.marginStart
                    params.marginStart = tm
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
        val context = ContextThemeWrapper(this.context, R.style.AppAlertDialog)
        val etName = TextInputEditText(context)
        val durationTimeView = DurationTimeView(context)
        val targetSelector = ScriptTargetSelector(context, scriptsConfig?.targetPackage.orEmpty(),
            scriptsConfig?.onLeaveAction ?: ON_LEAVE_NONE, type)
        var durationTime: Long = scriptsConfig?.durationTime
            ?: AppConfig.instance().durationTime
        val cbSynchronous = AppCompatCheckBox(context).apply {
            typeface = ResourcesCompat.getFont(context, R.font.sans)
            useServiceCheckColors()
        }
        val cbRelative = AppCompatCheckBox(context).apply {
            setText(R.string.relative_coordinates)
            isChecked = scriptsConfig?.relativeCoordinates ?: false
            typeface = ResourcesCompat.getFont(context, R.font.sans)
            useServiceCheckColors()
        }
        alertDialog =
            AlertDialog.Builder(context, R.style.AppAlertDialog)
                .setTitle(R.string.app_name)
                .setView(ScrollView(context).apply { addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    durationTimeView.consumer = Consumer {
                        durationTime = it
                    }
                    durationTimeView.updateDurationTime(durationTime)
                    addView(
                        TextInputLayout(context).apply {
                            useServiceFieldColors()
                            setHint(R.string.script_name)
                            addView(etName.apply {
                                value = scriptsConfig?.name ?: "Script ${loadScriptsConfig().size + 1}"
                                filters = arrayOf(InputFilter.LengthFilter(32))
                                typeface = ResourcesCompat.getFont(context, R.font.sans)
                            })
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
                        targetSelector,
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                            15f, 8f, 15f, 0f)
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
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40f, 15f, 0f, 15f, 0f)
                    )
                    addView(cbRelative, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40f, 15f, 0f, 15f, 0f))
                }) })
                .setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
                    if (etName.text.toString().isNotEmpty()) {
                        saveScriptConfig(
                            etName.text.toString(),
                            cbSynchronous.isChecked,
                            durationTime,
                            cbRelative.isChecked,
                            targetSelector.targetPackage,
                            targetSelector.onLeaveAction
                        )
                    } else {
                        AndroidUtils.toast(context.getString(R.string.name_cant_empty))
                    }
                }
                .setNegativeButton(R.string.scripts_config) { _: DialogInterface, _: Int ->
                    context.startActivity(Intent(context, LauncherActivity::class.java).apply {
                        putExtra("fragment", "ScriptsConfig")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                .create()
        try {
            AppDialogUi.show(alertDialog!!, type)
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
        val (screenWidth, screenHeight) = ScreenSize.of(context)
        val scaleX = if (scriptsConfig!!.relativeCoordinates && scriptsConfig!!.referenceWidth > 0)
            screenWidth.toFloat() / scriptsConfig!!.referenceWidth else 1f
        val scaleY = if (scriptsConfig!!.relativeCoordinates && scriptsConfig!!.referenceHeight > 0)
            screenHeight.toFloat() / scriptsConfig!!.referenceHeight else 1f
        for (widget in scriptsConfig!!.widgets) {
            addButton(widget.copy(
                x1 = (widget.x1 * scaleX).toInt(), y1 = (widget.y1 * scaleY).toInt(),
                x2 = (widget.x2 * scaleX).toInt(), y2 = (widget.y2 * scaleY).toInt()
            ))
        }
    }

    private fun saveScriptConfig(name: String, synchronousExecution: Boolean, durationTime: Long,
        relativeCoordinates: Boolean, targetPackage: String, onLeaveAction: Int) {
        val newScript = scriptsConfig == null
        if (newScript) {
            scriptsConfig = ScriptsConfig()
        }
        scriptsConfig!!.name = name
        scriptsConfig!!.synchronousExecution = synchronousExecution
        scriptsConfig!!.durationTime = durationTime
        scriptsConfig!!.relativeCoordinates = relativeCoordinates
        scriptsConfig!!.targetPackage = targetPackage
        scriptsConfig!!.onLeaveAction = onLeaveAction
        val (screenWidth, screenHeight) = ScreenSize.of(context)
        scriptsConfig!!.referenceWidth = screenWidth
        scriptsConfig!!.referenceHeight = screenHeight
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
                        0, 0, widget.enabled, widget.waitText, widget.waitTimeoutSeconds,
                        widget.gestureMode, widget.curvePercent, widget.waypoints
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
                        widget.enabled, widget.waitText, widget.waitTimeoutSeconds,
                        widget.gestureMode, widget.curvePercent, widget.waypoints
                    )
                )
            }
        }
        saveScriptsConfig(scriptsConfig!!)
        NotificationCenter.instance().postNotificationName(
            NotificationCenter.scriptsSaved,
            newScript,
            loadScriptsConfig().indexOfFirst { it === scriptsConfig }
        )
    }

    fun dismissSettings() {
        synchronized(_object) {
            cancelCountdown()
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
            emergencyStopButton = null
            countdownLabel = null
            NotificationCenter.instance()
                .postNotificationName(NotificationCenter.appServiceToggle, isEnabled)
            AndroidUtils.updateTileAndWidget()
        }
    }

    private fun removeViewHolderFromWindow(pointHolder: WidgetHolder) {
        if (pointHolder.view is LineView) {
            windowManager?.removeView(pointHolder.view.view1)
            windowManager?.removeView(pointHolder.view.view2)
        }
        windowManager?.removeView(pointHolder.view)
    }

    private fun cancelCountdown() {
        countdownRunnable?.let(mainHandler::removeCallbacks)
        countdownRunnable = null
        countdownLabel?.visibility = View.GONE
        emergencyStopButton?.visibility = if (isRunning) View.VISIBLE else View.GONE
    }

    private fun startStopWorker(skipCountdown: Boolean = false) {
        if (countdownRunnable != null) {
            cancelCountdown()
            return
        }
        if (!isRunning && !skipCountdown && AppConfig.instance().startDelaySeconds > 0) {
            if (widgets.drop(1).none { it.enabled }) {
                AndroidUtils.toast(context.getString(R.string.points_is_empty))
                return
            }
            var seconds = AppConfig.instance().startDelaySeconds
            val tick = object : Runnable {
                override fun run() {
                    if (!isEnabled) { cancelCountdown(); return }
                    if (seconds == 0) {
                        countdownRunnable = null
                        countdownLabel?.visibility = View.GONE
                        startStopWorker(skipCountdown = true)
                    } else {
                        countdownLabel?.text = seconds.toString()
                        countdownLabel?.visibility = View.VISIBLE
                        emergencyStopButton?.visibility = View.VISIBLE
                        seconds--
                        mainHandler.postDelayed(this, 1000)
                    }
                }
            }
            countdownRunnable = tick
            tick.run()
            return
        }
        val playStop = settingsButtons[0] as PlayPauseView
        NotificationCenter.instance().postNotificationName(NotificationCenter.accessibilityClear)
        if (!isRunning && widgets.drop(1).none { it.enabled }) {
            AndroidUtils.toast(context.getString(R.string.points_is_empty))
            return
        }
        isRunning = !isRunning
        emergencyStopButton?.visibility = if (isRunning) View.VISIBLE else View.GONE
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
        pointView.alpha = if (widget.enabled) 1f else 0.4f
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
                    enabled = widget.enabled,
                    waitText = widget.waitText,
                    waitTimeoutSeconds = widget.waitTimeoutSeconds,
                    gestureMode = widget.gestureMode,
                    curvePercent = widget.curvePercent,
                    waypoints = widget.waypoints,
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
            pointView2.alpha = if (widget.enabled) 1f else 0.4f
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
            lineView.gestureMode = widget.gestureMode
            lineView.curvePercent = widget.curvePercent
            lineView.waypoints = widget.waypoints
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
                    enabled = widget.enabled,
                    waitText = widget.waitText,
                    waitTimeoutSeconds = widget.waitTimeoutSeconds,
                    gestureMode = widget.gestureMode,
                    curvePercent = widget.curvePercent,
                    waypoints = widget.waypoints,
                )
            )
            windowManager!!.addView(pointView, prams)
            windowManager!!.addView(pointView2, prams2)
            windowManager!!.addView(lineView, prams0)
        }
    }

    private fun openWidgetDialog(view: View) {
        val context = ContextThemeWrapper(this.context, R.style.AppAlertDialog)
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
        val etClickTime = TextInputEditText(context)
        etClickTime.backgroundTintList = ColorStateList.valueOf(0xff0a9a58.toInt())
        etClickTime.value = when (durationType) {
            0 -> widgetHolder.duration
            1 -> widgetHolder.duration / 1000
            else -> widgetHolder.duration / (1000 * 60)
        }.toString()
        etClickTime.inputType = InputType.TYPE_CLASS_NUMBER
        etClickTime.filters = arrayOf(InputFilter.LengthFilter(4))
        val timeUnitNames = arrayOf("میلی ثانیه", "ثانیه", "دقیقه")
        root.addView(TextInputLayout(context).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setHint(R.string.click_time)
            defaultHintTextColor = ColorStateList.valueOf(0xff326b4d.toInt())
            hintTextColor = ColorStateList.valueOf(0xff087344.toInt())
            suffixText = timeUnitNames[durationType]
            suffixTextView.apply {
                setTextColor(0xff087344.toInt())
                textSize = 12f
                background = RippleDrawable(ColorStateList.valueOf(0x220a9a58), null,
                    GradientDrawable().apply {
                        setColor(Color.WHITE)
                        cornerRadius = AndroidUtils.dp(6f).toFloat()
                    })
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.round_arrow_drop_down_24, 0, 0, 0)
                isClickable = true
                setOnClickListener {
                    AppChoiceButton.showMenu(this, timeUnitNames, type) { index ->
                        durationType = index
                        suffixText = timeUnitNames[index]
                    }
                }
            }
            addView(etClickTime)
        }, LayoutHelper.createLinear(
            LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
            15f, 8f, 15f, 0f
        ))
        val etTime = TextInputEditText(context)
        etTime.value =
            (if (swipe) widgetHolder.swipeDuration else widgetHolder.longDuration).toString()
        etTime.inputType = InputType.TYPE_CLASS_NUMBER
        etTime.filters = arrayOf(InputFilter.LengthFilter(4))
        root.addView(
            TextInputLayout(context).apply {
                useServiceFieldColors()
                setHint(if (swipe) R.string.swipe_time else R.string.long_click_time)
                placeholderText = if (swipe) minimumSwipeClick.toString() else "1"
                addView(etTime)
            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                15f,
                5f,
                15f,
                10f
            )
        )

        val etCount = TextInputEditText(context)
        etCount.value = widgetHolder.count.toString()
        etCount.inputType = InputType.TYPE_CLASS_NUMBER
        etCount.filters = arrayOf(InputFilter.LengthFilter(4))
        root.addView(
            TextInputLayout(context).apply {
                useServiceFieldColors()
                setHint(R.string.repeat_count)
                placeholderText = "0 (${context.resources.getString(R.string.infinity)})"
                addView(etCount)
            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                15f,
                5f,
                15f,
                20f
            )
        )
        val enabledCheck = AppCompatCheckBox(context).apply {
            setText(R.string.step_enabled)
            isChecked = widgetHolder.enabled
            useServiceCheckColors()
        }
        root.addView(enabledCheck,
            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40f, 15f, 0f, 15f, 10f))
        val etWaitText = TextInputEditText(context).apply {
            setText(widgetHolder.waitText)
            isSingleLine = true
        }
        root.addView(TextInputLayout(context).apply {
            useServiceFieldColors()
            setHint(R.string.wait_for_text)
            addView(etWaitText)
        }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
            LayoutHelper.WRAP_CONTENT, 15f, 0f, 15f, 10f))
        val etWaitTimeout = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(widgetHolder.waitTimeoutSeconds.toString())
        }
        root.addView(TextInputLayout(context).apply {
            useServiceFieldColors()
            setHint(R.string.wait_timeout)
            addView(etWaitTimeout)
        }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
            LayoutHelper.WRAP_CONTENT, 15f, 0f, 15f, 10f))
        var gestureMode = widgetHolder.gestureMode
        val curveSeek = SeekBar(context).apply {
            max = 200
            progress = widgetHolder.curvePercent + 100
            progressTintList = ColorStateList.valueOf(0xff0a9a58.toInt())
            thumbTintList = ColorStateList.valueOf(0xff0a9a58.toInt())
            visibility = if (gestureMode == 1) View.VISIBLE else View.GONE
        }
        val etWaypoints = TextInputEditText(context).apply {
            setText(widgetHolder.waypoints)
        }
        val waypointsLayout = TextInputLayout(context).apply {
            useServiceFieldColors()
            setHint(R.string.waypoints_hint)
            addView(etWaypoints)
            visibility = if (gestureMode == 4) View.VISIBLE else View.GONE
        }
        if (swipe) {
            root.addView(AppChoiceButton(context,
                context.resources.getStringArray(R.array.gesture_modes), gestureMode,
                context.getString(R.string.gesture_mode), type) { position ->
                gestureMode = position
                curveSeek.visibility = if (position == 1) View.VISIBLE else View.GONE
                waypointsLayout.visibility = if (position == 4) View.VISIBLE else View.GONE
            }, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48f, 15f, 0f, 15f, 0f))
            root.addView(curveSeek, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48f, 15f, 0f, 15f, 10f))
            root.addView(waypointsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 15f, 0f, 15f, 10f))
        }

        alertDialog =
            AlertDialog.Builder(context, R.style.AppAlertDialog)
                .setTitle(R.string.app_name)
                .setView(ScrollView(context).apply { addView(root) })
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
                    widgetHolder.enabled = enabledCheck.isChecked
                    widgetHolder.waitText = etWaitText.text.toString().trim()
                    widgetHolder.waitTimeoutSeconds = etWaitTimeout.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
                    widgetHolder.gestureMode = gestureMode
                    widgetHolder.curvePercent = curveSeek.progress - 100
                    widgetHolder.waypoints = etWaypoints.text.toString().trim()
                    if (widgetHolder.view is LineView) {
                        val line = widgetHolder.view as LineView
                        line.gestureMode = gestureMode
                        line.curvePercent = widgetHolder.curvePercent
                        line.waypoints = widgetHolder.waypoints
                        line.invalidate()
                        line.view1?.alpha = if (widgetHolder.enabled) 1f else 0.4f
                        line.view2?.alpha = if (widgetHolder.enabled) 1f else 0.4f
                    } else {
                        widgetHolder.view.alpha = if (widgetHolder.enabled) 1f else 0.4f
                    }
                }.create()
        try {
            AppDialogUi.show(alertDialog!!, type)
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
        var enabled: Boolean = true,
        var waitText: String = "",
        var waitTimeoutSeconds: Int = 0,
        var waitingSince: Long = 0,
        var gestureMode: Int = 0,
        var curvePercent: Int = 0,
        var waypoints: String = "",
    )

    data class ScriptsConfig(
        var name: String = "",
        var synchronousExecution: Boolean = false,
        var durationTime: Long = 0,
        var widgets: ArrayList<Widget> = ArrayList(),
        var relativeCoordinates: Boolean = false,
        var referenceWidth: Int = 0,
        var referenceHeight: Int = 0,
        var targetPackage: String = "",
        var onLeaveAction: Int = ON_LEAVE_NONE,
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
        val enabled: Boolean = true,
        val waitText: String = "",
        val waitTimeoutSeconds: Int = 0,
        val gestureMode: Int = 0,
        val curvePercent: Int = 0,
        val waypoints: String = "",
    )

    inner class AppThread : Thread() {
        private var widgetIndex = 1
        private var liveStatus = true
        private val synchronousExecution =
            scriptsConfig?.synchronousExecution ?: AppConfig.instance().synchronousExecution
        private val durationTime =
            (scriptsConfig?.durationTime ?: AppConfig.instance().durationTime) * 1000
        private var realTime = 0L
        private var widgetActiveCount = widgets.drop(1).count { it.enabled }
        private lateinit var handler : Handler

        override fun run() {
            name = "AppServiceThread"
            for (i in 1..<widgets.size) {
                widgets[i].timer = 0
                widgets[i].counter = 0
                widgets[i].waitingSince = 0
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
                        when (runWidget(viewHolder)) {
                            true -> viewHolder.timer =
                                viewHolder.duration + viewHolder.swipeDuration + viewHolder.longDuration
                            null -> viewHolder.timer = 250
                            false -> Unit
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
                    when (runWidget(this)) {
                        true -> next(duration + swipeDuration + longDuration)
                        false -> next(0)
                        null -> {
                            duration(250)
                            if (isLive()) doWhile()
                        }
                    }
                }
            }
        }

        private fun runWidget(widgetHolder: WidgetHolder): Boolean? {
            if (!liveStatus || !isRunning || !waitForTargetApp()) return false
            if (!widgetHolder.enabled) return false
            if (widgetHolder.counter == -1) return false
            if (widgetHolder.waitText.isNotBlank()) {
                if (!AppAccessibilityService.hasVisibleText(widgetHolder.waitText)) {
                    if (widgetHolder.waitingSince == 0L)
                        widgetHolder.waitingSince = System.currentTimeMillis()
                    val timeout = widgetHolder.waitTimeoutSeconds * 1000L
                    if (timeout == 0L || System.currentTimeMillis() - widgetHolder.waitingSince < timeout)
                        return null
                    widgetHolder.counter = -1
                    widgetActiveCount--
                    if (widgetActiveCount == 0) cancel()
                    return false
                }
                widgetHolder.waitingSince = 0
            }
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
                            widgetHolder.swipeDuration.toLong(),
                            widgetHolder.gestureMode, widgetHolder.curvePercent,
                            widgetHolder.waypoints
                        )
                    }
                }
            }
            return true
        }

        private fun waitForTargetApp(): Boolean {
            val target = scriptsConfig?.targetPackage.orEmpty()
            val action = scriptsConfig?.onLeaveAction ?: ON_LEAVE_NONE
            if (target.isBlank() || action == ON_LEAVE_NONE) return true
            val pausedAt = System.currentTimeMillis()
            var paused = false
            while (liveStatus && isRunning &&
                AppAccessibilityService.foregroundPackage != target) {
                paused = true
                if (action == ON_LEAVE_STOP) {
                    cancel()
                    return false
                }
                try {
                    sleep(200)
                } catch (_: InterruptedException) {
                    return false
                }
            }
            if (paused && liveStatus && isRunning) {
                val pauseDuration = System.currentTimeMillis() - pausedAt
                widgets.drop(1).forEach { if (it.waitingSince > 0) it.waitingSince += pauseDuration }
            }
            return liveStatus && isRunning
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

private fun TextInputLayout.useServiceFieldColors() {
    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
    boxStrokeColor = 0xff0a9a58.toInt()
    boxBackgroundColor = 0xfff5fff8.toInt()
    defaultHintTextColor = ColorStateList.valueOf(0xff326b4d.toInt())
    hintTextColor = ColorStateList.valueOf(0xff087344.toInt())
}

private fun AppCompatCheckBox.useServiceCheckColors() {
    buttonTintList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(0xff0a9a58.toInt(), 0xff555555.toInt())
    )
    setTextColor(Color.BLACK)
}
