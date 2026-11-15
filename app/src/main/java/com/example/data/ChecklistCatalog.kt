package com.example.data

import com.example.model.ChecklistQuestionTemplate
import com.example.model.EquipmentCategory
import com.example.model.InspectionItem

/**
 * Modular catalog of playground equipment sections and inspection questions.
 *
 * Designed to be generic across play areas (no colour-specific swing references,
 * and structural/operational checks separated from lubrication/greasing checks).
 * Individual sections and questions can be toggled or extended in Settings.
 */
object ChecklistCatalog {

    val weatherPresets: List<String> = listOf(
        "Dry / Clear",
        "Overcast",
        "Wet / Rainy",
        "Windy",
        "Frost / Icy",
        "Showers",
    )

    val categories: List<EquipmentCategory> = listOf(
        EquipmentCategory(
            id = "general_site",
            sectionNumber = 1,
            title = "General Site & Ancillary Audit",
            subtitle = "Access, signage, litter, grounds, boundary fencing, seating, hardware wear, and lubrication",
            isCoreSiteSection = true,
            questions = listOf(
                ChecklistQuestionTemplate(
                    id = "gen_01",
                    categoryId = "general_site",
                    referenceCode = "1.1",
                    questionText = "Is site access clear and unobstructed?",
                ),
                ChecklistQuestionTemplate(
                    id = "gen_02",
                    categoryId = "general_site",
                    referenceCode = "1.2",
                    questionText = "Are safety and contact notices present and fully legible?",
                ),
                ChecklistQuestionTemplate(
                    id = "gen_03",
                    categoryId = "general_site",
                    referenceCode = "1.3",
                    questionText = "Is the site clean, tidy, and free of litter, glass, or hazards?",
                ),
                ChecklistQuestionTemplate(
                    id = "gen_04",
                    categoryId = "general_site",
                    referenceCode = "1.4",
                    questionText = "Is grass and surrounding vegetation adequately maintained?",
                ),
                ChecklistQuestionTemplate(
                    id = "gen_05",
                    categoryId = "general_site",
                    referenceCode = "1.5",
                    questionText = "Are boundary fencing, gates, and self-closing latches secure and functional?",
                ),
                ChecklistQuestionTemplate(
                    id = "gen_06",
                    categoryId = "general_site",
                    referenceCode = "1.6",
                    questionText = "Are benches and seating secure and in good repair?",
                ),
                ChecklistQuestionTemplate(
                    id = "gen_07",
                    categoryId = "general_site",
                    referenceCode = "1.7",
                    questionText = "Are shackles, chains, and moving pivots in good repair without excessive wear?",
                ),
                ChecklistQuestionTemplate(
                    id = "gen_08",
                    categoryId = "general_site",
                    referenceCode = "1.8",
                    questionText = "Are bearings and moving pivots adequately oiled/greased?",
                ),
            ),
        ),
        EquipmentCategory(
            id = "basket_swing",
            sectionNumber = 2,
            title = "Basket / Nest Swing",
            subtitle = "Basket seat, drainage, bumper edging, chains, crossbars, lubrication, posts, and surfacing",
            questions = listOf(
                ChecklistQuestionTemplate(
                    id = "swg_01",
                    categoryId = "basket_swing",
                    referenceCode = "2.1",
                    questionText = "Is the basket/nest swing seat intact, clean, and free from cracks or splits?",
                ),
                ChecklistQuestionTemplate(
                    id = "swg_02",
                    categoryId = "basket_swing",
                    referenceCode = "2.2",
                    questionText = "Are the central mesh and drainage slots clear of debris and water accumulation?",
                ),
                ChecklistQuestionTemplate(
                    id = "swg_03",
                    categoryId = "basket_swing",
                    referenceCode = "2.3",
                    questionText = "Are outer rubber bumper edging, suspension chains, and D-shackles secure and free from damage?",
                ),
                ChecklistQuestionTemplate(
                    id = "swg_04",
                    categoryId = "basket_swing",
                    referenceCode = "2.4",
                    questionText = "Are top frame crossbar swivels and bearings rotating freely without excessive play or noise?",
                ),
                ChecklistQuestionTemplate(
                    id = "swg_05",
                    categoryId = "basket_swing",
                    referenceCode = "2.5",
                    questionText = "Are top frame crossbar swivels and bearings adequately oiled/greased?",
                ),
                ChecklistQuestionTemplate(
                    id = "swg_06",
                    categoryId = "basket_swing",
                    referenceCode = "2.6",
                    questionText = "Are support posts and ground anchor shoes stable without decay, corrosion, or movement?",
                ),
                ChecklistQuestionTemplate(
                    id = "swg_07",
                    categoryId = "basket_swing",
                    referenceCode = "2.7",
                    questionText = "Is impact-absorbing surfacing level, clean, and free from trip hazards or lifting?",
                ),
            ),
        ),
        EquipmentCategory(
            id = "timber_agility",
            sectionNumber = 3,
            title = "Timber Agility & Climbing Frame Complex",
            subtitle = "Timber posts, anchor shoes, cargo net, traverse rings, overhead bars, balance logs, and surfacing",
            questions = listOf(
                ChecklistQuestionTemplate(
                    id = "tmb_01",
                    categoryId = "timber_agility",
                    referenceCode = "3.1",
                    questionText = "Are main timber posts and horizontal logs free from rot, severe splitting, or splinters?",
                ),
                ChecklistQuestionTemplate(
                    id = "tmb_02",
                    categoryId = "timber_agility",
                    referenceCode = "3.2",
                    questionText = "Are metal post anchor shoes firmly grounded and corrosion-free?",
                ),
                ChecklistQuestionTemplate(
                    id = "tmb_03",
                    categoryId = "timber_agility",
                    referenceCode = "3.3",
                    questionText = "Is the cargo rope climbing net securely tensioned with all joint clips intact?",
                ),
                ChecklistQuestionTemplate(
                    id = "tmb_04",
                    categoryId = "timber_agility",
                    referenceCode = "3.4",
                    questionText = "Are suspended traverse rings and mounting chains secure with intact protective covering?",
                ),
                ChecklistQuestionTemplate(
                    id = "tmb_05",
                    categoryId = "timber_agility",
                    referenceCode = "3.5",
                    questionText = "Are overhead bars/rungs securely bolted and free from rust or sharp edges?",
                ),
                ChecklistQuestionTemplate(
                    id = "tmb_06",
                    categoryId = "timber_agility",
                    referenceCode = "3.6",
                    questionText = "Is the low rope traverse line securely anchored and properly tensioned?",
                ),
                ChecklistQuestionTemplate(
                    id = "tmb_07",
                    categoryId = "timber_agility",
                    referenceCode = "3.7",
                    questionText = "Are anti-slip tread markings on balance beams/logs clear and effective?",
                ),
                ChecklistQuestionTemplate(
                    id = "tmb_08",
                    categoryId = "timber_agility",
                    referenceCode = "3.8",
                    questionText = "Is impact-absorbing surfacing intact, weed-free, and seamlessly joined?",
                ),
            ),
        ),
        EquipmentCategory(
            id = "space_net",
            sectionNumber = 4,
            title = "Space Net / Rope Climbing Pyramid",
            subtitle = "Central pole, steel-core rope netting, ground turnbuckles, and impact area",
            questions = listOf(
                ChecklistQuestionTemplate(
                    id = "net_01",
                    categoryId = "space_net",
                    referenceCode = "4.1",
                    questionText = "Is the central vertical support pole secure, plumb, and free from rust or damage?",
                ),
                ChecklistQuestionTemplate(
                    id = "net_02",
                    categoryId = "space_net",
                    referenceCode = "4.2",
                    questionText = "Are steel-core rope netting lines intact with no frayed strands or exposed metal core?",
                ),
                ChecklistQuestionTemplate(
                    id = "net_03",
                    categoryId = "space_net",
                    referenceCode = "4.3",
                    questionText = "Are ground anchor tensioners/turnbuckles locked and securely installed?",
                ),
                ChecklistQuestionTemplate(
                    id = "net_04",
                    categoryId = "space_net",
                    referenceCode = "4.4",
                    questionText = "Is the surrounding impact surfacing or grass area level and free of erosion or trip hazards?",
                ),
            ),
        ),
        EquipmentCategory(
            id = "dish_spinner",
            sectionNumber = 5,
            title = "Rotating Dish Spinner",
            subtitle = "Spinner bowl, cap plate, bearing operation, lubrication, dampening rings, EN 1176 badge, and surfacing",
            questions = listOf(
                ChecklistQuestionTemplate(
                    id = "spn_01",
                    categoryId = "dish_spinner",
                    referenceCode = "5.1",
                    questionText = "Is the spinner bowl/seat clean, smooth, and free of sharp burrs or dents?",
                ),
                ChecklistQuestionTemplate(
                    id = "spn_02",
                    categoryId = "dish_spinner",
                    referenceCode = "5.2",
                    questionText = "Are the central cap plate and flush security screws secure and complete?",
                ),
                ChecklistQuestionTemplate(
                    id = "spn_03",
                    categoryId = "dish_spinner",
                    referenceCode = "5.3",
                    questionText = "Does the rotating bearing operate smoothly without binding, excessive noise, or play?",
                ),
                ChecklistQuestionTemplate(
                    id = "spn_04",
                    categoryId = "dish_spinner",
                    referenceCode = "5.4",
                    questionText = "Is the rotating bearing mechanism adequately oiled/greased?",
                ),
                ChecklistQuestionTemplate(
                    id = "spn_05",
                    categoryId = "dish_spinner",
                    referenceCode = "5.5",
                    questionText = "Are underside dampening rings and mounting bolts secure and intact?",
                ),
                ChecklistQuestionTemplate(
                    id = "spn_06",
                    categoryId = "dish_spinner",
                    referenceCode = "5.6",
                    questionText = "Is the central support column rigidly anchored into the ground with zero frame movement?",
                ),
                ChecklistQuestionTemplate(
                    id = "spn_07",
                    categoryId = "dish_spinner",
                    referenceCode = "5.7",
                    questionText = "Is the manufacturer identification and compliance badge (EN 1176) attached and legible?",
                ),
                ChecklistQuestionTemplate(
                    id = "spn_08",
                    categoryId = "dish_spinner",
                    referenceCode = "5.8",
                    questionText = "Is the surrounding safety surfacing flush, level, and free of rutting?",
                ),
            ),
        ),
        EquipmentCategory(
            id = "wooden_seesaw",
            sectionNumber = 6,
            title = "Seesaw with Bump Dampers",
            subtitle = "Balance beam, safety hand grips, central pivot, lubrication, dampers, and support post",
            questions = listOf(
                ChecklistQuestionTemplate(
                    id = "ssw_01",
                    categoryId = "wooden_seesaw",
                    referenceCode = "6.1",
                    questionText = "Is the main balance beam free from rot, cracks, splits, or structural damage?",
                ),
                ChecklistQuestionTemplate(
                    id = "ssw_02",
                    categoryId = "wooden_seesaw",
                    referenceCode = "6.2",
                    questionText = "Are safety hand grips firmly attached at both ends?",
                ),
                ChecklistQuestionTemplate(
                    id = "ssw_03",
                    categoryId = "wooden_seesaw",
                    referenceCode = "6.3",
                    questionText = "Is the central pivot assembly secure and operating smoothly without excessive play?",
                ),
                ChecklistQuestionTemplate(
                    id = "ssw_04",
                    categoryId = "wooden_seesaw",
                    referenceCode = "6.4",
                    questionText = "Is the central pivot assembly adequately oiled/greased?",
                ),
                ChecklistQuestionTemplate(
                    id = "ssw_05",
                    categoryId = "wooden_seesaw",
                    referenceCode = "6.5",
                    questionText = "Are underside bump stoppers and ground dampers intact?",
                ),
                ChecklistQuestionTemplate(
                    id = "ssw_06",
                    categoryId = "wooden_seesaw",
                    referenceCode = "6.6",
                    questionText = "Is the central support post rigidly anchored into the ground?",
                ),
            ),
        ),
    )

    val allCategoryIds: Set<String> = categories.map { it.id }.toSet()

    fun buildInspectionItems(
        enabledCategoryIds: Set<String> = allCategoryIds,
        disabledQuestionIds: Set<String> = emptySet(),
        customQuestions: List<ChecklistQuestionTemplate> = emptyList(),
        existingItemsById: Map<String, InspectionItem> = emptyMap(),
    ): List<InspectionItem> {
        val customByCategory = customQuestions.groupBy { it.categoryId }
        return categories
            .filter { it.id in enabledCategoryIds }
            .flatMap { category ->
                val allCategoryQuestions = category.questions + (customByCategory[category.id] ?: emptyList())
                allCategoryQuestions
                    .filter { it.id !in disabledQuestionIds }
                    .map { template ->
                        existingItemsById[template.id] ?: InspectionItem(
                            id = template.id,
                            categoryId = category.id,
                            sectionNumber = category.sectionNumber,
                            categoryTitle = category.title,
                            referenceCode = template.referenceCode,
                            question = template.questionText,
                        )
                    }
            }
    }
}
