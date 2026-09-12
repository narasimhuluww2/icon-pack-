package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppSymbol
import com.example.data.model.IconFilter
import com.example.data.model.PresetStyle
import com.example.data.repository.ProjectRepository
import com.example.ui.viewmodel.IconEditorViewModel
import com.example.ui.viewmodel.ScreenState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MonoIcon Studio", appName)
  }

  @Test
  fun `repository loads initial demo projects`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = ProjectRepository(context)
    val projects = repository.projects.value
    assertTrue("Initial projects should not be empty", projects.isNotEmpty())
    assertEquals("Dark Bat", projects[0].name)
  }

  @Test
  fun `view model navigation and preset mutations work`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = IconEditorViewModel(app)

    assertEquals(ScreenState.DASHBOARD, viewModel.screenState.value)
    viewModel.startNewProject()
    assertEquals(ScreenState.EDITOR, viewModel.screenState.value)

    viewModel.applyPreset(PresetStyle.NEWSPAPER)
    assertEquals(IconFilter.NEWSPAPER, viewModel.currentProject.value.filter)
    assertTrue(viewModel.canUndo.value)

    viewModel.undo()
    assertTrue(viewModel.canRedo.value)
  }
}
