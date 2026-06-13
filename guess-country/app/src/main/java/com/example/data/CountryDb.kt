package com.example.data

object CountryDb {
    val countries = listOf(
        "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua and Barbuda",
        "Argentina", "Armenia", "Australia", "Austria", "Azerbaijan", "Bahamas", "Bahrain",
        "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan", "Bolivia",
        "Bosnia and Herzegovina", "Botswana", "Brazil", "Brunei", "Bulgaria", "Burkina Faso",
        "Burundi", "Cabo Verde", "Cambodia", "Cameroon", "Canada", "Central African Republic",
        "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Costa Rica", "Croatia",
        "Cuba", "Cyprus", "Czechia", "Denmark", "Djibouti", "Dominica", "Dominican Republic",
        "Ecuador", "Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia",
        "Eswatini", "Ethiopia", "Fiji", "Finland", "France", "Gabon", "Gambia", "Georgia",
        "Germany", "Ghana", "Greece", "Grenada", "Guatemala", "Guinea", "Guinea-Bissau",
        "Guyana", "Haiti", "Honduras", "Hungary", "Iceland", "India", "Indonesia", "Iran",
        "Iraq", "Ireland", "Israel", "Italy", "Jamaica", "Japan", "Jordan", "Kazakhstan",
        "Kenya", "Kiribati", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho",
        "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg", "Madagascar", "Malawi",
        "Malaysia", "Maldives", "Mali", "Malta", "Marshall Islands", "Mauritania", "Mauritius",
        "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco",
        "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand",
        "Nicaragua", "Niger", "Nigeria", "North Korea", "North Macedonia", "Norway", "Oman",
        "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea", "Paraguay", "Peru",
        "Philippines", "Poland", "Portugal", "Qatar", "Romania", "Russia", "Rwanda",
        "Saint Kitts and Nevis", "Saint Lucia", "Saint Vincent and the Grenadines", "Samoa",
        "San Marino", "Sao Tome and Principe", "Saudi Arabia", "Senegal", "Serbia", "Seychelles",
        "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia",
        "South Africa", "South Korea", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname",
        "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand",
        "Timor-Leste", "Togo", "Tonga", "Trinidad and Tobago", "Tunisia", "Turkey", "Turkmenistan",
        "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States",
        "Uruguay", "Uzbekistan", "Vanuatu", "Vatican City", "Venezuela", "Vietnam", "Yemen",
        "Zambia", "Zimbabwe"
    )

    /**
     * Determines whether a country name matches any in the database.
     * Returns:
     * - ValidationResult.Exact: exact matching (case-insensitive)
     * - ValidationResult.SpellingError: close match found (e.g. within levenshtein threshold)
     * - ValidationResult.NotFound: no match found at all
     */
    fun validateCountry(input: String): ValidationResult {
        val trimmed = input.trim().lowercase()
        if (trimmed.isEmpty()) return ValidationResult.NotFound

        // 1. Check exact match (ignoring case)
        val exactMatch = countries.firstOrNull { it.trim().lowercase() == trimmed }
        if (exactMatch != null) {
            return ValidationResult.Exact(exactMatch)
        }

        // 2. Perform spell check / fuzzy match
        var bestMatch: String? = null
        var lowestDistance = Int.MAX_VALUE

        for (country in countries) {
            val target = country.trim().lowercase()
            val distance = levenshtein(trimmed, target)
            val maxLen = maxOf(trimmed.length, target.length)
            
            // We want a high similarity (distance < 30% of max length) for spelling errors,
            // otherwise it's just not found.
            val threshold = (maxLen * 0.35f).toInt().coerceAtLeast(1)
            
            if (distance <= threshold && distance < lowestDistance) {
                lowestDistance = distance
                bestMatch = country
            }
        }

        return if (bestMatch != null) {
            ValidationResult.SpellingError(bestMatch)
        } else {
            ValidationResult.NotFound
        }
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,        // Deletion
                    dp[i][j - 1] + 1,        // Insertion
                    dp[i - 1][j - 1] + cost  // Substitution
                )
            }
        }
        return dp[len1][len2]
    }

    sealed interface ValidationResult {
        data class Exact(val actualName: String) : ValidationResult
        data class SpellingError(val suggestedName: String) : ValidationResult
        object NotFound : ValidationResult
    }
}
