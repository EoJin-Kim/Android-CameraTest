package com.ej.cameratest

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ej.cameratest.databinding.ActivityMainBinding
import com.ej.cameratest.databinding.FragmentMainBinding


class MainFragment : Fragment() {

    val CAMERA_CODE = 10001
    lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_main,container, false)
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
            transcation.addToBackStack("camera")
            transcation.commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            val image : Bitmap? = data?.getParcelableExtra(CameraActivity.PHOTO_IMAGE)
            binding.imageView.setImageBitmap(image)
        }
    }
    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}