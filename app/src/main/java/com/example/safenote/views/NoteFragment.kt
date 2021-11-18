package com.example.safenote.views

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.example.safenote.R
import com.example.safenote.databinding.FragmentNoteBinding
import com.example.safenote.utils.KeyStoreManager
import com.example.safenote.viewmodels.NoteViewModel
import com.example.safenote.viewmodels.VerificationViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job

class NoteFragment : Fragment() {

    private lateinit var noteViewModel : NoteViewModel
    private lateinit var verificationViewModel : VerificationViewModel
    private var _binding : FragmentNoteBinding? = null
    private val binding get() = _binding!!

    private var isNoteHidden = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verificationViewModel = ViewModelProviders.of(requireActivity()).get(VerificationViewModel::class.java)
        noteViewModel = ViewModelProviders.of(requireActivity()).get(NoteViewModel::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        KeyStoreManager.allowAccess(requireContext())

        binding.secretNoteEditText.setOnLongClickListener {
            onNoteRequest()
            true
        }

        binding.saveNoteBtn.setOnClickListener {
            onNoteSaved()
        }

        binding.changePasswordBtn.setOnClickListener {
            onChangePassword()
        }

        noteViewModel.setNoteContent(KeyStoreManager.getNote())

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
    private fun onNoteSaved() {
        val noteContent = noteViewModel.getNoteContent()
        if(isNoteHidden)
            KeyStoreManager.saveNote(noteContent)
        else
            KeyStoreManager.saveNote(binding.secretNoteEditText.text.toString())
        showSnack(getString(R.string.savedSuccessfully))
        hideNote()
    }

    // displays dialog with input for password
    private fun onChangePassword() {
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
                findNavController().navigate(R.id.action_noteFragment_to_setPasswordFragment)
            }
            else {
                showSnack(getString(R.string.incorrectPassword))
            }
        }

        alertDialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }

        val alertDialog = alertDialogBuilder.create()

        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
    }

    private fun hideNote() {
        binding.secretNoteEditText.hint = getString(R.string.notePlaceholder)
        noteViewModel.setNoteContent(binding.secretNoteEditText.text.toString())
        binding.secretNoteEditText.setText("")
        isNoteHidden = true
    }

    private fun showNote() {
        binding.secretNoteEditText.hint = ""
        binding.secretNoteEditText.setText(noteViewModel.getNoteContent())
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
}