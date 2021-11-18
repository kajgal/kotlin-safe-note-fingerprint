@file:Suppress("DEPRECATION")

package com.example.safenote.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.findNavController
import com.example.safenote.R
import com.example.safenote.databinding.FragmentMainBinding
import com.example.safenote.utils.KeyStoreManager
import com.example.safenote.viewmodels.MainViewModel

class MainFragment : Fragment() {

    private lateinit var mainViewModel : MainViewModel
    private var _binding : FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProviders.of(requireActivity()).get(MainViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        KeyStoreManager.setExitOnResume(false)

        binding.proceedBtn.setOnClickListener {
            if(mainViewModel.isFirstTimeUsage()) {
                findNavController().navigate(R.id.action_mainFragment_to_setPasswordFragment)
            }
            else {
                findNavController().navigate(R.id.action_mainFragment_to_verificationFragment)
            }
            KeyStoreManager.setExitOnResume(true)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}