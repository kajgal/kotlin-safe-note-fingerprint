package com.example.safenote.views

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.example.safenote.R
import com.example.safenote.databinding.FragmentSetPasswordBinding
import com.example.safenote.viewmodels.MainViewModel
import com.example.safenote.viewmodels.SetPasswordViewModel
import com.google.android.material.snackbar.Snackbar

class SetPasswordFragment : Fragment() {

    private lateinit var setPasswordViewModel: SetPasswordViewModel
    private lateinit var mainViewModel: MainViewModel
    private var _binding : FragmentSetPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPasswordViewModel = ViewModelProviders.of(requireActivity()).get(SetPasswordViewModel::class.java)
        mainViewModel = ViewModelProviders.of(requireActivity()).get(MainViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSetPasswordBinding.inflate(inflater, container, false)

        binding.confirmBtn.setOnClickListener {

            if(binding.userPassword.text.toString() == binding.userConfirmPassword.text.toString()) {
                refreshPasswordVerification()
                onPasswordStrengthVerified()

                if (setPasswordViewModel.isPasswordStrong()) {

                    if (mainViewModel.isFirstTimeUsage()) {
                        setPasswordViewModel.setPassword(getUserPassword())
                        findNavController().navigate(R.id.action_setPasswordFragment_to_noteFragment)
                    } else {
                        setPasswordViewModel.setPassword(getUserPassword())
                        onPasswordChanged()
                    }
                }
            }
            else {
                showSnack(getString(R.string.passwordsNotMatching))
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshPasswordVerification() {
        binding.passwordLength.setTextColor(Color.BLACK)
        binding.lowerAndUppercase.setTextColor(Color.BLACK)
        binding.lettersAndNumbers.setTextColor(Color.BLACK)
        binding.specialCharacters.setTextColor(Color.BLACK)
    }

    private fun onPasswordStrengthVerified() {

        val verificationFlags = setPasswordViewModel.getPasswordStrengthVerificationFlags(getUserPassword())

        // verify password length condition
        if(!verificationFlags[0])
            binding.passwordLength.setTextColor(Color.RED)
        else
            binding.passwordLength.setTextColor(Color.GREEN)

        // verify mixture of lower and uppercase letters condition
        if(!verificationFlags[1])
            binding.lowerAndUppercase.setTextColor(Color.RED)
        else
            binding.lowerAndUppercase.setTextColor(Color.GREEN)

        // verify mixture of letters and numbers condition
        if(!verificationFlags[2])
            binding.lettersAndNumbers.setTextColor(Color.RED)
        else
            binding.lettersAndNumbers.setTextColor(Color.GREEN)

        // verify special characters condition
        if(!verificationFlags[3])
            binding.specialCharacters.setTextColor(Color.RED)
        else
            binding.specialCharacters.setTextColor(Color.GREEN)

    }

    private fun onPasswordChanged() {
        showDialog(getString(R.string.passwordChangeText))
    }

    private fun getUserPassword() : String {
        return binding.userPassword.text.toString()
    }

    // displays dialog with message
    private fun showDialog(message : String) {

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setMessage(message)

        alertDialogBuilder.setPositiveButton(getString(R.string.ok)) { _, _ ->
            val intent = requireActivity().intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            requireActivity().overridePendingTransition(0, 0)
            requireActivity().finish()
            requireActivity().overridePendingTransition(0, 0)
            startActivity(intent)
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)

        val messageView = alertDialog.findViewById<TextView>(android.R.id.message)
        messageView.textSize = 18F
    }

    // displays passed text in snack
    private fun showSnack(text : String) {
        val snack = Snackbar.make(requireView(), text, Snackbar.LENGTH_SHORT)
        val snackView = snack.view
        val snackText  = snackView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        snackText.textAlignment = View.TEXT_ALIGNMENT_CENTER

        snack.show()
    }
}