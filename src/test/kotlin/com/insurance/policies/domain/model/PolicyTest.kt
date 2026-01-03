package com.insurance.policies.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Policy Domain Model")
class PolicyTest {

    private companion object {
        const val VALID_ADDRESS = "Kungsgatan 16"
        const val VALID_POSTAL_CODE = "11135"
    }

    @Nested
    @DisplayName("isActiveAt() - Date Range Queries")
    inner class IsActiveAtTests {

        @Test
        fun `should return true when date equals start date`() {
            // Given
            val startDate = LocalDate.of(2024, 1, 1)
            val policy = createPolicy(startDate = startDate, endDate = null)

            // When
            val result = policy.isActiveAt(startDate)

            // Then
            assertTrue(result, "Policy should be active on its start date")
        }

        @Test
        fun `should return true when date is after start date with no end date`() {
            // Given
            val startDate = LocalDate.of(2024, 1, 1)
            val queryDate = LocalDate.of(2024, 6, 15)
            val policy = createPolicy(startDate = startDate, endDate = null)

            // When
            val result = policy.isActiveAt(queryDate)

            // Then
            assertTrue(result, "Policy with no end date should be active for any future date")
        }

        @Test
        fun `should return false when date is before start date`() {
            // Given
            val startDate = LocalDate.of(2024, 1, 1)
            val queryDate = LocalDate.of(2023, 12, 31)
            val policy = createPolicy(startDate = startDate, endDate = null)

            // When
            val result = policy.isActiveAt(queryDate)

            // Then
            assertFalse(result, "Policy should not be active before its start date")
        }

        @Test
        fun `should return true when date equals end date`() {
            // Given
            val startDate = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 12, 31)
            val policy = createPolicy(startDate = startDate, endDate = endDate)

            // When
            val result = policy.isActiveAt(endDate)

            // Then
            assertTrue(result, "Policy should be active on its end date (inclusive)")
        }

        @Test
        fun `should return true when date is between start and end date`() {
            // Given
            val startDate = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 12, 31)
            val queryDate = LocalDate.of(2024, 6, 15)
            val policy = createPolicy(startDate = startDate, endDate = endDate)

            // When
            val result = policy.isActiveAt(queryDate)

            // Then
            assertTrue(result, "Policy should be active within its date range")
        }

        @Test
        fun `should return false when date is after end date`() {
            // Given
            val startDate = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 12, 31)
            val queryDate = LocalDate.of(2025, 1, 1)
            val policy = createPolicy(startDate = startDate, endDate = endDate)

            // When
            val result = policy.isActiveAt(queryDate)

            // Then
            assertFalse(result, "Policy should not be active after its end date")
        }

        @Test
        fun `should handle single day policy correctly`() {
            // Given
            val singleDate = LocalDate.of(2024, 1, 1)
            val policy = createPolicy(startDate = singleDate, endDate = singleDate)

            // When & Then
            assertTrue(policy.isActiveAt(singleDate), "Single day policy should be active on that day")
            assertFalse(policy.isActiveAt(singleDate.minusDays(1)), "Should not be active before")
            assertFalse(policy.isActiveAt(singleDate.plusDays(1)), "Should not be active after")
        }

        @Test
        fun `should handle leap year dates correctly`() {
            // Given - 2024 is a leap year
            val startDate = LocalDate.of(2024, 2, 28)
            val endDate = LocalDate.of(2024, 3, 1)
            val policy = createPolicy(startDate = startDate, endDate = endDate)

            // When & Then
            assertTrue(policy.isActiveAt(LocalDate.of(2024, 2, 28)))
            assertTrue(policy.isActiveAt(LocalDate.of(2024, 2, 29)), "Should handle leap day")
            assertTrue(policy.isActiveAt(LocalDate.of(2024, 3, 1)))
        }

        @Test
        fun `should handle year boundary correctly`() {
            // Given
            val startDate = LocalDate.of(2023, 12, 31)
            val endDate = LocalDate.of(2024, 1, 1)
            val policy = createPolicy(startDate = startDate, endDate = endDate)

            // When & Then
            assertTrue(policy.isActiveAt(LocalDate.of(2023, 12, 31)))
            assertTrue(policy.isActiveAt(LocalDate.of(2024, 1, 1)))
            assertFalse(policy.isActiveAt(LocalDate.of(2023, 12, 30)))
            assertFalse(policy.isActiveAt(LocalDate.of(2024, 1, 2)))
        }
    }

    @Nested
    @DisplayName("isCurrent() - Current Policy Check")
    inner class IsCurrentTests {

        @Test
        fun `should return true when end date is null`() {
            // Given
            val policy = createPolicy(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = null
            )

            // When
            val result = policy.isCurrent()

            // Then
            assertTrue(result, "Policy with no end date should be current")
        }

        @Test
        fun `should return false when end date is set`() {
            // Given
            val policy = createPolicy(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31)
            )

            // When
            val result = policy.isCurrent()

            // Then
            assertFalse(result, "Policy with end date should not be current")
        }

        @Test
        fun `should return false when end date is in the future`() {
            // Given
            val policy = createPolicy(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2099, 12, 31) // Far future
            )

            // When
            val result = policy.isCurrent()

            // Then
            assertFalse(result, "Policy with any end date should not be current, regardless of when")
        }

        @Test
        fun `should return false when end date is in the past`() {
            // Given
            val policy = createPolicy(
                startDate = LocalDate.of(2020, 1, 1),
                endDate = LocalDate.of(2020, 12, 31)
            )

            // When
            val result = policy.isCurrent()

            // Then
            assertFalse(result, "Expired policy should not be current")
        }

        @Test
        fun `should return false when end date is today`() {
            // Given
            val today = LocalDate.now()
            val policy = createPolicy(
                startDate = today.minusMonths(6),
                endDate = today
            )

            // When
            val result = policy.isCurrent()

            // Then
            assertFalse(result, "Policy ending today should not be current (endDate is set)")
        }
    }

    @Nested
    @DisplayName("Policy State Transitions")
    inner class PolicyStateTests {

        @Test
        fun `should transition from current to non-current when end date is set`() {
            // Given
            val policy = createPolicy(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = null
            )
            assertTrue(policy.isCurrent(), "Should start as current")

            // When
            policy.endDate = LocalDate.of(2024, 12, 31)

            // Then
            assertFalse(policy.isCurrent(), "Should no longer be current after end date is set")
        }

        @Test
        fun `should maintain active status after setting end date within query range`() {
            // Given
            val policy = createPolicy(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = null
            )
            val queryDate = LocalDate.of(2024, 6, 15)

            assertTrue(policy.isActiveAt(queryDate), "Should be active before setting end date")

            // When
            policy.endDate = LocalDate.of(2024, 12, 31)

            // Then
            assertTrue(policy.isActiveAt(queryDate), "Should still be active at query date")
        }

        @Test
        fun `should become inactive after setting end date before query range`() {
            // Given
            val policy = createPolicy(
                startDate = LocalDate.of(2024, 1, 1),
                endDate = null
            )
            val queryDate = LocalDate.of(2024, 12, 15)

            assertTrue(policy.isActiveAt(queryDate), "Should be active before setting end date")

            // When
            policy.endDate = LocalDate.of(2024, 6, 30)

            // Then
            assertFalse(policy.isActiveAt(queryDate), "Should not be active after end date")
        }
    }

    // Test helpers
    private fun createPolicy(
        address: String = VALID_ADDRESS,
        postalCode: String = VALID_POSTAL_CODE,
        startDate: LocalDate,
        endDate: LocalDate? = null
    ): Policy {
        return Policy(
            address = address,
            postalCode = postalCode,
            startDate = startDate,
            endDate = endDate
        )
    }
}
