package edu.graduation.takemedicineclock

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private val datas = arrayListOf(
        TimeBean(0),
        TimeBean(1),
        TimeBean(2)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar
        rvTime.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        rvTime.adapter = object : RecyclerView.Adapter<TimeViewHolder>() {

            override fun onCreateViewHolder(group: ViewGroup, posi: Int): TimeViewHolder {
                return TimeViewHolder(group)
            }

            override fun getItemCount() = datas.size

            override fun onBindViewHolder(holder: TimeViewHolder, posi: Int) {
                val bean = datas[posi]
                bean.index = posi
                holder.setData(bean)
            }
        }

        btnAddOne.setOnClickListener {
            addOneTime()
        }

        btnSetAlarm.setOnClickListener {
            checkDuplicateAddAlarm()
        }
    }

    private fun checkDuplicateAddAlarm() {
        if (datas.isEmpty()) {
            toast("还没有设置服药时间")
            return
        }
        doAsync {
            val sorted = datas.sortedBy { it.timeInMinutes() }
            var temp = sorted.first()
            var theSame = false
            sorted.takeLast(sorted.size - 1).forEach {
                if (it.timeInMinutes() == temp.timeInMinutes()) {
                    theSame = true
                    return@forEach
                }
                temp = it
            }
            runOnUiThread {
                if (theSame) {
                    alert {
                        title = "重复时间"
                        message = "${temp.timeStr}重复了，删除一个吗？"
                        okButton {
                            removeOne(temp)
                            checkDuplicateAddAlarm()
                        }
                        cancelButton { alertUserConfirmThenAddAlarm() }
                        show()
                    }
                } else {
                    alertUserConfirmThenAddAlarm()
                }
            }
        }
    }

    private fun alertUserConfirmThenAddAlarm() {
        runOnUiThread {
            alert {
                title = "添加提醒闹钟"
                message = "将要添加${datas.size}个闹钟（${datas.joinToString(",") { it.timeStr }} ），提醒服药，确认吗？"
                okButton {
                    val msg = getNoticeMsg()
                    datas.forEach {
                        createAlarm(msg, it.hour, it.mins)
                    }
                    alert {
                        title = "创建好了"
                        message = "去闹钟页面看看，确认下？"
                        positiveButton("看看") {
                            go2ViewAlarm()
                        }
                        negativeButton("不用了") {}
                        show()
                    }
                }
                cancelButton { }
                show()
            }
        }
    }

    private fun getNoticeMsg(): String {
        return buildString {
            val name = edtDrugName.text.trim().toString()
            if (name.isNotBlank()) {
                append("该服药(${name})啦！")
            } else {
                append("该服药啦！")
            }
            append(spnNotice.selectedItem)
            append(spnDrugPiece.selectedItem)
            append(spnDrugUnit.selectedItem)
            append("。")
            val notice = edtNotice.text.trim().toString()
            if (notice.isNotBlank()) {
                append(notice)
            }
        }
    }

    private fun addOneTime() {
        datas.add(TimeBean(datas.size))
        if (datas.removeAll { !it.enable }) {
            rvTime.adapter?.notifyDataSetChanged()
        } else {
            rvTime.adapter?.notifyItemInserted(datas.lastIndex)
        }
    }

    private fun createAlarm(message: String, hour: Int, minutes: Int) {

        //action为AlarmClock.ACTION_SET_ALARM
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            //闹钟的小时
            .putExtra(AlarmClock.EXTRA_HOUR, hour)
            //闹钟的分钟
            .putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            //响铃时提示的信息
            .putExtra(AlarmClock.EXTRA_MESSAGE, message)
            //用于指定该闹铃触发时是否振动
            .putExtra(AlarmClock.EXTRA_VIBRATE, true)
            //一个 content: URI，用于指定闹铃使用的铃声，也可指定 VALUE_RINGTONE_SILENT 以不使用铃声。
            //如需使用默认铃声，则无需指定此 extra。
//            .putExtra(AlarmClock.EXTRA_RINGTONE, ringtoneUri)
            //一个 ArrayList，其中包括应重复触发该闹铃的每个周日。
            // 每一天都必须使用 Calendar 类中的某个整型值（如 MONDAY）进行声明。
            //对于一次性闹铃，无需指定此 extra
            .putExtra(AlarmClock.EXTRA_DAYS, arrayListOf(1, 2, 3, 4, 5, 6, 7))
            //如果为true，则调用startActivity()不会进入手机的闹钟设置界面
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    val EXTRA_ALARM_SEARCH_MODE = "android.intent.extra.alarm.SEARCH_MODE"

    val ALARM_SEARCH_MODE_TIME = "android.time"

    val ALARM_SEARCH_MODE_NEXT = "android.next"

    val ALARM_SEARCH_MODE_ALL = "android.all"

    val ALARM_SEARCH_MODE_LABEL = "android.label"

    private fun go2ViewAlarm() {

        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun dismissAlarm() {

        val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM)
            //闹钟的小时
            .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, ALARM_SEARCH_MODE_ALL)
            .putExtra(AlarmClock.EXTRA_HOUR, 10)
            //闹钟的分钟
            .putExtra(AlarmClock.EXTRA_MINUTES, 34)
            //响铃时提示的信息
            .putExtra(AlarmClock.EXTRA_MESSAGE, "")
            //用于指定该闹铃触发时是否振动
            .putExtra(AlarmClock.EXTRA_VIBRATE, true)
            //一个 content: URI，用于指定闹铃使用的铃声，也可指定 VALUE_RINGTONE_SILENT 以不使用铃声。
            //如需使用默认铃声，则无需指定此 extra。
//            .putExtra(AlarmClock.EXTRA_RINGTONE, ringtoneUri)
            //一个 ArrayList，其中包括应重复触发该闹铃的每个周日。
            // 每一天都必须使用 Calendar 类中的某个整型值（如 MONDAY）进行声明。
            //对于一次性闹铃，无需指定此 extra
            .putExtra(AlarmClock.EXTRA_DAYS, arrayListOf(1, 2, 3, 4, 5, 6, 7))
            //如果为true，则调用startActivity()不会进入手机的闹钟设置界面
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    data class TimeBean(
        var index: Int, var hour: Int = when (index) {
            0 -> 7
            1 -> 12
            else -> 18
        }, var mins: Int = 0, var enable: Boolean = true
    ) {

        fun timeInMinutes() = hour * 60 + mins

        companion object {
            private val formate = DecimalFormat("00")
        }

        val title: String
            get() = "第${index + 1}次"

        val timeStr: String
            get() = "${formate.format(hour)}:${formate.format(mins)}"
    }

    inner class TimeViewHolder(group: ViewGroup, val context: Context = group.context) :
        RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_time, group, false)) {


        private val tvIndex = itemView.findViewById<TextView>(R.id.tvIndex)
        private val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
//        private val switchOn = itemView.findViewById<Switch>(R.id.switchOn)

        fun setData(bean: TimeBean) {
            tvIndex.text = bean.title
            tvTime.text = bean.timeStr
            itemView.setOnClickListener {
                TimePickerDialog(context, { view, hour, mins ->
                    bean.hour = hour
                    bean.mins = mins
                    tvTime.text = bean.timeStr
                }, bean.hour, bean.mins, true).show()
            }
            /*switchOn.isChecked = bean.enable
            switchOn.setOnCheckedChangeListener { _, isChecked ->
                bean.enable = isChecked
            }*/
            val function: (View) -> Boolean = {
                context.alert {
                    title = "删除"
                    message = "删除 <${bean.title}-${bean.timeStr}> 这条记录吗？"
                    okButton {
                        removeOne(bean)
                    }
                    cancelButton { }
                    show()
                }
                true
            }
            itemView.setOnLongClickListener(function)
        }

    }

    private fun removeOne(bean: TimeBean) {
        datas.remove(bean)
        rvTime.adapter?.notifyItemRemoved(bean.index)
        if (bean.index != datas.size) {
            datas.forEachIndexed { index, timeBean ->
                timeBean.index = index
            }
            rvTime.adapter?.notifyDataSetChanged()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        moveTaskToBack(false)
    }
}
