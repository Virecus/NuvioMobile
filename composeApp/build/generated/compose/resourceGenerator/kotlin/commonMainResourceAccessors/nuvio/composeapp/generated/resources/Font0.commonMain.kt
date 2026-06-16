@file:OptIn(InternalResourceApi::class)

package nuvio.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/nuvio.composeapp.generated.resources/"

@delegate:ResourceContentHash(632_319_142)
internal val Res.font.jetbrains_sans_bold: FontResource by lazy {
      FontResource("font:jetbrains_sans_bold", setOf(
        ResourceItem(setOf(), "${MD}font/jetbrains_sans_bold.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(1_474_104_359)
internal val Res.font.jetbrains_sans_regular: FontResource by lazy {
      FontResource("font:jetbrains_sans_regular", setOf(
        ResourceItem(setOf(), "${MD}font/jetbrains_sans_regular.ttf", -1, -1),
      ))
    }

@delegate:ResourceContentHash(1_099_343_131)
internal val Res.font.jetbrains_sans_semibold: FontResource by lazy {
      FontResource("font:jetbrains_sans_semibold", setOf(
        ResourceItem(setOf(), "${MD}font/jetbrains_sans_semibold.ttf", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainFont0Resources(map: MutableMap<String, FontResource>) {
  map.put("jetbrains_sans_bold", Res.font.jetbrains_sans_bold)
  map.put("jetbrains_sans_regular", Res.font.jetbrains_sans_regular)
  map.put("jetbrains_sans_semibold", Res.font.jetbrains_sans_semibold)
}
