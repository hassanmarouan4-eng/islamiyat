package org.dorossi.app.core

/** Validation problems a form field can have; the UI turns them into localized messages. */
sealed interface FieldError {
    data object Required : FieldError
    data object InvalidNumber : FieldError
    data object Duplicate : FieldError
    data object EndBeforeStart : FieldError
    data object Overlap : FieldError
    data class OutOfRange(val min: String?, val max: String?) : FieldError
}
