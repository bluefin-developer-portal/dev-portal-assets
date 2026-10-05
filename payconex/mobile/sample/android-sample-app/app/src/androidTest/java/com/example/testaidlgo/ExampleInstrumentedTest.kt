package com.bluefin.testaidlgo

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Installation smoke check only; this does not exercise the SDK or a reader.
        // Update the expected merchant applicationId when renaming the sample.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.bluefin.testaidlgo", appContext.packageName)
    }
}