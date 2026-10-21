/*
 * Copyright (c) 2012-2026 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core

import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Test

class TestAcc {
    val dpc1input= "dpc1,0,eyJkcGNfcmVxdWVzdCI6eyJ2ZXIiOiIxLjAiLCJjb3JyZWxhdGlvbl9pZCI6ImRlYjQ0NDZmLTIwODYtNGFmNS1hYTZkLWFjNDIyODVlZjNmZSIsInRpbWVzdGFtcCI6IkZyaSBKdWwgMTcgMTQ6MDg6MDIuMzk1IDIwMjYiLCJjbGllbnRfaW5mbyI6dHJ1ZX19"


    @Test
    fun testParseManagementAccMessage()
    {
        val r = AppCustomControl.parseAccMessage(dpc1input)

        Assert.assertEquals("dpc1", r.protocol)
        Assert.assertFalse(r.fragment)
        val msg:String = String(r.message)

        val expectedMsg = "{\"dpc_request\":{\"ver\":\"1.0\",\"correlation_id\":\"deb4446f-2086-4af5-aa6d-ac42285ef3fe\",\"timestamp\":\"Fri Jul 17 14:08:02.395 2026\",\"client_info\":true}}"
        Assert.assertEquals(msg, expectedMsg)


        assertThrows(IllegalArgumentException::class.java)
        {
            val input= "dpc1,eyJkcGNfcmVx"
            val r = AppCustomControl.parseAccMessage(input)
        }

        val input_hello = "firstwords,1,aGVsbG8="
        val input_hello2 = "firstwords,0,IHdvcmxk"

        val h = AppCustomControl.parseAccMessage(input_hello)
        val h2 = AppCustomControl.parseAccMessage(input_hello2)

        Assert.assertTrue(h.fragment)
        Assert.assertFalse(h2.fragment)

        Assert.assertEquals("firstwords", h.protocol)
        Assert.assertEquals("firstwords", h2.protocol)

        Assert.assertArrayEquals(h.message, "hello".toByteArray())
        Assert.assertArrayEquals(h2.message, " world".toByteArray())
    }

    @Test
    fun dpc1command()
    {
        val msg = AppCustomControl.parseAccMessage(dpc1input)
        val dpc1 = DPC1Protocol()

        val response = dpc1.processMessage(msg)

        Assert.assertNotNull(response)

        val r = response!!

        Assert.assertEquals( "dpc1", r.protocol)
        Assert.assertFalse(r.fragment)

        val jsonPayload = String(r.message)
        Assert.assertEquals("""{"dpc_response":{"client_info":{"os":{"extra":{"arch":"unknown","core_ver":0},"type":"Android"}},"ver":"1.0"}}""", jsonPayload)
    }
}