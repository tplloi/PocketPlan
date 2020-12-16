package com.pocket_plan.j7_003.data.settings.sub_categories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import com.pocket_plan.j7_003.MainActivity
import com.pocket_plan.j7_003.R
import com.pocket_plan.j7_003.data.fragmenttags.FT
import com.pocket_plan.j7_003.data.settings.SettingId
import com.pocket_plan.j7_003.data.settings.SettingsManager
import kotlinx.android.synthetic.main.fragment_settings_birthdays.view.*
import kotlinx.android.synthetic.main.fragment_settings_shopping.view.*

class SettingsBirthdays : Fragment() {
    private lateinit var swShowMonth: SwitchCompat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val myView = inflater.inflate(R.layout.fragment_settings_birthdays, container, false)

        initializeComponents(myView)
        initializeAdapters()
        initializeDisplayValues()
        initializeListeners()

        return myView
    }

    private fun initializeComponents(myView: View) {

        //initialize references to view
        swShowMonth = myView.swShowMonth
    }

    private fun initializeAdapters(){}

    private fun initializeDisplayValues() {

        swShowMonth.isChecked =
            SettingsManager.getSetting(SettingId.BIRTHDAY_SHOW_MONTH) as Boolean
    }

    private fun initializeListeners() {
        //Switch for only showing one category as expanded
        swShowMonth.setOnClickListener {
            SettingsManager.addSetting(SettingId.BIRTHDAY_SHOW_MONTH, swShowMonth.isChecked)
        }
    }
}