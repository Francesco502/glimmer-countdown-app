package com.example.timeapk.ui.event

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EventEntryInputFocusTest {
    @Test
    fun newEventTitleFieldRequestsInitialFocusAndAcceptsLabelTaps() {
        val source = readSource("ui/event/EventEntryScreen.kt")
        val titleFieldCall = source.substringAfter("value = eventDetails.title,")
            .substringBefore("SongInkChoiceRow(")
        val textField = source.substringAfter("private fun SongInkTextField(")
            .substringBefore("@Composable\nprivate fun SongInkDateRow(")

        assertTrue(titleFieldCall.contains("requestInitialFocus = eventDetails.id == 0 && eventDetails.title.isBlank()"))
        assertTrue(textField.contains("val focusRequester = remember { FocusRequester() }"))
        assertTrue(textField.contains(".focusRequester(focusRequester)"))
        assertTrue(textField.contains("focusRequester.requestFocus()"))
        assertTrue(textField.contains("keyboardController?.show()"))
    }

    @Test
    fun inkTextFieldKeepsImeCompositionStateForPinyinInput() {
        val source = readSource("ui/event/EventEntryScreen.kt")
        val textField = source.substringAfter("private fun SongInkTextField(")
            .substringBefore("@Composable\nprivate fun SongInkDateRow(")

        assertTrue(source.contains("import androidx.compose.ui.text.input.TextFieldValue"))
        assertTrue(textField.contains("var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }"))
        assertTrue(textField.contains("if (value != fieldValue.text)"))
        assertTrue(textField.contains("value = fieldValue"))
        assertTrue(textField.contains("onValueChange = { nextValue ->"))
        assertTrue(textField.contains("fieldValue = nextValue"))
        assertTrue(textField.contains("onValueChange(nextValue.text)"))
    }

    @Test
    fun inkTextFieldForcesImeVisibleWhenFocusedFromHardwareKeyboardMode() {
        val source = readSource("ui/event/EventEntryScreen.kt")
        val textField = source.substringAfter("private fun SongInkTextField(")
            .substringBefore("@Composable\nprivate fun SongInkDateRow(")

        assertTrue(source.contains("import android.view.inputmethod.InputMethodManager"))
        assertTrue(source.contains("import androidx.compose.ui.platform.LocalView"))
        assertTrue(textField.contains("val view = LocalView.current"))
        assertTrue(textField.contains("Context.INPUT_METHOD_SERVICE"))
        assertTrue(textField.contains("InputMethodManager.SHOW_FORCED"))
        assertTrue(textField.contains("inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED)"))
    }

    @Test
    fun saveMessagesResolveFromLocaleWrappedScreenContext() {
        val viewModelSource = readSource("ui/event/EventEntryViewModel.kt")
        val screenSource = readSource("ui/event/EventEntryScreen.kt")

        assertTrue(viewModelSource.contains("data class PartialSuccess(@param:StringRes val messageResId: Int)"))
        assertTrue(viewModelSource.contains("data class Failure(@param:StringRes val messageResId: Int)"))
        assertFalse(viewModelSource.contains("application.getString(R.string.save_event"))
        assertTrue(screenSource.contains("context.getString(result.messageResId)"))
        assertFalse(screenSource.contains("showSnackbar(result.message)"))
    }

    @Test
    fun activityRecreationDoesNotReinitializeTheSameEventDraft() {
        val viewModelSource = readSource("ui/event/EventEntryViewModel.kt")

        assertTrue(viewModelSource.contains("private data class PreparedEventKey(val eventId: Int?)"))
        assertTrue(viewModelSource.contains("if (preparedEventKey == requestedKey) return"))
        assertTrue(viewModelSource.contains("preparedEventKey = requestedKey"))
    }

    private fun readSource(relative: String): String {
        val direct = File("src/main/java/com/example/timeapk/$relative")
        if (direct.exists()) return direct.readText(Charsets.UTF_8)
        val fromRoot = File("app/src/main/java/com/example/timeapk/$relative")
        require(fromRoot.exists()) { "Missing source file: $relative" }
        return fromRoot.readText(Charsets.UTF_8)
    }
}
