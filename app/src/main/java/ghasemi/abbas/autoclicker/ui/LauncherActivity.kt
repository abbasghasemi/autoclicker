package ghasemi.abbas.autoclicker.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import ghasemi.abbas.autoclicker.AppConfig
import ghasemi.abbas.autoclicker.AppServiceHelper
import ghasemi.abbas.autoclicker.ApplicationLoader
import ghasemi.abbas.autoclicker.BuildConfig
import ghasemi.abbas.autoclicker.NotificationCenter
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.ui.components.PointView
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.BackController
import ghasemi.abbas.autoclicker.utils.LayoutHelper
import ghasemi.abbas.autoclicker.utils.rippleBackground
import ghasemi.abbas.autoclicker.utils.value


class LauncherActivity : AppCompatActivity(), BackController.OnInvoke {

    private var baseView: FrameLayout? = null
    private var dialog: Dialog? = null
    private var popupWindow: PopupWindow? = null
    private val baseFragments = ArrayList<BaseFragment>()
    private val backController = BackController()
    private var wating: Boolean = false
    private var created: Boolean = false

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(ApplicationLoader.applicationCreateConfigurationContext(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseView = FrameLayout(this)
        baseView!!.layoutParams =
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)

        setContentView(baseView)
        backController.registerOnBackInvokedCallback(this, this)
        AndroidUtils.updateTileAndWidget()
    }

    override fun onDestroy() {
        AndroidUtils.updateTileAndWidget()
        backController.unregisterOnBackInvokedCallback(this)
        for (i in baseFragments.size - 1 downTo 0) {
            baseFragments[i].onPause()
            baseFragments[i].onDestroyView()
            closeLastFragment()
        }
        super.onDestroy()
    }

    private fun createView() {
        if (created || wating) return
        baseView!!.removeAllViews()
        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.addView(
            FrameLayout(this).apply {
                addView(
                    TextView(this@LauncherActivity).apply {
                        setText(R.string.app_name)
                        setBackgroundColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.color_primary,
                                null
                            )
                        )
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        gravity = Gravity.CENTER
                        setTypeface(
                            ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold),
                            Typeface.BOLD
                        )
                    },
                    LayoutHelper.createFrame(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.MATCH_PARENT,
                    )
                )
                addView(
                    AppCompatImageView(this@LauncherActivity).apply {
                        setImageResource(R.drawable.round_share_24)
                        background = rippleBackground(mask = ColorDrawable(Color.WHITE))
                        setOnClickListener {
                            val shareText =
                                if (BuildConfig.FLAVOR == "cafebazaar") "https://cafebazaar.ir/app/" + BuildConfig.APPLICATION_ID
                                else "https://myket.ir/app/" + BuildConfig.APPLICATION_ID
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/*"
                            intent.putExtra(Intent.EXTRA_TEXT, shareText)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            try {
                                startActivity(Intent.createChooser(intent, "اشتراک گذاری"))
                            } catch (e: Exception) {
                                //
                            }
                        }
                    },
                    LayoutHelper.createFrame(
                        32f,
                        LayoutHelper.MATCH_PARENT,
                        Gravity.LEFT,
                        10f, 0f, 0f, 0f
                    )
                )

                addView(
                    AppCompatImageView(this@LauncherActivity).apply {
                        setImageResource(R.drawable.round_star_24)
                        background = rippleBackground(mask = ColorDrawable(Color.WHITE))
                        setOnClickListener {
                            val intent =
                                Intent(if (BuildConfig.FLAVOR == "cafebazaar") Intent.ACTION_EDIT else Intent.ACTION_VIEW)
                            intent.data = Uri.parse(
                                if (BuildConfig.FLAVOR == "cafebazaar") "bazaar://details?id=" + BuildConfig.APPLICATION_ID
                                else "myket://comment?id=" + BuildConfig.APPLICATION_ID
                            )
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                //
                            }
                        }
                    },
                    LayoutHelper.createFrame(
                        32f,
                        LayoutHelper.MATCH_PARENT,
                        Gravity.RIGHT,
                        0f, 0f, 10f, 0f
                    )
                )

            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                64f,
                Gravity.NO_GRAVITY
            )
        )
        if (!AndroidUtils.isAccessibilityServiceEnabled()) {
            wating = true
            val progressBar = ProgressBar(this)
            baseView!!.addView(
                progressBar,
                LayoutHelper.createFrame(55f, 55f, Gravity.CENTER)
            )
            AndroidUtils.runOnUIThread({
                if (isFinishing) {
                    return@runOnUIThread
                }
                wating = false
                baseView!!.removeView(progressBar)
                if (AndroidUtils.isAccessibilityServiceEnabled()) {
                    requireMainLayout(linearLayout)
                } else {
                    requirePermissionLayout(linearLayout)
                }
            }, 500)
        } else {
            requireMainLayout(linearLayout)
        }
        baseView!!.addView(
            ScrollView(this).apply {
                addView(
                    linearLayout, LayoutHelper.createScroll(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.MATCH_PARENT,
                        Gravity.NO_GRAVITY
                    )
                )
            },
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT,
                Gravity.NO_GRAVITY
            )
        )
    }

    private fun requireMainLayout(linearLayout: LinearLayout) {
        if (!AppConfig.instance().lastPermissionConfirmed) {
            AppConfig.instance().lastPermissionConfirmed = true
        }
        created = true
        linearLayout.apply {
            addView(
                TextInputLayout(this@LauncherActivity).apply {
                    val timeTypeName = arrayOf("میلی ثانیه", "ثانیه", "دقیقه")
                    suffixText = timeTypeName[AppConfig.instance().clickTimeType]
                    suffixTextView.apply {
                        textSize = 12f
                        setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.round_arrow_drop_down_24,
                            0,
                            0,
                            0
                        )
                        setOnClickListener {
                            popupWindow = PopupWindow().apply {
                                isOutsideTouchable = true
                                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                width = ViewGroup.LayoutParams.WRAP_CONTENT
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                                inputMethodMode = PopupWindow.INPUT_METHOD_FROM_FOCUSABLE
                                elevation = AndroidUtils.dpf(5f)
                                contentView = LinearLayout(this@LauncherActivity).apply {
                                    orientation = LinearLayout.VERTICAL
                                    layoutParams = LayoutHelper.createLinear(
                                        LayoutHelper.WRAP_CONTENT,
                                        LayoutHelper.WRAP_CONTENT,
                                        Gravity.NO_GRAVITY,
                                        15f, 15f, 15f, 15f
                                    )
                                    clipToOutline = true
                                    outlineProvider = ViewOutlineProvider.BACKGROUND
                                    val r = AndroidUtils.dpf(10f)
                                    background = ShapeDrawable(
                                        RoundRectShape(
                                            floatArrayOf(r, r, r, r, r, r, r, r),
                                            null,
                                            null
                                        )
                                    ).apply {
                                        paint.color = Color.WHITE
                                    }

                                    for (index in 0..2)
                                        addView(
                                            TextView(this@LauncherActivity).apply {
                                                gravity = Gravity.CENTER_VERTICAL
                                                setTextColor(Color.BLACK)
                                                textSize = 14f
                                                typeface = ResourcesCompat.getFont(
                                                    this@LauncherActivity,
                                                    R.font.sans
                                                )
                                                setPadding(
                                                    r.toInt(),
                                                    r.toInt(),
                                                    r.toInt(),
                                                    r.toInt()
                                                )
                                                background =
                                                    rippleBackground(mask = ColorDrawable(Color.GRAY))
                                                text = timeTypeName[index]
                                                setOnClickListener {
                                                    suffixText = timeTypeName[index]
                                                    AppConfig.instance().clickTimeType = index
                                                    if (index == 0 && AppConfig.instance().clickTime < AppServiceHelper.minimumTimeClick) {
                                                        AppConfig.instance().clickTime =
                                                            AppServiceHelper.minimumTimeClick
                                                    }
                                                    dismiss()
                                                }
                                            },
                                            LayoutHelper.createFrame(
                                                LayoutHelper.MATCH_PARENT,
                                                48f
                                            )
                                        )

                                }
                                showAsDropDown(it)
                            }
                        }
                    }
                    setHint(R.string.click_time)
                    addView(TextInputEditText(this@LauncherActivity).apply {
                        value = AppConfig.instance().clickTime.toString()
                        filters = arrayOf(
                            InputFilter.LengthFilter(4),
                        )
                        inputType = InputType.TYPE_CLASS_NUMBER
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {

                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {

                            }

                            override fun afterTextChanged(s: Editable?) {
                                if (s.toString().isNotEmpty() && (
                                            AppConfig.instance().clickTimeType == 0 && s.toString()
                                                .toInt() >= AppServiceHelper.minimumTimeClick ||
                                                    AppConfig.instance().clickTimeType != 0 && s.toString()
                                                .toInt() >= 1
                                            )
                                ) {
                                    AppConfig.instance().clickTime = s.toString().toInt()
                                }
                            }
                        })
                    })
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    15f, 15f, 15f, 0f
                )
            )

            addView(
                TextInputLayout(this@LauncherActivity).apply {
                    setHint(R.string.long_click_time)
                    suffixText = "میلی ثانیه"
                    suffixTextView.apply {
                        textSize = 12f
                    }
                    addView(TextInputEditText(this@LauncherActivity).apply {
                        value = AppConfig.instance().longClickTime.toString()
                        filters = arrayOf(
                            InputFilter.LengthFilter(4),
                        )
                        inputType = InputType.TYPE_CLASS_NUMBER
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {

                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {

                            }

                            override fun afterTextChanged(s: Editable?) {
                                if (s.toString().isNotEmpty() && s.toString()
                                        .toInt() >= 1
                                ) {
                                    AppConfig.instance().longClickTime = s.toString().toInt()
                                }
                            }
                        })
                    })
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    15f, 15f, 15f, 0f
                )
            )

            addView(
                TextInputLayout(this@LauncherActivity).apply {
                    setHint(R.string.swipe_time)
                    suffixText = "میلی ثانیه"
                    suffixTextView.apply {
                        textSize = 12f
                    }
                    addView(TextInputEditText(this@LauncherActivity).apply {
                        value = AppConfig.instance().swipeTime.toString()
                        filters = arrayOf(
                            InputFilter.LengthFilter(4),
                        )
                        inputType = InputType.TYPE_CLASS_NUMBER
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {

                            }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {

                            }

                            override fun afterTextChanged(s: Editable?) {
                                if (s.toString().isNotEmpty() && s.toString()
                                        .toInt() >= AppServiceHelper.minimumSwipeClick
                                ) {
                                    AppConfig.instance().swipeTime = s.toString().toInt()
                                }
                            }
                        })
                    })
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    15f, 15f, 15f, 0f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    addView(
                        LinearLayout(this@LauncherActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            addView(TextView(this@LauncherActivity).apply {
                                setText(R.string.maximum_duration_time)
                                textSize = 14f
                                typeface = ResourcesCompat.getFont(
                                    this@LauncherActivity,
                                    R.font.sans
                                )
                                setTextColor(Color.BLACK)
                            })
                            background = rippleBackground(mask = ColorDrawable(Color.GRAY))
                            val durationTime = TextView(this@LauncherActivity)
                            fun durationTime(): CharSequence {
                                return when (AppConfig.instance().durationTime) {
                                    0L -> getString(R.string.infinity)
                                    in 1..59 -> "${AppConfig.instance().durationTime}s"
                                    in 60..3599 -> "${AppConfig.instance().durationTime / 60}m ${AppConfig.instance().durationTime % 60}s"
                                    else -> {
                                        val h = AppConfig.instance().durationTime / 3600
                                        val m =
                                            (AppConfig.instance().durationTime - h * 60 * 60) / 60
                                        "${h}h ${m}m ${AppConfig.instance().durationTime % 60}s"
                                    }
                                }
                            }
                            addView(durationTime.apply {
                                text = durationTime()
                                gravity = LayoutHelper.absoluteGravityStart
                                textSize = 12f
                                typeface = ResourcesCompat.getFont(
                                    this@LauncherActivity,
                                    R.font.sans
                                )
                                setTextColor(Color.DKGRAY)
                            })
                            setOnClickListener {
                                dialog = DurationTimeDialog(this@LauncherActivity) {
                                    AppConfig.instance().durationTime = it
                                    durationTime.text = durationTime()
                                }
                                dialog?.show()
                            }
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.MATCH_PARENT,
                            LayoutHelper.WRAP_CONTENT,
                            0.5f
                        )
                    )
                    addView(
                        TextInputLayout(this@LauncherActivity).apply {
                            setHint(R.string.repeat_count)
                            suffixText =
                                if (AppConfig.instance().repeatCount == 0) getString(R.string.infinity) else ""
                            suffixTextView.apply {
                                textSize = 12f
                            }
                            addView(TextInputEditText(this@LauncherActivity).apply {
                                value = AppConfig.instance().repeatCount.toString()
                                filters = arrayOf(
                                    InputFilter.LengthFilter(4),
                                )
                                inputType = InputType.TYPE_CLASS_NUMBER
                                addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(
                                        s: CharSequence?,
                                        start: Int,
                                        count: Int,
                                        after: Int
                                    ) {

                                    }

                                    override fun onTextChanged(
                                        s: CharSequence?,
                                        start: Int,
                                        before: Int,
                                        count: Int
                                    ) {

                                    }

                                    override fun afterTextChanged(s: Editable?) {
                                        if (s.toString().isNotEmpty() && s.toString()
                                                .toInt() >= 0
                                        ) {
                                            AppConfig.instance().repeatCount =
                                                s.toString().toInt()
                                            suffixText =
                                                if (s.toString()
                                                        .toInt() == 0
                                                ) getString(R.string.infinity) else ""
                                        }
                                    }
                                })
                            })
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.MATCH_PARENT,
                            LayoutHelper.WRAP_CONTENT,
                            0.5f
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    15f, 15f, 15f, 0f
                )
            )

            addView(
                MaterialCheckBox(this@LauncherActivity).apply {
                    setText(R.string.synchronous_execution_of_widgets)
                    isChecked = AppConfig.instance().synchronousExecution
                    setOnCheckedChangeListener { _, isChecked ->
                        AppConfig.instance().synchronousExecution = isChecked
                    }
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    40f,
                    15f, 0f, 15f, 0f
                )
            )

            addView(
                MaterialCheckBox(this@LauncherActivity).apply {
                    setText(R.string.hidden_widgets)
                    isChecked = AppConfig.instance().hiddenWidgetsExecution
                    setOnCheckedChangeListener { _, isChecked ->
                        AppConfig.instance().hiddenWidgetsExecution = isChecked
                    }
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    40f,
                    15f, 0f, 15f, 0f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    addView(
                        MaterialButton(this@LauncherActivity).apply {
                            setText(R.string.scripts_config)
                            setOnClickListener {
                                startFragment(ScriptsConfigActivity())
                            }
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                            insetBottom = 0
                            insetTop = 0
                            setBackgroundColor(0xff009688.toInt())
                        },
                        LayoutHelper.createLinearRelatively(
                            0f,
                            LayoutHelper.WRAP_CONTENT,
                            Gravity.NO_GRAVITY,
                            1f,
                            0f,
                            0f,
                            10f,
                            0f
                        )
                    )

                    addView(
                        MaterialButton(this@LauncherActivity).apply {
                            setText(R.string.testing)
                            setOnClickListener {
                                startFragment(TestAutoClickerActivity())
                            }
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                            insetBottom = 0
                            insetTop = 0
                            strokeWidth = AndroidUtils.dp(1f)
                            setBackgroundColor(Color.WHITE)
                            rippleColor = ColorStateList.valueOf(
                                ColorUtils.setAlphaComponent(
                                    0xff009688.toInt(),
                                    20
                                )
                            )
                            strokeColor = ColorStateList.valueOf(0xff009688.toInt())
                            setTextColor(0xff009688.toInt())
                        },
                        LayoutHelper.createLinear(
                            0f,
                            LayoutHelper.WRAP_CONTENT,
                            1f
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    15f, 20f, 15f, 0f
                )
            )

            addView(
                MaterialButton(this@LauncherActivity).apply {
                    setText(R.string.start_activity)
                    setOnClickListener {
                        if (AndroidUtils.isAccessibilityServiceEnabled()) {
                            NotificationCenter.instance()
                                .postNotificationName(NotificationCenter.appServiceStart)
                        } else {
                            accessibilityPermission()
                        }
                    }
                    typeface =
                        ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                    insetBottom = 0
                    insetTop = 0
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    15f, 10f, 15f, 0f
                )
            )

            addView(
                TextView(this@LauncherActivity).apply {
                    setText("راهنمای برنامه")
                    textSize = 18f
                    typeface = ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                    setTextColor(Color.BLACK)
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 20f, 15f, 0f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        AppCompatImageView(this@LauncherActivity).apply {
                            setImageResource(R.drawable.play_to_pause)
                            setColorFilter(Color.BLACK)
                        },
                        LayoutHelper.createLinearRelatively(
                            32f,
                            32f,
                            Gravity.CENTER_VERTICAL,
                            0f,
                            0f,
                            0f,
                            10f,
                            0f
                        )
                    )
                    addView(
                        TextView(this@LauncherActivity).apply {
                            text = "شروع/خاتمه فعالیت"
                            textSize = 15f
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT,
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 10f, 15f, 10f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        AppCompatImageView(this@LauncherActivity).apply {
                            setImageResource(R.drawable.round_add_24)
                            setColorFilter(Color.BLACK)
                        },
                        LayoutHelper.createLinearRelatively(
                            32f,
                            32f,
                            Gravity.CENTER_VERTICAL,
                            0f,
                            0f,
                            0f,
                            10f,
                            0f
                        )
                    )
                    addView(
                        TextView(this@LauncherActivity).apply {
                            text = "اضافه کردن ویجت کلیک/لمس طولانی اضافه کردن ویجت کشیدن"
                            textSize = 15f
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT,
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 10f, 15f, 10f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        AppCompatImageView(this@LauncherActivity).apply {
                            setImageResource(R.drawable.round_delete_outline_24)
                            setColorFilter(Color.BLACK)
                        },
                        LayoutHelper.createLinearRelatively(
                            32f,
                            32f,
                            Gravity.CENTER_VERTICAL,
                            0f,
                            0f,
                            0f,
                            10f,
                            0f
                        )
                    )
                    addView(
                        TextView(this@LauncherActivity).apply {
                            text = "حذف ویجت/لمس طولانی حذف تمامی ویجت ها"
                            textSize = 15f
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT,
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 10f, 15f, 10f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        AppCompatImageView(this@LauncherActivity).apply {
                            setImageResource(R.drawable.round_home_24)
                            setColorFilter(Color.BLACK)
                        },
                        LayoutHelper.createLinearRelatively(
                            32f,
                            32f,
                            Gravity.CENTER_VERTICAL,
                            0f,
                            0f,
                            0f,
                            10f,
                            0f
                        )
                    )
                    addView(
                        TextView(this@LauncherActivity).apply {
                            text = "بازکردن برنامه"
                            textSize = 15f
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT,
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 10f, 15f, 10f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        AppCompatImageView(this@LauncherActivity).apply {
                            setImageResource(R.drawable.round_exit_to_app_24)
                            setColorFilter(Color.BLACK)
                        },
                        LayoutHelper.createLinearRelatively(
                            32f,
                            32f,
                            Gravity.CENTER_VERTICAL,
                            0f,
                            0f,
                            0f,
                            10f,
                            0f
                        )
                    )
                    addView(
                        TextView(this@LauncherActivity).apply {
                            text = "با دوبار کلیک کردن پنجره بسته می شود"
                            textSize = 15f
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT,
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 10f, 15f, 10f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        AppCompatImageView(this@LauncherActivity).apply {
                            setImageResource(R.drawable.round_zoom_in_map_24)
                            setColorFilter(Color.BLACK)
                        },
                        LayoutHelper.createLinearRelatively(
                            32f,
                            32f,
                            Gravity.CENTER_VERTICAL,
                            0f,
                            0f,
                            0f,
                            10f,
                            0f
                        )
                    )
                    addView(
                        TextView(this@LauncherActivity).apply {
                            text = "تغییر لوکیشن پنجره/لمس طولانی تغییر حالت افقی و عمودی"
                            textSize = 15f
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT,
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 10f, 15f, 10f
                )
            )

            addView(
                LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        PointView(this@LauncherActivity),
                        LayoutHelper.createLinearRelatively(
                            32f,
                            32f,
                            Gravity.CENTER_VERTICAL,
                            0f,
                            0f,
                            0f,
                            10f,
                            0f
                        )
                    )
                    addView(
                        TextView(this@LauncherActivity).apply {
                            text =
                                "نقطه شروع/مشخص کردن زمان برای هر ویجت/لمس طولانی ویجت را حذف میکند"
                            textSize = 15f
                            typeface =
                                ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT,
                        )
                    )
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 10f, 15f, 10f
                )
            )

            addView(
                TextView(this@LauncherActivity).apply {
                    setText(R.string.diagnosis)
                    textSize = 18f
                    typeface = ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                    setTextColor(Color.BLACK)
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 20f, 15f, 0f
                )
            )

            requirePermissionLayout(this)

            addView(
                TextView(this@LauncherActivity).apply {
                    text = getString(R.string.version, BuildConfig.VERSION_NAME)
                    textSize = 15f
                    typeface = ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                    setTextColor(Color.BLACK)
                },
                LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL,
                    0f, 10f, 0f, 10f
                )
            )
        }
        AndroidUtils.runOnUIThread({
            checkFragment(intent)
        })
    }

    private fun requirePermissionLayout(linearLayout: LinearLayout) {
        val isXiaomi = "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)
        linearLayout.apply {
            addView(
                TextView(this@LauncherActivity).apply {
                    setText(if (created) R.string.reactive_accessibility_service else R.string.active_accessibility_service)
                    textSize = 16f
                    typeface = ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                    setTextColor(Color.BLACK)
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 15f, 15f, 10f
                )
            )
            addView(
                MaterialButton(this@LauncherActivity).apply {
                    setText(R.string.permission_confirmation)
                    setOnClickListener {
                        accessibilityPermission()
                    }
                    insetBottom = 0
                    insetTop = 0
                    typeface =
                        ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                },
                LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL,
                    15f, 0f, 15f, 15f
                )
            )

            if (availableAutoStartManagement()) {
                addView(
                    TextView(this@LauncherActivity).apply {
                        text =
                            "فعال بودن اجرای برنامه در پسزمینه الزامی است، در غیر اینصورت با بسته شدن یا توقف برنامه فعالیت آن نیز خاتمه خواهد یافت."
                        textSize = 16f
                        typeface = ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        setTextColor(Color.BLACK)
                    },
                    LayoutHelper.createLinear(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.WRAP_CONTENT,
                        Gravity.NO_GRAVITY,
                        15f, 0f, 15f, 10f
                    )
                )

                addView(
                    MaterialButton(this@LauncherActivity).apply {
                        setText(R.string.allow_activity_in_background)
                        insetBottom = 0
                        insetTop = 0
                        setOnClickListener {
                            requestAutoStartManagement()
                        }
                        typeface =
                            ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                    },
                    LayoutHelper.createLinear(
                        LayoutHelper.WRAP_CONTENT,
                        LayoutHelper.WRAP_CONTENT,
                        Gravity.CENTER_HORIZONTAL,
                        15f, 0f, 15f, 15f
                    )
                )
            }

            if (isXiaomi) {
                addView(
                    TextView(this@LauncherActivity).apply {
                        text =
                            "پیشنهاد می شود بهینه سازی باطری را برای جلوگیری از ایجاد اخلال در عملکرد برنامه غیرفعال کنید."
                        textSize = 16f
                        typeface = ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                        setTextColor(Color.BLACK)
                    },
                    LayoutHelper.createLinear(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.WRAP_CONTENT,
                        Gravity.NO_GRAVITY,
                        15f, 0f, 15f, 10f
                    )
                )

                addView(
                    MaterialButton(this@LauncherActivity).apply {
                        setText(R.string.deactivation)
                        insetBottom = 0
                        insetTop = 0
                        setOnClickListener {
                            miuiPowerkeeper()
                        }
                        typeface =
                            ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                    },
                    LayoutHelper.createLinear(
                        LayoutHelper.WRAP_CONTENT,
                        LayoutHelper.WRAP_CONTENT,
                        Gravity.CENTER_HORIZONTAL,
                        15f, 0f, 15f, 15f
                    )
                )
            }

            addView(
                TextView(this@LauncherActivity).apply {
                    text =
                        "ممکن است فعالیت برنامه توسط سیستم متوقف و باعث بروز اخلال در عملکرد آن شود، برای جلوگیری از توقف اجازه فعالیت در پسزمینه یا تنظیمات مربوط به بهینه سازی باطری را بررسی نمایید."
                    textSize = 16f
                    typeface = ResourcesCompat.getFont(this@LauncherActivity, R.font.sans)
                    setTextColor(Color.BLACK)
                },
                LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.NO_GRAVITY,
                    15f, 0f, 15f, 10f
                )
            )

            addView(
                MaterialButton(this@LauncherActivity).apply {
                    setText(R.string.app_settings)
                    insetBottom = 0
                    insetTop = 0
                    setOnClickListener {
                        appSettings()
                    }
                    typeface =
                        ResourcesCompat.getFont(this@LauncherActivity, R.font.sans_bold)
                },
                LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT,
                    LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL,
                    15f, 0f, 15f, 15f
                )
            )
        }
    }

    private fun isCallable(intent: Intent): Boolean {
        return intent.resolveActivityInfo(packageManager, 0) != null;

        val list = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return list.size > 0
    }

    val powermanagerIntents = mutableListOf(
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.letv.android.letvsafe",
                "com.letv.android.letvsafe.AutobootManageActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.startupapp.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.evenwell.powersaving.g3",
                "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.powersaver.PowerSaverSettings"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.asus.mobilemanager",
                "com.asus.mobilemanager.entry.FunctionActivity"
            )
        ).setData(
            Uri.parse("mobilemanager://function/entry/AutoStart")
        )
    )

    private fun miuiPowerkeeper() {
        val intent = Intent()
        intent.component = ComponentName(
            "com.miui.powerkeeper",
            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
        )
        intent.putExtra("package_name", packageName)
        intent.putExtra("package_label", getText(R.string.app_name))
        try {
            startActivity(intent)
        } catch (e: Exception) {
        }
    }

    private fun availableAutoStartManagement(): Boolean {
        val manufacturer = Build.MANUFACTURER
        return when {
            "xiaomi".equals(manufacturer, ignoreCase = true) -> true
            "oppo".equals(manufacturer, ignoreCase = true) -> true
            "vivo".equals(manufacturer, ignoreCase = true) -> true
            "letv".equals(manufacturer, ignoreCase = true) -> true
            "honor".equals(manufacturer, ignoreCase = true) -> true
            "asus".equals(manufacturer, ignoreCase = true) -> true
            "nokia".equals(manufacturer, ignoreCase = true) -> true
            "huawei".equals(manufacturer, ignoreCase = true) -> true
            else -> false
        }
    }

    private fun requestAutoStartManagement() {

        val intent = Intent()
        val manufacturer = Build.MANUFACTURER
        when {
            "xiaomi".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }

            "oppo".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }

            "vivo".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }

            "letv".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName(
                    "com.letv.android.letvsafe",
                    "com.letv.android.letvsafe.AutobootManageActivity"
                )
            }

            "honor".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            }

            "asus".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.powersaver.PowerSaverSettings"
                )
            }

            "nokia".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName(
                    "com.evenwell.powersaving.g3",
                    "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
                )
            }

            "huawei".equals(manufacturer, ignoreCase = true) -> {
                intent.component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            //
        }
    }

    private fun accessibilityPermission() {
        if (AppConfig.instance().lastPermissionConfirmed) {
            requestAccessibility()
        } else {
            if (dialog != null && dialog!!.isShowing) {
                return
            }
            val runnable = Runnable {
                if ("xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage("در تلفن شما ممکن است تایید دسترسی، توسط سیستم اجازه داده نشود. در این صورت گزینه 'اجازه دادن به تنظیمات محدود شده' را فعال سازی کنید.")
                        .setPositiveButton(R.string.accessibility_permission) { _: DialogInterface, _: Int ->
                            requestAccessibility()
                        }
                        .setNegativeButton(R.string.activation) { _: DialogInterface, _: Int ->
                            appSettings()
                        }
                        .show()
                } else {
                    requestAccessibility()
                }
            }
            if (dialog == null) {
                var confirm =false
                dialog = PermissionDialog(
                    this,
                    R.string.accessibility_permission,
                    R.string.accessibility_permission_description,
                    R.drawable.round_settings_accessibility_24
                ) {
                    confirm = true
                    runnable.run()
                }
                dialog?.setOnDismissListener {
                   if (!confirm) dialog = null
                }
                dialog?.show()
            } else {
                runnable.run()
            }
        }

    }

    private fun requestAccessibility() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun appSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
    }

    private fun overlayPermission() {
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(
                "package:$packageName"
            )
        )
    }

    private fun ignoreBatteryOptimize() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
//            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
//            intent.data = Uri.parse("package:$packageName")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                //
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dialog?.apply {
            if (isShowing) dismiss()
        }
        if (popupWindow != null && popupWindow!!.isShowing) {
            popupWindow?.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidUtils.runOnUIThread({
            createView()
        })
    }

    override fun onBackInvoked() {
        if (baseFragments.size > 0) {
            if (baseFragments.last().onBackInvoked()) {
                baseFragments.last().onPause()
                baseFragments.last().onDestroyView()
                closeLastFragment()
            }
            return
        }
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkFragment(intent)
    }

    private fun checkFragment(intent: Intent?) {
        val fragment = intent?.getStringExtra("fragment")
        if (fragment != null) {
            if (fragment == "ScriptsConfig") {
                val baseFragment = ScriptsConfigActivity()
                if (baseFragments.isNotEmpty() && baseFragment::class.java.name == baseFragments.last()::class.java.name) {
                    return
                }
                startFragment(baseFragment)
                intent.removeExtra("fragment")
            }
        }
    }

    private fun startFragment(baseFragment: BaseFragment) {
        AndroidUtils.hideKeyboard(this)
        window.decorView.clearFocus()
        baseFragment.parentLayout = this
        baseFragment.onCreateView(this)
        baseFragments.add(baseFragment)
        baseView?.addView(
            baseFragment.root, if (baseFragment.root.layoutParams == null)
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
            else baseFragment.root.layoutParams
        )
    }

    fun closeLastFragment() {
        AndroidUtils.hideKeyboard(this)
        val f = baseFragments.removeLast()
        if (f.root.parent != null && f.root.parent is ViewGroup) {
            try {
                (f.root.parent as ViewGroup).removeView(f.root)
            } catch (e: Exception) {
                //
            }
        }
        f.parentLayout = null
    }
}