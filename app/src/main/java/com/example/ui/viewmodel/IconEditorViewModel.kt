package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ProjectRepository
import com.example.image.ImageProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class ScreenState {
    DASHBOARD,
    EDITOR,
    HOME_SCREEN_PREVIEW
}

class IconEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    val projects: StateFlow<List<IconProject>> = repository.projects

    private val _screenState = MutableStateFlow(ScreenState.DASHBOARD)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _currentProject = MutableStateFlow(IconProject())
    val currentProject: StateFlow<IconProject> = _currentProject.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    // Undo / Redo History
    private val undoStack = mutableListOf<IconProject>()
    private val redoStack = mutableListOf<IconProject>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Wallpaper configuration for Home Screen Preview
    val selectedWallpaper = MutableStateFlow(BuiltInArtwork.NEWSPAPER_COLLAGE)
    val wallpaperBlur = MutableStateFlow(0f)
    val wallpaperBrightness = MutableStateFlow(-15f)

    private var renderJob: Job? = null

    init {
        // Render initial preview whenever current project changes (debounced)
        viewModelScope.launch {
            _currentProject.collect { project ->
                scheduleRender(project)
            }
        }
    }

    fun navigateTo(screen: ScreenState) {
        _screenState.value = screen
    }

    fun startNewProject(builtInArtwork: BuiltInArtwork = BuiltInArtwork.BAT_SILHOUETTE) {
        undoStack.clear()
        redoStack.clear()
        updateHistoryState()

        _currentProject.value = IconProject(
            name = "Icon ${System.currentTimeMillis() % 1000}",
            builtInArtwork = builtInArtwork,
            symbol = AppSymbol.BAT,
            filter = IconFilter.COMIC_INK
        )
        _screenState.value = ScreenState.EDITOR
    }

    fun openProject(project: IconProject) {
        undoStack.clear()
        redoStack.clear()
        updateHistoryState()

        _currentProject.value = project
        _screenState.value = ScreenState.EDITOR
    }

    fun saveCurrentProject() {
        val proj = _currentProject.value
        repository.saveProject(proj)
        Toast.makeText(getApplication(), "Project '${proj.name}' saved!", Toast.LENGTH_SHORT).show()
    }

    fun duplicateProject(project: IconProject) {
        val dup = repository.duplicateProject(project)
        Toast.makeText(getApplication(), "Duplicated as '${dup.name}'", Toast.LENGTH_SHORT).show()
    }

    fun deleteProject(projectId: String) {
        repository.deleteProject(projectId)
        Toast.makeText(getApplication(), "Project deleted", Toast.LENGTH_SHORT).show()
    }

    fun toggleFavorite(projectId: String) {
        repository.toggleFavorite(projectId)
    }

    // --- State Mutations with History Tracking ---

    fun updateProject(transform: (IconProject) -> IconProject, addToHistory: Boolean = true) {
        val old = _currentProject.value
        val newProj = transform(old)
        if (old == newProj) return

        if (addToHistory) {
            undoStack.add(old)
            if (undoStack.size > 30) undoStack.removeAt(0)
            redoStack.clear()
            updateHistoryState()
        }

        _currentProject.value = newProj
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_currentProject.value)
            updateHistoryState()
            _currentProject.value = prev
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_currentProject.value)
            updateHistoryState()
            _currentProject.value = next
        }
    }

    private fun updateHistoryState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun resetEffects() {
        updateProject({
            it.copy(
                contrast = 35f,
                brightness = 0f,
                exposure = 0f,
                shadows = 20f,
                highlights = 25f,
                grain = 15f,
                noise = 0f,
                sharpen = 20f,
                blur = 0f,
                vignette = 25f,
                threshold = 50f,
                halftone = 0f
            )
        })
    }

    fun resetCrop() {
        updateProject({
            it.copy(zoom = 1.0f, rotation = 0f, panX = 0f, panY = 0f)
        })
    }

    fun fitCrop() {
        updateProject({
            it.copy(zoom = 0.85f, panX = 0f, panY = 0f)
        })
    }

    fun fillCrop() {
        updateProject({
            it.copy(zoom = 1.35f, panX = 0f, panY = 0f)
        })
    }

    fun applyPreset(preset: PresetStyle) {
        updateProject({
            when (preset) {
                PresetStyle.BATMAN_NOIR -> it.copy(
                    preset = preset,
                    filter = IconFilter.DARK_COMIC,
                    background = IconBackground.BLACK,
                    border = IconBorder.COMIC_INK,
                    borderWidth = 5f,
                    contrast = 60f,
                    vignette = 40f,
                    grain = 20f,
                    threshold = 50f,
                    isSymbolWhite = true
                )
                PresetStyle.NEWSPAPER -> it.copy(
                    preset = preset,
                    filter = IconFilter.NEWSPAPER,
                    background = IconBackground.NEWSPAPER_TEXTURE,
                    border = IconBorder.DOUBLE,
                    borderWidth = 4f,
                    contrast = 30f,
                    halftone = 45f,
                    grain = 35f,
                    threshold = 45f
                )
                PresetStyle.COMIC_PANEL -> it.copy(
                    preset = preset,
                    filter = IconFilter.COMIC_INK,
                    background = IconBackground.BLACK,
                    border = IconBorder.COMIC_INK,
                    borderWidth = 6f,
                    contrast = 50f,
                    vignette = 20f,
                    threshold = 55f
                )
                PresetStyle.MINIMAL_BLACK -> it.copy(
                    preset = preset,
                    filter = IconFilter.MINIMAL,
                    background = IconBackground.BLACK,
                    border = IconBorder.THIN_WHITE,
                    borderWidth = 2.5f,
                    contrast = 40f,
                    grain = 0f,
                    noise = 0f,
                    vignette = 0f,
                    halftone = 0f
                )
                PresetStyle.VINTAGE_PAPER -> it.copy(
                    preset = preset,
                    filter = IconFilter.VINTAGE,
                    background = IconBackground.LIGHT_GRAY,
                    border = IconBorder.ROUNDED,
                    borderWidth = 3f,
                    contrast = 25f,
                    grain = 30f,
                    isSymbolWhite = false
                )
                PresetStyle.DARK_HERO -> it.copy(
                    preset = preset,
                    filter = IconFilter.DARK_COMIC,
                    background = IconBackground.BLACK,
                    border = IconBorder.THIN_WHITE,
                    borderWidth = 3f,
                    contrast = 55f,
                    shadows = 40f,
                    highlights = 35f,
                    vignette = 35f,
                    grain = 15f
                )
            }
        })
    }

    private fun scheduleRender(project: IconProject) {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            delay(40) // Responsive debounce for silky smooth UI interactions
            _isRendering.value = true
            try {
                val bmp = ImageProcessor.renderIconBitmap(
                    context = getApplication(),
                    project = project,
                    targetSize = 512
                )
                _previewBitmap.value = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRendering.value = false
            }
        }
    }

    fun exportPng(
        context: Context,
        resolution: Int,
        transparentBg: Boolean,
        onComplete: (File?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val targetProject = if (transparentBg) {
                    _currentProject.value.copy(background = IconBackground.TRANSPARENT)
                } else {
                    _currentProject.value
                }
                val bmp = ImageProcessor.renderIconBitmap(context, targetProject, resolution)
                val safeName = targetProject.name.replace("\\s+".toRegex(), "_").lowercase()
                val file = ImageProcessor.exportToPngFile(context, bmp, "monoicon_${safeName}_${resolution}x${resolution}")
                onComplete(file)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }

    fun shareExportedFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share MonoIcon"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export saved to: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }
}
