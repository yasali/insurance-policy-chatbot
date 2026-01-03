package com.insurance.policies.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("Claim Domain Model")
class ClaimTest {

    private companion object {
        const val VALID_PERSONAL_NUMBER = "199001011234"
        const val VALID_ADDRESS = "Kungsgatan 16"
        const val VALID_POSTAL_CODE = "11135"
    }

    @Nested
    @DisplayName("State Machine - extract()")
    inner class ExtractTests {

        @Test
        fun `should successfully extract data from pending claim`() {
            // Given
            val claim = createClaim()
            val claimData = createValidClaimData()
            val confidence = 0.85

            assertEquals(ClaimStatus.PENDING, claim.status)
            assertNull(claim.extractedData)
            assertNull(claim.confidence)

            // When
            claim.extract(claimData, confidence)

            // Then
            assertEquals(ClaimStatus.EXTRACTED, claim.status)
            assertEquals(claimData, claim.extractedData)
            assertEquals(0.85, claim.confidence)
        }

        @Test
        fun `should allow confidence of 0_0`() {
            // Given
            val claim = createClaim()
            val claimData = createValidClaimData()

            // When
            claim.extract(claimData, 0.0)

            // Then
            assertEquals(ClaimStatus.EXTRACTED, claim.status)
            assertEquals(0.0, claim.confidence)
        }

        @Test
        fun `should allow confidence of 1_0`() {
            // Given
            val claim = createClaim()
            val claimData = createValidClaimData()

            // When
            claim.extract(claimData, 1.0)

            // Then
            assertEquals(ClaimStatus.EXTRACTED, claim.status)
            assertEquals(1.0, claim.confidence)
        }

        @Test
        fun `should throw exception when confidence is negative`() {
            // Given
            val claim = createClaim()
            val claimData = createValidClaimData()

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                claim.extract(claimData, -0.1)
            }
            assertTrue(exception.message!!.contains("between 0.0 and 1.0"))
        }

        @Test
        fun `should throw exception when confidence exceeds 1_0`() {
            // Given
            val claim = createClaim()
            val claimData = createValidClaimData()

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                claim.extract(claimData, 1.1)
            }
            assertTrue(exception.message!!.contains("between 0.0 and 1.0"))
        }

        @Test
        fun `should allow reextraction on already extracted claim`() {
            // Given
            val claim = createClaim()
            val firstData = createValidClaimData(damageType = "Water damage")
            val secondData = createValidClaimData(damageType = "Fire damage")

            claim.extract(firstData, 0.5)

            // When
            claim.extract(secondData, 0.9)

            // Then
            assertEquals(ClaimStatus.EXTRACTED, claim.status)
            assertEquals(secondData, claim.extractedData)
            assertEquals(0.9, claim.confidence)
        }

        @Test
        fun `should preserve claim status as EXTRACTED when reextracting`() {
            // Given
            val claim = createClaim()
            claim.extract(createValidClaimData(), 0.5)

            assertEquals(ClaimStatus.EXTRACTED, claim.status)

            // When
            claim.extract(createValidClaimData(), 0.8)

            // Then
            assertEquals(ClaimStatus.EXTRACTED, claim.status, "Should remain EXTRACTED")
        }
    }

    @Nested
    @DisplayName("State Machine - validate()")
    inner class ValidateTests {

        @Test
        fun `should successfully validate extracted claim`() {
            // Given
            val claim = createClaim()
            claim.extract(createValidClaimData(), 0.85)

            assertEquals(ClaimStatus.EXTRACTED, claim.status)

            // When
            claim.validate()

            // Then
            assertEquals(ClaimStatus.VALIDATED, claim.status)
        }

        @Test
        fun `should throw exception when validating pending claim`() {
            // Given
            val claim = createClaim()
            assertEquals(ClaimStatus.PENDING, claim.status)

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                claim.validate()
            }
            assertTrue(exception.message!!.contains("must be extracted before validation"))
        }

        @Test
        fun `should throw exception when validating rejected claim`() {
            // Given
            val claim = createClaim()
            claim.reject()

            assertEquals(ClaimStatus.REJECTED, claim.status)

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                claim.validate()
            }
            assertTrue(exception.message!!.contains("must be extracted before validation"))
        }

        @Test
        fun `should throw exception when validating already validated claim`() {
            // Given
            val claim = createClaim()
            claim.extract(createValidClaimData(), 0.85)
            claim.validate()

            assertEquals(ClaimStatus.VALIDATED, claim.status)

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                claim.validate()
            }
            assertTrue(exception.message!!.contains("must be extracted before validation"))
        }
    }

    @Nested
    @DisplayName("State Machine - reject()")
    inner class RejectTests {

        @Test
        fun `should successfully reject pending claim`() {
            // Given
            val claim = createClaim()
            assertEquals(ClaimStatus.PENDING, claim.status)

            // When
            claim.reject()

            // Then
            assertEquals(ClaimStatus.REJECTED, claim.status)
        }

        @Test
        fun `should successfully reject extracted claim`() {
            // Given
            val claim = createClaim()
            claim.extract(createValidClaimData(), 0.85)

            assertEquals(ClaimStatus.EXTRACTED, claim.status)

            // When
            claim.reject()

            // Then
            assertEquals(ClaimStatus.REJECTED, claim.status)
        }

        @Test
        fun `should successfully reject validated claim`() {
            // Given
            val claim = createClaim()
            claim.extract(createValidClaimData(), 0.85)
            claim.validate()

            assertEquals(ClaimStatus.VALIDATED, claim.status)

            // When
            claim.reject()

            // Then
            assertEquals(ClaimStatus.REJECTED, claim.status)
        }

        @Test
        fun `should allow rejecting already rejected claim`() {
            // Given
            val claim = createClaim()
            claim.reject()

            assertEquals(ClaimStatus.REJECTED, claim.status)

            // When
            claim.reject()

            // Then
            assertEquals(ClaimStatus.REJECTED, claim.status)
        }
    }

    @Nested
    @DisplayName("ClaimData Validation")
    inner class ClaimDataValidationTests {

        @Test
        fun `should create valid ClaimData with all fields`() {
            // Given & When
            val claimData = ClaimData(
                damageType = "Water damage",
                location = "Kitchen",
                incidentDate = LocalDate.of(2024, 1, 15),
                estimatedCost = 5000.0,
                description = "Pipe burst under sink",
                additionalDetails = mapOf("urgency" to "high")
            )

            // Then
            assertEquals("Water damage", claimData.damageType)
            assertEquals("Kitchen", claimData.location)
            assertEquals(LocalDate.of(2024, 1, 15), claimData.incidentDate)
            assertEquals(5000.0, claimData.estimatedCost)
            assertEquals("Pipe burst under sink", claimData.description)
            assertEquals(mapOf("urgency" to "high"), claimData.additionalDetails)
        }

        @Test
        fun `should create valid ClaimData with only required fields`() {
            // Given & When
            val claimData = ClaimData(
                damageType = "Fire damage",
                location = "Living room",
                description = "Small electrical fire"
            )

            // Then
            assertEquals("Fire damage", claimData.damageType)
            assertEquals("Living room", claimData.location)
            assertEquals("Small electrical fire", claimData.description)
            assertNull(claimData.incidentDate)
            assertNull(claimData.estimatedCost)
            assertNull(claimData.additionalDetails)
        }

        @Test
        fun `should throw exception when damageType is blank`() {
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                ClaimData(
                    damageType = "   ",
                    location = "Kitchen",
                    description = "Damage occurred"
                )
            }
            assertTrue(exception.message!!.contains("Damage type cannot be blank"))
        }

        @Test
        fun `should throw exception when location is blank`() {
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                ClaimData(
                    damageType = "Water damage",
                    location = "",
                    description = "Damage occurred"
                )
            }
            assertTrue(exception.message!!.contains("Location cannot be blank"))
        }

        @Test
        fun `should throw exception when description is blank`() {
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                ClaimData(
                    damageType = "Water damage",
                    location = "Kitchen",
                    description = "  "
                )
            }
            assertTrue(exception.message!!.contains("Description cannot be blank"))
        }

        @Test
        fun `should throw exception when estimatedCost is negative`() {
            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                ClaimData(
                    damageType = "Water damage",
                    location = "Kitchen",
                    description = "Damage occurred",
                    estimatedCost = -100.0
                )
            }
            assertTrue(exception.message!!.contains("Estimated cost cannot be negative"))
        }

        @Test
        fun `should allow estimatedCost of zero`() {
            // Given & When
            val claimData = ClaimData(
                damageType = "Minor damage",
                location = "Hallway",
                description = "No cost",
                estimatedCost = 0.0
            )

            // Then
            assertEquals(0.0, claimData.estimatedCost)
        }

        @Test
        fun `should handle large estimatedCost values`() {
            // Given & When
            val claimData = ClaimData(
                damageType = "Total loss",
                location = "Entire house",
                description = "Major structural damage",
                estimatedCost = 999999.99
            )

            // Then
            assertEquals(999999.99, claimData.estimatedCost)
        }
    }

    // Test helpers
    private fun createClaim(rawText: String = "I dropped my phone and the screen broke"): Claim {
        val policy = createPolicy()
        return Claim(
            policy = policy,
            rawText = rawText
        )
    }

    private fun createPolicy(): Policy {
        val insurance = Insurance(personalNumber = VALID_PERSONAL_NUMBER)
        val policy = Policy(
            address = VALID_ADDRESS,
            postalCode = VALID_POSTAL_CODE,
            startDate = LocalDate.of(2024, 1, 1)
        )
        insurance.addPolicy(policy)
        return policy
    }

    private fun createValidClaimData(
        damageType: String = "Water damage",
        location: String = "Kitchen",
        incidentDate: LocalDate? = LocalDate.of(2024, 1, 15),
        estimatedCost: Double? = 5000.0,
        description: String = "Pipe burst under sink"
    ): ClaimData {
        return ClaimData(
            damageType = damageType,
            location = location,
            incidentDate = incidentDate,
            estimatedCost = estimatedCost,
            description = description
        )
    }
}
