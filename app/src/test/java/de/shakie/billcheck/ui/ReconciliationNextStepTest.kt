package de.shakie.billcheck.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconciliationNextStepTest {
    @Test
    fun `statement image without extracted lines points to image analysis`() {
        assertEquals(
            ReconciliationNextStep.ANALYZE_IMAGE,
            reconciliationNextStep(hasImage = true, statementLineCount = 0, analysisUpdatedAt = null),
        )
    }

    @Test
    fun `extracted lines without reconciliation result point to reconciliation`() {
        assertEquals(
            ReconciliationNextStep.RUN_RECONCILIATION,
            reconciliationNextStep(hasImage = true, statementLineCount = 11, analysisUpdatedAt = null),
        )
    }

    @Test
    fun `completed reconciliation does not keep pulsing`() {
        assertEquals(
            ReconciliationNextStep.NONE,
            reconciliationNextStep(hasImage = true, statementLineCount = 11, analysisUpdatedAt = 1L),
        )
    }

    @Test
    fun `empty reconciliation without an image has no misleading next action`() {
        assertEquals(
            ReconciliationNextStep.NONE,
            reconciliationNextStep(hasImage = false, statementLineCount = 0, analysisUpdatedAt = null),
        )
    }
}
