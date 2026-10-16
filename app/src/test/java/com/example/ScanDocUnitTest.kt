package com.example

import com.example.data.model.DocumentEntity
import com.example.data.model.PageEntity
import com.example.util.AdMobConfig
import com.example.util.PdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDocUnitTest {

    @Test
    fun testDocumentEntityCreation() {
        val doc = DocumentEntity(
            id = 1,
            title = "Hợp đồng kinh tế",
            pageCount = 3,
            pageSizePreference = "A4"
        )
        assertEquals("Hợp đồng kinh tế", doc.title)
        assertEquals(3, doc.pageCount)
        assertEquals("A4", doc.pageSizePreference)
    }

    @Test
    fun testPageEntityCreation() {
        val page = PageEntity(
            id = 10,
            documentId = 1,
            pageNumber = 1,
            originalImagePath = "/path/orig.jpg",
            croppedImagePath = "/path/crop.jpg",
            filterType = "DOCUMENT"
        )
        assertEquals(1, page.pageNumber)
        assertEquals("DOCUMENT", page.filterType)
    }

    @Test
    fun testPdfPageSizes() {
        assertEquals(595, PdfGenerator.PageSize.A4.widthPt)
        assertEquals(842, PdfGenerator.PageSize.A4.heightPt)
        assertEquals(420, PdfGenerator.PageSize.A5.widthPt)
        assertEquals(595, PdfGenerator.PageSize.A5.heightPt)
    }

    @Test
    fun testAdMobConfiguration() {
        assertTrue(AdMobConfig.IS_TEST_MODE)
        assertNotNull(AdMobConfig.BANNER_AD_UNIT_ID)
        assertNotNull(AdMobConfig.INTERSTITIAL_AD_UNIT_ID)
        assertNotNull(AdMobConfig.REWARDED_AD_UNIT_ID)
    }
}
