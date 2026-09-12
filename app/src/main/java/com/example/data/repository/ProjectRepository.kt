package com.example.data.repository

import android.content.Context
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val projectsFile = File(context.filesDir, "monoicon_projects.json")
    private val _projects = MutableStateFlow<List<IconProject>>(emptyList())
    val projects: StateFlow<List<IconProject>> = _projects.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        if (!projectsFile.exists()) {
            val initialList = createInitialDemoProjects()
            _projects.value = initialList
            saveToFile(initialList)
        } else {
            try {
                val jsonStr = projectsFile.readText()
                val jsonArray = JSONArray(jsonStr)
                val list = mutableListOf<IconProject>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(deserializeProject(obj))
                }
                if (list.isEmpty()) {
                    val initialList = createInitialDemoProjects()
                    _projects.value = initialList
                    saveToFile(initialList)
                } else {
                    _projects.value = list
                }
            } catch (e: Exception) {
                val initialList = createInitialDemoProjects()
                _projects.value = initialList
                saveToFile(initialList)
            }
        }
    }

    fun saveProject(project: IconProject) {
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        if (index != -1) {
            current[index] = project
        } else {
            current.add(0, project)
        }
        _projects.value = current
        saveToFile(current)
    }

    fun duplicateProject(project: IconProject): IconProject {
        val duplicated = project.copy(
            id = UUID.randomUUID().toString(),
            name = "${project.name} Copy",
            createdAt = System.currentTimeMillis()
        )
        val current = _projects.value.toMutableList()
        current.add(0, duplicated)
        _projects.value = current
        saveToFile(current)
        return duplicated
    }

    fun deleteProject(projectId: String) {
        val current = _projects.value.filter { it.id != projectId }
        _projects.value = current
        saveToFile(current)
    }

    fun toggleFavorite(projectId: String) {
        val current = _projects.value.map {
            if (it.id == projectId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _projects.value = current
        saveToFile(current)
    }

    private fun saveToFile(list: List<IconProject>) {
        try {
            val array = JSONArray()
            for (p in list) {
                array.put(serializeProject(p))
            }
            projectsFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeProject(p: IconProject): JSONObject {
        return JSONObject().apply {
            put("id", p.id)
            put("name", p.name)
            put("customImageUri", p.customImageUri ?: "")
            put("builtInArtwork", p.builtInArtwork.name)
            put("zoom", p.zoom.toDouble())
            put("rotation", p.rotation.toDouble())
            put("panX", p.panX.toDouble())
            put("panY", p.panY.toDouble())
            put("filter", p.filter.name)
            put("preset", p.preset?.name ?: "")
            put("contrast", p.contrast.toDouble())
            put("brightness", p.brightness.toDouble())
            put("exposure", p.exposure.toDouble())
            put("shadows", p.shadows.toDouble())
            put("highlights", p.highlights.toDouble())
            put("grain", p.grain.toDouble())
            put("noise", p.noise.toDouble())
            put("sharpen", p.sharpen.toDouble())
            put("blur", p.blur.toDouble())
            put("vignette", p.vignette.toDouble())
            put("threshold", p.threshold.toDouble())
            put("halftone", p.halftone.toDouble())
            put("shape", p.shape.name)
            put("cornerRadius", p.cornerRadius.toDouble())
            put("background", p.background.name)
            put("border", p.border.name)
            put("borderWidth", p.borderWidth.toDouble())
            put("symbol", p.symbol.name)
            put("symbolSize", p.symbolSize.toDouble())
            put("symbolPosition", p.symbolPosition.name)
            put("symbolOpacity", p.symbolOpacity.toDouble())
            put("isSymbolWhite", p.isSymbolWhite)
            put("symbolRotation", p.symbolRotation.toDouble())
            put("appText", p.appText)
            put("fontSize", p.fontSize.toDouble())
            put("isUppercase", p.isUppercase)
            put("isTextBold", p.isTextBold)
            put("letterSpacing", p.letterSpacing.toDouble())
            put("textOpacity", p.textOpacity.toDouble())
            put("createdAt", p.createdAt)
            put("isFavorite", p.isFavorite)
        }
    }

    private fun deserializeProject(obj: JSONObject): IconProject {
        val customUri = obj.optString("customImageUri", "").ifEmpty { null }
        val builtInArtwork = try {
            BuiltInArtwork.valueOf(obj.optString("builtInArtwork", BuiltInArtwork.BAT_SILHOUETTE.name))
        } catch (_: Exception) { BuiltInArtwork.BAT_SILHOUETTE }
        val filter = try {
            IconFilter.valueOf(obj.optString("filter", IconFilter.COMIC_INK.name))
        } catch (_: Exception) { IconFilter.COMIC_INK }
        val preset = try {
            val pName = obj.optString("preset", "")
            if (pName.isNotEmpty()) PresetStyle.valueOf(pName) else null
        } catch (_: Exception) { null }
        val shape = try {
            IconShape.valueOf(obj.optString("shape", IconShape.ROUNDED_SQUARE.name))
        } catch (_: Exception) { IconShape.ROUNDED_SQUARE }
        val background = try {
            IconBackground.valueOf(obj.optString("background", IconBackground.BLACK.name))
        } catch (_: Exception) { IconBackground.BLACK }
        val border = try {
            IconBorder.valueOf(obj.optString("border", IconBorder.COMIC_INK.name))
        } catch (_: Exception) { IconBorder.COMIC_INK }
        val symbol = try {
            AppSymbol.valueOf(obj.optString("symbol", AppSymbol.BAT.name))
        } catch (_: Exception) { AppSymbol.BAT }
        val symbolPos = try {
            SymbolPosition.valueOf(obj.optString("symbolPosition", SymbolPosition.CENTER.name))
        } catch (_: Exception) { SymbolPosition.CENTER }

        return IconProject(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name", "My App"),
            customImageUri = customUri,
            builtInArtwork = builtInArtwork,
            zoom = obj.optDouble("zoom", 1.0).toFloat(),
            rotation = obj.optDouble("rotation", 0.0).toFloat(),
            panX = obj.optDouble("panX", 0.0).toFloat(),
            panY = obj.optDouble("panY", 0.0).toFloat(),
            filter = filter,
            preset = preset,
            contrast = obj.optDouble("contrast", 35.0).toFloat(),
            brightness = obj.optDouble("brightness", 0.0).toFloat(),
            exposure = obj.optDouble("exposure", 0.0).toFloat(),
            shadows = obj.optDouble("shadows", 20.0).toFloat(),
            highlights = obj.optDouble("highlights", 25.0).toFloat(),
            grain = obj.optDouble("grain", 15.0).toFloat(),
            noise = obj.optDouble("noise", 0.0).toFloat(),
            sharpen = obj.optDouble("sharpen", 20.0).toFloat(),
            blur = obj.optDouble("blur", 0.0).toFloat(),
            vignette = obj.optDouble("vignette", 25.0).toFloat(),
            threshold = obj.optDouble("threshold", 50.0).toFloat(),
            halftone = obj.optDouble("halftone", 0.0).toFloat(),
            shape = shape,
            cornerRadius = obj.optDouble("cornerRadius", 24.0).toFloat(),
            background = background,
            border = border,
            borderWidth = obj.optDouble("borderWidth", 4.0).toFloat(),
            symbol = symbol,
            symbolSize = obj.optDouble("symbolSize", 55.0).toFloat(),
            symbolPosition = symbolPos,
            symbolOpacity = obj.optDouble("symbolOpacity", 1.0).toFloat(),
            isSymbolWhite = obj.optBoolean("isSymbolWhite", true),
            symbolRotation = obj.optDouble("symbolRotation", 0.0).toFloat(),
            appText = obj.optString("appText", ""),
            fontSize = obj.optDouble("fontSize", 14.0).toFloat(),
            isUppercase = obj.optBoolean("isUppercase", true),
            isTextBold = obj.optBoolean("isTextBold", true),
            letterSpacing = obj.optDouble("letterSpacing", 2.0).toFloat(),
            textOpacity = obj.optDouble("textOpacity", 0.9).toFloat(),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            isFavorite = obj.optBoolean("isFavorite", false)
        )
    }

    private fun createInitialDemoProjects(): List<IconProject> {
        return listOf(
            IconProject(
                name = "Dark Bat",
                builtInArtwork = BuiltInArtwork.BAT_SILHOUETTE,
                filter = IconFilter.DARK_COMIC,
                preset = PresetStyle.BATMAN_NOIR,
                symbol = AppSymbol.BAT,
                symbolSize = 75f,
                background = IconBackground.BLACK,
                border = IconBorder.COMIC_INK,
                borderWidth = 5f,
                vignette = 40f,
                contrast = 50f,
                appText = "BATMAN",
                isFavorite = true
            ),
            IconProject(
                name = "News Daily",
                builtInArtwork = BuiltInArtwork.NEWSPAPER_COLLAGE,
                filter = IconFilter.NEWSPAPER,
                preset = PresetStyle.NEWSPAPER,
                symbol = AppSymbol.NOTES,
                symbolSize = 48f,
                halftone = 45f,
                grain = 30f,
                background = IconBackground.NEWSPAPER_TEXTURE,
                border = IconBorder.DOUBLE,
                borderWidth = 4f,
                appText = "NEWS",
                isFavorite = true
            ),
            IconProject(
                name = "Comic Camera",
                builtInArtwork = BuiltInArtwork.COMIC_CAMERA,
                filter = IconFilter.COMIC_INK,
                preset = PresetStyle.COMIC_PANEL,
                symbol = AppSymbol.CAMERA,
                symbolSize = 65f,
                contrast = 40f,
                background = IconBackground.BLACK,
                border = IconBorder.THIN_WHITE,
                borderWidth = 3f,
                appText = "CAMERA"
            ),
            IconProject(
                name = "Vintage Clock",
                builtInArtwork = BuiltInArtwork.VINTAGE_CLOCK,
                filter = IconFilter.VINTAGE,
                preset = PresetStyle.VINTAGE_PAPER,
                symbol = AppSymbol.NONE,
                grain = 25f,
                background = IconBackground.DARK_GRAY,
                border = IconBorder.ROUNDED,
                borderWidth = 4f,
                appText = "CLOCK"
            ),
            IconProject(
                name = "Noir Music",
                builtInArtwork = BuiltInArtwork.NOIR_MUSIC,
                filter = IconFilter.PURE_BW,
                preset = PresetStyle.MINIMAL_BLACK,
                symbol = AppSymbol.MUSIC,
                symbolSize = 60f,
                background = IconBackground.BLACK,
                border = IconBorder.COMIC_INK,
                borderWidth = 4f,
                appText = "MUSIC"
            ),
            IconProject(
                name = "Dark Calendar",
                builtInArtwork = BuiltInArtwork.DARK_CALENDAR,
                filter = IconFilter.DARK_COMIC,
                preset = PresetStyle.DARK_HERO,
                symbol = AppSymbol.CALENDAR,
                symbolSize = 60f,
                background = IconBackground.BLACK,
                border = IconBorder.THIN_WHITE,
                borderWidth = 3f,
                appText = "CALENDAR"
            ),
            IconProject(
                name = "Game Controller",
                builtInArtwork = BuiltInArtwork.COMIC_GAME,
                filter = IconFilter.COMIC_INK,
                preset = PresetStyle.COMIC_PANEL,
                symbol = AppSymbol.GAME,
                symbolSize = 65f,
                background = IconBackground.BLACK,
                border = IconBorder.COMIC_INK,
                borderWidth = 4f,
                appText = "GAMES"
            ),
            IconProject(
                name = "Minimal Notes",
                builtInArtwork = BuiltInArtwork.MINIMAL_NOTES,
                filter = IconFilter.MINIMAL,
                preset = PresetStyle.MINIMAL_BLACK,
                symbol = AppSymbol.NOTES,
                symbolSize = 55f,
                background = IconBackground.BLACK,
                border = IconBorder.THIN_WHITE,
                borderWidth = 2f,
                appText = "NOTES"
            )
        )
    }
}
