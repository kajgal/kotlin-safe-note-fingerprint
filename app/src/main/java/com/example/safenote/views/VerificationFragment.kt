package com.example.safenote.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.example.safenote.R
import com.example.safenote.databinding.FragmentVerificationBinding
import com.example.safenote.viewmodels.VerificationViewModel
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.material.snackbar.Snackbar
import com.scottyab.rootbeer.RootBeer
import java.util.concurrent.Executor
import androidx.biometric.BiometricPrompt
import com.example.safenote.utils.CryptographyManager
import com.example.safenote.utils.SharedPreferencesManager

@Suppress("DEPRECATION")
class VerificationFragment : Fragment(), GoogleApiClient.ConnectionCallbacks {

    private lateinit var googleApiClient : GoogleApiClient
    private lateinit var verificationViewModel : VerificationViewModel
    private lateinit var authTimer : CountDownTimer

    private lateinit var executor : Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var biometricPromptInfo : BiometricPrompt.PromptInfo

    private var _binding : FragmentVerificationBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        executor = ContextCompat.getMainExecutor(requireContext())
        verificationViewModel = ViewModelProviders.of(requireActivity()).get(VerificationViewModel::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        _binding = FragmentVerificationBinding.inflate(inflater, container, false)

        binding.rootedAuthBox.setOnClickListener { handleRootedAuth() }

        binding.deviceAuthBox.setOnClickListener { handleDeviceAuth() }

        binding.captchaAuthBox.setOnClickListener { handleCaptchaAuth() }

        verificationViewModel.resetVerification()
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
    @RequiresApi(Build.VERSION_CODES.N)
    private fun handleDeviceAuth() {
        // force user to finish captcha auth first
        if(verificationViewModel.isCaptchaVerified()) {

            biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if(errorCode == BiometricPrompt.ERROR_LOCKOUT) {

                        biometricPrompt.cancelAuthentication()

                        if(verificationViewModel.isFirstTimeUsage()) {
                            showDialog(getString(R.string.currentLock))
                        }
                        else {
                            SharedPreferencesManager.removeNote()
                            showDialog(getString(R.string.messageGone))
                        }
                    }
                }

                @RequiresApi(Build.VERSION_CODES.N)
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    binding.deviceAuthImg.setImageResource(R.drawable.correct)

                    val cryptoObject = result.cryptoObject?.cipher

                    if (cryptoObject != null) {
                        verificationViewModel.onAuthSuccess(cryptoObject)
                    }

                    onAuthSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    biometricPrompt.cancelAuthentication()
                    showDialog(getString(R.string.warningAttempt))
                }
            })

            biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.accessToNote))
                    .setSubtitle(getString(R.string.accessToNoteSub))
                    .setNegativeButtonText(getString(R.string.cancel))
                    .build()

            when(BiometricManager.from(requireContext()).canAuthenticate()) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    try {
                        if(verificationViewModel.isFirstTimeUsage())
                            biometricPrompt.authenticate(biometricPromptInfo)
                        else
                            biometricPrompt.authenticate(biometricPromptInfo, BiometricPrompt.CryptoObject(CryptographyManager.initCipherForDecryption()))
                    }
                    catch (e : KeyPermanentlyInvalidatedException) {
                        verificationViewModel.onKeyPermanentlyInvalidated()
                        showDialog(getString(R.string.onEnrollment), requestExit = true)
                    }
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
                else -> {
                    showSnack(getString(R.string.currentLock))
                }
            }
        }
        else {
            showSnack(getString(R.string.captchaFirst))
        }
    }
    // handles interaction with captchaAuth box
    @RequiresApi(Build.VERSION_CODES.N)
    private fun handleCaptchaAuth() {

        if(!verificationViewModel.isCaptchaVerified()) {
            SafetyNet.SafetyNetApi.verifyWithRecaptcha(googleApiClient, verificationViewModel.captchaSiteKey)
                .setResultCallback { result ->
                    if (result.status.isSuccess) {
                        binding.captchaAuthImg.setImageResource(R.drawable.correct)
                        verificationViewModel.setIsCaptchaVerified()
                        binding.deviceAuthBox.alpha = 1.0F
                    }
                }
        }
    }

    // maintains remaining time for user to authenticate
    private fun startTimeCounter() {
        authTimer = object : CountDownTimer(verificationViewModel.TIMEOUT_AFTER, 1000) {
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
    @RequiresApi(Build.VERSION_CODES.N)
    private fun onAuthSuccess() {
        if(verificationViewModel.isDeviceNotRooted()) {
            authTimer.cancel()
            findNavController().navigate(R.id.action_verificationFragment_to_noteFragment)
        }
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
    private fun showDialog(message: String, requestExit : Boolean = false) {

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setMessage(message)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)

        if(requestExit) {
            alertDialogBuilder.setPositiveButton(getString(R.string.ok)) { _, _ -> onTimeout() }
            alertDialog.setOnCancelListener {
                onTimeout()
            }
        }
        else {
            alertDialogBuilder.setPositiveButton(getString(R.string.ok), null)

        }

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