package com.devidea.timeleft

import com.devidea.timeleft.EntityItemInfo
import com.devidea.timeleft.MainActivity
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.appwidget.AppWidgetManager
import com.devidea.timeleft.AppDatabase
import com.devidea.timeleft.AppWidget
import android.app.PendingIntent
import android.app.AlarmManager
import android.widget.RemoteViews
import com.devidea.timeleft.R
import com.devidea.timeleft.AdapterItem
import com.devidea.timeleft.EntityWidgetInfo
import com.devidea.timeleft.DatabaseDao
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
import androidx.room.*

@Dao
interface DatabaseDao {
    @Insert
    fun saveItem(entityItemInfo: EntityItemInfo?)

    @get:Query("SELECT * FROM EntityItemInfo")
    val item: List<EntityItemInfo?>

    @Query("DELETE FROM EntityItemInfo WHERE id = :ID")
    fun deleteItem(ID: Int)

    @Query("SELECT * FROM EntityItemInfo WHERE id = :ID")
    fun getSelectItem(ID: Int): EntityItemInfo

    @Query("UPDATE EntityItemInfo SET startValue = :updateStart, endValue = :updateEnd WHERE id = :ID")
    fun updateItem(updateStart: String?, updateEnd: String?, ID: Int)

    //위젯엔티티
    @Insert
    fun saveWidget(entityWidgetInfo: EntityWidgetInfo?)

    @Query("SELECT widgetID FROM EntityWidgetInfo")
    fun get(): IntArray?

    @Query("SELECT type FROM EntityWidgetInfo WHERE widgetID = :ID")
    fun getType(ID: Int): String?

    @Query("SELECT ItemID FROM EntityWidgetInfo WHERE widgetID = :ID")
    fun getTypeID(ID: Int): Int

    @Query("DELETE FROM EntityWidgetInfo WHERE widgetID = :ID")
    fun delete(ID: Int)

    @Query("DELETE FROM EntityWidgetInfo WHERE ItemID = :ID")
    fun deleteCustomWidget(ID: Int)
}