package com.example.data.model

import java.util.UUID

enum class IconFilter(val displayName: String, val description: String) {
    PURE_BW("Pure B&W", "Deep blacks, stark whites, high contrast"),
    COMIC_INK("Comic Ink", "Heavy ink outlines & posterization"),
    NEWSPAPER("Newspaper", "Halftone dots & vintage newsprint"),
    DARK_COMIC("Dark Comic", "Dramatic shadows & white highlights"),
    SKETCH("Sketch", "Pencil & line-art contours"),
    MINIMAL("Minimal", "Clean high-contrast grayscale"),
    VINTAGE("Vintage", "Faded print & worn paper grain")
}

enum class IconShape(val displayName: String) {
    ROUNDED_SQUARE("Rounded Square"),
    SQUARE("Square"),
    CIRCLE("Circle"),
    SQUIRCLE("Squircle")
}

enum class IconBackground(val displayName: String) {
    BLACK("Black"),
    WHITE("White"),
    DARK_GRAY("Dark Gray"),
    LIGHT_GRAY("Light Gray"),
    TRANSPARENT("Transparent"),
    NEWSPAPER_TEXTURE("Newspaper Texture"),
    COMIC_TEXTURE("Comic Texture")
}

enum class IconBorder(val displayName: String) {
    NONE("No Border"),
    THIN_WHITE("Thin White"),
    THIN_BLACK("Thin Black"),
    DOUBLE("Double Border"),
    COMIC_INK("Comic Ink"),
    ROUNDED("Rounded Border")
}

enum class AppSymbol(val displayName: String) {
    NONE("None"),
    BAT("Bat Silhouette"),
    CAMERA("Camera"),
    CALENDAR("Calendar"),
    MAIL("Mail"),
    MUSIC("Music"),
    PHONE("Phone"),
    SETTINGS("Settings"),
    NOTES("Notes"),
    CALCULATOR("Calculator"),
    BROWSER("Browser"),
    GAME("Game Controller"),
    USER("User"),
    STAR("Star"),
    HEART("Heart"),
    CHAT("Chat"),
    CODE("Code / Terminal"),
    SHIELD("Shield"),
    BOLT("Lightning")
}

enum class SymbolPosition(val displayName: String) {
    CENTER("Center"),
    TOP_RIGHT("Top-Right"),
    BOTTOM_RIGHT("Bottom-Right"),
    BOTTOM_LEFT("Bottom-Left"),
    TOP_LEFT("Top-Left")
}

enum class PresetStyle(val displayName: String, val subtitle: String) {
    BATMAN_NOIR("Batman Noir", "Black background, stark white highlights, vignette"),
    NEWSPAPER("Newspaper", "Grayscale, halftone dots, vintage print grain"),
    COMIC_PANEL("Comic Panel", "Heavy black ink outlines & comic texture"),
    MINIMAL_BLACK("Minimal Black", "Pure black backdrop, crisp minimal icon"),
    VINTAGE_PAPER("Vintage Paper", "Faded newsprint tone, dark ink"),
    DARK_HERO("Dark Hero", "Dramatic shadows, brooding highlights")
}

enum class BuiltInArtwork(val displayName: String, val category: String) {
    DARK_HERO("Dark Hero", "Media"),
    BAT_SILHOUETTE("Bat Silhouette", "Entertainment"),
    NEWSPAPER_COLLAGE("Newspaper Print", "Custom"),
    COMIC_CAMERA("Comic Camera", "Media"),
    VINTAGE_CLOCK("Vintage Clock", "Productivity"),
    NOIR_MUSIC("Noir Music", "Media"),
    DARK_CALENDAR("Dark Calendar", "Productivity"),
    COMIC_GAME("Game Controller", "Entertainment"),
    MINIMAL_NOTES("Minimal Notes", "Productivity"),
    COMIC_BROWSER("Web Browser", "System"),
    PHONE_DIALER("Phone Call", "System"),
    SETTINGS_COG("Settings Cog", "System")
}

data class IconProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "My App",
    // Image source
    val customImageUri: String? = null,
    val builtInArtwork: BuiltInArtwork = BuiltInArtwork.BAT_SILHOUETTE,
    // Cropping & Transform
    val zoom: Float = 1.0f,
    val rotation: Float = 0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    // Filters & Presets
    val filter: IconFilter = IconFilter.COMIC_INK,
    val preset: PresetStyle? = PresetStyle.BATMAN_NOIR,
    // Effects sliders (-100 to 100 or 0 to 100)
    val contrast: Float = 35f,
    val brightness: Float = 0f,
    val exposure: Float = 0f,
    val shadows: Float = 20f,
    val highlights: Float = 25f,
    val grain: Float = 15f,
    val noise: Float = 0f,
    val sharpen: Float = 20f,
    val blur: Float = 0f,
    val vignette: Float = 25f,
    val threshold: Float = 50f,
    val halftone: Float = 0f,
    // Shape & Geometry
    val shape: IconShape = IconShape.ROUNDED_SQUARE,
    val cornerRadius: Float = 24f, // 0 to 50%
    // Background & Border
    val background: IconBackground = IconBackground.BLACK,
    val border: IconBorder = IconBorder.COMIC_INK,
    val borderWidth: Float = 4f, // 1 to 16 dp
    // Symbol overlay
    val symbol: AppSymbol = AppSymbol.BAT,
    val symbolSize: Float = 55f, // percentage
    val symbolPosition: SymbolPosition = SymbolPosition.CENTER,
    val symbolOpacity: Float = 1.0f,
    val isSymbolWhite: Boolean = true,
    val symbolRotation: Float = 0f,
    // Text overlay
    val appText: String = "",
    val fontSize: Float = 14f,
    val isUppercase: Boolean = true,
    val isTextBold: Boolean = true,
    val letterSpacing: Float = 2f,
    val textOpacity: Float = 0.9f,
    // Meta
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
