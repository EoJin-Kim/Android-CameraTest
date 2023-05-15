package com.ej.cameratest

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ej.cameratest.databinding.ActivityMainBinding
import com.ej.cameratest.databinding.FragmentResultBinding


class ResultFragment : Fragment() {
    lateinit var binding: FragmentResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_result,container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = ResultFragment()
    }
}