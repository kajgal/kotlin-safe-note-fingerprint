package com.example.safenote.viewmodels

import androidx.lifecycle.ViewModel

class NoteViewModel : ViewModel() {

    private var noteContent = ""

    fun setNoteContent(note : String) {
        noteContent = note
    }

    fun getNoteContent() : String {
        return noteContent
    }
}