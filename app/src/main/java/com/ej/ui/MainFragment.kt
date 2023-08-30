package com.ej.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ej.cameratest.R
import com.ej.cameratest.databinding.FragmentMainBinding


class MainFragment : Fragment() {

    val CAMERA_CODE = 10001
    lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main,container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.camera.setOnClickListener {
//            val intent = Intent(requireContext(),CameraActivity::class.java)
//            activity?.startActivityForResult(intent, CAMERA_CODE)

            val cameraFragment = CameraFragment.newInstance()
            val transcation = parentFragmentManager.beginTransaction()
            transcation.replace(R.id.frameLayout,cameraFragment)
            transcation.addToBackStack(CameraFragment.TAG)
            transcation.commit()
        }

        binding.camera2.setOnClickListener {
            val cameraFragment = CameraMlFragment.newInstance()
            val transcation = parentFragmentManager.beginTransaction()
            transcation.replace(R.id.frameLayout,cameraFragment)
            transcation.addToBackStack(CameraFragment.TAG)
            transcation.commit()
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}