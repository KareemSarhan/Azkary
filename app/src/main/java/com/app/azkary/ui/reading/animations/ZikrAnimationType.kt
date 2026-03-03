package com.app.azkary.ui.reading.animations

enum class ZikrAnimationType {
    NONE,
    TREE_GROWTH,
    STAR_TWINKLE,
    RAIN_MERCY,
    HEART_PULSE,
    LIGHT_GLOW
}

object ZikrAnimationRegistry {
    
    private val animationPatterns = mapOf(
        ZikrAnimationType.TREE_GROWTH to listOf(
            "سبحان الله وبحمده",
            "سبحان الله العظيم",
            "سبحان الله",
            "subhanallah",
            "glory"
        ),
        ZikrAnimationType.STAR_TWINKLE to listOf(
            "لا إله إلا الله",
            "الله أكبر",
            "la ilaha",
            "allahu akbar",
            "god is great"
        ),
        ZikrAnimationType.RAIN_MERCY to listOf(
            "أستغفر الله",
            "اللهم اغفر لي",
            "rahma",
            "mercy",
            "forgive",
            "maghfirah"
        ),
        ZikrAnimationType.HEART_PULSE to listOf(
            "اللهم حببني",
            "أحبك يا الله",
            "love",
            "hub",
            "heart"
        ),
        ZikrAnimationType.LIGHT_GLOW to listOf(
            "نور",
            "اللهم نور",
            "light",
            "nur",
            "guidance"
        )
    )
    
    fun getAnimationType(arabicText: String?, transliteration: String?): ZikrAnimationType {
        val combinedText = ((arabicText ?: "") + " " + (transliteration ?: "")).lowercase()
        
        for ((type, patterns) in animationPatterns) {
            for (pattern in patterns) {
                if (combinedText.contains(pattern.lowercase())) {
                    return type
                }
            }
        }
        
        return ZikrAnimationType.NONE
    }
}
