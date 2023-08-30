package com.ej.cameratest.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.ej.cameratest.R
import com.ej.cameratest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
    }

    override fun onStart() {
        super.onStart()
        val transaction = supportFragmentManager.beginTransaction()

        val fragment = MainFragment.newInstance()
        transaction.replace(R.id.frameLayout,fragment)
        transaction.commit()
    }



}