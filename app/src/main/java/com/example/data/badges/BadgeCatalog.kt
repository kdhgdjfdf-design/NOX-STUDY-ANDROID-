package com.example.data.badges

enum class BadgeShape {
    STAR,
    SHIELD,
    CROWN,
    MEDAL,
    FLAME,
    LOTUS
}

enum class BadgeMetricType {
    COMPLETED_SESSIONS,
    TOTAL_FOCUS_MINUTES,
    DISTINCT_STUDY_DAYS,
    COMPLETED_TASKS,
    FOCUS_BREAK_CYCLES,
    FLASHCARD_REVIEWS,
    REVIEWED_LESSONS
}

data class BadgeDefinition(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val criteriaAr: String,
    val shape: BadgeShape,
    val metricType: BadgeMetricType,
    val targetValue: Int,
    val unitAr: String,
    val accentHex: Long,
    val secondaryHex: Long
)

data class BadgeUiModel(
    val definition: BadgeDefinition,
    val isUnlocked: Boolean,
    val unlockedAt: Long?,
    val hasCelebrated: Boolean,
    val currentValue: Int,
    val progressFraction: Float
)

object BadgeCatalog {

    /**
     * Extensible badge registry. To add a new badge in the future, append a BadgeDefinition here
     * (and add a BadgeMetricType if a brand-new metric is needed).
     */
    val allBadges: List<BadgeDefinition> = listOf(
        BadgeDefinition(
            id = "first_step",
            titleAr = "البداية القوية",
            descriptionAr = "خطوتك الأولى نحو تركيز عميق ومنظم دون مشتتات.",
            criteriaAr = "إتمام أول جلسة تركيز مكتملة",
            shape = BadgeShape.STAR,
            metricType = BadgeMetricType.COMPLETED_SESSIONS,
            targetValue = 1,
            unitAr = "جلسة",
            accentHex = 0xFFD99B26,
            secondaryHex = 0xFFF3C79A
        ),
        BadgeDefinition(
            id = "disciplined_mind",
            titleAr = "عقل منضبط",
            descriptionAr = "الانتظام يصنع الفارق عندما يتحول التركيز إلى عادة.",
            criteriaAr = "إتمام 5 جلسات تركيز مكتملة",
            shape = BadgeShape.SHIELD,
            metricType = BadgeMetricType.COMPLETED_SESSIONS,
            targetValue = 5,
            unitAr = "جلسات",
            accentHex = 0xFF4A7C9B,
            secondaryHex = 0xFFB8D4E6
        ),
        BadgeDefinition(
            id = "focus_master",
            titleAr = "سيد التركيز",
            descriptionAr = "أداء رفيع يعكس قدرة حقيقية على الغوص في الدراسة.",
            criteriaAr = "إتمام 15 جلسة تركيز مكتملة",
            shape = BadgeShape.CROWN,
            metricType = BadgeMetricType.COMPLETED_SESSIONS,
            targetValue = 15,
            unitAr = "جلسة",
            accentHex = 0xFFC88A1E,
            secondaryHex = 0xFFFFDF8E
        ),
        BadgeDefinition(
            id = "deep_researcher",
            titleAr = "باحث الأعماق",
            descriptionAr = "ساعات من الصفاء الذهني والمثابرة العلمية.",
            criteriaAr = "تحقيق 180 دقيقة تركيز فعلية",
            shape = BadgeShape.MEDAL,
            metricType = BadgeMetricType.TOTAL_FOCUS_MINUTES,
            targetValue = 180,
            unitAr = "دقيقة",
            accentHex = 0xFF5F6B55,
            secondaryHex = 0xFFC5D4B8
        ),
        BadgeDefinition(
            id = "continuity_flame",
            titleAr = "الاستمرارية",
            descriptionAr = "الاستمرار في الأيام المتفرقة يثبّت المعرفة ويقوّي الإرادة.",
            criteriaAr = "الدراسة بتركيز في 3 أيام مختلفة",
            shape = BadgeShape.FLAME,
            metricType = BadgeMetricType.DISTINCT_STUDY_DAYS,
            targetValue = 3,
            unitAr = "أيام",
            accentHex = 0xFFB86D49,
            secondaryHex = 0xFFF2C2AB
        ),
        BadgeDefinition(
            id = "milestone_streak",
            titleAr = "ثبات العزيمة",
            descriptionAr = "أسبوع كامل من الالتزام الدراسي الهادئ والمتوازن.",
            criteriaAr = "الدراسة بتركيز في 7 أيام مختلفة",
            shape = BadgeShape.SHIELD,
            metricType = BadgeMetricType.DISTINCT_STUDY_DAYS,
            targetValue = 7,
            unitAr = "أيام",
            accentHex = 0xFF7B6399,
            secondaryHex = 0xFFD4C5E8
        ),
        BadgeDefinition(
            id = "task_conqueror",
            titleAr = "إنجاز وراء إنجاز",
            descriptionAr = "تحويل الخطط والواجبات إلى أهداف منجزة واحدة تلو الأخرى.",
            criteriaAr = "إتمام 5 مهام أو واجبات دراسية",
            shape = BadgeShape.MEDAL,
            metricType = BadgeMetricType.COMPLETED_TASKS,
            targetValue = 5,
            unitAr = "مهام",
            accentHex = 0xFF427A4E,
            secondaryHex = 0xFFB4DFC0
        ),
        BadgeDefinition(
            id = "quiet_warrior",
            titleAr = "المحارب الهادئ",
            descriptionAr = "التوازن بين الجهد والراحة هو سر الإنجاز المستدام.",
            criteriaAr = "إكمال 3 دورات تركيز + استراحة كاملة",
            shape = BadgeShape.LOTUS,
            metricType = BadgeMetricType.FOCUS_BREAK_CYCLES,
            targetValue = 3,
            unitAr = "دورات",
            accentHex = 0xFF3E5E7E,
            secondaryHex = 0xFFBACFE3
        ),
        BadgeDefinition(
            id = "active_memory",
            titleAr = "ذاكرة نشطة",
            descriptionAr = "الاسترجاع النشط يرسّخ المعلومات في الذاكرة طويلة المدى.",
            criteriaAr = "مراجعة وتقييم 10 بطاقات مراجعة",
            shape = BadgeShape.STAR,
            metricType = BadgeMetricType.FLASHCARD_REVIEWS,
            targetValue = 10,
            unitAr = "بطاقات",
            accentHex = 0xFF8A5836,
            secondaryHex = 0xFFE5C4AB
        ),
        BadgeDefinition(
            id = "revision_keeper",
            titleAr = "الملتزم بالمراجعة",
            descriptionAr = "العودة للدروس ومراجعتها تصنع إتقاناً لا يُنسى.",
            criteriaAr = "وضع علامة «تمت مراجعته» على 5 دروس",
            shape = BadgeShape.CROWN,
            metricType = BadgeMetricType.REVIEWED_LESSONS,
            targetValue = 5,
            unitAr = "دروس",
            accentHex = 0xFF6B5A82,
            secondaryHex = 0xFFC9BEDB
        )
    )

    fun findById(id: String): BadgeDefinition? = allBadges.find { it.id == id }
}
