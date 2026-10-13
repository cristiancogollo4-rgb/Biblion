package com.cristiancogollo.biblion.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "com.cristiancogollo.biblion"
private const val UI_TIMEOUT = 8_000L

internal fun MacrobenchmarkScope.openReader(device: UiDevice) {
    dismissTutorialIfPresent(device)

    val isGenesisOneOpen = device.hasObject(By.text("Genesis 1")) &&
        device.hasObject(By.desc("Opciones de lectura"))
    if (!isGenesisOneOpen) {
        if (device.hasObject(By.desc("Opciones de lectura"))) {
            device.pressBack()
            device.waitForIdle()
        }

        var genesis = device.findObject(By.text("Genesis"))
        if (genesis == null) {
            val openBible = device.wait(
                Until.findObject(By.text("Biblia")),
                UI_TIMEOUT,
            ) ?: device.wait(
                Until.findObject(By.text("Abrir Biblia")),
                1_000L,
            ) ?: device.wait(
                Until.findObject(By.text("Open Bible")),
                1_000L,
            )
            checkNotNull(openBible) { "No se encontró la navegación hacia la Biblia" }
            openBible.click()
            genesis = device.wait(Until.findObject(By.text("Genesis")), UI_TIMEOUT)
        }

        val genesisBook = checkNotNull(genesis) {
            "No se encontró Génesis en la pantalla de libros"
        }
        genesisBook.click()
    }

    check(
        device.wait(Until.hasObject(By.desc("Opciones de lectura")), UI_TIMEOUT)
    ) { "El lector no abrió Génesis 1" }
    check(
        device.wait(Until.hasObject(By.textContains("En el principio")), UI_TIMEOUT)
    ) { "Los versículos de Génesis 1 no terminaron de cargar" }
    device.waitForIdle()
}

internal fun UiDevice.scrollReaderDownAndUp() {
    val centerX = displayWidth / 2
    val top = (displayHeight * 0.25f).toInt()
    val bottom = (displayHeight * 0.78f).toInt()
    repeat(4) { swipe(centerX, bottom, centerX, top, 18) }
    repeat(4) { swipe(centerX, top, centerX, bottom, 18) }
    waitForIdle()
}

private fun dismissTutorialIfPresent(device: UiDevice) {
    val skip = device.wait(
        Until.findObject(By.textStartsWith("Omitir")),
        2_500L,
    ) ?: device.wait(
        Until.findObject(By.textStartsWith("Skip")),
        1_000L,
    )
    skip?.click()
    if (skip != null) device.waitForIdle()
}
