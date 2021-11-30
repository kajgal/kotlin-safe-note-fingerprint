package com.example.safenote.views

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.example.safenote.R
import com.example.safenote.databinding.FragmentNoteBinding
import com.example.safenote.utils.CryptographyManager
import com.example.safenote.viewmodels.VerificationViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executor

class NoteFragment : Fragment() {

    private lateinit var verificationViewModel : VerificationViewModel
    private var _binding : FragmentNoteBinding? = null
    private val binding get() = _binding!!

    private var isNoteHidden = true

    private lateinit var executor : Executor
    private lateinit var biometricPrompt : BiometricPrompt
    private lateinit var biometricPromptInfo : BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        executor = ContextCompat.getMainExecutor(requireContext())
        verificationViewModel = ViewModelProviders.of(requireActivity()).get(VerificationViewModel::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)

        binding.secretNoteEditText.showSoftInputOnFocus = false

        binding.secretNoteEditText.setOnLongClickListener {
            onNoteRequest()
            true
        }

        binding.saveNoteBtn.setOnClickListener {
            onNoteSaved()
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onNoteRequest() {

        if(isNoteHidden)
            showNote()
        else
            hideNote()
    }

    // handles saving note
    @RequiresApi(Build.VERSION_CODES.N)
    private fun onNoteSaved() {

        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if(errorCode == BiometricPrompt.ERROR_LOCKOUT)
                    onTooManyFailedAttempts()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                verificationViewModel.saveNoteContent(verificationViewModel.getNoteContent(), result.cryptoObject!!.cipher!!)
                showSnack(getString(R.string.savedSuccessfully))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.savingNewData))
            .setSubtitle(getString(R.string.savingNewDataSub))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        when(BiometricManager.from(requireContext()).canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                if(!isNoteHidden)
                    hideNote()
                biometricPrompt.authenticate(biometricPromptInfo, BiometricPrompt.CryptoObject(CryptographyManager.initCipherForEncryption()))
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showSnack(getString(R.string.hardwareUnavailable))
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showSnack(getString(R.string.noEnrolled))
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showSnack(getString(R.string.noHardware))
            }
        }
    }

    private fun hideNote(override : Boolean = true) {
        binding.secretNoteEditText.showSoftInputOnFocus = false
        binding.secretNoteEditText.hint = getString(R.string.notePlaceholder)
        verificationViewModel.setNoteContent(binding.secretNoteEditText.text.toString())
        binding.secretNoteEditText.setText("")
        isNoteHidden = true
    }

    private fun showNote() {
        binding.secretNoteEditText.showSoftInputOnFocus = true
        binding.secretNoteEditText.hint = ""
        binding.secretNoteEditText.setText(verificationViewModel.getNoteContent())
        isNoteHidden = false
    }

    // displays passed text in snack
    private fun showSnack(text : String) {
        val snack = Snackbar.make(requireView(), text, Snackbar.LENGTH_SHORT)
        val snackView = snack.view
        val snackText  = snackView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        snackText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        snack.show()
    }

    private fun onTooManyFailedAttempts() {
        val intent = requireActivity().intent
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        requireActivity().overridePendingTransition(0, 0)
        requireActivity().finish()
        requireActivity().overridePendingTransition(0, 0)
        startActivity(intent)
    }
}