package ghasemi.abbas.autoclicker.ui

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Consumer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ag.recyclerview.easyadapter.ItemAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import ghasemi.abbas.autoclicker.AppServiceHelper
import ghasemi.abbas.autoclicker.AppAccessibilityService
import ghasemi.abbas.autoclicker.NotificationCenter
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.ScreenSize
import ghasemi.abbas.autoclicker.ScriptTransfer
import ghasemi.abbas.autoclicker.ui.components.DurationTimeView
import ghasemi.abbas.autoclicker.ui.components.AppChoiceButton
import ghasemi.abbas.autoclicker.ui.components.ScriptTargetSelector
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.LayoutHelper
import ghasemi.abbas.autoclicker.utils.rippleBackground
import ghasemi.abbas.autoclicker.utils.circularRippleBackground
import ghasemi.abbas.autoclicker.utils.value

class ScriptsConfigActivity : BaseFragment(),
    ItemAdapter.ViewBinding<ScriptsConfigActivity.ViewModel, AppServiceHelper.ScriptsConfig>,
    NotificationCenter.NotificationCenterDelegate {

    private lateinit var itemAdapter: ItemAdapter<ScriptsConfigActivity.ViewModel, AppServiceHelper.ScriptsConfig>
    private var alertDialog: AlertDialog? = null
    private var stepEditDialog: AlertDialog? = null

    override fun onCreateView(context: Context) {
        super.onCreateView(context)
        val textTitle = TextView(context)
        val finishButton = AppCompatImageView(context)
        root.addView(
            FrameLayout(context).apply {
                addView(
                    textTitle.apply {
                        setText(R.string.scripts_config)
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
                            ResourcesCompat.getFont(context, R.font.sans_bold),
                            Typeface.BOLD
                        )
                    },
                    LayoutHelper.createFrame(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.MATCH_PARENT,
                    )
                )
                addView(
                    finishButton.apply {
                        setImageResource(R.drawable.round_arrow_back_24)
                        background = circularRippleBackground()
                        setPadding(AndroidUtils.dp(7f), AndroidUtils.dp(7f), AndroidUtils.dp(7f), AndroidUtils.dp(7f))
                        setOnClickListener {
                            finishFragment()
                        }
                    },
                    LayoutHelper.createFrame(
                        40f,
                        40f,
                        Gravity.LEFT or Gravity.CENTER_VERTICAL,
                        10f, 0f, 0f, 0f
                    )
                )
            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                64f,
                Gravity.NO_GRAVITY
            )
        )
        itemAdapter =
            ItemAdapter(AppServiceHelper.loadScriptsConfig(), this@ScriptsConfigActivity)
        root.addView(LinearLayout(context).apply {
            setPadding(AndroidUtils.dp(16f), AndroidUtils.dp(6f),
                AndroidUtils.dp(16f), AndroidUtils.dp(6f))
            addView(MaterialButton(context).apply {
                setText(R.string.add_script_button)
                insetTop = 0
                insetBottom = 0
                setOnClickListener { startNewScript() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = AndroidUtils.dp(8f)
            })
            addView(MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                setText(R.string.import_script_button)
                insetTop = 0
                insetBottom = 0
                setOnClickListener { showImportDialog() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 60f, Gravity.TOP,
            0f, 64f, 0f, 0f))
        root.addView(
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                adapter =
                    itemAdapter
            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT,
                Gravity.NO_GRAVITY,
                0f, 124f, 0f, 0f
            )
        )

        NotificationCenter.instance().addObserver(this, NotificationCenter.scriptsSaved)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        NotificationCenter.instance().removeObserver(this, NotificationCenter.scriptsSaved)
    }

    override fun onPause() {
        super.onPause()
        alertDialog?.dismiss()
        stepEditDialog?.dismiss()
    }
    override fun didReceivedNotification(event: Int, vararg args: Any) {
        if (event == NotificationCenter.scriptsSaved) {
            if (args[0] as Boolean) {
                itemAdapter.notifyItemInserted(args[1] as Int)
            } else {
                alertDialog?.dismiss()
                itemAdapter.notifyItemChanged(args[1] as Int)
            }
        }
    }

    override fun createItem(
        inflater: LayoutInflater,
        parent: ViewGroup,
        itemType: Int
    ): ItemAdapter.Binding<ViewModel> {
        val viewModel = ViewModel()
        return ItemAdapter.Binding(viewModel.view, viewModel)
    }

    override fun bindItem(
        view: ViewModel,
        item: AppServiceHelper.ScriptsConfig,
        position: Int,
        itemType: Int
    ): Boolean {
        view.title.text = item.name
        view.edit.setOnClickListener {
            alertDialog = AlertDialog.Builder(context!!, R.style.AppAlertDialog)
                .setView(ScrollView(context).apply { addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    val etName = TextInputEditText(context)
                    val targetSelector = ScriptTargetSelector(context, item.targetPackage, item.onLeaveAction)
                    val durationTimeView = DurationTimeView(context)
                    var durationTime: Long = item.durationTime
                    durationTimeView.consumer = Consumer {
                        durationTime = it
                    }
                    durationTimeView.updateDurationTime(durationTime)
                    val cbSynchronous = AppCompatCheckBox(context).apply {
                        typeface = ResourcesCompat.getFont(context, R.font.sans)
                    }
                    val cbRelative = AppCompatCheckBox(context).apply {
                        setText(R.string.relative_coordinates)
                        isChecked = item.relativeCoordinates
                        typeface = ResourcesCompat.getFont(context, R.font.sans)
                    }
                    addView(
                        TextInputLayout(context).apply {
                            setHint(R.string.script_name)
                            addView(etName.apply {
                                value = item.name
                                filters = arrayOf(InputFilter.LengthFilter(32))
                                typeface = ResourcesCompat.getFont(context, R.font.sans)
                            })
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.MATCH_PARENT,
                            LayoutHelper.WRAP_CONTENT,
                            15f,
                            20f,
                            15f,
                            0f
                        )
                    )
                    addView(targetSelector, LayoutHelper.createLinear(
                        LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 15f, 8f, 15f, 0f))
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
                            isChecked = item.synchronousExecution
                        },
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40f, 15f, 0f, 15f, 0f)
                    )
                    addView(cbRelative,
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40f, 15f, 0f, 15f, 0f))
                    addView(
                        MaterialButton(context).apply {
                            setText(R.string.save)
                            insetTop = 0
                            insetBottom = 0
                            typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
                            setOnClickListener {
                                if (etName.text.toString().isNotEmpty()) {
                                    item.name = etName.text.toString()
                                    item.synchronousExecution = cbSynchronous.isChecked
                                    item.relativeCoordinates = cbRelative.isChecked
                                    item.targetPackage = targetSelector.targetPackage
                                    item.onLeaveAction = targetSelector.onLeaveAction
                                    if (item.relativeCoordinates &&
                                        (item.referenceWidth == 0 || item.referenceHeight == 0)) {
                                        val (width, height) = ScreenSize.of(context)
                                        item.referenceWidth = width
                                        item.referenceHeight = height
                                    }
                                    item.durationTime = durationTime
                                    AppServiceHelper.saveScriptsConfig(item)
                                    alertDialog?.dismiss()
                                    itemAdapter.notifyItemChanged(position)
                                } else {
                                    AndroidUtils.toast(context.getString(R.string.name_cant_empty))
                                }
                            }
                        },
                        LayoutHelper.createLinear(
                            LayoutHelper.MATCH_PARENT,
                            LayoutHelper.WRAP_CONTENT,
                            15f,
                            0f,
                            15f,
                            15f
                        )
                    )
                }) })
                .create().let { AppDialogUi.show(it) }
        }
        view.delete.setOnClickListener {
            val ctx = context!!
            val content = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(AndroidUtils.dp(20f), AndroidUtils.dp(8f),
                    AndroidUtils.dp(20f), AndroidUtils.dp(18f))
                addView(TextView(ctx).apply {
                    setText(R.string.do_you_want_this_script_removed)
                    setTextColor(Color.BLACK)
                    textSize = 15f
                    typeface = ResourcesCompat.getFont(ctx, R.font.sans)
                })
                addView(LinearLayout(ctx).apply {
                    val cancel = MaterialButton(ctx, null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                        setText(R.string.no)
                        setOnClickListener { alertDialog?.dismiss() }
                    }
                    addView(cancel, LinearLayout.LayoutParams(0, AndroidUtils.dp(48f), 1f).apply {
                        marginEnd = AndroidUtils.dp(8f)
                    })
                    addView(MaterialButton(ctx).apply {
                        setText(R.string.yes)
                        setOnClickListener {
                            AppServiceHelper.saveScriptsConfig(item, true)
                            alertDialog?.dismiss()
                            itemAdapter.notifyItemRemoved(position)
                            itemAdapter.notifyItemRangeChanged(position, itemAdapter.itemCount)
                        }
                    }, LinearLayout.LayoutParams(0, AndroidUtils.dp(48f), 1f))
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = AndroidUtils.dp(20f) })
            }
            alertDialog = AlertDialog.Builder(ctx, R.style.AppAlertDialog)
                .setTitle(item.name)
                .setView(content)
                .create().let { AppDialogUi.show(it) }
        }
        view.share.setOnClickListener {
            val ctx = context!!
            runCatching {
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, item.name)
                    putExtra(Intent.EXTRA_TEXT, ScriptTransfer.encode(item))
                }, ctx.getString(R.string.share_script)))
            }.onFailure { AndroidUtils.toast(ctx.getString(R.string.share_script_failed)) }
        }
        view.steps.setOnClickListener { showStepsDialog(item, position) }
        view.select.setOnClickListener {
            NotificationCenter.instance()
                .postNotificationName(NotificationCenter.appServiceStart, item)
            if (item.targetPackage.isNotBlank()) {
                context?.packageManager?.getLaunchIntentForPackage(item.targetPackage)?.let { launch ->
                    context?.startActivity(launch)
                }
            }
        }
        return true
    }

    private fun showStepsDialog(script: AppServiceHelper.ScriptsConfig, scriptPosition: Int) {
        val ctx = context!!
        val list = MaxHeightRecyclerView(ctx).apply {
            maxHeight = minOf(ctx.resources.displayMetrics.heightPixels - AndroidUtils.dp(220f),
                AndroidUtils.dp(420f)).coerceAtLeast(AndroidUtils.dp(140f))
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
            setPadding(AndroidUtils.dp(8f), AndroidUtils.dp(8f), AndroidUtils.dp(8f), 0)
            adapter = StepAdapter(script, scriptPosition)
        }
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(ctx).apply {
                setText(R.string.step_editor_hint)
                textSize = 12f
                setPadding(AndroidUtils.dp(12f), AndroidUtils.dp(8f), AndroidUtils.dp(12f), 0)
            })
            if (script.widgets.isEmpty()) {
                addView(TextView(ctx).apply {
                    setText(R.string.no_script_steps)
                    setTextColor(Color.DKGRAY)
                    textSize = 14f
                    setPadding(AndroidUtils.dp(18f), AndroidUtils.dp(18f),
                        AndroidUtils.dp(18f), AndroidUtils.dp(24f))
                })
            } else {
                addView(list, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            addView(MaterialButton(ctx).apply {
                setText(R.string.close)
                insetTop = 0
                insetBottom = 0
                setOnClickListener { alertDialog?.dismiss() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtils.dp(48f)).apply {
                leftMargin = AndroidUtils.dp(16f)
                rightMargin = AndroidUtils.dp(16f)
                topMargin = AndroidUtils.dp(8f)
                bottomMargin = AndroidUtils.dp(16f)
            })
        }
        alertDialog = AlertDialog.Builder(ctx, R.style.AppAlertDialog)
            .setTitle(R.string.edit_steps)
            .setView(content)
            .create().let { AppDialogUi.show(it) }
    }

    private class MaxHeightRecyclerView(context: Context) : RecyclerView(context) {
        var maxHeight: Int = 0

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec,
                View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST))
        }
    }

    private fun startNewScript() {
        val ctx = context ?: return
        if (!AndroidUtils.isAccessibilityServiceEnabled()) {
            ctx.startActivity(Intent(ctx, LauncherActivity::class.java).apply {
                putExtra("show_accessibility_guide", true)
            })
            return
        }
        val start = {
            NotificationCenter.instance().postNotificationName(NotificationCenter.appServiceStart)
        }
        if (AppServiceHelper.isEnabled) {
            alertDialog = AlertDialog.Builder(ctx, R.style.AppAlertDialog)
                .setTitle(R.string.add_script)
                .setMessage(R.string.start_new_script_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    NotificationCenter.instance().postNotificationName(NotificationCenter.appServiceStop)
                    AndroidUtils.runOnUIThread({ start() }, 300)
                }
                .setNegativeButton(R.string.no, null)
                .create().let { AppDialogUi.show(it) }
        } else {
            start()
        }
    }

    private inner class StepAdapter(
        private val script: AppServiceHelper.ScriptsConfig,
        private val scriptPosition: Int
    ) : RecyclerView.Adapter<StepAdapter.Holder>() {
        inner class Holder(val root: LinearLayout, val title: TextView,
                           val editStep: MaterialButton,
                           val up: MaterialButton, val down: MaterialButton,
                           val duplicate: MaterialButton, val toggle: MaterialButton,
                           val preview: MaterialButton) : RecyclerView.ViewHolder(root)

        override fun getItemCount() = script.widgets.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val root = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = AndroidUtils.dp(8f)
                }
                setPadding(AndroidUtils.dp(10f), AndroidUtils.dp(8f),
                    AndroidUtils.dp(10f), AndroidUtils.dp(8f))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = AndroidUtils.dp(10f).toFloat()
                    setStroke(AndroidUtils.dp(1f), 0xffd9e5dd.toInt())
                }
            }
            val title = TextView(parent.context).apply {
                textSize = 15f
                setTextColor(Color.BLACK)
                typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
            }
            root.addView(title)
            fun action(text: Int, icon: Int): MaterialButton = MaterialButton(parent.context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                setText(text)
                setIconResource(icon)
                iconSize = AndroidUtils.dp(18f)
                iconPadding = AndroidUtils.dp(5f)
                insetTop = 0
                insetBottom = 0
                typeface = ResourcesCompat.getFont(context, R.font.sans)
                isAllCaps = false
            }
            fun row(first: MaterialButton, second: MaterialButton) = LinearLayout(parent.context).apply {
                addView(first, LinearLayout.LayoutParams(0, AndroidUtils.dp(44f), 1f).apply {
                    marginEnd = AndroidUtils.dp(6f)
                })
                addView(second, LinearLayout.LayoutParams(0, AndroidUtils.dp(44f), 1f))
            }
            val editStep = action(R.string.step_settings, R.drawable.round_edit_24)
            root.addView(editStep, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtils.dp(44f)).apply { topMargin = AndroidUtils.dp(8f) })
            val up = action(R.string.move_up, R.drawable.round_arrow_upward_24)
            val down = action(R.string.move_down, R.drawable.round_arrow_downward_24)
            root.addView(row(up, down), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = AndroidUtils.dp(4f) })
            val duplicate = action(R.string.duplicate_step, R.drawable.round_content_copy_24)
            val toggle = action(R.string.disable_step, R.drawable.round_visibility_24)
            root.addView(row(duplicate, toggle), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = AndroidUtils.dp(4f) })
            val preview = MaterialButton(parent.context).apply {
                setText(R.string.test_step)
                setIconResource(R.drawable.round_start_24)
                insetTop = 0
                insetBottom = 0
                typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
                isAllCaps = false
            }
            root.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtils.dp(44f)).apply { topMargin = AndroidUtils.dp(6f) })
            return Holder(root, title, editStep, up, down, duplicate, toggle, preview)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val step = script.widgets[position]
            holder.title.text = "${position + 1}. ${if (step.type == 0) "لمس" else "سوایپ"} • ${if (step.waitText.isBlank()) "بدون شرط" else step.waitText}"
            holder.title.alpha = if (step.enabled) 1f else 0.45f
            holder.editStep.setOnClickListener {
                val index = holder.adapterPosition
                if (index !in script.widgets.indices) return@setOnClickListener
                val current = script.widgets[index]
                val ctx = holder.root.context
                val text = TextInputEditText(ctx).apply {
                    setText(current.waitText)
                    typeface = ResourcesCompat.getFont(ctx, R.font.sans)
                }
                val timeout = TextInputEditText(ctx).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(current.waitTimeoutSeconds.toString())
                    typeface = ResourcesCompat.getFont(ctx, R.font.sans)
                }
                val form = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(AndroidUtils.dp(16f), AndroidUtils.dp(8f),
                        AndroidUtils.dp(16f), AndroidUtils.dp(16f))
                    addView(TextInputLayout(ctx).apply {
                        setHint(R.string.wait_for_text)
                        addView(text)
                    })
                    addView(TextInputLayout(ctx).apply {
                        setHint(R.string.wait_timeout)
                        addView(timeout)
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = AndroidUtils.dp(8f) })
                }
                var mode = current.gestureMode
                val curve = SeekBar(ctx).apply {
                    max = 200
                    progress = current.curvePercent + 100
                    visibility = if (mode == 1) View.VISIBLE else View.GONE
                }
                val waypoints = TextInputEditText(ctx).apply {
                    setText(current.waypoints)
                }
                if (current.type == 1) {
                    val modes = ctx.resources.getStringArray(R.array.gesture_modes)
                    val curveLabel = TextView(ctx).apply {
                        setText(R.string.curve_amount)
                        setTextColor(Color.BLACK)
                        textSize = 13f
                        typeface = ResourcesCompat.getFont(ctx, R.font.sans)
                        visibility = if (mode == 1) View.VISIBLE else View.GONE
                    }
                    val waypointsContainer = TextInputLayout(ctx).apply {
                        setHint(R.string.waypoints_hint)
                        visibility = if (mode == 4) View.VISIBLE else View.GONE
                        addView(waypoints)
                    }
                    form.addView(TextView(ctx).apply {
                        setText(R.string.gesture_mode)
                        setTextColor(Color.BLACK)
                        textSize = 14f
                        typeface = ResourcesCompat.getFont(ctx, R.font.sans_bold)
                        gravity = Gravity.RIGHT
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = AndroidUtils.dp(16f) })
                    val modeButton = AppChoiceButton(ctx, modes, mode,
                        ctx.getString(R.string.gesture_mode)) { selected ->
                        mode = selected
                        curveLabel.visibility = if (selected == 1) View.VISIBLE else View.GONE
                        curve.visibility = curveLabel.visibility
                        waypointsContainer.visibility = if (selected == 4) View.VISIBLE else View.GONE
                    }
                    form.addView(modeButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        AndroidUtils.dp(48f)).apply { topMargin = AndroidUtils.dp(4f) })
                    form.addView(curveLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = AndroidUtils.dp(8f) })
                    form.addView(curve)
                    form.addView(waypointsContainer, LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
                form.addView(LinearLayout(ctx).apply {
                    addView(MaterialButton(ctx, null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                        setText(R.string.cancel)
                        setOnClickListener { stepEditDialog?.dismiss() }
                    }, LinearLayout.LayoutParams(0, AndroidUtils.dp(48f), 1f).apply {
                        marginEnd = AndroidUtils.dp(8f)
                    })
                    addView(MaterialButton(ctx).apply {
                        setText(R.string.save)
                        setOnClickListener {
                        script.widgets[index] = current.copy(
                            waitText = text.text.toString().trim(),
                            waitTimeoutSeconds = timeout.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0,
                            gestureMode = mode,
                            curvePercent = curve.progress - 100,
                            waypoints = waypoints.text.toString().trim()
                        )
                        AppServiceHelper.saveScriptsConfig(script)
                        notifyItemChanged(index)
                        stepEditDialog?.dismiss()
                        }
                    }, LinearLayout.LayoutParams(0, AndroidUtils.dp(48f), 1f))
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = AndroidUtils.dp(18f) })
                stepEditDialog = AlertDialog.Builder(ctx, R.style.AppAlertDialog)
                    .setTitle(R.string.step_settings)
                    .setView(ScrollView(ctx).apply { addView(form) })
                    .create().let { AppDialogUi.show(it) }
            }
            holder.toggle.setText(if (step.enabled) R.string.disable_step else R.string.enable_step)
            holder.up.isEnabled = position > 0
            holder.down.isEnabled = position < script.widgets.lastIndex
            holder.up.setOnClickListener { change(holder.adapterPosition) { index ->
                if (index > 0) java.util.Collections.swap(script.widgets, index, index - 1)
            } }
            holder.down.setOnClickListener { change(holder.adapterPosition) { index ->
                if (index < script.widgets.lastIndex) java.util.Collections.swap(script.widgets, index, index + 1)
            } }
            holder.duplicate.setOnClickListener { change(holder.adapterPosition) { index ->
                script.widgets.add(index + 1, script.widgets[index].copy())
            } }
            holder.toggle.setOnClickListener { change(holder.adapterPosition) { index ->
                script.widgets[index] = script.widgets[index].copy(enabled = !script.widgets[index].enabled)
            } }
            holder.preview.setOnClickListener {
                val index = holder.adapterPosition
                if (index !in script.widgets.indices) return@setOnClickListener
                if (AppServiceHelper.isRunning) {
                    AndroidUtils.toast(context!!.getString(R.string.test_step_running))
                    return@setOnClickListener
                }
                if (script.targetPackage.isBlank()) {
                    AndroidUtils.toast(context!!.getString(R.string.test_step_needs_app))
                    return@setOnClickListener
                }
                if (!AppAccessibilityService.isConnected()) {
                    AndroidUtils.toast(context!!.getString(R.string.active_accessibility_service))
                    return@setOnClickListener
                }
                val launch = context!!.packageManager.getLaunchIntentForPackage(script.targetPackage)
                if (launch == null) {
                    AndroidUtils.toast(context!!.getString(R.string.test_step_unavailable))
                    return@setOnClickListener
                }
                NotificationCenter.instance().postNotificationName(
                    NotificationCenter.previewStep, script.widgets[index], script)
                AndroidUtils.toast(context!!.getString(R.string.test_step_countdown))
                context!!.startActivity(launch)
            }
        }

        private fun change(index: Int, action: (Int) -> Unit) {
            if (index !in script.widgets.indices) return
            action(index)
            script.widgets = ArrayList(script.widgets.mapIndexed { i, widget -> widget.copy(number = i + 1) })
            AppServiceHelper.saveScriptsConfig(script)
            notifyDataSetChanged()
            itemAdapter.notifyItemChanged(scriptPosition)
        }
    }

    private fun showImportDialog() {
        val ctx = context ?: return
        val input = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 5
            typeface = ResourcesCompat.getFont(ctx, R.font.sans)
        }
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipboardText = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
        if (clipboardText.startsWith("AUTOCLICKER_SCRIPT_V1:")) input.setText(clipboardText)
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AndroidUtils.dp(16f), AndroidUtils.dp(8f),
                AndroidUtils.dp(16f), AndroidUtils.dp(16f))
            addView(TextInputLayout(ctx).apply {
                setHint(R.string.script_code_hint)
                addView(input)
            })
            addView(MaterialButton(ctx, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                setText(R.string.paste_script)
                setOnClickListener {
                    val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                    input.setText(text)
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtils.dp(44f)).apply { topMargin = AndroidUtils.dp(8f) })
            addView(MaterialButton(ctx).apply {
                setText(R.string.import_script)
                setOnClickListener {
                    val script = ScriptTransfer.decode(input.text?.toString()?.trim())
                    if (script == null) {
                        AndroidUtils.toast(ctx.getString(R.string.invalid_script_code))
                        return@setOnClickListener
                    }
                    AppServiceHelper.saveScriptsConfig(script)
                    itemAdapter.notifyItemInserted(
                        AppServiceHelper.loadScriptsConfig().indexOfFirst { it === script })
                    alertDialog?.dismiss()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                AndroidUtils.dp(48f)).apply { topMargin = AndroidUtils.dp(8f) })
        }
        alertDialog = AlertDialog.Builder(ctx, R.style.AppAlertDialog)
            .setTitle(R.string.import_script)
            .setView(content)
            .create().let { AppDialogUi.show(it) }
    }

    inner class ViewModel {
        val view: MaterialCardView
        val title = AppCompatTextView(context!!)
        val edit = AppCompatImageView(context!!)
        val steps = AppCompatImageView(context!!)
        val share = AppCompatImageView(context!!)
        val delete = AppCompatImageView(context!!)
        val select = AppCompatImageView(context!!)

        init {
            val materialCardView = MaterialCardView(context).apply {
                layoutParams =
                    LayoutHelper.createLinear(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.WRAP_CONTENT,
                        15f,
                        10f,
                        15f,
                        5f
                    )
                radius = AndroidUtils.dpf(10f)
                cardElevation = AndroidUtils.dpf(3f)
                strokeWidth = 0
                setCardBackgroundColor(ColorStateList.valueOf(0xffEFEBE9.toInt()))
                addView(
                    LinearLayout(context).apply {
                        addView(
                            title.apply {
                                textSize = 18f
                                setTextColor(Color.BLACK)
                                typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
                            },
                            LayoutHelper.createLinearRelatively(
                                0f,
                                LayoutHelper.WRAP_CONTENT,
                                Gravity.CENTER_VERTICAL,
                                1f,
                                0f, 0f, 10f, 0f
                            )
                        )
                        fun addAction(view: AppCompatImageView, icon: Int, tint: Int,
                                      description: Int, last: Boolean = false) {
                            addView(view.apply {
                                setImageResource(icon)
                                imageTintList = ColorStateList.valueOf(tint)
                                contentDescription = context.getString(description)
                                background = circularRippleBackground()
                                setPadding(AndroidUtils.dp(7f), AndroidUtils.dp(7f),
                                    AndroidUtils.dp(7f), AndroidUtils.dp(7f))
                            }, LinearLayout.LayoutParams(AndroidUtils.dp(38f), AndroidUtils.dp(38f)).apply {
                                gravity = Gravity.CENTER_VERTICAL
                                if (!last) marginEnd = AndroidUtils.dp(3f)
                            })
                        }
                        addAction(steps, R.drawable.round_touch_app_24, 0xff008577.toInt(),
                            R.string.edit_steps)
                        addAction(share, R.drawable.round_share_24, 0xff3155a4.toInt(),
                            R.string.share_script)
                        addAction(edit, R.drawable.round_edit_24, 0xff00a7bd.toInt(),
                            R.string.edit_script_action)
                        addAction(delete, R.drawable.round_delete_outline_24, 0xffe53935.toInt(),
                            R.string.delete_script_action)
                        addAction(select, R.drawable.round_start_24, 0xff088b4c.toInt(),
                            R.string.run_script_action, true)
                    },
                    LayoutHelper.createLinear(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.WRAP_CONTENT,
                        12f,
                        12f,
                        12f,
                        12f
                    )
                )
            }
            view = materialCardView
        }
    }

}
