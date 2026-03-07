package com.vladdrummer.prayerkmp.feature.prayer

import com.vladdrummer.prayerkmp.feature.padeg.Padeg
import com.vladdrummer.prayerkmp.feature.arrays.PrayerArraysRepository
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.DEFAULT_DUHOVNIK
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.DEFAULT_NAME_IMENIT
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.DEFAULT_PERSON_NAME
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.PersonalPerson
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.statusList
import com.vladdrummer.prayerkmp.feature.storage.AppStorage
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import com.vladdrummer.prayerkmp.feature.strings.getString
import com.vladdrummer.prayerkmp.feature.yearperiod.ChurchYearPeriod
import com.vladdrummer.prayerkmp.feature.yearperiod.ChurchYearPeriodResolver
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

data class PrayerTextResult(
    val text: String,
    val period: ChurchYearPeriod,
)

class PrayerTextBuilder(
    private val storage: AppStorage,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private companion object {
        private const val MORNING_RULE_SIZE = 22
        private const val EVENING_RULE_SIZE = 21
    }

    suspend fun build(
        resId: String,
        date: LocalDate,
        fontIndex: Int = 0,
    ): PrayerTextResult {
        val period = ChurchYearPeriodResolver.currentPeriod(date)
        val personal = loadPersonalContext()
        var result = basePrayerText(resId, period, personal, fontIndex)

        result += extraTailForPrayer(resId, personal, period)
        result = applyComplexLegacyCases(resId, result, personal, date)
        result = applyPeriodSubstitutions(result, period)
        result = normalizeHtml(result)

        return PrayerTextResult(text = result, period = period)
    }

    private suspend fun basePrayerText(
        resId: String,
        period: ChurchYearPeriod,
        personal: PersonalContext,
        fontIndex: Int,
    ): String {
        if (resId == "morningdummy" || resId == "eveningdummy") {
            return buildMorningEveningRule(resId, period, personal, fontIndex)
        }
        if (resId == "poOkonchaniiNorm" && period == ChurchYearPeriod.EasterToAscension) {
            return getString("poOkonchanii3period")
        }
        if (period == ChurchYearPeriod.Easter && (resId == "trehcannonbeginning" || resId == "cannon_pokayanniy")) {
            return getString("cannonPashalnii") + getString("cannonPashalniia")
        }
        return getString(resId).let { if (it == resId) "Не найден текст молитвы: $resId" else it }
    }

    private suspend fun buildMorningEveningRule(
        resId: String,
        period: ChurchYearPeriod,
        personal: PersonalContext,
        fontIndex: Int,
    ): String {
        val isMorning = resId == "morningdummy"
        val isChurchSlavonic = fontIndex == 2
        val sourceName = when {
            isMorning && isChurchSlavonic -> "morningCS"
            isMorning -> "morning"
            !isMorning && isChurchSlavonic -> "eveningCS"
            else -> "evening"
        }
        val parts = PrayerArraysRepository.getArray(sourceName).toMutableList()
        println(
            "PrayerTextBuilder: building $resId from array '$sourceName', parts.size=${parts.size}, fontIndex=$fontIndex"
        )
        if (parts.isEmpty()) return getString(resId)
        if (period == ChurchYearPeriod.Easter) return getString("Pasha")

        if (isMorning) {
            if (period == ChurchYearPeriod.Easter || period == ChurchYearPeriod.EasterToAscension) {
                if (parts.isNotEmpty()) parts[0] = getString("morning_subst_beginn_3_yearperiod")
                if (parts.size > 21) parts[21] = getString("morning_subst_end_3_period")
            }
            if (period == ChurchYearPeriod.AscensionToTrinity && parts.isNotEmpty()) {
                parts[0] = getString("morning_subst_beginn_4_period")
            }
        } else {
            if (period == ChurchYearPeriod.Easter || period == ChurchYearPeriod.EasterToAscension) {
                if (parts.isNotEmpty()) parts[0] = getString("evening_subst_beginn_3_yearperiod")
                if (parts.size > 13) parts[13] = getString("evening_subst_end_3_period")
            }
            if (period == ChurchYearPeriod.AscensionToTrinity && parts.isNotEmpty()) {
                parts[0] = getString("evening_subst_beginn_4_period")
            }
        }

        val enabled = loadRuleEnabledFlags(isMorning)
        val builder = StringBuilder()
        for (index in enabled.indices) {
            if (enabled[index] && index < parts.size) {
                if (isMorning && index == 19 && enabled.size > 18 && enabled[18]) {
                    builder.append(getString("EsliMojesh"))
                }
                builder.append(parts[index])
                if (isMorning) builder.append(morningLegacyInsert(index, personal))
            }

            // Legacy behavior: additional prayers are inserted by fixed index in the loop,
            // regardless of whether that base rule element is enabled.
            if (isMorning && index == 20) {
                builder.append(
                    loadAndBuildAdditionalPrayers(
                        key = AppStorageKeys.AdditionalMorningPrayers,
                        date = currentLocalDate(),
                        fontIndex = fontIndex,
                    )
                )
            }
            if (!isMorning && index == 12) {
                builder.append(
                    loadAndBuildAdditionalPrayers(
                        key = AppStorageKeys.AdditionalEveningPrayers,
                        date = currentLocalDate(),
                        fontIndex = fontIndex,
                    )
                )
            }
        }
        return builder.toString()
    }

    private suspend fun loadRuleEnabledFlags(isMorning: Boolean): BooleanArray {
        val expectedSize = if (isMorning) MORNING_RULE_SIZE else EVENING_RULE_SIZE
        val key = if (isMorning) AppStorageKeys.MorningRuleEnabled else AppStorageKeys.EveningRuleEnabled
        val raw = storage.stringFlow(key, "").first()
        if (raw.isBlank()) return BooleanArray(expectedSize) { true }

        val parsed = runCatching {
            json.decodeFromString(ListSerializer(Boolean.serializer()), raw)
        }.getOrDefault(emptyList())

        if (parsed.isEmpty()) return BooleanArray(expectedSize) { true }
        return BooleanArray(expectedSize) { index -> parsed.getOrNull(index) ?: true }
    }

    private suspend fun loadAdditionalPrayerIds(key: String): List<String> {
        val raw = storage.stringFlow(key, "").first()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private suspend fun saveAdditionalPrayerIds(key: String, values: List<String>) {
        storage.setString(key, json.encodeToString(ListSerializer(String.serializer()), values))
    }

    private suspend fun loadAndBuildAdditionalPrayers(
        key: String,
        date: LocalDate,
        fontIndex: Int,
    ): String {
        val ids = loadAdditionalPrayerIds(key).toMutableList()
        if (ids.isEmpty()) return ""

        val out = StringBuilder()
        var changed = false
        var index = 0
        while (index < ids.size) {
            val additionalResId = ids[index]
            if (additionalResId == "morningdummy" || additionalResId == "eveningdummy") {
                ids.removeAt(index)
                changed = true
                continue
            }

            val built = runCatching { build(additionalResId, date, fontIndex).text }.getOrNull()
            if (built == null) {
                ids.removeAt(index)
                changed = true
                continue
            }
            out.append(built)
            index++
        }

        if (changed) {
            saveAdditionalPrayerIds(key, ids)
        }
        return out.toString()
    }

    private fun morningLegacyInsert(
        index: Int,
        personal: PersonalContext,
    ): String {
        return when (index) {
            14 -> " ${personal.nameImenit}, я&#x301;ко аз усе&#x301;рдно к тебе&#x301; прибега&#x301;ю, ско&#x301;рому помо&#x301;щнику и моли&#x301;твеннику о душе&#x301; мое&#x301;й.<br /><br/>"
            17 -> {
                val children = personal.childrenNamesForMorning(4)
                val godChildren = personal.godChildrenNamesForMorning(4)
                " ${personal.duhovnikName(4)}  роди&#x301;телей мои&#x301;х ${personal.parentsNamesForMorning(4)}, сро&#x301;дников ${personal.relativesNamesForMorning(4)}" +
                    (if (children.isNotBlank()) ", $children" else "") +
                    (if (godChildren.isNotBlank()) ", крестников: $godChildren" else "") +
                    ", нача&#x301;льников, наста&#x301;вников, благоде&#x301;телей ${personal.benefactorsNamesForMorning(4)}, и всех правосла&#x301;вных христиа&#x301;н.<br /><br/>"
            }
            18 -> " ${personal.deadNamesForMorning(4)} и всех правосла&#x301;вных христиа&#x301;н, и прости им вся согрешения вольная и невольная, и даруй им Царствие Небесное.<br /><br/>"
            19 -> {
                "<br />${personal.duhovnikName(4)}, и святы&#x301;ми его&#x301; моли&#x301;твами прости&#x301; моя&#x301; согреше&#x301;ния. <br /><i><font color=\"#aa2c2c\">(Поклон)</font></i><br />" +
                    "<br />Спаси&#x301;, Го&#x301;споди, и поми&#x301;луй роди&#x301;тели моя&#x301; ${personal.parentsNamesForMorning(4)}, бра&#x301;тию и сестры&#x301;, и сро&#x301;дники моя&#x301; по пло&#x301;ти, и вся бли&#x301;жния ро&#x301;да моего&#x301;, и дру&#x301;ги, и да&#x301;руй им ми&#x301;рная Твоя&#x301; и преми&#x301;рная блага&#x301;я. <br /><font color=\"#aa2c2c\"><i>(Поклон)</i></font><br /><br />" +
                    "<font color=\"#aa2c2c\">С</font>паси, Го&#x301;споди, и поми&#x301;луй по мно&#x301;жеству щедро&#x301;т Твоих вся священнои&#x301;ноки, и&#x301;ноки же и и&#x301;нокини, и вся в де&#x301;встве же и бла&#x301;гоговении и по&#x301;стничестве живу&#x301;щия в монастыре&#x301;х, в пустынях, в пещерах, гора&#x301;х, столпе&#x301;х, затво&#x301;рех, разсе&#x301;линах ка&#x301;менных, острове&#x301;х же морских, и на всяком месте владычествия Твоего правове&#x301;рно живу&#x301;щия, и благоче&#x301;стно служащия Ти, и моля&#x301;щияся Тебе: облегчи им тяготу, и утеши их скорбь, и к по&#x301;двигу о Тебе силу и кре&#x301;пость им пода&#x301;ждь, и молитвами их даруй ми оставле&#x301;ние грехов. <font color=\"#aa2c2c\"><i>(Поклон)</i></font><br /><br /><br />" +
                    "<font color=\"#aa2c2c\">С</font>паси&#x301;, Го&#x301;споди, и поми&#x301;луй ста&#x301;рцы и ю&#x301;ныя, ни&#x301;щия и сироты&#x301; и вдови&#x301;цы, и су&#x301;щия в боле&#x301;зни и в печа&#x301;лех, беда&#x301;х же и ско&#x301;рбех, обстоя&#x301;ниих и плене&#x301;ниих, темни&#x301;цах же и заточе&#x301;ниих, изря&#x301;днее же в гоне&#x301;ниих, Тебе&#x301; ра&#x301;ди и ве&#x301;ры правосла&#x301;вныя, от язы&#x301;к безбо&#x301;жных, от отсту&#x301;пник и от еретико&#x301;в, су&#x301;щия рабы&#x301; Твоя&#x301;, и помяни&#x301; я&#x301;, посети&#x301;, укрепи&#x301;, уте&#x301;ши, и вско&#x301;ре си&#x301;лою Твое&#x301;ю осла&#x301;бу, свобо&#x301;ду и изба&#x301;ву им пода&#x301;ждь.<br /><i><font color=\"#aa2c2c\">(Поклон)</font></i><br />" +
                    "<br /><font color=\"#aa2c2c\">С</font>паси&#x301;, Го&#x301;споди, и поми&#x301;луй благотворя&#x301;щия нам, ми&#x301;лующия и пита&#x301;ющия нас, да&#x301;вшия нам ми&#x301;лостыни, и запове&#x301;давшия нам недосто&#x301;йным моли&#x301;тися о них, и упокоева&#x301;ющия нас, и сотвори&#x301; ми&#x301;лость Твою&#x301; с ни&#x301;ми, да&#x301;руя им вся, я&#x301;же ко спасе&#x301;нию проше&#x301;ния, и ве&#x301;чных благ восприя&#x301;тие. <i><font color=\"#aa2c2c\">(Поклон)</font></i><br />" +
                    "<br /><font color=\"#aa2c2c\">С</font>паси&#x301;, Го&#x301;споди, и поми&#x301;луй по&#x301;сланныя в слу&#x301;жбу, путеше&#x301;ствующия, отцы&#x301; и бра&#x301;тию на&#x301;шу, и вся правосла&#x301;вныя христиа&#x301;ны <br /><i><font color=\"#aa2c2c\">(Поклон)</font></i><br />" +
                    "<br /><font color=\"#aa2c2c\">С</font>паси&#x301;, Го&#x301;споди, и поми&#x301;луй и&#x301;хже аз безу&#x301;мием мои&#x301;м соблазни&#x301;х, и от пути&#x301; спаси&#x301;тельнаго отврати&#x301;х, к дело&#x301;м злым и неподо&#x301;бным приведо&#x301;х; Боже&#x301;ственным Твои&#x301;м Про&#x301;мыслом к пути&#x301; спасе&#x301;ния па&#x301;ки возврати&#x301;.<br /><i><font color=\"#aa2c2c\">(Поклон)</font></i><br />" +
                    "<br /><font color=\"#aa2c2c\">С</font>паси&#x301;, Го&#x301;споди, и поми&#x301;луй ненави&#x301;дящия и оби&#x301;дящия мя, и творя&#x301;щия ми напа&#x301;сти, и не оста&#x301;ви их поги&#x301;бнути мене&#x301; ра&#x301;ди, гре&#x301;шнаго. <br /><i><font color=\"#aa2c2c\">(Поклон)</font></i><br />" +
                    "<br /><font color=\"#aa2c2c\">О</font>тступи&#x301;вшия от правосла&#x301;вныя ве&#x301;ры и поги&#x301;бельными ересьми&#x301; ослепле&#x301;нныя, све&#x301;том Твоего&#x301; позна&#x301;ния просвети&#x301; и Святе&#x301;й Твое&#x301;й Апо&#x301;стольстей Собо&#x301;рней Це&#x301;ркви причти. Правове&#x301;рие же утверди&#x301;, и воздви&#x301;гни рог христиа&#x301;нский, и низпосли&#x301; на нас ми&#x301;лости Твоя&#x301; бога&#x301;тыя.<br /><i><font color=\"#aa2c2c\">(Поклон)</font></i><br /><br />"
            }
            20 -> {
                " ${personal.deadNamesForMorning(4)} и всех сро&#x301;дников по пло&#x301;ти; и прости&#x301; их вся согреше&#x301;ния во&#x301;льная и нево&#x301;льная, да&#x301;руя им Ца&#x301;рствие и прича&#x301;стие ве&#x301;чных Твои&#x301;х благи&#x301;х и Твоея&#x301; безконе&#x301;чныя и блаже&#x301;нныя жи&#x301;зни наслажде&#x301;ние. <br /><i><font color=\"#aa2c2c\">(Поклон)</font></i><br />" +
                    "<br /><font color=\"#aa2c2c\">П</font>омяни&#x301;, Го&#x301;споди, и вся в наде&#x301;жди воскресе&#x301;ния и жи&#x301;зни ве&#x301;чныя усо&#x301;пшия, отцы&#x301; и бра&#x301;тию на&#x301;шу, и сестры&#x301;, и зде лежа&#x301;щия и повсю&#x301;ду, правосла&#x301;вныя христиа&#x301;ны, и со святы&#x301;ми Твои&#x301;ми, иде&#x301;же присеща&#x301;ет свет лица&#x301; Твоего&#x301;, всели&#x301;, и нас поми&#x301;луй, я&#x301;ко Благ и Человеколю&#x301;бец. <font color=\"#aa2c2c\">А</font>ми&#x301;нь.<br /><i><font color=\"#aa2c2c\">(Поклон)</font></i><br />" +
                    "<br /><font color=\"#aa2c2c\">П</font>ода&#x301;ждь, Го&#x301;споди, оставле&#x301;ние грехо&#x301;в всем пре&#x301;жде отше&#x301;дшим в ве&#x301;ре и наде&#x301;жди воскресе&#x301;ния, отце&#x301;м, бра&#x301;тиям и се&#x301;страм на&#x301;шим и сотвори&#x301; им ве&#x301;чную па&#x301;мять.<i><font color=\"#aa2c2c\">(Трижды)</font></i><br /><br />"
            }
            else -> ""
        }
    }

    private suspend fun extraTailForPrayer(
        resId: String,
        personal: PersonalContext,
        period: ChurchYearPeriod,
    ): String {
        return when (resId) {
            "trehcannonbeginning" -> if (period == ChurchYearPeriod.Easter) "" else " ${getString("trehcannonbeginning2")} ${personal.myName(4)}${getString("trehcannonend")}"
            "canon_molebniy_presv_bogorodice" -> getString("canon_molebniy_presv_bogorodice2")
            "canonZaBolashih" -> getString("canonZaBolashih2")
            "AcafistIiSusuSladchaishemu" -> getString("AcafistIiSusuSladchaishemu2")
            "AcafistPresvyatoyBogorodice" -> getString("AcafistPresvyatoyBogorodice2")
            "canon_krestu" -> getString("canon_krestu2")
            "acathistAngeluHranitelu" -> getString("acathistAngeluHranitelu2")
            "acathistPanteleimonu" -> getString("acathistPanteleimonu2")
            "acathist_serapimu" -> getString("acathist_serapimu2")
            "acathistUmagchenieZlihSerdec" -> getString("acathistUmagchenieZlihSerdec2")
            "acathistIoannuPredteche" -> getString("acathistIoannuPredteche2")
            "acathistNeupivaemayaChasha" -> getString("acathistNeupivaemayaChasha2")
            "acathistBogorodiceSkoroposlushnica" -> getString("acathistBogorodiceSkoroposlushnica2")
            "acathist_nechayannaya_radost" -> getString("acathist_nechayannaya_radost2")
            "acathist_pribavlenie_uma" -> getString("acathist_pribavlenie_uma2")
            "cannon_paisiyu_velikomu" -> getString("cannon_paisiyu_velikomu2")
            "andrey_kritskiy_1" -> getString("andrey_kritskiy_1a")
            "andrey_kritskiy_2" -> getString("andrey_kritskiy_2a")
            "andrey_kritskiy_3" -> getString("andrey_kritskiy_3a") + getString("andrey_kritskiy_3b")
            "andrey_kritskiy_4" -> getString("andrey_kritskiy_4a")
            "acafistNikolayu" -> getString("acafistNikolayu2")
            "cannonPashalnii" -> getString("cannonPashalniia")
            "acathist_matrone" -> getString("acathist_matronea")
            "canon_angelu_hranitelu" -> " ${personal.myName(4)} ${getString("canon_angelu_hranitelu_end")}"
            "readings_prayer_beginning" -> " ${personal.myName(4)} ${getString("readings_prayer_end")}"
            "pravilo_ot_oskverneniya" -> " ${personal.myName(4)} ${getString("pravilo_ot_oskverneniya_end")}"
            "kirillu_i_marii_roditelam_sergia_radonejskogo" -> " ${personal.allNames(4)} ${getString("kirillu_i_marii_roditelam_sergia_radonejskogo_end")}"
            "soboru_dvenadcati" -> " ${personal.allNames(4)} ${getString("soboru_dvenadcati_end")}"
            "prokopiyu" -> " ${personal.allNames(4)} ${getString("prokopiyu_end")}"
            "soroka_sevastiyskim" -> " ${personal.allNames(4)} ${getString("soroka_sevastiyskim_end")}"
            "tamare" -> " ${personal.allNames(4)} ${getString("tamare_end")}"
            "vladimiru" -> " ${personal.allNames(4)} ${getString("vladimiru_end")}"
            "seraphim_sarovskiy" -> " ${personal.allNames(4)} ${getString("seraphim_sarovskiy_end")}"
            "vsem_svyatim_v_rossii_prosiyavshim" -> " ${personal.allNames(4)} ${getString("vsem_svyatim_v_rossii_prosiyavshim_end")}"
            "kirillu_i_mephodiyu" -> " ${personal.allNames(4)} ${getString("kirillu_i_mephodiyu_end")}"
            "ravnoapostolnoy_olge" -> " ${personal.allNames(4)} ${getString("ravnoapostolnoy_olge_end")}"
            "daniil_moskovskiy" -> " ${personal.allNames(4)} ${getString("daniil_moskovskiy_end")}"
            "konstantinu_i_elene" -> " ${personal.allNames(4)} ${getString("konstantinu_i_elene_end")}"
            "vyacheslav_cheshskiy" -> " ${personal.allNames(4)} ${getString("vyacheslav_cheshskiy_end")}"
            "olegu_bryanskomu" -> " ${personal.allNames(4)} ${getString("olegu_bryanskomu_end")}"
            "nine_gruzii" -> " ${personal.allNames(4)} ${getString("nine_gruzii_end")}"
            "kseniya_peterburjskaya" -> " ${personal.allNames(4)} ${getString("kseniya_peterburjskaya_end")}"
            "spiridonu_trimifuntskomu" -> {
                val add = if (personal.nameLooksLikeDefault()) " раб твой " else if (personal.isMale) " раб твой " else " раба твоя "
                " ${personal.allNames(4)} ${getString("spiridonu_trimifuntskomu_1")} $add${personal.nameImenit}${getString("spiridonu_trimifuntskomu_end")}"
            }
            "andreyu_kritskomu" -> {
                val add = if (personal.nameLooksLikeDefault()) " раба твоего " else if (personal.isMale) " раба твоего " else " рабу твою "
                " ${personal.allNames(4)} ${getString("andreyu_kritskomu_1")} $add${personal.myName(2)}${getString("andreyu_kritskomu_end")}"
            }
            "acathistMihailuArchangelu" -> {
                " ${getString("acathistMihailuArchangelu_p1")} ${personal.allNames(4)}${getString("acathistMihailuArchangelu_p2")} ${personal.allNames(4)}${getString("acathistMihailuArchangelu_p3")} ${personal.allNames(4)} ${getString("acathistMihailuArchangelu_p4")}"
            }
            "amvrosiu_optinskomu" -> " ${personal.allNames(4)} ${getString("amvrosiu_optinskomu_end")}"
            "anastasii_uzoreshitelnice" -> " ${personal.allNames(4)} ${getString("anastasii_uzoreshitelnice_end")}"
            "stephanu" -> " ${personal.allNames(4)} ${getString("stephanu_end")}"
            "posledovanie" -> {
                val post = if (period == ChurchYearPeriod.GreatFast) getString("posledovaniepost") else ""
                post + getString("posledovanieend") + getString("posledovanieend2") + getString("posledovanieend3")
            }
            else -> ""
        }
    }

    private suspend fun applyComplexLegacyCases(
        resId: String,
        text: String,
        personal: PersonalContext,
        date: LocalDate,
    ): String {
        var result = text
        if (resId == "angel_daily_dummy") {
            return when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "${getString("monday_angel")} ${personal.allNames(4)} ${getString("monday_angel_end")}"
                DayOfWeek.TUESDAY -> "${getString("tuesday_angel")} ${personal.allNames(4)} ${getString("tuesday_angel_end")}"
                DayOfWeek.WEDNESDAY -> getString("wendesday_angel")
                DayOfWeek.THURSDAY -> getString("thursday_angel")
                DayOfWeek.FRIDAY -> "${getString("friday_angel")} ${if (personal.isMale) "грешнаго" else "грешную"} ${getString("friday_angel_end")}"
                DayOfWeek.SATURDAY -> "${getString("saturday_angel")} ${if (personal.isMale) "ленивого" else "ленивую"} ${getString("saturday_angel_end")}"
                DayOfWeek.SUNDAY -> getString("sunday_angel")
            }
        }
        if (resId == "IIsusu1") {
            val all4 = personal.allNames(4)
            result += " $all4 ${getString("IIsusu2")} $all4 ${getString("IIsusu3")} ${personal.allNames(2)} ${getString("IIsusu4")} ${personal.myName(2)} ${getString("IIsusu5")}"
        }
        if (resId == "deti1") {
            val names6 = personal.childrenNames(6)
            val names4 = personal.childrenNames(4)
            result += " $names6 ${getString("deti2")} $names4${getString("deti3")} $names4 ${getString("deti4")} $names4 ${getString("deti5")} $names4${getString("deti6")} $names4 ${getString("deti7")}${if (personal.isMale) "грешнаго" else "грешную"}${getString("deti8")}"
        }
        if (resId == "Bogorodice1") {
            val all4 = personal.allNamesForBogorodice(4)
            val all6 = personal.allNamesForBogorodice(6)
            result += " $all4 ${getString("Bogorodice2")} $all6 ${getString("Bogorodice3")} $all4 ${getString("Bogorodice4")} "
        }
        if (resId == "psaltyrPoOkonchanii") {
            val tail = if (personal.nameLooksLikeDefault()) "грешнаго (грешную)." else if (personal.isMale) "грешнаго." else "грешную."
            result += " $tail${getString("psaltyrPoOkonchanii2")} <br /><br />"
        }
        if (resId == "ejednevnaya_parfeniya_kievskogo_1") {
            val myName = if (personal.nameLooksLikeDefault()) "" else "${personal.myName(4)}, "
            result += " $myName${personal.allNames(4)}${getString("ejednevnaya_parfeniya_kievskogo_2")}"
        }
        if (resId == "marii_egipetskoi_1") {
            val myName = if (personal.nameLooksLikeDefault()) "" else "${personal.myName(4)}, "
            result += " $myName${personal.allNames(4)}${getString("marii_egipetskoi_2")}"
        }
        if (resId == "kiprianu_i_iustini_1") {
            val myName = if (personal.nameLooksLikeDefault()) "" else "${personal.myName(6)}, "
            result += " $myName${personal.allNames(6)}${getString("kiprianu_i_iustini_2")}"
        }
        if (resId == "o_roditelah_jivih_1") {
            result += " ${personal.parentsNames(6)}${getString("o_roditelah_jivih_2")}"
        }
        if (resId == "molitvi_o_duhovnom_otce_1") {
            val dayPart = currentDayPart()
            val duhovnik2 = personal.duhovnikName(2)
            val gresh = if (!personal.nameLooksLikeDefault() && !personal.isMale) " великую грешницу " else " великого грешника "
            result += " $duhovnik2 ${getString("molitvi_o_duhovnom_otce_2")}$gresh${getString("molitvi_o_duhovnom_otce_3")}${getString("molitvi_o_duhovnom_otce_4")} $duhovnik2 ${getString("molitvi_o_duhovnom_otce_5")} $dayPart${getString("molitvi_o_duhovnom_otce_6")} $duhovnik2${getString("molitvi_o_duhovnom_otce_7")}"
        }
        if (resId == "acathistSpiridonuTrimifundtskomu_1") {
            val myName = if (personal.nameLooksLikeDefault()) "" else "${personal.myName(3)}, "
            result += "${getString("acathistSpiridonuTrimifundtskomu_1_2")} $myName${personal.allNames(3)}${getString("acathistSpiridonuTrimifundtskomu_2")}"
        }
        if (resId == "o_detyah_i_krestnikah_1") {
            val children4 = personal.childrenNames(4)
            val children2 = personal.childrenNames(2)
            val children3 = personal.childrenNames(3)
            val godChildren4 = personal.godChildrenNames(4)
            val godChildren2 = personal.godChildrenNames(2)
            val godChildren3 = personal.godChildrenNames(3)
            result += "$children4 ${getString("o_detyah_i_krestnikah_2")}$godChildren4${getString("o_detyah_i_krestnikah_2a")}$godChildren4${getString("o_detyah_i_krestnikah_2b")}$godChildren2, ${getString("o_detyah_i_krestnikah_2c")}$children2${getString("o_detyah_i_krestnikah_3")}$godChildren2${getString("o_detyah_i_krestnikah_3a")}$children3${getString("o_detyah_i_krestnikah_4")}$godChildren3 ${getString("o_detyah_i_krestnikah_4a")}"
        }
        return result
    }

    private fun currentDayPart(): String {
        val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        return if (hour in 5..16) "день сей " else "ночь сию "
    }

    private suspend fun applyPeriodSubstitutions(text: String, period: ChurchYearPeriod): String {
        var out = text
        return when (period) {
            ChurchYearPeriod.Easter, ChurchYearPeriod.EasterToAscension -> {
                out = out.replace(getString("to_subst_tsaru_header"), "")
                out = out.replace("tsaryu_nebesni", getString("subst_hristos_voskrese"))
                out = out.replace("dostoyno_est", getString("subst_angel_vopiyashe"))
                out
            }
            ChurchYearPeriod.AscensionToTrinity -> {
                out = out.replace(getString("to_subst_tsaru_header"), "")
                out = out.replace("tsaryu_nebesni", "")
                out = out.replace("dostoyno_est", getString("to_subst_dostoyno_est"))
                out
            }
            else -> {
                out = out.replace("tsaryu_nebesni", getString("to_subst_tsaru_all"))
                out = out.replace("dostoyno_est", getString("to_subst_dostoyno_est"))
                out
            }
        }
    }

    private fun normalizeHtml(text: String): String {
        return text.trim()
    }

    private suspend fun loadPersonalContext(): PersonalContext {
        val name = storage.stringFlow(AppStorageKeys.NameImenit, DEFAULT_NAME_IMENIT).first()
        val duhovnik = storage.stringFlow(AppStorageKeys.Duhovnik, DEFAULT_DUHOVNIK).first()
        val isMale = storage.booleanFlow(AppStorageKeys.MyGenderMale, false).first()
        val parents = decodePeople(storage.stringFlow(AppStorageKeys.PersonalParents, "").first())
        val relatives = decodePeople(storage.stringFlow(AppStorageKeys.PersonalRelatives, "").first())
        val children = decodePeople(storage.stringFlow(AppStorageKeys.PersonalChildren, "").first())
        val godChildren = decodePeople(storage.stringFlow(AppStorageKeys.PersonalGodChildren, "").first())
        val benefactors = decodePeople(storage.stringFlow(AppStorageKeys.PersonalBenefactors, "").first())
        val dead = decodePeople(storage.stringFlow(AppStorageKeys.PersonalDead, "").first())
        return PersonalContext(
            nameImenit = name,
            duhovnik = duhovnik,
            isMale = isMale,
            parents = parents,
            relatives = relatives,
            children = children,
            godChildren = godChildren,
            benefactors = benefactors,
            dead = dead,
        )
    }

    private fun decodePeople(raw: String): List<PersonalPerson> {
        if (raw.isBlank()) return listOf(PersonalPerson())
        return runCatching {
            json.decodeFromString(ListSerializer(PersonalPerson.serializer()), raw)
        }.getOrDefault(listOf(PersonalPerson())).ifEmpty { listOf(PersonalPerson()) }
    }
}

private data class PersonalContext(
    val nameImenit: String,
    val duhovnik: String,
    val isMale: Boolean,
    val parents: List<PersonalPerson>,
    val relatives: List<PersonalPerson>,
    val children: List<PersonalPerson>,
    val godChildren: List<PersonalPerson>,
    val benefactors: List<PersonalPerson>,
    val dead: List<PersonalPerson>,
) {
    fun myName(padeg: Int): String {
        if (nameImenit == DEFAULT_NAME_IMENIT || nameImenit.isBlank()) return nameImenit
        return runCatching { Padeg.getFIOPadeg("", nameImenit, "", isMale, padeg) }.getOrDefault(nameImenit)
    }

    fun allNames(padeg: Int): String {
        val names = mutableListOf<String>()
        if (duhovnik != DEFAULT_DUHOVNIK && duhovnik.isNotBlank()) {
            names += padegName(duhovnik, 1, padeg)
        }
        names += sectionNames(parents, padeg, isDead = false)
        names += sectionNames(relatives, padeg, isDead = false)
        names += sectionNames(children, padeg, isDead = false)
        names += sectionNames(godChildren, padeg, isDead = false)
        names += sectionNames(benefactors, padeg, isDead = false)
        if (names.isEmpty()) return "(Имена)"
        return names.joinToString(", ")
    }

    fun childrenNames(padeg: Int): String = sectionNames(children, padeg, isDead = false).ifEmpty { listOf(DEFAULT_PERSON_NAME) }.joinToString(", ")
    fun godChildrenNames(padeg: Int): String = sectionNames(godChildren, padeg, isDead = false).ifEmpty { listOf(DEFAULT_PERSON_NAME) }.joinToString(", ")
    fun parentsNames(padeg: Int): String = sectionNames(parents, padeg, isDead = false).ifEmpty { listOf(DEFAULT_PERSON_NAME) }.joinToString(", ")
    fun duhovnikName(padeg: Int): String = if (duhovnik == DEFAULT_DUHOVNIK || duhovnik.isBlank()) DEFAULT_DUHOVNIK else padegName(duhovnik, 1, padeg)
    fun nameLooksLikeDefault(): Boolean = nameImenit == DEFAULT_NAME_IMENIT || nameImenit.isBlank() || nameImenit.contains("мя)", ignoreCase = true)
    fun parentsNamesForMorning(padeg: Int): String = rawSectionNames(parents, padeg, isDead = false, includeDefault = true, skipIfPlaceholderFirst = false)
    fun relativesNamesForMorning(padeg: Int): String = rawSectionNames(relatives, padeg, isDead = false, includeDefault = true, skipIfPlaceholderFirst = false)
    fun childrenNamesForMorning(padeg: Int): String = rawSectionNames(children, padeg, isDead = false, includeDefault = false, skipIfPlaceholderFirst = true)
    fun godChildrenNamesForMorning(padeg: Int): String = rawSectionNames(godChildren, padeg, isDead = false, includeDefault = false, skipIfPlaceholderFirst = true)
    fun benefactorsNamesForMorning(padeg: Int): String = rawSectionNames(benefactors, padeg, isDead = false, includeDefault = true, skipIfPlaceholderFirst = false)
    fun deadNamesForMorning(padeg: Int): String = rawSectionNames(dead, padeg, isDead = true, includeDefault = true, skipIfPlaceholderFirst = false)

    fun allNamesForBogorodice(padeg: Int): String {
        val names = mutableListOf<String>()
        if (!nameImenit.contains("мя ре", ignoreCase = true) && nameImenit.isNotBlank() && nameImenit != DEFAULT_NAME_IMENIT) {
            names += myName(padeg)
        }
        if (duhovnik != DEFAULT_DUHOVNIK && duhovnik.isNotBlank() && !duhovnik.contains("мя ег", ignoreCase = true)) {
            names += padegName(duhovnik, 1, padeg)
        }
        names += sectionNames(parents, padeg, isDead = false)
        if (relatives.any { it.name.isNotBlank() && !it.name.contains("Имена", ignoreCase = true) }) {
            names += sectionNames(relatives, padeg, isDead = false)
            names += sectionNames(children, padeg, isDead = false)
            names += sectionNames(godChildren, padeg, isDead = false)
        }
        names += sectionNames(benefactors, padeg, isDead = false)
        return if (names.isEmpty()) "(Имена)" else names.joinToString(", ")
    }

    private fun sectionNames(people: List<PersonalPerson>, padeg: Int, isDead: Boolean): List<String> {
        return people
            .filter { it.name.isNotBlank() && !it.name.contains("Имена", ignoreCase = true) }
            .map { "${statusString(it, isDead)}${padegName(it.name, it.gender, padeg)}" }
    }

    private fun rawSectionNames(
        people: List<PersonalPerson>,
        padeg: Int,
        isDead: Boolean,
        includeDefault: Boolean,
        skipIfPlaceholderFirst: Boolean,
    ): String {
        if (people.isEmpty()) return if (includeDefault) DEFAULT_PERSON_NAME else ""
        val first = people.firstOrNull()?.name.orEmpty()
        if (skipIfPlaceholderFirst && isPlaceholder(first)) return ""
        return people
            .map { person ->
                val safeName = person.name.takeIf { it.isNotBlank() } ?: DEFAULT_PERSON_NAME
                val rendered = if (isPlaceholder(safeName)) safeName else padegName(safeName, person.gender, padeg)
                "${statusString(person, isDead)}$rendered"
            }
            .joinToString(", ")
    }

    private fun isPlaceholder(name: String): Boolean {
        return name.contains("Имена", ignoreCase = true) || name.contains("мя)", ignoreCase = true)
    }

    private fun padegName(name: String, gender: Int, padeg: Int): String {
        val male = gender == 1
        return runCatching { Padeg.getFIOPadeg("", name, "", male, padeg) }.getOrDefault(name)
    }

    private fun statusString(person: PersonalPerson, isDead: Boolean): String {
        return statusList(isDead).getOrNull(person.status).orEmpty().let { if (it.isBlank()) "" else "$it " }
    }
}
