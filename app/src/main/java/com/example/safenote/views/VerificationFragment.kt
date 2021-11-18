package com.example.safenote.views

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.Context.KEYGUARD_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.example.safenote.R
import com.example.safenote.databinding.FragmentVerificationBinding
import com.example.safenote.utils.KeyStoreManager
import com.example.safenote.viewmodels.VerificationViewModel
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.material.snackbar.Snackbar
import com.scottyab.rootbeer.RootBeer
import java.security.Key

@Suppress("DEPRECATION")
class VerificationFragment : Fragment(), GoogleApiClient.ConnectionCallbacks {

    private lateinit var googleApiClient : GoogleApiClient
    private lateinit var verificationViewModel : VerificationViewModel
    private lateinit var authTimer : CountDownTimer
    private var _binding : FragmentVerificationBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verificationViewModel = ViewModelProviders.of(requireActivity()).get(VerificationViewModel::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        _binding = FragmentVerificationBinding.inflate(inflater, container, false)

        binding.rootedAuthBox.setOnClickListener { handleRootedAuth() }

        binding.deviceAuthBox.setOnClickListener { handleDeviceAuth() }

        binding.captchaAuthBox.setOnClickListener { handleCaptchaAuth() }

        binding.userPasswordAuthBox.setOnClickListener { handleUserPasswordAuth() }

        connectGoogleApiClient()
        preCheck()
        startTimeCounter()

        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        authTimer.cancel()
    }

    // pre check for rooted device
    private fun preCheck() {
        val rootBeer = RootBeer(requireContext())

        if(rootBeer.isRooted) {
            binding.rootedAuthImg.setImageResource(R.drawable.invalid)
        }
        else {
            binding.rootedAuthImg.setImageResource(R.drawable.correct)
            verificationViewModel.setIsDeviceNotRooted()
        }
    }

    // handles interaction with rootedAuth box
    private fun handleRootedAuth() {
        showDialog(getString(R.string.rootedAuthInfo))
    }

    // handles interaction with deviceAccessAuth box
    private fun handleDeviceAuth() {

        if(verificationViewModel.isDeviceAccessVerified())
            return

        val keyguardManager = requireActivity().getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        if(keyguardManager.isKeyguardSecure) {
            val authIntent = keyguardManager.createConfirmDeviceCredentialIntent("", "")
            KeyStoreManager.setExitIfBackgrounded(false)
            startActivityForResult(authIntent, KeyStoreManager.DEVICE_ACCESS_CODE)
        }
        else {
            showDialog(getString(R.string.noDevicePassword))
        }
    }

    // handles interaction with captchaAuth box
    private fun handleCaptchaAuth() {

        if(verificationViewModel.isCaptchaVerified())
            return

        SafetyNet.SafetyNetApi.verifyWithRecaptcha(googleApiClient, KeyStoreManager.captchaSiteKey)
                .setResultCallback { result ->
                    if(result.status.isSuccess) {
                        binding.captchaAuthImg.setImageResource(R.drawable.correct)
                        verificationViewModel.setIsCaptchaVerified()
                        onAuthSuccess()
                    }
                }
    }

    // handles device authentication result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == KeyStoreManager.DEVICE_ACCESS_CODE) {
            if(resultCode == RESULT_OK) {
                verificationViewModel.setIsDeviceAccessVerified()
                binding.deviceAuthImg.setImageResource(R.drawable.correct)
                onAuthSuccess()
            }
        }
    }

    // handles interaction with userPasswordAuth box
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ShowToast")
    private fun handleUserPasswordAuth() {

        if(verificationViewModel.isUserPasswordVerified())
            return

        val alertDialogBuilder = AlertDialog.Builder(requireContext())

        val passwordInput = EditText(requireContext())
        passwordInput.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
        passwordInput.hint = getString(R.string.userPasswordBoxText)
        passwordInput.textAlignment = View.TEXT_ALIGNMENT_CENTER
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        alertDialogBuilder.setView(passwordInput)

        alertDialogBuilder.setPositiveButton(getString(R.string.ok)) { _, _ ->

            val userInput = passwordInput.text.toString()

            if(verificationViewModel.isUserPasswordCorrect(userInput)) {
                binding.userPasswordAuthStatus.setImageResource(R.drawable.correct)
                verificationViewModel.setIsUserPasswordVerified()
                onAuthSuccess()
            }
            else {
                showSnack(getString(R.string.incorrectPassword))
            }
        }

        alertDialogBuilder.setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialog, _ ->
            dialog.cancel()
        })

        val alertDialog = alertDialogBuilder.create()

        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
    }

    // maintains remaining time for user to authenticate
    private fun startTimeCounter() {
        authTimer = object : CountDownTimer(KeyStoreManager.TIMEOUT_AFTER, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(p0: Long) {
                val remainingTimeText = getString(R.string.remainingTime)
                val currentTime = p0 / 1000
                binding.remainingTime.text = "$remainingTimeText $currentTime"

                if(currentTime < 10)
                    binding.remainingTime.setTextColor(Color.RED)
            }

            override fun onFinish() {
                onTimeout()
            }
        }.start()
    }

    // redirect to note screen if all authentication steps are finished with success
    private fun onAuthSuccess() {
        if(verificationViewModel.isSuccessfullyVerified()) {
            authTimer.cancel()
            findNavController().navigate(R.id.action_verificationFragment_to_noteFragment)
        }
        KeyStoreManager.setExitIfBackgrounded(true)
    }

    // redirect to main screen if authentication steps are not finished on time
    private fun onTimeout() {
        val intent = requireActivity().intent
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        requireActivity().overridePendingTransition(0, 0)
        requireActivity().finish()

        requireActivity().overridePendingTransition(0, 0)
        startActivity(intent)
    }

    // displays passed text in snack
    private fun showSnack(text : String) {
        val snack = Snackbar.make(requireView(), text, Snackbar.LENGTH_SHORT)
        val snackView = snack.view
        val snackText  = snackView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        snackText.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snack.show()
    }

    // displays dialog with message
    private fun showDialog(message : String) {

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton(getString(R.string.ok), null)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)

        val messageView = alertDialog.findViewById<TextView>(android.R.id.message)
        messageView.textSize = 18F
    }

    // sets up connection to Google Api Client for Captcha
    private fun connectGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(requireContext())
                .addApi(SafetyNet.API)
                .addConnectionCallbacks(this)
                .build()

        googleApiClient.connect()
    }

    override fun onConnected(p0: Bundle?) {}
    override fun onConnectionSuspended(p0: Int) {}
}