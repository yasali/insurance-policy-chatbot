package com.insurance.policies.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("Insurance Domain Model")
class InsuranceTest {

    private companion object {
        const val VALID_PERSONAL_NUMBER = "199001011234"
        const val VALID_ADDRESS = "Kungsgatan 16"
        const val VALID_POSTAL_CODE = "11135"
    }

    @Nested
    @DisplayName("Policy Timeline Management")
    inner class PolicyTimelineTests {

        @Test
        fun `should successfully add first policy to insurance`() {
            // Given
            val insurance = createInsurance()
            val policy = createPolicy(startDate = LocalDate.of(2024, 1, 1))

            // When
            insurance.addPolicy(policy)

            // Then
            assertEquals(1, insurance.policies.size)
            assertEquals(policy, insurance.policies.first())
            assertNull(policy.endDate, "First policy should have no end date")
        }

        @Test
        fun `should successfully add second policy and set end date of first policy`() {
            // Given
            val insurance = createInsurance()
            val firstPolicy = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            val secondPolicy = createPolicy(startDate = LocalDate.of(2024, 6, 1))

            // When
            insurance.addPolicy(firstPolicy)
            insurance.addPolicy(secondPolicy)

            // Then
            assertEquals(2, insurance.policies.size)
            assertEquals(LocalDate.of(2024, 5, 31), firstPolicy.endDate,
                "First policy end date should be day before second policy starts")
            assertNull(secondPolicy.endDate, "Second policy should have no end date")
        }

        @Test
        fun `should add multiple policies with correct end dates`() {
            // Given
            val insurance = createInsurance()
            val policy1 = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            val policy2 = createPolicy(startDate = LocalDate.of(2024, 3, 1))
            val policy3 = createPolicy(startDate = LocalDate.of(2024, 6, 1))

            // When
            insurance.addPolicy(policy1)
            insurance.addPolicy(policy2)
            insurance.addPolicy(policy3)

            // Then
            assertEquals(3, insurance.policies.size)
            assertEquals(LocalDate.of(2024, 2, 29), policy1.endDate) // 2024 is leap year
            assertEquals(LocalDate.of(2024, 5, 31), policy2.endDate)
            assertNull(policy3.endDate)
        }

        @Test
        fun `should throw exception when new policy start date equals last policy start date`() {
            // Given
            val insurance = createInsurance()
            val startDate = LocalDate.of(2024, 1, 1)
            val firstPolicy = createPolicy(startDate = startDate)
            val secondPolicy = createPolicy(startDate = startDate)

            insurance.addPolicy(firstPolicy)

            // When & Then
            val exception = assertThrows<IllegalStateException> {
                insurance.addPolicy(secondPolicy)
            }
            assertTrue(exception.message!!.contains("must start after"))
        }

        @Test
        fun `should throw exception when new policy start date is before last policy start date`() {
            // Given
            val insurance = createInsurance()
            val firstPolicy = createPolicy(startDate = LocalDate.of(2024, 6, 1))
            val secondPolicy = createPolicy(startDate = LocalDate.of(2024, 1, 1))

            insurance.addPolicy(firstPolicy)

            // When & Then
            val exception = assertThrows<IllegalStateException> {
                insurance.addPolicy(secondPolicy)
            }
            assertTrue(exception.message!!.contains("must start after"))
            assertTrue(exception.message!!.contains("2024-06-01"))
            assertTrue(exception.message!!.contains("2024-01-01"))
        }

        @Test
        fun `should not modify previous policy end date if it was already set`() {
            // Given
            val insurance = createInsurance()
            val firstPolicy = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            val secondPolicy = createPolicy(startDate = LocalDate.of(2024, 6, 1))

            insurance.addPolicy(firstPolicy)
            firstPolicy.endDate = LocalDate.of(2024, 3, 15) // Manually set end date

            // When
            insurance.addPolicy(secondPolicy)

            // Then
            assertEquals(LocalDate.of(2024, 3, 15), firstPolicy.endDate,
                "Should not override existing end date")
        }
    }

    @Nested
    @DisplayName("Query Policy by Date")
    inner class QueryPolicyTests {

        @Test
        fun `should return null when no policies exist`() {
            // Given
            val insurance = createInsurance()
            val queryDate = LocalDate.of(2024, 3, 15)

            // When
            val result = insurance.getPolicyAtDate(queryDate)

            // Then
            assertNull(result)
        }

        @Test
        fun `should return policy when date is on start date`() {
            // Given
            val insurance = createInsurance()
            val startDate = LocalDate.of(2024, 1, 1)
            val policy = createPolicy(startDate = startDate)
            insurance.addPolicy(policy)

            // When
            val result = insurance.getPolicyAtDate(startDate)

            // Then
            assertEquals(policy, result)
        }

        @Test
        fun `should return policy when date is on end date`() {
            // Given
            val insurance = createInsurance()
            val policy1 = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            val policy2 = createPolicy(startDate = LocalDate.of(2024, 6, 1))

            insurance.addPolicy(policy1)
            insurance.addPolicy(policy2)

            // When
            val result = insurance.getPolicyAtDate(LocalDate.of(2024, 5, 31))

            // Then
            assertEquals(policy1, result, "Should return policy on its end date")
        }

        @Test
        fun `should return policy when date is within range`() {
            // Given
            val insurance = createInsurance()
            val policy = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            insurance.addPolicy(policy)

            // When
            val result = insurance.getPolicyAtDate(LocalDate.of(2024, 3, 15))

            // Then
            assertEquals(policy, result)
        }

        @Test
        fun `should return null when date is before first policy`() {
            // Given
            val insurance = createInsurance()
            val policy = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            insurance.addPolicy(policy)

            // When
            val result = insurance.getPolicyAtDate(LocalDate.of(2023, 12, 31))

            // Then
            assertNull(result)
        }

        @Test
        fun `should return correct policy when multiple policies exist`() {
            // Given
            val insurance = createInsurance()
            val policy1 = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            val policy2 = createPolicy(startDate = LocalDate.of(2024, 4, 1))
            val policy3 = createPolicy(startDate = LocalDate.of(2024, 7, 1))

            insurance.addPolicy(policy1)
            insurance.addPolicy(policy2)
            insurance.addPolicy(policy3)

            // When & Then
            assertEquals(policy1, insurance.getPolicyAtDate(LocalDate.of(2024, 2, 15)))
            assertEquals(policy2, insurance.getPolicyAtDate(LocalDate.of(2024, 5, 15)))
            assertEquals(policy3, insurance.getPolicyAtDate(LocalDate.of(2024, 8, 15)))
        }

        @Test
        fun `should return null when date is after last policy end date`() {
            // Given
            val insurance = createInsurance()
            val policy1 = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            val policy2 = createPolicy(startDate = LocalDate.of(2024, 6, 1))

            insurance.addPolicy(policy1)
            insurance.addPolicy(policy2)

            // Manually set end date on current policy
            policy2.endDate = LocalDate.of(2024, 12, 31)

            // When
            val result = insurance.getPolicyAtDate(LocalDate.of(2025, 1, 1))

            // Then
            assertNull(result)
        }

        @Test
        fun `should handle boundary dates correctly between policies`() {
            // Given
            val insurance = createInsurance()
            val policy1 = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            val policy2 = createPolicy(startDate = LocalDate.of(2024, 6, 1))

            insurance.addPolicy(policy1)
            insurance.addPolicy(policy2)

            // When & Then
            assertEquals(policy1, insurance.getPolicyAtDate(LocalDate.of(2024, 5, 31)),
                "Last day of first policy")
            assertEquals(policy2, insurance.getPolicyAtDate(LocalDate.of(2024, 6, 1)),
                "First day of second policy")
        }
    }

    @Nested
    @DisplayName("Get Current Policy")
    inner class GetCurrentPolicyTests {

        @Test
        fun `should return null when no policies exist`() {
            // Given
            val insurance = createInsurance()

            // When
            val result = insurance.getCurrentPolicy()

            // Then
            assertNull(result)
        }

        @Test
        fun `should return policy when only one policy exists with no end date`() {
            // Given
            val insurance = createInsurance()
            val policy = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            insurance.addPolicy(policy)

            // When
            val result = insurance.getCurrentPolicy()

            // Then
            assertEquals(policy, result)
        }

        @Test
        fun `should return current policy when multiple policies exist`() {
            // Given
            val insurance = createInsurance()
            val policy1 = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            val policy2 = createPolicy(startDate = LocalDate.of(2024, 6, 1))

            insurance.addPolicy(policy1)
            insurance.addPolicy(policy2)

            // When
            val result = insurance.getCurrentPolicy()

            // Then
            assertEquals(policy2, result)
            assertNull(policy2.endDate)
            assertNotNull(policy1.endDate)
        }

        @Test
        fun `should return null when all policies have end dates`() {
            // Given
            val insurance = createInsurance()
            val policy = createPolicy(startDate = LocalDate.of(2024, 1, 1))
            insurance.addPolicy(policy)

            // Manually set end date
            policy.endDate = LocalDate.of(2024, 12, 31)

            // When
            val result = insurance.getCurrentPolicy()

            // Then
            assertNull(result)
        }
    }

    // Test helpers
    private fun createInsurance(
        personalNumber: String = VALID_PERSONAL_NUMBER
    ): Insurance {
        return Insurance(personalNumber = personalNumber)
    }

    private fun createPolicy(
        address: String = VALID_ADDRESS,
        postalCode: String = VALID_POSTAL_CODE,
        startDate: LocalDate
    ): Policy {
        return Policy(
            address = address,
            postalCode = postalCode,
            startDate = startDate
        )
    }
}
