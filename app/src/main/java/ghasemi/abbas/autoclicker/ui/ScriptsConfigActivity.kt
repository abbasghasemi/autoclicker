package ghasemi.abbas.autoclicker.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
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
import ghasemi.abbas.autoclicker.NotificationCenter
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.ui.components.DurationTimeView
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.LayoutHelper
import ghasemi.abbas.autoclicker.utils.rippleBackground
import ghasemi.abbas.autoclicker.utils.value

class ScriptsConfigActivity : BaseFragment(),
    ItemAdapter.ViewBinding<ScriptsConfigActivity.ViewModel, AppServiceHelper.ScriptsConfig>,
    NotificationCenter.NotificationCenterDelegate {

    private lateinit var itemAdapter: ItemAdapter<ScriptsConfigActivity.ViewModel, AppServiceHelper.ScriptsConfig>
    private var alertDialog: AlertDialog? = null

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
                        background = rippleBackground(mask = ColorDrawable(Color.WHITE))
                        setOnClickListener {
                            finishFragment()
                        }
                    },
                    LayoutHelper.createFrame(
                        32f,
                        LayoutHelper.MATCH_PARENT,
                        Gravity.LEFT,
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
                0f, 64f, 0f, 0f
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
            alertDialog = AlertDialog.Builder(context)
                .setView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    val etName = TextInputEditText(context)
                    val durationTimeView = DurationTimeView(context)
                    var durationTime: Long = item.durationTime
                    durationTimeView.consumer = Consumer {
                        durationTime = it
                    }
                    durationTimeView.updateDurationTime(durationTime)
                    val cbSynchronous = AppCompatCheckBox(context)
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
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40f, 15f, 0f, 15f, 20f)
                    )
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
                })
                .show()
        }
        view.delete.setOnClickListener {
            alertDialog = AlertDialog.Builder(context)
                .setTitle(item.name)
                .setMessage(R.string.do_you_want_this_script_removed)
                .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                    AppServiceHelper.saveScriptsConfig(item, true)
                    itemAdapter.notifyItemRemoved(position)
                    itemAdapter.notifyItemRangeChanged(position, itemAdapter.itemCount)
                }.setNegativeButton(R.string.no, null)
                .show()
        }
        view.select.setOnClickListener {
            NotificationCenter.instance()
                .postNotificationName(NotificationCenter.appServiceStart, item)
        }
        return true
    }

    inner class ViewModel {
        val view: MaterialCardView
        val title = AppCompatTextView(context!!)
        val edit = AppCompatImageView(context!!)
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
                        addView(
                            edit.apply {
                                background = rippleBackground()
                                setImageResource(R.drawable.round_edit_24)
                                imageTintList = ColorStateList.valueOf(0xff00BCD4.toInt())
                            },
                            LayoutHelper.createLinearRelatively(
                                32f,
                                32f,
                                Gravity.CENTER_VERTICAL,
                                0f, 0f, 0f, 5f, 0f
                            )
                        )
                        addView(
                            delete.apply {
                                background = rippleBackground()
                                setImageResource(R.drawable.round_delete_outline_24)
                                imageTintList = ColorStateList.valueOf(0xffF44336.toInt())
                            },
                            LayoutHelper.createLinearRelatively(
                                32f,
                                32f,
                                Gravity.CENTER_VERTICAL,
                                0f, 0f, 0f, 5f, 0f
                            )
                        )
                        addView(
                            select.apply {
                                background = rippleBackground()
                                setImageResource(R.drawable.round_start_24)
                                imageTintList = ColorStateList.valueOf(
                                    ResourcesCompat.getColor(
                                        context.resources,
                                        R.color.color_primary,
                                        null
                                    )
                                )
                            },
                            LayoutHelper.createLinear(
                                32f,
                                32f,
                                Gravity.CENTER_VERTICAL,
                            )
                        )
                    },
                    LayoutHelper.createLinear(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.WRAP_CONTENT,
                        10f,
                        20f,
                        10f,
                        20f
                    )
                )
            }
            view = materialCardView
        }
    }

}