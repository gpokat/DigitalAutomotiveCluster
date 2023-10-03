package com.example.simplecluster

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import io.mockk.MockKSettings.recordPrivateCalls
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

//configured and learned mockk
//https://mockk.io/#relaxed-mock
class UnitTestsUsingMockk {

    private lateinit var speedometerTests: Speedometer
    //@MockK private lateinit var context: Context

    @Before
    fun setUp() {
        //context = mockk<Context>() // mocked context
       speedometerTests= mockk<Speedometer>(relaxed = true) //simple mock of class with nulls
        //speedometerTests= spyk<Speedometer>(context) //mock and spy for class methods
        //speedometerTests = spyk(Speedometer(context), recordPrivateCalls = true) //mocked private functions
        //every { test1.setSpeed(150) } answers { callOriginal() } //call real method
        //every {classUnderTest.animatorRpm } returns mockk() //example of returning mock object on spy call
    }

    @Test
    fun setSpeedTest() {
        speedometerTests.setSpeed(150)
        //assertEquals(, 150) //getSpeed (need to define public method)
        verify(exactly = 1) { speedometerTests.setSpeed(any()) } //check calls number
    }

    @Test
    fun setEngineRpmTest() {
        speedometerTests.setEngineRpm(150F)
        verify(exactly = 1) { speedometerTests.setEngineRpm(any()) } //check calls number
    }
}