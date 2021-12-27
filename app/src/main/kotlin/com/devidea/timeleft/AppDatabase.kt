package com.devidea.timeleft

import com.devidea.timeleft.EntityItemInfo
import com.devidea.timeleft.MainActivity
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.room.Room
import com.devidea.timeleft.AppDatabase
import com.devidea.timeleft.AppWidget
import android.app.PendingIntent
import android.app.AlarmManager
import android.widget.RemoteViews
import com.devidea.timeleft.R
import com.devidea.timeleft.AdapterItem
import androidx.room.Database
import com.devidea.timeleft.EntityWidgetInfo
import androidx.room.RoomDatabase
import com.devidea.timeleft.DatabaseDao
import androidx.room.Dao
import com.devidea.timeleft.InterfaceItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.devidea.timeleft.TopRecyclerView
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import me.relex.circleindicator.CircleIndicator2
import android.content.DialogInterface
import com.devidea.timeleft.CreateTimeActivity
import com.devidea.timeleft.CreateMonthActivity
import android.widget.Toast
import android.os.Looper
import com.devidea.timeleft.BottomRecyclerView
import com.devidea.timeleft.ItemGenerate
import androidx.room.PrimaryKey
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.widget.ProgressBar
import android.app.Activity
import android.widget.Spinner
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.util.SparseBooleanArray
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.widget.EditText
import com.devidea.timeleft.ItemSave
import android.app.TimePickerDialog.OnTimeSetListener
import android.widget.TimePicker
import android.app.TimePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.widget.DatePicker
import android.app.DatePickerDialog

@Database(
    entities = [EntityWidgetInfo::class, EntityItemInfo::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun DatabaseDao(): DatabaseDao
}