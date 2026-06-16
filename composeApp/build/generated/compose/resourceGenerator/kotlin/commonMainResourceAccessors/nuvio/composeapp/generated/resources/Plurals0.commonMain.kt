@file:OptIn(InternalResourceApi::class)

package nuvio.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/nuvio.composeapp.generated.resources/"

@delegate:ResourceContentHash(1_186_034_857)
internal val Res.plurals.cw_airs_in_days: PluralStringResource by lazy {
      PluralStringResource("plurals:cw_airs_in_days", "cw_airs_in_days", setOf(
        ResourceItem(setOf(LanguageQualifier("de"), ), "${MD}values-de/strings.commonMain.cvr", 88, 103),
        ResourceItem(setOf(LanguageQualifier("el"), ), "${MD}values-el/strings.commonMain.cvr", 112, 139),
        ResourceItem(setOf(LanguageQualifier("es"), ), "${MD}values-es/strings.commonMain.cvr", 114, 173),
        ResourceItem(setOf(LanguageQualifier("fr"), ), "${MD}values-fr/strings.commonMain.cvr", 118, 145),
        ResourceItem(setOf(LanguageQualifier("id"), ), "${MD}values-id/strings.commonMain.cvr", 67, 62),
        ResourceItem(setOf(LanguageQualifier("in"), ), "${MD}values-in/strings.commonMain.cvr", 67, 62),
        ResourceItem(setOf(LanguageQualifier("it"), ), "${MD}values-it/strings.commonMain.cvr", 118, 137),
        ResourceItem(setOf(LanguageQualifier("nb"), ), "${MD}values-nb/strings.commonMain.cvr", 80, 83),
        ResourceItem(setOf(LanguageQualifier("pl"), ), "${MD}values-pl/strings.commonMain.cvr", 131, 146),
        ResourceItem(setOf(LanguageQualifier("pt"), ), "${MD}values-pt/strings.commonMain.cvr", 84, 91),
        ResourceItem(setOf(LanguageQualifier("tr"), ), "${MD}values-tr/strings.commonMain.cvr", 92, 115),
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 84, 83),
      ))
    }

@delegate:ResourceContentHash(249_460_624)
internal val Res.plurals.cw_airs_in_days_short: PluralStringResource by lazy {
      PluralStringResource("plurals:cw_airs_in_days_short", "cw_airs_in_days_short", setOf(
        ResourceItem(setOf(LanguageQualifier("de"), ), "${MD}values-de/strings.commonMain.cvr", 10, 77),
        ResourceItem(setOf(LanguageQualifier("el"), ), "${MD}values-el/strings.commonMain.cvr", 10, 101),
        ResourceItem(setOf(LanguageQualifier("es"), ), "${MD}values-es/strings.commonMain.cvr", 10, 103),
        ResourceItem(setOf(LanguageQualifier("fr"), ), "${MD}values-fr/strings.commonMain.cvr", 10, 107),
        ResourceItem(setOf(LanguageQualifier("id"), ), "${MD}values-id/strings.commonMain.cvr", 10, 56),
        ResourceItem(setOf(LanguageQualifier("in"), ), "${MD}values-in/strings.commonMain.cvr", 10, 56),
        ResourceItem(setOf(LanguageQualifier("it"), ), "${MD}values-it/strings.commonMain.cvr", 10, 107),
        ResourceItem(setOf(LanguageQualifier("nb"), ), "${MD}values-nb/strings.commonMain.cvr", 10, 69),
        ResourceItem(setOf(LanguageQualifier("pl"), ), "${MD}values-pl/strings.commonMain.cvr", 10, 120),
        ResourceItem(setOf(LanguageQualifier("pt"), ), "${MD}values-pt/strings.commonMain.cvr", 10, 73),
        ResourceItem(setOf(LanguageQualifier("tr"), ), "${MD}values-tr/strings.commonMain.cvr", 10, 81),
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 10, 73),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainPlurals0Resources(map: MutableMap<String, PluralStringResource>) {
  map.put("cw_airs_in_days", Res.plurals.cw_airs_in_days)
  map.put("cw_airs_in_days_short", Res.plurals.cw_airs_in_days_short)
}
